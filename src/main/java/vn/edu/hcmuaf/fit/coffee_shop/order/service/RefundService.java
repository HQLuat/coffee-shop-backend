package vn.edu.hcmuaf.fit.coffee_shop.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final OrderRepository orderRepository;
    private final RefundTransactionRepository refundRepository;
    private final RestTemplate restTemplate;

    private static final String APP_ID = "2553";
    private static final String KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String ZALOPAY_REFUND_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/refund";
    private static final String ZALOPAY_QUERY_REFUND_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/query_refund";
    private static final int MAX_DESCRIPTION_LENGTH = 100;

    @Transactional
    public RefundResponse createRefund(Long orderId, Long amount, String description) throws Exception {
        log.info("Starting refund process for order #{}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        validateRefundable(order, amount);

        String mRefundId = generateMRefundId();
        long timestamp = System.currentTimeMillis();
        String refundDescription = normalizeDescription(description, order.getOrderCode());

        RefundTransaction refund = createRefundTransaction(order, mRefundId, amount, refundDescription);

        try {
            Map<String, Object> responseBody = callZaloPayRefundAPI(
                    order, mRefundId, amount, refundDescription, timestamp
            );

            Integer returnCode = (Integer) responseBody.get("return_code");
            String returnMessage = (String) responseBody.get("return_message");

            log.info("ZaloPay response: return_code={}, message={}", returnCode, returnMessage);

            refund.setReturnCode(returnCode);
            refund.setReturnMessage(returnMessage);

            if (returnCode == 1) {
                // return_code = 1: SUCCESS
                log.info("Refund SUCCESS");
                refund.setStatus(OrderStatus.REFUNDED);
                refund.setProcessedAt(LocalDateTime.now());
                refundRepository.saveAndFlush(refund);

                order.setStatus(OrderStatus.REFUNDED);
                orderRepository.saveAndFlush(order);

            } else if (returnCode == 2) {
                log.error("Refund FAILED: {}", returnMessage);
                refund.setStatus(OrderStatus.REFUND_FAILED);
                refundRepository.saveAndFlush(refund);

            } else if (returnCode == 3) {
                log.info("Refund PROCESSING - will verify later");
                refund.setStatus(OrderStatus.REFUND_PROCESSING);
                refundRepository.saveAndFlush(refund);

            } else {
                log.error("Unknown return_code: {}, message: {}", returnCode, returnMessage);
                refund.setStatus(OrderStatus.REFUND_FAILED);
                refundRepository.saveAndFlush(refund);
            }

            return buildRefundResponse(refund, returnCode, returnMessage);

        } catch (Exception e) {
            log.error("Error calling ZaloPay refund API", e);
            refund.setStatus(OrderStatus.REFUND_FAILED);
            refund.setReturnMessage("Error: " + e.getMessage());
            refundRepository.save(refund);
            throw new RuntimeException("Lỗi khi gọi API hoàn tiền ZaloPay: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void verifyProcessingRefunds() {
        List<RefundTransaction> processingRefunds = refundRepository
                .findAll()
                .stream()
                .filter(r -> r.getStatus() == OrderStatus.REFUND_PROCESSING)
                .toList();

        if (processingRefunds.isEmpty()) {
            return;
        }

        log.info("Found {} PROCESSING refunds to verify", processingRefunds.size());

        for (RefundTransaction refund : processingRefunds) {
            try {
                log.info("Verifying refund {}", refund.getRefundId());

                Map<String, Object> status = queryRefundStatusInternal(refund.getRefundId());
                Integer returnCode = (Integer) status.get("return_code");

                if (returnCode == 1) {
                    log.info("Refund {} is now SUCCESS", refund.getRefundId());
                    refund.setStatus(OrderStatus.REFUNDED);
                    refund.setProcessedAt(LocalDateTime.now());
                    refundRepository.saveAndFlush(refund);

                    Order order = refund.getOrder();
                    order.setStatus(OrderStatus.REFUNDED);
                    orderRepository.saveAndFlush(order);

                    log.info("Order #{} updated to REFUNDED", order.getId());

                } else if (returnCode == 2) {

                    log.error("Refund {} FAILED", refund.getRefundId());
                    refund.setStatus(OrderStatus.REFUND_FAILED);
                    refundRepository.saveAndFlush(refund);
                }
                // return_code = 3: Still PROCESSING, check again later

            } catch (Exception e) {
                log.error("Error verifying refund {}", refund.getRefundId(), e);
            }
        }
    }

    @Transactional
    public Map<String, Object> queryRefundStatus(String mRefundId) throws Exception {
        RefundTransaction refund = refundRepository.findByRefundId(mRefundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch hoàn tiền"));

        Map<String, Object> response = queryRefundStatusInternal(mRefundId);

        // Update local status if needed
        Integer returnCode = (Integer) response.get("return_code");
        if (returnCode == 1 && refund.getStatus() != OrderStatus.REFUNDED) {
            refund.setStatus(OrderStatus.REFUNDED);
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.saveAndFlush(refund);

            Order order = refund.getOrder();
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.saveAndFlush(order);

        }  else if (returnCode == 2 && refund.getStatus() != OrderStatus.REFUND_FAILED) {
            refund.setStatus(OrderStatus.REFUND_FAILED);
            refundRepository.saveAndFlush(refund);
        }

        response.put("localStatus", refund.getStatus().name());
        response.put("localStatusDisplay", refund.getStatus().getDisplayName());

        return response;
    }

    private RefundTransaction createRefundTransaction(Order order, String mRefundId,
                                                      Long amount, String description) {
        RefundTransaction refund = RefundTransaction.builder()
                .order(order)
                .refundId(mRefundId)
                .refundAmount(BigDecimal.valueOf(amount))
                .description(description)
                .status(OrderStatus.REFUND_PENDING)
                .build();
        return refundRepository.save(refund);
    }

    private Map<String, Object> callZaloPayRefundAPI(Order order, String mRefundId,
                                                     Long amount, String description, long timestamp) throws Exception {

        // Generate MAC
        String macData = APP_ID + "|" + order.getZaloPayZpTransId() + "|" +
                amount + "|" + description + "|" + timestamp;
        String mac = generateMac(macData, KEY1);

        // Prepare form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("app_id", APP_ID);
        formData.add("zp_trans_id", order.getZaloPayZpTransId());
        formData.add("amount", String.valueOf(amount));
        formData.add("description", description);
        formData.add("timestamp", String.valueOf(timestamp));
        formData.add("m_refund_id", mRefundId);
        formData.add("mac", mac);

        // Call API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        log.info("Sending refund request to ZaloPay");
        ResponseEntity<Map> response = restTemplate.postForEntity(
                ZALOPAY_REFUND_ENDPOINT, request, Map.class);

        return response.getBody();
    }

    private Map<String, Object> queryRefundStatusInternal(String mRefundId) throws Exception {
        long timestamp = System.currentTimeMillis();
        String macData = APP_ID + "|" + mRefundId + "|" + timestamp;
        String mac = generateMac(macData, KEY1);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("app_id", APP_ID);
        formData.add("m_refund_id", mRefundId);
        formData.add("timestamp", String.valueOf(timestamp));
        formData.add("mac", mac);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                ZALOPAY_QUERY_REFUND_ENDPOINT, request, Map.class);

        return response.getBody();
    }

    private void validateRefundable(Order order, Long amount) {
        if (!order.getStatus().canRefund()) {
            throw new RuntimeException("Đơn hàng không thể hoàn tiền ở trạng thái: " + order.getStatus().getDisplayName());
        }

        if (order.getZaloPayZpTransId() == null || order.getZaloPayZpTransId().trim().isEmpty()) {
            throw new RuntimeException("Đơn hàng chưa được thanh toán thành công");
        }
        if (order.getZaloPayZpTransId().length() > 15) {
            throw new RuntimeException("zp_trans_id không hợp lệ");
        }
        if (amount <= 0 || amount > order.getTotalAmount().longValue()) {
            throw new RuntimeException("Số tiền hoàn không hợp lệ");
        }
    }

    private String normalizeDescription(String description, String orderCode) {
        String desc = description != null ? description.trim() : "Refund order #" + orderCode;
        desc = removeVietnameseAccents(desc);
        return desc.length() > MAX_DESCRIPTION_LENGTH
                ? desc.substring(0, MAX_DESCRIPTION_LENGTH)
                : desc;
    }

    private String removeVietnameseAccents(String str) {
        if (str == null) return "";
        return str.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("đ", "d")
                .replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A")
                .replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E")
                .replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O")
                .replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U")
                .replaceAll("Đ", "D");
    }

    private String generateMRefundId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String datePart = sdf.format(new Date());
        String randomPart = String.valueOf(new Random().nextInt(1_000_000_000));
        return datePart + "_" + APP_ID + "_" + randomPart;
    }

    private String generateMac(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private RefundResponse buildRefundResponse(RefundTransaction refund,
                                               Integer returnCode, String returnMessage) {
        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .orderId(refund.getOrder().getId())
                .refundAmount(refund.getRefundAmount())
                .returnCode(returnCode)
                .returnMessage(returnMessage)
                .message(getRefundMessage(returnCode, returnMessage))
                .build();
    }

    private String getRefundMessage(Integer returnCode, String returnMessage) {
        return switch (returnCode) {
            case 1 -> "Hoàn tiền thành công";
            case 2 -> "Hoàn tiền thất bại";
            case 3 -> "Đang xử lý hoàn tiền";
            default -> "Hoàn tiền thất bại: " + returnMessage;
        };
    }

    public List<RefundTransaction> getRefundHistory(Long orderId) {
        return refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }
}