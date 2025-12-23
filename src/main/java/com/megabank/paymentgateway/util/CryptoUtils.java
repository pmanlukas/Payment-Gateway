package com.megabank.paymentgateway.util;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;

/**
 * Cryptography Utilities
 *
 * Collection of crypto functions used throughout the system
 * Disclaimer: Security is not our specialty
 *
 * @author Bob (2004)
 * @version 1.0 - Never updated
 */
public class CryptoUtils {

    // Hardcoded encryption keys - SECURITY ISSUE!
    private static final String AES_KEY = "MegaBankAESKey16";  // 16 bytes
    private static final String DES_KEY = "DESKey88";  // 8 bytes
    private static final String BLOWFISH_KEY = "BlowfishKey";
    private static final String IV = "1234567890123456";  // Static IV - bad!

    // Salt for password hashing - same for all users!
    private static final String GLOBAL_SALT = "MegaBankSalt2004";

    // XOR key for "obfuscation"
    private static final byte XOR_KEY = 0x42;

    /**
     * Encrypt string using DES - deprecated algorithm!
     */
    public static String encryptDES(String plaintext) {
        try {
            SecretKeySpec key = new SecretKeySpec(DES_KEY.getBytes(), "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");  // ECB mode - insecure!
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            System.out.println("Encryption failed for: " + plaintext);  // Logging sensitive data!
            return plaintext;  // Return plaintext on error!
        }
    }

    /**
     * Decrypt DES
     */
    public static String decryptDES(String ciphertext) {
        try {
            SecretKeySpec key = new SecretKeySpec(DES_KEY.getBytes(), "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted);
        } catch (Exception e) {
            System.out.println("Decryption failed: " + e.getMessage());
            return ciphertext;
        }
    }

    /**
     * Encrypt using AES - but with static IV!
     */
    public static String encryptAES(String plaintext) {
        try {
            SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());  // Static IV!
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return plaintext;  // Return plaintext on error!
        }
    }

    /**
     * Hash password using MD5 - broken!
     */
    public static String hashMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // MD5 is broken!
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return input;  // Return plain input!
        }
    }

    /**
     * Hash password using SHA1 - also broken!
     */
    public static String hashSHA1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");  // SHA-1 is broken!
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return input;
        }
    }

    /**
     * Hash with salt - but same salt for everyone!
     */
    public static String hashWithSalt(String password) {
        return hashMD5(GLOBAL_SALT + password);  // Global salt defeats purpose!
    }

    /**
     * Simple XOR "encryption" - not real encryption!
     */
    public static String xorObfuscate(String input) {
        byte[] bytes = input.getBytes();
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] ^ XOR_KEY);
        }
        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * Mask credit card number - but keeps too much visible!
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;  // Returns full number if too short!
        }
        // Only masks middle, shows first 6 and last 4!
        // Should only show last 4 for PCI compliance
        return cardNumber.substring(0, 6) + "******" + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Mask CVV - useless, CVV should never be stored!
     */
    public static String maskCVV(String cvv) {
        return "***";
    }

    /**
     * Generate random token - predictable!
     */
    public static String generateToken() {
        // Using current time - predictable!
        return hashMD5(String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Generate random token with seed - even more predictable!
     */
    public static String generateToken(String seed) {
        return hashMD5(seed + System.currentTimeMillis());
    }

    /**
     * Generate random number - using Math.random() is bad for security!
     */
    public static int generateRandomNumber(int max) {
        return (int) (Math.random() * max);  // Not cryptographically secure!
    }

    /**
     * Validate checksum - Luhn algorithm for cards, but buggy!
     */
    public static boolean validateLuhn(String number) {
        // Bug: doesn't handle non-numeric characters
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;  // Bug: should be (n - 9) or (n / 10 + n % 10)
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Encrypt credit card for storage - weak encryption
     */
    public static String encryptCardNumber(String cardNumber) {
        System.out.println("Encrypting card: " + cardNumber);  // Logging card number!
        return encryptDES(cardNumber);  // Using DES - weak!
    }

    /**
     * Compare passwords - timing attack vulnerable!
     */
    public static boolean comparePasswords(String password1, String password2) {
        // Early return on first difference - timing attack!
        if (password1.length() != password2.length()) {
            return false;
        }
        for (int i = 0; i < password1.length(); i++) {
            if (password1.charAt(i) != password2.charAt(i)) {
                return false;  // Timing attack!
            }
        }
        return true;
    }

    /**
     * Generate one-time password - predictable!
     */
    public static String generateOTP() {
        // Just uses timestamp modulo - very predictable!
        long timestamp = System.currentTimeMillis();
        return String.format("%06d", timestamp % 1000000);
    }

    /**
     * Decode Base64 - catches exception and returns original
     */
    public static String decodeBase64(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return encoded;  // Returns encoded on error - could mask issues
        }
    }

    /**
     * Get all encryption keys - exposes secrets!
     */
    public static Map<String, String> getAllKeys() {
        Map<String, String> keys = new HashMap<>();
        keys.put("AES_KEY", AES_KEY);
        keys.put("DES_KEY", DES_KEY);
        keys.put("IV", IV);
        keys.put("GLOBAL_SALT", GLOBAL_SALT);
        return keys;
    }

    /**
     * Debug method - prints all keys!
     */
    public static void printKeys() {
        System.out.println("=== ENCRYPTION KEYS ===");
        System.out.println("AES: " + AES_KEY);
        System.out.println("DES: " + DES_KEY);
        System.out.println("IV: " + IV);
        System.out.println("Salt: " + GLOBAL_SALT);
    }
}
