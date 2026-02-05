package com.muyi.common.util.codec;

import com.muyi.common.util.time.TimeUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 加密工具类
 * 提供常用的加密/解密/签名方法
 */
public final class CryptoUtils {
    
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    
    private CryptoUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== MD5 ====================
    
    /**
     * MD5 加密
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }
        return md5(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * MD5 加密
     */
    public static String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    /**
     * MD5 加密（大写）
     */
    public static String md5Upper(String input) {
        String result = md5(input);
        return result != null ? result.toUpperCase() : null;
    }
    
    // ==================== SHA ====================
    
    /**
     * SHA-1 加密
     */
    public static String sha1(String input) {
        return sha(input, "SHA-1");
    }
    
    /**
     * SHA-256 加密
     */
    public static String sha256(String input) {
        return sha(input, "SHA-256");
    }
    
    /**
     * SHA-512 加密
     */
    public static String sha512(String input) {
        return sha(input, "SHA-512");
    }
    
    private static String sha(String input, String algorithm) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + " algorithm not found", e);
        }
    }
    
    // ==================== HMAC ====================
    
    /**
     * HMAC-SHA256 签名
     */
    public static String hmacSha256(String data, String key) {
        return hmac(data, key, "HmacSHA256");
    }
    
    /**
     * HMAC-SHA1 签名
     */
    public static String hmacSha1(String data, String key) {
        return hmac(data, key, "HmacSHA1");
    }
    
    /**
     * HMAC-MD5 签名
     */
    public static String hmacMd5(String data, String key) {
        return hmac(data, key, "HmacMD5");
    }
    
    private static String hmac(String data, String key, String algorithm) {
        if (data == null || key == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKey);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(algorithm + " error", e);
        }
    }
    
    // ==================== Base64 ====================
    
    /**
     * Base64 编码
     */
    public static String base64Encode(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Base64 编码
     */
    public static String base64Encode(byte[] input) {
        if (input == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(input);
    }
    
    /**
     * Base64 解码
     * @return 解码后的字符串，如果输入无效则返回 null
     */
    public static String base64Decode(String input) {
        if (input == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;  // 无效的 Base64 输入
        }
    }
    
    /**
     * Base64 解码为字节数组
     * @return 解码后的字节数组，如果输入无效则返回 null
     */
    public static byte[] base64DecodeToBytes(String input) {
        if (input == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(input);
        } catch (IllegalArgumentException e) {
            return null;  // 无效的 Base64 输入
        }
    }
    
    /**
     * URL 安全的 Base64 编码
     */
    public static String base64UrlEncode(String input) {
        if (input == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * URL 安全的 Base64 解码
     * @return 解码后的字符串，如果输入无效则返回 null
     */
    public static String base64UrlDecode(String input) {
        if (input == null) {
            return null;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;  // 无效的 Base64 输入
        }
    }
    
    // ==================== AES ====================
    
    /**
     * AES 加密（ECB 模式）
     */
    public static String aesEncrypt(String data, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(padKey(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt error", e);
        }
    }
    
    /**
     * AES 解密（ECB 模式）
     */
    public static String aesDecrypt(String encryptedData, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(padKey(key), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt error", e);
        }
    }
    
    /**
     * AES 加密（CBC 模式）
     */
    public static String aesEncryptCBC(String data, String key, String iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(padKey(key), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(padKey(iv));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES CBC encrypt error", e);
        }
    }
    
    /**
     * AES 解密（CBC 模式）
     */
    public static String aesDecryptCBC(String encryptedData, String key, String iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(padKey(key), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(padKey(iv));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES CBC decrypt error", e);
        }
    }
    
    /**
     * 补齐密钥到16字节
     */
    private static byte[] padKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[16];
        System.arraycopy(keyBytes, 0, result, 0, Math.min(keyBytes.length, 16));
        return result;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    /**
     * 十六进制字符串转字节数组
     * @return 字节数组，如果输入无效则返回 null
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return null;
        }
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            // 检查是否是有效的十六进制字符
            if (high == -1 || low == -1) {
                return null;  // 包含无效字符，返回 null
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }
    
    // ==================== 签名验证 ====================
    
    /**
     * 生成简单签名（用于接口验签）
     * 格式：md5(params + timestamp + secret)
     */
    public static String generateSign(String params, long timestamp, String secret) {
        return md5(params + timestamp + secret);
    }
    
    /**
     * 验证签名（使用时间安全的比较，防止时序攻击）
     */
    public static boolean verifySign(String params, long timestamp, String secret, String sign) {
        String expected = generateSign(params, timestamp, secret);
        return expected != null && sign != null && constantTimeEquals(expected, sign);
    }
    
    /**
     * 时间安全的字符串比较（防止时序攻击）
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    /**
     * 验证签名（带时间窗口）
     * @param validSeconds 签名有效期（秒）
     */
    public static boolean verifySign(String params, long timestamp, String secret, String sign, int validSeconds) {
        // 检查时间是否在有效期内
        long now = TimeUtils.currentTimeMillis();
        if (Math.abs(now - timestamp) > validSeconds * 1000L) {
            return false;
        }
        return verifySign(params, timestamp, secret, sign);
    }
}
