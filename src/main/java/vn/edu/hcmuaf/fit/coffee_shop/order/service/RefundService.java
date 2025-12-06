package vn.edu.hcmuaf.fit.coffee_shop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.RefundResponse;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.*;
import vn.edu.hcmuaf.fit.coffee_shop.order.repository.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final OrderRepository orderRepository;
    private final RefundTransactionRepository refundRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ZaloPay Configuration
    private static final String APP_ID = "2553";
    private static final String KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String ZALOPAY_REFUND_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/refund";
    private static final String ZALOPAY_QUERY_REFUND_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/query_refund";

    /**
     * Tạo yêu cầu hoàn tiền - FIXED
     */
    @Transactional
    public RefundResponse createRefund(Long orderId, Long amount, String description) throws Exception {
        // 1. Kiểm tra order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // 2. Validate order có thể refund
        validateRefundable(order, amount);

        // 3. Generate refund ID
        String mRefundId = generateMRefundId();
        long timestamp = System.currentTimeMillis();

        // FIX: Lấy zp_trans_id (QUAN TRỌNG!)
        String zpTransId = order.getZaloPayZpTransId();

        System.out.println("💰 Creating refund request:");
        System.out.println("  Order ID: " + orderId);
        System.out.println("  Order Code: " + order.getOrderCode());
        System.out.println("  zp_trans_id: " + zpTransId);
        System.out.println("  m_refund_id: " + mRefundId);
        System.out.println("  amount: " + amount);

        // 4. Tạo refund transaction trong DB trước
        RefundTransaction refundTransaction = RefundTransaction.builder()
                .order(order)
                .refundId(mRefundId)
                .refundAmount(BigDecimal.valueOf(amount))
                .description(description != null ? description : "Hoàn tiền đơn hàng #" + order.getOrderCode())
                .status(RefundStatus.PENDING)
                .build();
        refundRepository.save(refundTransaction);

        // 5. Prepare refund data cho ZaloPay
        Map<String, Object> refundData = new LinkedHashMap<>();
        refundData.put("app_id", Integer.parseInt(APP_ID));
        refundData.put("zp_trans_id", zpTransId); // FIX: Đảm bảo có giá trị
        refundData.put("amount", amount);
        refundData.put("description", refundTransaction.getDescription());
        refundData.put("timestamp", timestamp);
        refundData.put("m_refund_id", mRefundId);

        // 6. FIX: Generate MAC theo đúng format ZaloPay yêu cầu
        // Format: app_id|zp_trans_id|amount|description|timestamp
        String dataForMac = APP_ID + "|" + zpTransId + "|" + amount + "|" +
                refundTransaction.getDescription() + "|" + timestamp;

        System.out.println("📝 Data for MAC: " + dataForMac);

        String mac = generateMac(dataForMac, KEY1);
        refundData.put("mac", mac);

        System.out.println("📤 Refund request data: " + refundData);

        try {
            // 7. Call ZaloPay API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(refundData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ZALOPAY_REFUND_ENDPOINT,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            System.out.println("📥 Refund Response: " + responseBody);

            if (responseBody != null) {
                Integer returnCode = (Integer) responseBody.get("return_code");
                String returnMessage = (String) responseBody.get("return_message");

                // 8. Update refund transaction
                refundTransaction.setReturnCode(returnCode);
                refundTransaction.setReturnMessage(returnMessage);

                if (returnCode == 1) {
                    // Refund thành công
                    refundTransaction.setStatus(RefundStatus.SUCCESS);
                    refundTransaction.setProcessedAt(LocalDateTime.now());

                    // Update order status
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    System.out.println("✅ Refund SUCCESS");

                } else if (returnCode == 2) {
                    // Refund đang xử lý
                    refundTransaction.setStatus(RefundStatus.PROCESSING);
                    System.out.println("⏳ Refund PROCESSING");
                } else {
                    // Refund thất bại
                    refundTransaction.setStatus(RefundStatus.FAILED);
                    System.err.println("❌ Refund FAILED: " + returnMessage);
                }

                refundRepository.save(refundTransaction);

                // 9. Build response
                return RefundResponse.builder()
                        .refundId(mRefundId)
                        .orderId(orderId)
                        .refundAmount(BigDecimal.valueOf(amount))
                        .returnCode(returnCode)
                        .returnMessage(returnMessage)
                        .message(getRefundMessage(returnCode, returnMessage))
                        .build();
            }

            throw new RuntimeException("Không nhận được response từ ZaloPay");

        } catch (Exception e) {
            // Update status failed
            refundTransaction.setStatus(RefundStatus.FAILED);
            refundTransaction.setReturnMessage("Error: " + e.getMessage());
            refundRepository.save(refundTransaction);

            System.err.println("❌ Error calling ZaloPay refund: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi gọi API hoàn tiền ZaloPay: " + e.getMessage());
        }
    }

    /**
     * Query trạng thái hoàn tiền
     */
    @Transactional
    public Map<String, Object> queryRefundStatus(String mRefundId) throws Exception {
        // 1. Tìm refund transaction trong DB
        RefundTransaction refundTransaction = refundRepository.findByRefundId(mRefundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch hoàn tiền"));

        long timestamp = System.currentTimeMillis();

        // 2. Prepare query data
        Map<String, Object> queryData = new LinkedHashMap<>();
        queryData.put("app_id", Integer.parseInt(APP_ID));
        queryData.put("m_refund_id", mRefundId);
        queryData.put("timestamp", timestamp);

        // 3. Generate MAC: app_id|m_refund_id|timestamp
        String data = APP_ID + "|" + mRefundId + "|" + timestamp;
        String mac = generateMac(data, KEY1);
        queryData.put("mac", mac);

        System.out.println("🔍 Querying refund status:");
        System.out.println("m_refund_id: " + mRefundId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(queryData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ZALOPAY_QUERY_REFUND_ENDPOINT,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            System.out.println("📥 Query Refund Response: " + responseBody);

            if (responseBody != null) {
                Integer returnCode = (Integer) responseBody.get("return_code");

                // Update status trong DB dựa trên response
                if (returnCode == 1 && refundTransaction.getStatus() != RefundStatus.SUCCESS) {
                    refundTransaction.setStatus(RefundStatus.SUCCESS);
                    refundTransaction.setProcessedAt(LocalDateTime.now());
                    refundRepository.save(refundTransaction);
                } else if (returnCode == 2 && refundTransaction.getStatus() == RefundStatus.PENDING) {
                    refundTransaction.setStatus(RefundStatus.PROCESSING);
                    refundRepository.save(refundTransaction);
                } else if (returnCode == 3) {
                    refundTransaction.setStatus(RefundStatus.FAILED);
                    refundRepository.save(refundTransaction);
                }

                // Thêm thông tin từ DB vào response
                responseBody.put("localStatus", refundTransaction.getStatus().name());
                responseBody.put("localStatusDisplay", refundTransaction.getStatus().getDisplayName());
            }

            return responseBody;

        } catch (Exception e) {
            System.err.println("❌ Error querying refund status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi truy vấn trạng thái hoàn tiền: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử refund của đơn hàng
     */
    public List<RefundTransaction> getRefundHistory(Long orderId) {
        return refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * Validate order có thể refund không
     */
    private void validateRefundable(Order order, Long amount) {
        System.out.println("🔍 Validating refund for order: " + order.getId());

        // Kiểm tra đã thanh toán qua ZaloPay chưa
        if (order.getZaloPayTransId() == null) {
            throw new RuntimeException("Đơn hàng này không được thanh toán qua ZaloPay");
        }

        // FIX: Kiểm tra có zp_trans_id chưa (đã thanh toán thành công)
        if (order.getZaloPayZpTransId() == null || order.getZaloPayZpTransId().isEmpty()) {
            throw new RuntimeException("Đơn hàng chưa có zp_trans_id. Vui lòng verify payment trước khi refund!");
        }

        System.out.println("✅ Order has zp_trans_id: " + order.getZaloPayZpTransId());

        // Kiểm tra số tiền hoàn
        if (amount <= 0) {
            throw new RuntimeException("Số tiền hoàn phải lớn hơn 0");
        }

        if (amount > order.getTotalAmount().longValue()) {
            throw new RuntimeException("Số tiền hoàn không được vượt quá tổng tiền đơn hàng");
        }

        // Kiểm tra đã refund chưa
        List<RefundTransaction> successfulRefunds = refundRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .filter(r -> r.getStatus() == RefundStatus.SUCCESS)
                .toList();

        long totalRefunded = successfulRefunds.stream()
                .mapToLong(r -> r.getRefundAmount().longValue())
                .sum();

        if (totalRefunded + amount > order.getTotalAmount().longValue()) {
            throw new RuntimeException("Tổng số tiền hoàn vượt quá tổng tiền đơn hàng");
        }
    }

    /**
     * Generate m_refund_id với timestamp để tránh trùng
     */
    private String generateMRefundId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmssSSS");
        String timestamp = sdf.format(new Date());
        return timestamp + "_" + APP_ID;
    }

    /**
     * Generate MAC
     */
    private String generateMac(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);

        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Get message dựa trên return code
     */
    private String getRefundMessage(Integer returnCode, String returnMessage) {
        return switch (returnCode) {
            case 1 -> "Hoàn tiền thành công";
            case 2 -> "Yêu cầu hoàn tiền đang được xử lý";
            case 3 -> "Giao dịch hoàn tiền thất bại";
            default -> "Hoàn tiền thất bại: " + returnMessage;
        };
    }
}