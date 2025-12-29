package com.megabank.paymentgateway.config;

import java.sql.*;
import java.util.*;

/**
 * Database Configuration
 *
 * Centralized database configuration... that's also duplicated elsewhere.
 *
 * This file should be the source of truth but isn't.
 */
public class DatabaseConfig {

    // These are duplicated in DatabaseHelper, PaymentManager, and application.properties!
    public static final String PROD_URL = "jdbc:mysql://prod-db.megabank.internal:3306/payments";
    public static final String DEV_URL = "jdbc:mysql://localhost:3306/payments_dev";
    public static final String UAT_URL = "jdbc:mysql://uat-db.megabank.internal:3306/payments";

    // Same credentials everywhere!
    public static final String DB_USERNAME = "root";
    public static final String DB_PASSWORD = "root123";

    // Connection pool settings (never actually used)
    public static final int MIN_CONNECTIONS = 5;
    public static final int MAX_CONNECTIONS = 100;
    public static final int CONNECTION_TIMEOUT = 30000;
    public static final int IDLE_TIMEOUT = 600000;

    // Retry settings
    public static final int MAX_RETRIES = 3;
    public static final int RETRY_DELAY_MS = 1000;

    // SSL settings (disabled)
    public static final boolean USE_SSL = false;
    public static final boolean VERIFY_SERVER_CERTIFICATE = false;

    /**
     * Get connection - but this just duplicates DatabaseHelper!
     */
    public static Connection getConnection() throws SQLException {
        String url = getUrl();
        System.out.println("Connecting to: " + url);
        System.out.println("Username: " + DB_USERNAME);
        System.out.println("Password: " + DB_PASSWORD);  // Logging password!

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found");
        }

        return DriverManager.getConnection(url, DB_USERNAME, DB_PASSWORD);
    }

    /**
     * Get URL based on environment
     */
    private static String getUrl() {
        String env = System.getProperty("env", "PROD");  // Defaults to PROD!

        if ("DEV".equalsIgnoreCase(env)) {
            return DEV_URL;
        } else if ("UAT".equalsIgnoreCase(env)) {
            return UAT_URL;
        } else {
            return PROD_URL;
        }
    }

    /**
     * Test connection - logs sensitive info
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            boolean valid = conn.isValid(5);
            conn.close();
            System.out.println("Database connection test: " + (valid ? "SUCCESS" : "FAILED"));
            return valid;
        } catch (SQLException e) {
            System.out.println("Database connection test FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all config as map - exposes everything!
     */
    public static Map<String, Object> getAllConfig() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("prod_url", PROD_URL);
        config.put("dev_url", DEV_URL);
        config.put("uat_url", UAT_URL);
        config.put("username", DB_USERNAME);
        config.put("password", DB_PASSWORD);  // Exposing password!
        config.put("use_ssl", USE_SSL);
        return config;
    }

    /**
     * Print config - for debugging
     */
    public static void printConfig() {
        System.out.println("=== DATABASE CONFIGURATION ===");
        System.out.println("PROD URL: " + PROD_URL);
        System.out.println("DEV URL: " + DEV_URL);
        System.out.println("UAT URL: " + UAT_URL);
        System.out.println("Username: " + DB_USERNAME);
        System.out.println("Password: " + DB_PASSWORD);
        System.out.println("Use SSL: " + USE_SSL);
    }
}
