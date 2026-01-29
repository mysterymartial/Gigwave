package com.gigwave.infrastructure.payments.onepipe;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * TripleDES encryption for OnePipe v2 API.
 * Used for auth.secure ("accountNumber;bankCode") and meta.bvn.
 * Algorithm: DESede/ECB/PKCS5Padding. Key: ONEPIPE_SECRET_KEY adjusted to 24 bytes for 3DES. Output: Base64.
 */
public final class OnePipeTripleDesUtil {

    private static final String TRANSFORMATION = "DESede/ECB/PKCS5Padding";
    private static final String ALGORITHM = "DESede";
    private static final int KEY_LENGTH_3DES = 24;

    private OnePipeTripleDesUtil() {}

    /**
     * Encrypt plaintext with TripleDES and return Base64-encoded ciphertext.
     *
     * @param plaintext value to encrypt (e.g. "1234567890;058" or BVN)
     * @param secretKey ONEPIPE_SECRET_KEY (trimmed; resized to 24 bytes)
     * @return Base64-encoded ciphertext
     */
    public static String encrypt(String plaintext, String secretKey) throws GeneralSecurityException {
        if (plaintext == null || secretKey == null) {
            throw new IllegalArgumentException("plaintext and secretKey must be non-null");
        }
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = resizeKey(keyBytes);
        SecretKeySpec spec = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, spec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /** Resize key to 24 bytes: use as-is, truncate, or right-pad with zeros. */
    private static byte[] resizeKey(byte[] keyBytes) {
        if (keyBytes.length == KEY_LENGTH_3DES) {
            return keyBytes;
        }
        byte[] out = new byte[KEY_LENGTH_3DES];
        if (keyBytes.length > KEY_LENGTH_3DES) {
            System.arraycopy(keyBytes, 0, out, 0, KEY_LENGTH_3DES);
        } else {
            System.arraycopy(keyBytes, 0, out, 0, keyBytes.length);
        }
        return out;
    }
}
