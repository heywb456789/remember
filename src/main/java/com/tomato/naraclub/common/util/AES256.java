package com.tomato.naraclub.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

public class AES256 {

    // AES/CBC/PKCS5Padding 변환 문자열
    private static final String TRANSFORM = "AES/CBC/PKCS5Padding";

    /**
     * key 문자열을 MD5 해시해서 16바이트 IV로 사용
     */
    private static byte[] deriveIv(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    /**
     * key 문자열을 SHA-256 해시해서 32바이트 AES 키로 사용
     */
    private static byte[] deriveKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    /**
     * 평문을 AES-256/CBC/PKCS5Padding으로 암호화하고,
     * 결과를 Base64 문자열로 반환
     */
    public static String encrypt(String plainText, String key) {
        byte[] ivBytes  = deriveIv(key);
        byte[] keyBytes = deriveKey(key);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(ivBytes));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES 암호화 실패", e);
        }
    }

    /**
     * Base64로 인코딩된 암호문을 복호화하여 평문을 반환
     */
    public static String decrypt(String encryptedBase64, String key) {
        byte[] ivBytes  = deriveIv(key);
        byte[] keyBytes = deriveKey(key);

        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(ivBytes));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 복호화 실패", e);
        }
    }
}
