package com.gigwave.infrastructure.payments.onepipe;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * TripleDES encryption for OnePipe v2 API (PaywithAccount / NIBSS).
 * Used for auth.secure ("accountNumber;bankCode") and meta.bvn.
 * Key: MD5(secretKey in UTF-16LE), expanded to 24 bytes (first 16 + bytes 0-7 repeated).
 * Cipher: DESede/CBC/PKCS5Padding with 8-byte zero IV. Plaintext/ciphertext: UTF-16LE / Base64.
 */
public final class OnePipeTripleDesUtil {

    private static final String TRANSFORMATION = "DESede/CBC/PKCS5Padding";
    private static final String ALGORITHM = "DESede";
    private static final int KEY_LENGTH_3DES = 24;

    private OnePipeTripleDesUtil() {}

    /**
     * Encrypt plaintext with TripleDES and return Base64-encoded ciphertext.
     * Matches OnePipe/PaywithAccount: MD5-derived key (UTF-16LE), CBC, zero IV, plaintext UTF-16LE.
     *
     * @param plaintext value to encrypt (e.g. "accountNo;bankCode" or BVN)
     * @param secretKey ONEPIPE_SECRET_KEY (used as-is; key derived via MD5)
     * @return Base64-encoded ciphertext
     */
    public static String encrypt(String plaintext, String secretKey) throws GeneralSecurityException {
        if (plaintext == null || secretKey == null) {
            throw new IllegalArgumentException("plaintext and secretKey must be non-null");
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digestOfPassword = md.digest(secretKey.getBytes(StandardCharsets.UTF_16LE));
        byte[] keyBytes = Arrays.copyOf(digestOfPassword, KEY_LENGTH_3DES);
        for (int j = 0, k = 16; j < 8; ) {
            keyBytes[k++] = keyBytes[j++];
        }
        SecretKeySpec spec = new SecretKeySpec(keyBytes, 0, KEY_LENGTH_3DES, ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, spec, iv);
        byte[] plainTextBytes = plaintext.getBytes(StandardCharsets.UTF_16LE);
        byte[] cipherText = cipher.doFinal(plainTextBytes);
        return Base64.getEncoder().encodeToString(cipherText);
    }
}
