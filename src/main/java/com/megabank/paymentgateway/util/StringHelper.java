package com.megabank.paymentgateway.util;

/**
 * String Helper Utilities
 * 
 * Common string manipulation functions
 */
public class StringHelper {
    
    /**
     * Check if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * Safely trim a string (handles null)
     */
    public static String safeTrim(String str) {
        return str == null ? null : str.trim();
    }
    
    /**
     * Truncate string to max length
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
    
    /**
     * Left pad string with character
     */
    public static String leftPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - str.length()) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }
    
    /**
     * Right pad string with character
     */
    public static String rightPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(padChar);
        }
        return sb.toString();
    }
}
