package com.tomato.naraclub.common.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CryptoKeyGenerator {

    public static class SymmetricKeys {
        public final byte[] key;
        public final byte[] iv;
        public final byte[] hmacKey;

        public SymmetricKeys(byte[] key, byte[] iv, byte[] hmacKey) {
            this.key = key;
            this.iv = iv;
            this.hmacKey = hmacKey;
        }
        public byte[] getKey() { return key; }
        public byte[] getIv() { return iv; }
        public byte[] getHmacKey() { return hmacKey; }
    }

    /** 
     * req_dtim + req_no + token_val 을 SHA-256 → Base64 한 뒤 key/iv/hmacKey 를 추출 
     */
    public static SymmetricKeys derive(String reqDtim, String reqNo, String tokenVal) {
        try {
            String seed = reqDtim.trim() + reqNo.trim() + tokenVal.trim();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            String b64  = Base64.getEncoder().encodeToString(hash);

            // key: 앞 16글자, iv: 뒤 16글자, hmacKey: 앞 32글자
            byte[] keyBytes     = b64.substring(0, 16).getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes      = b64.substring(b64.length() - 16).getBytes(StandardCharsets.UTF_8);
            byte[] hmacKeyBytes = b64.substring(0, 32).getBytes(StandardCharsets.UTF_8);

            return new SymmetricKeys(keyBytes, ivBytes, hmacKeyBytes);
        } catch (Exception e) {
            throw new RuntimeException("대칭키 생성 실패", e);
        }
    }

    /** AES/CBC/PKCS5Padding 으로 암호화 */
    public static String encryptAes(String plainJson, SymmetricKeys sk) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(sk.key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(sk.iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] enc = cipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new RuntimeException("AES 암호화 실패", e);
        }
    }

    /** HMAC-SHA256 으로 무결성 키 생성 */
    public static String calcHmac(String encData, SymmetricKeys sk) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(sk.hmacKey, "HmacSHA256");
            mac.init(sks);
            byte[] hmac = mac.doFinal(encData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("HMACSHA256 실패", e);
        }
    }

    /**
     * AES/CBC/PKCS5Padding 복호화 후 원문 반환
     */
    public static String decryptAes(String encData, SymmetricKeys sk) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encData);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(sk.getKey(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(sk.getIv());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 복호화 실패", e);
        }
    }
}
