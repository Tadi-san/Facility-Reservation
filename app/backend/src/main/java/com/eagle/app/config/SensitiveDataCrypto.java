package com.eagle.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SensitiveDataCrypto {
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SensitiveDataCrypto(@Value("${eagle.data-encryption-key}") String value) {
        byte[] keyBytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[32];
        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = i < keyBytes.length ? keyBytes[i] : (byte) 0;
        }
        this.key = new SecretKeySpec(normalized, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt sensitive value", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank() || !cipherText.contains(":")) return cipherText;
        try {
            String[] parts = cipherText.split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decrypt sensitive value", ex);
        }
    }
}
