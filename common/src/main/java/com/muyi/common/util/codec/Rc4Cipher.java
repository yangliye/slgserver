package com.muyi.common.util.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * RC4 流加密（带 drop-N 防初始偏差攻击）
 * <p>
 * 特点：
 * <ul>
 *   <li>收发方向使用不同密钥流（防 keystream reuse 攻击）</li>
 *   <li>默认丢弃前 1024 字节 keystream（RFC 7465 建议）</li>
 *   <li>原地 XOR，零拷贝</li>
 * </ul>
 * <p>
 * 密钥派生：从 masterKey 派生两个方向的独立密钥
 * <pre>
 * sendKey = SHA-256("s2c" + masterKey) 前 16 字节
 * recvKey = SHA-256("c2s" + masterKey) 前 16 字节
 * </pre>
 * 服务端和客户端的 send/recv 方向相反：
 * 服务端 encrypt 用 s2c，decrypt 用 c2s；
 * 客户端 encrypt 用 c2s，decrypt 用 s2c。
 *
 * @author muyi
 */
public class Rc4Cipher implements PacketCipher {

    private static final int DEFAULT_DROP_BYTES = 1024;
    private static final int KEY_LENGTH = 16;
    private static final byte[] S2C_LABEL = "s2c".getBytes();
    private static final byte[] C2S_LABEL = "c2s".getBytes();

    private final Rc4State encryptState;
    private final Rc4State decryptState;

    /**
     * 服务端构造：encrypt=s2c, decrypt=c2s
     */
    public static Rc4Cipher forServer(byte[] masterKey) {
        return new Rc4Cipher(deriveKey(S2C_LABEL, masterKey), deriveKey(C2S_LABEL, masterKey));
    }

    /**
     * 客户端构造：encrypt=c2s, decrypt=s2c
     */
    public static Rc4Cipher forClient(byte[] masterKey) {
        return new Rc4Cipher(deriveKey(C2S_LABEL, masterKey), deriveKey(S2C_LABEL, masterKey));
    }

    /**
     * 生成随机 masterKey
     */
    public static byte[] generateKey() {
        byte[] key = new byte[KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private Rc4Cipher(byte[] encryptKey, byte[] decryptKey) {
        this.encryptState = new Rc4State(encryptKey, DEFAULT_DROP_BYTES);
        this.decryptState = new Rc4State(decryptKey, DEFAULT_DROP_BYTES);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        encryptState.xor(data);
        return data;
    }

    @Override
    public byte[] decrypt(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        decryptState.xor(data);
        return data;
    }

    /**
     * 从 label + masterKey 派生子密钥
     */
    private static byte[] deriveKey(byte[] label, byte[] masterKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(label);
            md.update(masterKey);
            byte[] hash = md.digest();
            return Arrays.copyOf(hash, KEY_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

}
