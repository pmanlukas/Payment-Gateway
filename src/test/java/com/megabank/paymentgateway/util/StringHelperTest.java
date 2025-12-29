package com.megabank.paymentgateway.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for StringHelper utility class
 */
public class StringHelperTest {

    @Test
    public void testIsEmpty_null() {
        assertTrue(StringHelper.isEmpty(null));
    }

    @Test
    public void testIsEmpty_emptyString() {
        assertTrue(StringHelper.isEmpty(""));
    }

    @Test
    public void testIsEmpty_whitespace() {
        assertTrue(StringHelper.isEmpty("   "));
    }

    @Test
    public void testIsEmpty_normalString() {
        assertFalse(StringHelper.isEmpty("test"));
    }

    @Test
    public void testIsNotEmpty_normalString() {
        assertTrue(StringHelper.isNotEmpty("test"));
    }

    @Test
    public void testIsNotEmpty_null() {
        assertFalse(StringHelper.isNotEmpty(null));
    }

    @Test
    public void testTrim_null() {
        assertNull(StringHelper.safeTrim(null));
    }

    @Test
    public void testTrim_withSpaces() {
        assertEquals("test", StringHelper.safeTrim("  test  "));
    }

    @Test
    public void testTrim_noSpaces() {
        assertEquals("test", StringHelper.safeTrim("test"));
    }
}
