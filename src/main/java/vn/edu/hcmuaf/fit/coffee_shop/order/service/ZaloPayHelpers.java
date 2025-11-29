package vn.edu.hcmuaf.fit.coffee_shop.order.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZaloPayHelpers {

    private static int transIdDefault = 1;

    /**
     * Tạo app_trans_id theo format của ZaloPay
     * Format: yyMMdd_hhmmss_000001
     */
    public static synchronized String getAppTransId() {
        if (transIdDefault >= 100000) {
            transIdDefault = 1;
        }
        transIdDefault += 1;

        SimpleDateFormat formatDateTime = new SimpleDateFormat("yyMMdd_HHmmss");
        String timeString = formatDateTime.format(new Date());

        return String.format("%s_%06d", timeString, transIdDefault);
    }

    /**
     * Tạo MAC signature cho ZaloPay
     * @param key - KEY1 hoặc KEY2 của ZaloPay
     * @param data - Chuỗi data cần mã hóa
     * @return MAC string (hex format)
     */
    public static String getMac(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        return HMacHexStringEncode("HmacSHA256", key, data);
    }

    /**
     * Encode HMac SHA256
     */
    private static String HMacHexStringEncode(String algorithm, String key, String data)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac sha256Hmac = Mac.getInstance(algorithm);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
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
     * Tạo data string theo format của ZaloPay
     */
    public static String createDataString(String appId, String appTransId, String appUser,
                                          long amount, long appTime, String embedData, String item) {
        return appId + "|" + appTransId + "|" + appUser + "|" + amount + "|"
                + appTime + "|" + embedData + "|" + item;
    }
}
