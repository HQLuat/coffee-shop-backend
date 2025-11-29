package vn.edu.hcmuaf.fit.coffee_shop.order.service;

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

    // ZaloPay Configuration (Sandbox)
    private static final String APP_ID = "2553";
    private static final String KEY1 = "PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL";
    private static final String KEY2 = "kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz";
    private static final String ZALOPAY_ENDPOINT = "https://sb-openapi.zalopay.vn/v2/create";

    // Counter cho transaction ID
    private static int transIdCounter = 1;

    public ZaloPayResponse createZaloPayOrder(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Generate app_trans_id với format đúng: yyMMdd_xxxxxx
        String appTransId = generateAppTransId();
        long appTime = System.currentTimeMillis();
        long amount = order.getTotalAmount().longValue();

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

        // Callback URL - thay đổi thành URL thực tế của bạn
        orderData.put("callback_url", "https://yourdomain.com/api/orders/zalopay/callback");

        // Generate MAC theo đúng format
        // Format: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
        String data = APP_ID + "|" + appTransId + "|" + order.getUser().getEmail() + "|"
                + amount + "|" + appTime + "|" + embedData + "|" + itemJson;

        String mac = generateMac(data, KEY1);
        orderData.put("mac", mac);

        System.out.println("📤 Sending request to ZaloPay:");
        System.out.println("app_trans_id: " + appTransId);
        System.out.println("Data string: " + data);
        System.out.println("MAC: " + mac);
        System.out.println("Full request: " + orderData);

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

    public boolean verifyCallback(Map<String, String> callbackData) {
        try {
            String receivedMac = callbackData.get("mac");
            String data = callbackData.get("data");

            String calculatedMac = generateMac(data, KEY2);

            return calculatedMac.equals(receivedMac);
        } catch (Exception e) {
            System.err.println("Error verifying callback: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate app_trans_id theo format chuẩn của ZaloPay: yyMMdd_xxxxxx
     * Trong đó xxxxxx là số tự tăng từ 000001 đến 999999
     */
    private synchronized String generateAppTransId() {
        // Reset counter nếu vượt quá 999999
        if (transIdCounter >= 1000000) {
            transIdCounter = 1;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());

        // Format: yyMMdd_xxxxxx (6 chữ số)
        String transId = String.format("%s_%06d", date, transIdCounter);
        transIdCounter++;

        return transId;
    }

    /**
     * Build item JSON theo format của ZaloPay
     * Format: [{"itemid":"","itemname":"","itemprice":0,"itemquantity":0}]
     */
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

    /**
     * Escape special characters trong JSON string
     */
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

    /**
     * Query payment status từ ZaloPay
     */
    public Map<String, Object> queryPaymentStatus(String appTransId) throws Exception {
        String queryEndpoint = "https://sb-openapi.zalopay.vn/v2/query";

        // Prepare data for MAC
        // Format: app_id|app_trans_id|key1
        String data = APP_ID + "|" + appTransId + "|" + KEY1;
        String mac = generateMac(data, KEY1);

        // Prepare request body
        Map<String, Object> queryData = new LinkedHashMap<>();
        queryData.put("app_id", Integer.parseInt(APP_ID));
        queryData.put("app_trans_id", appTransId);
        queryData.put("mac", mac);

        System.out.println("🔍 Querying payment status:");
        System.out.println("app_trans_id: " + appTransId);
        System.out.println("Data string: " + data);
        System.out.println("MAC: " + mac);

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
     * Generate MAC signature sử dụng HmacSHA256
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
}