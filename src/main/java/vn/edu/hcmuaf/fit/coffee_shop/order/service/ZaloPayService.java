package vn.edu.hcmuaf.fit.coffee_shop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.RefundResponse;
import vn.edu.hcmuaf.fit.coffee_shop.order.dto.ZaloPayResponse;
import vn.edu.hcmuaf.fit.coffee_shop.order.entity.Order;
import vn.edu.hcmuaf.fit.coffee_shop.order.repository.OrderRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ZaloPay Configuration (Sandbox)
    private static final String APP_ID = "2553";
    private static final String KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String KEY2 = "kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz";
    private static final String ZALOPAY_CREATE_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/create";
    private static final String ZALOPAY_REFUND_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/refund";
    private static final String ZALOPAY_QUERY_REFUND_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/query_refund";

    // Counter cho transaction ID
    private static int transIdCounter = 1;
    private static int refundIdCounter = 1;

    public ZaloPayResponse createZaloPayOrder(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Generate app_trans_id với format đúng: yyMMdd_xxxxxx
        String appTransId = generateAppTransId();
        long appTime = System.currentTimeMillis();
        long amount = order.getTotalAmount().longValue();

        // Lưu app_trans_id vào order
        order.setZaloPayTransId(appTransId);
        orderRepository.save(order);

        // Build embed_data (empty JSON object)
        String embedData = "{}";

        // Build item JSON
        String itemJson = buildItemJson(order);

        // Prepare order data theo đúng thứ tự của ZaloPay
        Map<String, Object> orderData = new LinkedHashMap<>();
        orderData.put("app_id", Integer.parseInt(APP_ID));
        orderData.put("app_trans_id", appTransId);
        orderData.put("app_user", order.getUser().getEmail());
        orderData.put("app_time", appTime);
        orderData.put("amount", amount);
        orderData.put("item", itemJson);
        orderData.put("embed_data", embedData);
        orderData.put("description", "Thanh toán đơn hàng #" + order.getOrderCode());
        orderData.put("bank_code", "");
        orderData.put("callback_url", "https://yourdomain.com/api/orders/zalopay/callback");

        // Generate MAC
        String data = APP_ID + "|" + appTransId + "|" + order.getUser().getEmail() + "|"
                + amount + "|" + appTime + "|" + embedData + "|" + itemJson;

        String mac = generateMac(data, KEY1);
        orderData.put("mac", mac);

        System.out.println("📤 Sending request to ZaloPay:");
        System.out.println("app_trans_id: " + appTransId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ZALOPAY_CREATE_ENDPOINT,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            System.out.println("📥 ZaloPay Response: " + responseBody);

            if (responseBody != null) {
                Integer returnCode = (Integer) responseBody.get("return_code");

                if (returnCode == 1) {
                    String orderUrl = (String) responseBody.get("order_url");

                    return ZaloPayResponse.builder()
                            .orderUrl(orderUrl)
                            .appTransId(appTransId)
                            .orderId(orderId)
                            .message("Tạo đơn thanh toán ZaloPay thành công")
                            .build();
                } else {
                    String returnMessage = (String) responseBody.get("return_message");
                    Integer subReturnCode = (Integer) responseBody.get("sub_return_code");
                    throw new RuntimeException("ZaloPay error [" + returnCode + "/" + subReturnCode + "]: " + returnMessage);
                }
            }

            throw new RuntimeException("Không nhận được response từ ZaloPay");

        } catch (Exception e) {
            System.err.println("❌ Error calling ZaloPay: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi kết nối ZaloPay: " + e.getMessage());
        }
    }

    /**
     * Refund (hoàn tiền) cho đơn hàng
     *
     * @param orderId ID của đơn hàng cần hoàn tiền
     * @param amount Số tiền hoàn (có thể hoàn một phần hoặc toàn bộ)
     * @param description Lý do hoàn tiền
     * @return RefundResponse
     */
    public RefundResponse refundOrder(Long orderId, Long amount, String description) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Kiểm tra xem đơn hàng đã thanh toán qua ZaloPay chưa
        if (order.getZaloPayTransId() == null) {
            throw new RuntimeException("Đơn hàng này không được thanh toán qua ZaloPay");
        }

        // Kiểm tra số tiền hoàn không vượt quá số tiền đơn hàng
        if (amount > order.getTotalAmount().longValue()) {
            throw new RuntimeException("Số tiền hoàn không được vượt quá tổng tiền đơn hàng");
        }

        // Generate m_refund_id theo format: yyMMdd_APP_ID_xxxxxx
        String mRefundId = generateMRefundId();
        long timestamp = System.currentTimeMillis();

        // Prepare refund data
        Map<String, Object> refundData = new LinkedHashMap<>();
        refundData.put("app_id", Integer.parseInt(APP_ID));
        refundData.put("zp_trans_id", order.getZaloPayZpTransId() != null ? order.getZaloPayZpTransId() : "");
        refundData.put("m_refund_id", mRefundId);
        refundData.put("timestamp", timestamp);
        refundData.put("amount", amount);
        refundData.put("description", description != null ? description : "Hoàn tiền đơn hàng #" + order.getOrderCode());

        // Generate MAC for refund
        // Format: app_id|zp_trans_id|amount|description|timestamp
        String data = APP_ID + "|" +
                (order.getZaloPayZpTransId() != null ? order.getZaloPayZpTransId() : "") + "|" +
                amount + "|" +
                refundData.get("description") + "|" +
                timestamp;

        String mac = generateMac(data, KEY1);
        refundData.put("mac", mac);

        System.out.println("💰 Sending refund request to ZaloPay:");
        System.out.println("m_refund_id: " + mRefundId);
        System.out.println("zp_trans_id: " + order.getZaloPayZpTransId());
        System.out.println("amount: " + amount);
        System.out.println("Data string: " + data);
        System.out.println("MAC: " + mac);

        try {
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

                RefundResponse.RefundResponseBuilder builder = RefundResponse.builder()
                        .refundId(mRefundId)
                        .orderId(orderId)
                        .refundAmount(java.math.BigDecimal.valueOf(amount))
                        .returnCode(returnCode)
                        .returnMessage(returnMessage);

                if (returnCode == 1) {
                    // Refund thành công
                    builder.message("Hoàn tiền thành công");
                } else if (returnCode == 2) {
                    // Refund đang được xử lý
                    builder.message("Yêu cầu hoàn tiền đang được xử lý");
                } else {
                    // Refund thất bại
                    builder.message("Hoàn tiền thất bại: " + returnMessage);
                }

                return builder.build();
            }

            throw new RuntimeException("Không nhận được response từ ZaloPay");

        } catch (Exception e) {
            System.err.println("❌ Error calling ZaloPay refund: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi gọi API hoàn tiền ZaloPay: " + e.getMessage());
        }
    }

    /**
     * Query refund status (Kiểm tra trạng thái hoàn tiền)
     */
    public Map<String, Object> queryRefundStatus(String mRefundId) throws Exception {
        long timestamp = System.currentTimeMillis();

        // Prepare query data
        Map<String, Object> queryData = new LinkedHashMap<>();
        queryData.put("app_id", Integer.parseInt(APP_ID));
        queryData.put("m_refund_id", mRefundId);
        queryData.put("timestamp", timestamp);

        // Generate MAC
        // Format: app_id|m_refund_id|timestamp
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

            return responseBody;

        } catch (Exception e) {
            System.err.println("❌ Error querying refund status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi truy vấn trạng thái hoàn tiền: " + e.getMessage());
        }
    }

    public boolean verifyCallback(Map<String, String> callbackData) {
        try {
            String receivedMac = callbackData.get("mac");
            String dataStr = callbackData.get("data");

            String calculatedMac = generateMac(dataStr, KEY2);

            if (calculatedMac.equals(receivedMac)) {
                // Parse data để lấy zp_trans_id và cập nhật vào order
                Map<String, Object> data = objectMapper.readValue(dataStr, Map.class);
                String appTransId = (String) data.get("app_trans_id");
                String zpTransId = String.valueOf(data.get("zp_trans_id"));

                // Cập nhật zp_trans_id vào order
                orderRepository.findByOrderCode(appTransId).ifPresent(order -> {
                    order.setZaloPayZpTransId(zpTransId);
                    orderRepository.save(order);
                });

                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error verifying callback: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> queryPaymentStatus(String appTransId) throws Exception {
        String queryEndpoint = "https://sb-openapi.zalopay.vn/v2/query";

        String data = APP_ID + "|" + appTransId + "|" + KEY1;
        String mac = generateMac(data, KEY1);

        Map<String, Object> queryData = new LinkedHashMap<>();
        queryData.put("app_id", Integer.parseInt(APP_ID));
        queryData.put("app_trans_id", appTransId);
        queryData.put("mac", mac);

        System.out.println("🔍 Querying payment status:");
        System.out.println("app_trans_id: " + appTransId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(queryData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    queryEndpoint,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            System.out.println("📥 Query Response: " + responseBody);

            return responseBody;

        } catch (Exception e) {
            System.err.println("❌ Error querying payment status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi truy vấn trạng thái thanh toán: " + e.getMessage());
        }
    }

    /**
     * Generate app_trans_id theo format: yyMMdd_xxxxxx
     */
    private synchronized String generateAppTransId() {
        if (transIdCounter >= 1000000) {
            transIdCounter = 1;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());
        String transId = String.format("%s_%06d", date, transIdCounter);
        transIdCounter++;

        return transId;
    }

    /**
     * Generate m_refund_id theo format: yyMMdd_APP_ID_xxxxxx
     */
    private synchronized String generateMRefundId() {
        if (refundIdCounter >= 1000000) {
            refundIdCounter = 1;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());
        String refundId = String.format("%s_%s_%06d", date, APP_ID, refundIdCounter);
        refundIdCounter++;

        return refundId;
    }

    private String buildItemJson(Order order) {
        StringBuilder items = new StringBuilder("[");

        for (int i = 0; i < order.getItems().size(); i++) {
            if (i > 0) {
                items.append(",");
            }

            var item = order.getItems().get(i);
            items.append("{")
                    .append("\"itemid\":\"").append(item.getProductId()).append("\",")
                    .append("\"itemname\":\"").append(escapeJson(item.getProductName())).append("\",")
                    .append("\"itemprice\":").append(item.getPrice().longValue()).append(",")
                    .append("\"itemquantity\":").append(item.getQuantity())
                    .append("}");
        }

        items.append("]");
        return items.toString();
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

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
}