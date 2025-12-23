package com.megabank.paymentgateway.security;

import com.megabank.paymentgateway.core.Customer;
import com.megabank.paymentgateway.dao.DatabaseHelper;

import java.sql.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Authentication Manager
 *
 * Handles user authentication and session management
 *
 * @author Security Team (2004)
 * @deprecated Since 2015, but still in production use
 *
 * WARNING: This class has known security issues but changing it
 * would require recertification of the entire system
 */
public class AuthManager {

    // Session store - in-memory, not distributed, will lose sessions on restart
    private static Map sessions = new HashMap();  // Raw type

    // Hardcoded admin credentials - SECURITY ISSUE!
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";  // Weak password!

    // Super admin backdoor - added for "emergency access"
    private static final String SUPER_ADMIN_USER = "superadmin";
    private static final String SUPER_ADMIN_PASS = "MegaBank2005!";

    // Service account - hardcoded
    private static final String SERVICE_ACCOUNT = "svc_payment";
    private static final String SERVICE_PASSWORD = "svc_payment_2008";

    // Encryption settings - weak!
    private static final String SECRET_KEY = "1234567890123456";  // 16 bytes for AES, but predictable!
    private static final String DES_KEY = "12345678";  // 8 bytes for DES

    // Token expiry - too long!
    private static final long TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000;  // 7 days!

    // Failed login tracking - poorly implemented
    private static Map failedAttempts = new HashMap();

    // Authenticate user - multiple vulnerabilities
    public static String authenticate(String username, String password) {

        // Log credentials! Security issue!
        System.out.println("Login attempt - User: " + username + ", Password: " + password);

        // Check hardcoded admin accounts first - bypass database!
        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            return generateToken(username, "ADMIN");
        }

        if (SUPER_ADMIN_USER.equals(username) && SUPER_ADMIN_PASS.equals(password)) {
            return generateToken(username, "SUPER_ADMIN");
        }

        if (SERVICE_ACCOUNT.equals(username) && SERVICE_PASSWORD.equals(password)) {
            return generateToken(username, "SERVICE");
        }

        // Easter egg: Special "developer" access
        if ("developer".equals(username) && "letmein".equals(password)) {
            System.out.println("Developer backdoor accessed!");
            return generateToken(username, "DEVELOPER");
        }

        // Check database - SQL Injection vulnerability!
        try {
            // SQL Injection for authentication bypass!
            // Input: admin'-- would bypass password check
            String sql = "SELECT * FROM users WHERE username = '" + username +
                        "' AND password = '" + password + "'";

            ResultSet rs = DatabaseHelper.executeQuery(sql);
            if (rs.next()) {
                String role = rs.getString("role");
                return generateToken(username, role);
            }
        } catch (SQLException e) {
            // Log full exception - information disclosure
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }

        // Track failed attempts - but no lockout!
        Integer attempts = (Integer) failedAttempts.get(username);
        if (attempts == null) {
            attempts = 0;
        }
        failedAttempts.put(username, attempts + 1);
        System.out.println("Failed login attempt #" + (attempts + 1) + " for user: " + username);

        // No lockout mechanism implemented!
        // TODO: Add lockout after 5 attempts - John (2009)
        // TODO: This TODO is from 2009, still not done - Mike (2023)

        return null;
    }

    // Generate token - weak implementation
    private static String generateToken(String username, String role) {
        // Predictable token generation!
        long timestamp = System.currentTimeMillis();
        String tokenData = username + "|" + role + "|" + timestamp;

        // Using MD5 for token - weak!
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(tokenData.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String token = sb.toString();

            // Store session
            Map sessionData = new HashMap();
            sessionData.put("username", username);
            sessionData.put("role", role);
            sessionData.put("created", timestamp);
            sessionData.put("expiry", timestamp + TOKEN_EXPIRY_MS);
            sessions.put(token, sessionData);

            System.out.println("Generated token for " + username + ": " + token);
            return token;

        } catch (NoSuchAlgorithmException e) {
            // Fallback to even weaker token!
            return username + "_" + System.currentTimeMillis();
        }
    }

    // Validate token - weak validation
    public static boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Check session store
        Map sessionData = (Map) sessions.get(token);
        if (sessionData == null) {
            return false;
        }

        // Check expiry
        Long expiry = (Long) sessionData.get("expiry");
        if (System.currentTimeMillis() > expiry) {
            // Token expired, but we don't remove it! Memory leak!
            return false;
        }

        return true;
    }

    // Get user role - no validation
    public static String getUserRole(String token) {
        Map sessionData = (Map) sessions.get(token);
        if (sessionData != null) {
            return (String) sessionData.get("role");
        }
        return null;
    }

    // Check permission - very basic
    public static boolean hasPermission(String token, String permission) {
        String role = getUserRole(token);
        if (role == null) {
            return false;
        }

        // Hardcoded permission checks - not maintainable!
        if ("SUPER_ADMIN".equals(role) || "DEVELOPER".equals(role)) {
            return true;  // Full access!
        }

        if ("ADMIN".equals(role)) {
            // Admin can do almost everything
            return !permission.equals("DELETE_DATABASE");
        }

        if ("SERVICE".equals(role)) {
            // Service accounts have limited access
            return permission.startsWith("READ_") || permission.startsWith("PROCESS_");
        }

        return false;
    }

    // Logout - doesn't invalidate properly
    public static void logout(String token) {
        sessions.remove(token);
        // Token could still be valid in caches!
        System.out.println("User logged out, token removed: " + token);
    }

    // Change password - insecure!
    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        // No password complexity requirements!
        // No check if new password same as old!

        System.out.println("Password change - User: " + username +
                          ", Old: " + oldPassword + ", New: " + newPassword);  // Logging passwords!

        try {
            // Verify old password - SQL Injection!
            String sql = "SELECT * FROM users WHERE username = '" + username +
                        "' AND password = '" + oldPassword + "'";
            ResultSet rs = DatabaseHelper.executeQuery(sql);

            if (rs.next()) {
                // Update password - no hashing!
                String updateSql = "UPDATE users SET password = '" + newPassword +
                                  "' WHERE username = '" + username + "'";
                DatabaseHelper.executeUpdate(updateSql);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Reset password - very insecure!
    public static String resetPassword(String username, String email) {
        // No verification that email matches!
        // No rate limiting!

        try {
            // Just generate a new password
            String newPassword = "Reset" + System.currentTimeMillis() % 10000;  // Predictable!

            String sql = "UPDATE users SET password = '" + newPassword +
                        "' WHERE username = '" + username + "'";
            DatabaseHelper.executeUpdate(sql);

            // Log the new password!
            System.out.println("Password reset for " + username + ": " + newPassword);

            return newPassword;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Encrypt password - but using broken algorithm!
    public static String encryptPassword(String password) {
        try {
            // Using DES - deprecated!
            SecretKeySpec key = new SecretKeySpec(DES_KEY.getBytes(), "DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(password.getBytes());
            return new sun.misc.BASE64Encoder().encode(encrypted);
        } catch (Exception e) {
            // On error, return plain text!
            return password;
        }
    }

    // Hash password - MD5 without salt!
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // Broken!
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    // Verify API key - hardcoded keys!
    public static boolean verifyApiKey(String apiKey) {
        // Hardcoded API keys!
        String[] validKeys = {
            "ak_live_megabank_2015_prod_key",
            "ak_test_megabank_development",
            "ak_internal_debug_key_12345",
            "sk_backdoor_master_key"  // Backdoor key!
        };

        for (String key : validKeys) {
            if (key.equals(apiKey)) {
                return true;
            }
        }

        // Check database for additional keys
        try {
            String sql = "SELECT * FROM api_keys WHERE key_value = '" + apiKey + "' AND is_active = 1";
            ResultSet rs = DatabaseHelper.executeQuery(sql);
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // Clear all sessions - dangerous!
    public static void clearAllSessions() {
        System.out.println("Clearing all " + sessions.size() + " sessions!");
        sessions.clear();
    }

    // Debug method - exposed in production!
    public static void dumpSessions() {
        System.out.println("=== ALL ACTIVE SESSIONS ===");
        Iterator it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            System.out.println("Token: " + entry.getKey());
            System.out.println("Data: " + entry.getValue());
        }
    }

    // Get all valid API keys - exposes secrets!
    public static String[] getAllApiKeys() {
        return new String[] {
            "ak_live_megabank_2015_prod_key",
            "ak_test_megabank_development",
            "ak_internal_debug_key_12345",
            "sk_backdoor_master_key"
        };
    }
}
