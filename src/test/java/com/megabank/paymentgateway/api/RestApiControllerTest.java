package com.megabank.paymentgateway.api;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for RestApiController
 *
 * These tests verify the REST API endpoints work correctly.
 * Note: These are unit tests, not integration tests with real HTTP.
 */
public class RestApiControllerTest {

    private RestApiController controller;

    @Before
    public void setUp() {
        controller = new RestApiController();
    }

    @Test
    public void testIsValidApiKey_validKey() {
        // Test with hardcoded test key
        boolean result = controller.isValidApiKey("test_key_12345");
        assertTrue("Valid API key should be accepted", result);
    }

    @Test
    public void testIsValidApiKey_invalidKey() {
        boolean result = controller.isValidApiKey("invalid_key");
        assertFalse("Invalid API key should be rejected", result);
    }

    @Test
    public void testIsValidApiKey_nullKey() {
        boolean result = controller.isValidApiKey(null);
        assertFalse("Null API key should be rejected", result);
    }

    @Test
    public void testIsValidApiKey_emptyKey() {
        boolean result = controller.isValidApiKey("");
        assertFalse("Empty API key should be rejected", result);
    }

    @Test
    public void testIsValidApiKey_allHardcodedKeys() {
        // Test all hardcoded API keys
        String[] validKeys = {
            "test_key_12345",
            "prod_key_67890",
            "internal_key_admin",
            "backdoor_key_debug123"
        };

        for (String key : validKeys) {
            assertTrue("Key should be valid: " + key, controller.isValidApiKey(key));
        }
    }

    // Note: Full HTTP integration tests would require MockMvc or similar
    // These would test actual servlet request/response handling
}
