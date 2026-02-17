package be.codeforbelgium.openinzichten.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesMessageEncryptionServiceTest {

    @Test
    void encryptAndDecrypt_withProvidedKey_roundtrips() {
        AesMessageEncryptionService svc = new AesMessageEncryptionService("my-secret-passphrase");
        String plain = "hello world";
        String cipher = svc.encrypt(plain);
        assertNotNull(cipher);
        assertNotEquals(plain, cipher);

        String dec = svc.decrypt(cipher);
        assertEquals(plain, dec);
    }

    @Test
    void encryptNull_returnsNull_and_decryptNull_returnsNull() {
        AesMessageEncryptionService svc = new AesMessageEncryptionService("k");
        assertNull(svc.encrypt(null));
        assertNull(svc.decrypt(null));
    }

    @Test
    void ephemeralKey_mismatch_decrypt_returnsOriginalCiphertext() {
        // Create a service with no configured key -> ephemeral generated key
        AesMessageEncryptionService svc1 = new AesMessageEncryptionService("");
        String plain = "sensitive";
        String cipher = svc1.encrypt(plain);
        assertNotNull(cipher);

        // New instance created later will have a different ephemeral key
        AesMessageEncryptionService svc2 = new AesMessageEncryptionService("");
        // svc2.decrypt should fail to decrypt and return the raw stored value
        String decrypted = svc2.decrypt(cipher);
        assertEquals(cipher, decrypted);
    }

    @Test
    void decrypt_tooShort_decoded_returnsNull() {
        AesMessageEncryptionService svc = new AesMessageEncryptionService("k");
        // Create a Base64 encoding of a short byte array (< IV_LENGTH which is 12)
        byte[] shortBytes = new byte[5];
        String b64 = Base64.getEncoder().encodeToString(shortBytes);
        assertNull(svc.decrypt(b64));
    }

    @Test
    void encrypt_withInvalidKey_throwsRuntimeException_and_isCaught_branch() throws Exception {
        AesMessageEncryptionService svc = new AesMessageEncryptionService("my-secret-passphrase");
        // Replace the internal keySpec with an invalid-size AES key to force Cipher.init to fail
        java.lang.reflect.Field f = AesMessageEncryptionService.class.getDeclaredField("keySpec");
        f.setAccessible(true);
        f.set(svc, new javax.crypto.spec.SecretKeySpec(new byte[]{0x01}, "AES"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> svc.encrypt("hello"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof java.security.InvalidKeyException || ex.getCause() instanceof java.security.InvalidKeyException);
    }
}
