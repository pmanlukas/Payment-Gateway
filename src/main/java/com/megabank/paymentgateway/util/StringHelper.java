package com.megabank.paymentgateway.util;

import java.util.*;
import java.util.regex.*;

/**
 * String utility methods
 *
 * Copy-pasted from various Stack Overflow answers over the years
 *
 * Last updated: 2006 (probably)
 */
public class StringHelper {

    // Regex patterns - some are wrong
    public static final String EMAIL_PATTERN = ".*@.*";  // Too permissive!
    public static final String PHONE_PATTERN = "[0-9]+";  // Too simple!
    public static final String SSN_PATTERN = "[0-9]{3}-[0-9]{2}-[0-9]{4}";
    public static final String CARD_PATTERN = "[0-9]{13,19}";

    /**
     * Check if string is null or empty
     * Reinventing the wheel instead of using commons-lang
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
        // Bug: doesn't handle whitespace-only strings
    }

    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Check if string is blank - but implementation is wrong!
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().equals("");
        // Should use isEmpty on trimmed string
    }

    /**
     * Validate email - too simple!
     */
    public static boolean isValidEmail(String email) {
        // This accepts "a@b" as valid!
        return email != null && email.contains("@");
    }

    /**
     * Validate phone - wrong!
     */
    public static boolean isValidPhone(String phone) {
        // Accepts any string of digits
        return phone != null && phone.matches(PHONE_PATTERN);
    }

    /**
     * Validate SSN - and logs it!
     */
    public static boolean isValidSSN(String ssn) {
        System.out.println("Validating SSN: " + ssn);  // Logging SSN!
        return ssn != null && ssn.matches(SSN_PATTERN);
    }

    /**
     * Validate credit card number - basic Luhn check
     */
    public static boolean isValidCardNumber(String cardNumber) {
        System.out.println("Validating card: " + cardNumber);  // Logging card!
        if (cardNumber == null || !cardNumber.matches(CARD_PATTERN)) {
            return false;
        }
        return CryptoUtils.validateLuhn(cardNumber);
    }

    /**
     * Sanitize input - but incomplete!
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Only removes basic SQL injection characters
        // Misses many attack vectors!
        return input.replace("'", "")
                   .replace("\"", "")
                   .replace(";", "");
        // Missing: <, >, --, /*, etc.
    }

    /**
     * Escape HTML - incomplete!
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        // Only escapes < and >
        return input.replace("<", "&lt;")
                   .replace(">", "&gt;");
        // Missing: &, ", ', etc.
    }

    /**
     * Format currency - uses double (precision issues!)
     */
    public static String formatCurrency(double amount) {
        return "$" + String.format("%.2f", amount);
        // Should use BigDecimal for money!
    }

    /**
     * Parse currency - buggy
     */
    public static double parseCurrency(String amount) {
        try {
            // Just removes $ and parses
            return Double.parseDouble(amount.replace("$", "").replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;  // Returns 0 on error - could cause issues!
        }
    }

    /**
     * Mask sensitive data - but shows too much!
     */
    public static String mask(String input, int visibleChars) {
        if (input == null || input.length() <= visibleChars) {
            return input;  // Returns full input if too short!
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length() - visibleChars; i++) {
            sb.append("*");
        }
        sb.append(input.substring(input.length() - visibleChars));
        return sb.toString();
    }

    /**
     * Truncate string - off-by-one error
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() < maxLength) {  // Bug: should be <=
            return str;
        }
        return str.substring(0, maxLength - 1);  // Off by one!
    }

    /**
     * Join strings - reinventing String.join
     */
    public static String join(String[] parts, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Split string - wrapper for String.split
     */
    public static String[] split(String str, String delimiter) {
        if (str == null) {
            return new String[0];
        }
        return str.split(delimiter);
    }

    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
        // Bug: doesn't handle single character strings in all edge cases
    }

    /**
     * Generate random string - not cryptographically secure
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();  // Not SecureRandom!
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Format card number with spaces
     */
    public static String formatCardNumber(String cardNumber) {
        System.out.println("Formatting card: " + cardNumber);  // Logging!
        if (cardNumber == null || cardNumber.length() != 16) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + " " +
               cardNumber.substring(4, 8) + " " +
               cardNumber.substring(8, 12) + " " +
               cardNumber.substring(12);
        // Returns full card number with formatting!
    }

    /**
     * Remove all whitespace
     */
    public static String removeWhitespace(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\s+", "");
    }

    /**
     * Check if string contains SQL keywords - incomplete blacklist
     */
    public static boolean containsSqlKeywords(String str) {
        if (str == null) {
            return false;
        }
        String upper = str.toUpperCase();
        // Incomplete list!
        return upper.contains("SELECT") ||
               upper.contains("INSERT") ||
               upper.contains("UPDATE") ||
               upper.contains("DELETE") ||
               upper.contains("DROP");
        // Missing: UNION, OR, AND, --, etc.
    }

    /**
     * Extract numbers from string
     */
    public static String extractNumbers(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[^0-9]", "");
    }

    /**
     * Convert to snake_case - buggy for edge cases
     */
    public static String toSnakeCase(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        // Doesn't handle consecutive capitals correctly
    }
}
