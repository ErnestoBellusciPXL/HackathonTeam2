package be.codeforbelgium.openinzichten.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesMessageEncryptionService implements MessageEncryptionService {
    private static final Logger log = LoggerFactory.getLogger(AesMessageEncryptionService.class);
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public AesMessageEncryptionService(@Value("${app.encryption.key:}") String key) {
        if (key == null || key.isBlank()) {
            log.warn("No encryption key provided (app.encryption.key); message encryption will be disabled");
            try {
                SecretKey gen = KeyGenerator.getInstance("AES").generateKey();
                this.keySpec = new SecretKeySpec(gen.getEncoded(), "AES");
            } catch (Exception e) {
                throw new EncryptionException("Failed to generate AES key", e);
            }
        } else {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
                this.keySpec = new SecretKeySpec(keyBytes, "AES");
            } catch (Exception e) {
                throw new EncryptionException("Failed to derive AES key from configuration", e);
            }
        }
    }

    @Override
    public String encrypt(String plain) {
        if (plain == null)
            return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null)
            return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            if (all.length < IV_LENGTH)
                return null;
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] ct = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Decryption failed for input - returning raw stored value", e);
            return cipherText;
        }
    }

    private static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
