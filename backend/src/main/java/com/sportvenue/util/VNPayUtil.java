package com.sportvenue.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tiện ích ký và xác thực checksum VNPay (HMAC-SHA512).
 * <p>
 * Quy ước của VNPay sandbox/production:
 * <ul>
 *   <li>Sắp xếp tham số theo thứ tự alphabet của key (A → Z)</li>
 *   <li>URL-encode cả key lẫn value (US-ASCII, '+' được thay bằng '%20')</li>
 *   <li>Nối các cặp key=value bằng '&amp;'</li>
 *   <li>Ký chuỗi trên bằng HMAC-SHA512 với hash-secret, hex-encode kết quả</li>
 *   <li>Khi return: rebuild signing string KHÔNG bao gồm vnp_SecureHash, so sánh lại</li>
 * </ul>
 */
public final class VNPayUtil {

    public static final String SECURE_HASH_TYPE = "HmacSHA512";
    public static final String SECURE_HASH_FIELD = "vnp_SecureHash";

    private VNPayUtil() {
    }

    /**
     * Tính HMAC-SHA512 hex-encoded.
     */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance(SECURE_HASH_TYPE);
            SecretKeySpec keySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), SECURE_HASH_TYPE);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * rawHmac.length);
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tính HMAC-SHA512 cho VNPay", ex);
        }
    }

    /**
     * URL-encode theo chuẩn VNPay (US-ASCII, dấu '+' được chuyển thành '%20').
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.name())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Sắp xếp map theo key (alphabet) rồi nối thành chuỗi query thuần để ký.
     * Trả về {@code ?key1=val1&key2=val2} (KHÔNG bao gồm vnp_SecureHash).
     */
    public static String buildSigningString(Map<String, String> params) {
        List<String> sortedKeys = params.keySet().stream()
                .filter(k -> k != null && !k.isEmpty())
                .filter(k -> !SECURE_HASH_FIELD.equals(k))
                .sorted()
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);
            String value = params.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            sb.append(urlEncode(key))
              .append('=')
              .append(urlEncode(value));
            if (i < sortedKeys.size() - 1) {
                sb.append('&');
            }
        }
        return sb.toString();
    }

    /**
     * Sinh query string đã ký: {@code ?key=val&...&vnp_SecureHash=<hex>}.
     * Phù hợp để gắn ngay sau {@code vnPayConfig.getUrl()}.
     */
    public static String buildSignedQuery(Map<String, String> params, String secret) {
        String signing = buildSigningString(params);
        String hash = hmacSHA512(secret, signing);
        if (signing.isEmpty()) {
            return "?vnp_SecureHash=" + hash;
        }
        return "?" + signing + "&" + SECURE_HASH_FIELD + "=" + hash;
    }

    /**
     * Trích các tham số vnp_* từ HttpServletRequest.getParameterMap().
     */
    public static Map<String, String> extractVnpParams(Map<String, String[]> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, String[]> e : raw.entrySet()) {
            String key = e.getKey();
            if (key != null && key.startsWith("vnp_") && e.getValue() != null && e.getValue().length > 0) {
                out.put(key, e.getValue()[0]);
            }
        }
        return out;
    }

    /**
     * Xác thực checksum VNPay. Tách {@code vnp_SecureHash}, rebuild signing string, so sánh constant-time.
     */
    public static boolean verifyChecksum(Map<String, String> params, String secret) {
        if (params == null || !params.containsKey(SECURE_HASH_FIELD)) {
            return false;
        }
        String receivedHash = params.get(SECURE_HASH_FIELD);
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }
        Map<String, String> signingParams = new LinkedHashMap<>(params);
        signingParams.remove(SECURE_HASH_FIELD);
        signingParams.remove("vnp_SecureHashType"); // không tham gia ký
        String expected = hmacSHA512(secret, buildSigningString(signingParams));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                receivedHash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Helper: trích bookingId từ {@code vnp_OrderInfo} theo format
     * {@code "Thanh toan don dat san #<id>"}. Trả về null nếu không khớp.
     */
    public static Integer extractBookingIdFromOrderInfo(String orderInfo) {
        if (orderInfo == null) {
            return null;
        }
        int hashIdx = orderInfo.lastIndexOf('#');
        if (hashIdx < 0 || hashIdx == orderInfo.length() - 1) {
            return null;
        }
        try {
            return Integer.parseInt(orderInfo.substring(hashIdx + 1).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
