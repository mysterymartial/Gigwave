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
 * Two modes: "paywithaccount" (CBC + MD5 key + UTF-16LE) and "ecb" (ECB + raw 24-byte key + UTF-8).
 */
public final class OnePipeTripleDesUtil {

    private static final String TRANSFORMATION_CBC = "DESede/CBC/PKCS5Padding";
    private static final String TRANSFORMATION_ECB = "DESede/ECB/PKCS5Padding";
    private static final String ALGORITHM = "DESede";
    private static final int KEY_LENGTH_3DES = 24;

    private OnePipeTripleDesUtil() {}

    /**
     * Encrypt plaintext with TripleDES (PaywithAccount style): MD5-derived key, CBC, zero IV, UTF-16LE.
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
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, spec, iv);
        byte[] plainTextBytes = plaintext.getBytes(StandardCharsets.UTF_16LE);
        byte[] cipherText = cipher.doFinal(plainTextBytes);
        return Base64.getEncoder().encodeToString(cipherText);
    }

    /**
     * Encrypt plaintext with TripleDES (ECB style): raw secret key resized to 24 bytes, UTF-8.
     * Use when OnePipe expects DESede/ECB/PKCS5Padding per some docs (e.g. onepipe.encryption=ecb).
     */
    public static String encryptEcb(String plaintext, String secretKey) throws GeneralSecurityException {
        if (plaintext == null || secretKey == null) {
            throw new IllegalArgumentException("plaintext and secretKey must be non-null");
        }
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = resizeKey(keyBytes);
        SecretKeySpec spec = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB);
        cipher.init(Cipher.ENCRYPT_MODE, spec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

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
