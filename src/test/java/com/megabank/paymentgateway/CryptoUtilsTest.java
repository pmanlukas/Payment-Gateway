package com.megabank.paymentgateway;

import com.megabank.paymentgateway.util.CryptoUtils;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

/**
 * Tests for CryptoUtils
 *
 * These tests verify our cryptographic functions "work"
 * Note: They don't verify they're actually secure!
 */
public class CryptoUtilsTest {

    /**
     * Test DES encryption - but DES is deprecated!
     */
    @Test
    public void testDESEncryption() {
        String plaintext = "Hello World";
        String encrypted = CryptoUtils.encryptDES(plaintext);

        // Verify it's different from plaintext
        assertFalse(plaintext.equals(encrypted));

        // Verify decryption works
        String decrypted = CryptoUtils.decryptDES(encrypted);
        assertEquals(plaintext, decrypted);

        // Bug: Doesn't test that encryption is actually secure!
    }

    /**
     * Test with sensitive data - logs it!
     */
    @Test
    public void testEncryptCardNumber() {
        String cardNumber = "4111111111111111";
        String encrypted = CryptoUtils.encryptCardNumber(cardNumber);

        // Just checks it's not null
        assertNotNull(encrypted);

        // System.out in code will log the card number!
    }

    /**
     * Test MD5 hashing - MD5 is broken!
     */
    @Test
    public void testMD5Hash() {
        String input = "password123";
        String hash = CryptoUtils.hashMD5(input);

        // Verify hash is consistent
        assertEquals(hash, CryptoUtils.hashMD5(input));

        // Verify it's 32 hex characters (MD5)
        assertEquals(32, hash.length());

        // Note: Doesn't verify MD5 is secure (it isn't!)
    }

    /**
     * Test SHA1 - also broken!
     */
    @Test
    public void testSHA1Hash() {
        String input = "password123";
        String hash = CryptoUtils.hashSHA1(input);

        assertEquals(40, hash.length());  // SHA1 is 40 hex chars

        // Bug: SHA1 is deprecated for security use
    }

    /**
     * Test password hashing with salt - but same salt for all!
     */
    @Test
    public void testHashWithSalt() {
        String password1 = "password123";
        String password2 = "password456";

        String hash1 = CryptoUtils.hashWithSalt(password1);
        String hash2 = CryptoUtils.hashWithSalt(password2);

        // Different passwords should have different hashes
        assertFalse(hash1.equals(hash2));

        // Bug: Same password always produces same hash (global salt!)
        assertEquals(hash1, CryptoUtils.hashWithSalt(password1));
    }

    /**
     * Test XOR obfuscation - not real encryption!
     */
    @Test
    public void testXORObfuscation() {
        String input = "secret data";
        String obfuscated = CryptoUtils.xorObfuscate(input);

        // Verify it's different
        assertFalse(input.equals(obfuscated));

        // Bug: XOR is trivially reversible!
    }

    /**
     * Test card masking - shows too much!
     */
    @Test
    public void testMaskCardNumber() {
        String card = "4111111111111111";
        String masked = CryptoUtils.maskCardNumber(card);

        // Current implementation shows first 6 and last 4!
        // PCI says only last 4 should be visible
        assertEquals("411111******1111", masked);

        // This test passes but the implementation is WRONG for PCI compliance!
    }

    /**
     * Test token generation - predictable!
     */
    @Test
    public void testGenerateToken() {
        String token1 = CryptoUtils.generateToken();
        String token2 = CryptoUtils.generateToken();

        // Tokens should be different
        // Bug: If called in same millisecond, they'll be the same!
        assertNotNull(token1);
        assertNotNull(token2);
    }

    /**
     * Test random number - not cryptographically secure!
     */
    @Test
    public void testRandomNumber() {
        int random = CryptoUtils.generateRandomNumber(1000);

        assertTrue(random >= 0);
        assertTrue(random < 1000);

        // Bug: Uses Math.random() which is not secure for crypto!
    }

    /**
     * Test Luhn validation - has bug!
     */
    @Test
    public void testLuhnValidation() {
        // Valid test card number
        assertTrue(CryptoUtils.validateLuhn("4111111111111111"));

        // Invalid number
        assertFalse(CryptoUtils.validateLuhn("4111111111111112"));

        // Bug: Doesn't handle non-numeric input properly
    }

    /**
     * Test Luhn with non-numeric - disabled because crashes
     */
    @Test
    @Ignore("Throws NumberFormatException")
    public void testLuhnWithNonNumeric() {
        CryptoUtils.validateLuhn("4111-1111-1111-1111");
    }

    /**
     * Test password comparison - timing attack vulnerable!
     */
    @Test
    public void testComparePasswords() {
        assertTrue(CryptoUtils.comparePasswords("password", "password"));
        assertFalse(CryptoUtils.comparePasswords("password", "different"));

        // Bug: Early return enables timing attacks!
    }

    /**
     * Test OTP generation - predictable!
     */
    @Test
    public void testGenerateOTP() {
        String otp = CryptoUtils.generateOTP();

        // Check it's 6 digits
        assertEquals(6, otp.length());

        // Bug: Based on timestamp - predictable!
    }

    /**
     * Test that all keys are exposed - this shouldn't exist!
     */
    @Test
    public void testGetAllKeys() {
        // This method should not exist!
        java.util.Map<String, String> keys = CryptoUtils.getAllKeys();

        assertNotNull(keys.get("AES_KEY"));
        assertNotNull(keys.get("DES_KEY"));

        // We're literally testing that secrets are exposed!
    }

    // Missing security tests:
    // - Key rotation
    // - Proper IV handling
    // - Salt uniqueness
    // - Constant-time comparison
    // - Secure random generation
    // - Key management
}
