package com.muyi.common.util.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptoUtils 测试类
 */
class CryptoUtilsTest {

    @Test
    void testMd5() {
        String result = CryptoUtils.md5("hello");
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("5d41402abc4b2a76b9719d911017c592", result);
        
        // 相同输入应产生相同输出
        assertEquals(CryptoUtils.md5("hello"), CryptoUtils.md5("hello"));
        
        // 不同输入产生不同输出
        assertNotEquals(CryptoUtils.md5("hello"), CryptoUtils.md5("world"));
        
        assertNull(CryptoUtils.md5((String) null));
    }

    @Test
    void testMd5Bytes() {
        byte[] input = "hello".getBytes();
        String result = CryptoUtils.md5(input);
        assertNotNull(result);
        assertEquals(32, result.length());
        assertEquals("5d41402abc4b2a76b9719d911017c592", result);
    }

    @Test
    void testMd5Upper() {
        String result = CryptoUtils.md5Upper("hello");
        assertNotNull(result);
        assertEquals("5D41402ABC4B2A76B9719D911017C592", result);
        
        assertNull(CryptoUtils.md5Upper(null));
    }

    @Test
    void testSha1() {
        String result = CryptoUtils.sha1("hello");
        assertNotNull(result);
        assertEquals(40, result.length());
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", result);
        
        assertNull(CryptoUtils.sha1(null));
    }

    @Test
    void testSha256() {
        String result = CryptoUtils.sha256("hello");
        assertNotNull(result);
        assertEquals(64, result.length());
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result);
        
        assertNull(CryptoUtils.sha256(null));
    }

    @Test
    void testSha512() {
        String result = CryptoUtils.sha512("hello");
        assertNotNull(result);
        assertEquals(128, result.length());
        
        assertNull(CryptoUtils.sha512(null));
    }

    @Test
    void testHmacSha1() {
        String result = CryptoUtils.hmacSha1("hello", "secret");
        assertNotNull(result);
        
        // 相同输入相同密钥应产生相同输出
        assertEquals(CryptoUtils.hmacSha1("hello", "secret"), CryptoUtils.hmacSha1("hello", "secret"));
        
        // 不同密钥产生不同输出
        assertNotEquals(CryptoUtils.hmacSha1("hello", "secret1"), CryptoUtils.hmacSha1("hello", "secret2"));
        
        assertNull(CryptoUtils.hmacSha1(null, "secret"));
        assertNull(CryptoUtils.hmacSha1("hello", null));
    }

    @Test
    void testHmacSha256() {
        String result = CryptoUtils.hmacSha256("hello", "secret");
        assertNotNull(result);
        
        assertNull(CryptoUtils.hmacSha256(null, "secret"));
    }

    @Test
    void testHmacMd5() {
        String result = CryptoUtils.hmacMd5("hello", "secret");
        assertNotNull(result);
        
        assertNull(CryptoUtils.hmacMd5(null, "secret"));
    }

    @Test
    void testBase64Encode() {
        String result = CryptoUtils.base64Encode("hello");
        assertEquals("aGVsbG8=", result);
        
        assertNull(CryptoUtils.base64Encode((String) null));
    }

    @Test
    void testBase64Decode() {
        String result = CryptoUtils.base64Decode("aGVsbG8=");
        assertEquals("hello", result);
        
        assertNull(CryptoUtils.base64Decode(null));
        assertNull(CryptoUtils.base64Decode("!!!invalid!!!")); // 无效 Base64
    }

    @Test
    void testBase64EncodeBytes() {
        byte[] bytes = "hello".getBytes();
        String result = CryptoUtils.base64Encode(bytes);
        assertEquals("aGVsbG8=", result);
        
        assertNull(CryptoUtils.base64Encode((byte[]) null));
    }

    @Test
    void testBase64DecodeToBytes() {
        byte[] result = CryptoUtils.base64DecodeToBytes("aGVsbG8=");
        assertNotNull(result);
        assertArrayEquals("hello".getBytes(), result);
        
        assertNull(CryptoUtils.base64DecodeToBytes(null));
        assertNull(CryptoUtils.base64DecodeToBytes("!!!invalid!!!"));
    }

    @Test
    void testBase64UrlEncode() {
        // URL 安全的 Base64 不包含 + / =
        String result = CryptoUtils.base64UrlEncode("hello?world!");
        assertNotNull(result);
        assertFalse(result.contains("+"));
        assertFalse(result.contains("/"));
        
        assertNull(CryptoUtils.base64UrlEncode(null));
    }

    @Test
    void testBase64UrlDecode() {
        String encoded = CryptoUtils.base64UrlEncode("hello?world!");
        String decoded = CryptoUtils.base64UrlDecode(encoded);
        assertEquals("hello?world!", decoded);
        
        assertNull(CryptoUtils.base64UrlDecode(null));
        assertNull(CryptoUtils.base64UrlDecode("!!!invalid!!!"));
    }

    @Test
    void testBytesToHex() {
        byte[] bytes = {0x0A, 0x1B, (byte) 0xFF};
        String result = CryptoUtils.bytesToHex(bytes);
        assertEquals("0a1bff", result);
        
        assertNull(CryptoUtils.bytesToHex(null));
    }

    @Test
    void testHexToBytes() {
        byte[] result = CryptoUtils.hexToBytes("0a1bff");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(0x0A, result[0]);
        assertEquals(0x1B, result[1]);
        assertEquals((byte) 0xFF, result[2]);
        
        // 大写也支持
        byte[] upper = CryptoUtils.hexToBytes("0A1BFF");
        assertArrayEquals(result, upper);
        
        // 无效输入
        assertNull(CryptoUtils.hexToBytes(null));
        assertNull(CryptoUtils.hexToBytes("abc")); // 奇数长度
        assertNull(CryptoUtils.hexToBytes("gg")); // 非法字符
    }

    @Test
    void testAesEncryptDecrypt() {
        String key = "1234567890123456"; // 16 字节密钥
        String plainText = "Hello, AES!";
        
        String encrypted = CryptoUtils.aesEncrypt(plainText, key);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        
        String decrypted = CryptoUtils.aesDecrypt(encrypted, key);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAesEncryptDecryptCBC() {
        String key = "1234567890123456"; // 16 字节密钥
        String iv = "abcdefghijklmnop";  // 16 字节 IV
        String plainText = "Hello, AES CBC!";
        
        String encrypted = CryptoUtils.aesEncryptCBC(plainText, key, iv);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        
        String decrypted = CryptoUtils.aesDecryptCBC(encrypted, key, iv);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testAesShortKey() {
        // 短密钥会被自动补齐
        String key = "short"; 
        String plainText = "Hello";
        
        String encrypted = CryptoUtils.aesEncrypt(plainText, key);
        assertNotNull(encrypted);
        
        String decrypted = CryptoUtils.aesDecrypt(encrypted, key);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testGenerateSign() {
        String params = "userId=1&amount=100";
        long timestamp = 1700000000L;
        String secret = "my_secret";
        
        String sign = CryptoUtils.generateSign(params, timestamp, secret);
        assertNotNull(sign);
        
        // 验证签名
        assertTrue(CryptoUtils.verifySign(params, timestamp, secret, sign));
        
        // 错误的签名
        assertFalse(CryptoUtils.verifySign(params, timestamp, secret, "wrong_sign"));
    }

    @Test
    void testVerifySignWithTimeWindow() {
        String params = "test";
        String secret = "secret";
        long timestamp = System.currentTimeMillis();
        
        String sign = CryptoUtils.generateSign(params, timestamp, secret);
        
        // 在有效期内
        assertTrue(CryptoUtils.verifySign(params, timestamp, secret, sign, 60)); // 60秒窗口
        
        // 超过有效期（使用过去很久的时间戳）
        long oldTimestamp = timestamp - 120_000; // 2分钟前
        String oldSign = CryptoUtils.generateSign(params, oldTimestamp, secret);
        assertFalse(CryptoUtils.verifySign(params, oldTimestamp, secret, oldSign, 60)); // 60秒窗口
    }
}
