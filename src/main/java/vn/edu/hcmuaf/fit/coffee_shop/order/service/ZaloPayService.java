package vn.edu.hcmuaf.fit.coffee_shop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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
    private static final String ZALOPAY_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/create";

    public ZaloPayResponse createZaloPayOrder(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Generate app_trans_id
        String appTransId = generateAppTransId();
        long appTime = System.currentTimeMillis();
        long amount = order.getTotalAmount().longValue();

        // Prepare order data
        Map<String, Object> orderData = new LinkedHashMap<>();
        orderData.put("app_id", APP_ID);
        orderData.put("app_trans_id", appTransId);
        orderData.put("app_user", order.getUser().getEmail());
        orderData.put("app_time", appTime);
        orderData.put("amount", amount);
        orderData.put("item", buildItemJson(order));
        orderData.put("description", "Thanh toán đơn hàng #" + order.getOrderCode());
        orderData.put("bank_code", "");
        orderData.put("callback_url", "https://localhost:8080/api/orders/zalopay/callback");

        // Generate MAC
        String data = APP_ID + "|" + appTransId + "|" + order.getUser().getEmail() + "|"
                + amount + "|" + appTime + "|"
                + "" + "|" + buildItemJson(order);
        String mac = generateMac(data, KEY1);
        orderData.put("mac", mac);

        System.out.println("📤 Sending request to ZaloPay:");
        System.out.println("Data: " + data);
        System.out.println("MAC: " + mac);

        try {
            // Gửi HTTP POST request đến ZaloPay
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ZALOPAY_ENDPOINT,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            System.out.println("📥 ZaloPay Response: " + responseBody);

            if (responseBody != null) {
                Integer returnCode = (Integer) responseBody.get("return_code");

                if (returnCode == 1) {
                    // Thành công
                    String orderUrl = (String) responseBody.get("order_url");

                    return ZaloPayResponse.builder()
                            .orderUrl(orderUrl)
                            .appTransId(appTransId)
                            .orderId(orderId)
                            .message("Tạo đơn thanh toán ZaloPay thành công")
                            .build();
                } else {
                    // Lỗi từ ZaloPay
                    String returnMessage = (String) responseBody.get("return_message");
                    throw new RuntimeException("ZaloPay error: " + returnMessage);
                }
            }

            throw new RuntimeException("Không nhận được response từ ZaloPay");

        } catch (Exception e) {
            System.err.println("❌ Error calling ZaloPay: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi kết nối ZaloPay: " + e.getMessage());
        }
    }

    public boolean verifyCallback(Map<String, String> callbackData) {
        try {
            String receivedMac = callbackData.get("mac");
            String data = callbackData.get("data");

            String calculatedMac = generateMac(data, KEY2);

            return calculatedMac.equals(receivedMac);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateAppTransId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());
        String random = String.format("%06d", new Random().nextInt(1000000));
        return date + "_" + random;
    }

    private String buildItemJson(Order order) {
        // Simple JSON format for items
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < order.getItems().size(); i++) {
            if (i > 0) items.append(",");
            var item = order.getItems().get(i);
            items.append("{\"name\":\"").append(item.getProductName()).append("\",")
                    .append("\"quantity\":").append(item.getQuantity()).append(",")
                    .append("\"price\":").append(item.getPrice().longValue()).append("}");
        }
        items.append("]");
        return items.toString();
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
}