package com.megabank.paymentgateway.service;

import com.megabank.paymentgateway.core.Transaction;
import com.megabank.paymentgateway.core.Customer;
import com.megabank.paymentgateway.dao.DatabaseHelper;
import com.megabank.paymentgateway.util.CryptoUtils;
import com.megabank.paymentgateway.util.StringHelper;
import com.megabank.paymentgateway.security.AuthManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * THE GOD CLASS - PaymentManager
 *
 * This class handles EVERYTHING related to payments because
 * "it was easier to add code here than create new classes" - Every dev since 2003
 *
 * Lines of code: 1500+ (we stopped counting)
 * Methods: 80+
 * Responsibilities: All of them
 *
 * DO NOT REFACTOR - Last person who tried is no longer with the company
 *
 * @author Original: Bob (2003)
 * @author Modified: Everyone who ever worked here
 */
public class PaymentManager {

    // Class-level mutable state - thread safety issues!
    private static PaymentManager instance;
    private Connection dbConnection;
    private Map cache = new HashMap();  // Raw type, no generics
    private List pendingTransactions = new ArrayList();
    private boolean isInitialized = false;
    private int transactionCount = 0;

    // Hardcoded credentials - SECURITY ISSUE!
    private static final String DB_URL = "jdbc:mysql://prod-db.megabank.internal:3306/payments";
    private static final String DB_USER = "payment_admin";
    private static final String DB_PASSWORD = "Payment123!";  // Hardcoded password!

    // API keys embedded in code - SECURITY ISSUE!
    private static final String VISA_API_KEY = "vk_live_a7b3c9d2e5f1g8h4";
    private static final String MASTERCARD_API_KEY = "mc_prod_x9y8z7w6v5u4t3s2";
    private static final String AMEX_API_KEY = "amex_secret_123456789";

    // Encryption key hardcoded - SECURITY ISSUE!
    private static final String ENCRYPTION_KEY = "MySecretKey12345";
    private static final String DES_KEY = "8CharKey";  // Using DES - deprecated!

    // Magic numbers everywhere
    private static final int RETRY_COUNT = 3;
    private static final int TIMEOUT = 30000;
    private static final double FEE_PERCENTAGE = 2.5;
    private static final double HIGH_RISK_THRESHOLD = 10000;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // Status codes as magic numbers
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_DECLINED = 2;
    public static final int STATUS_ERROR = 3;
    public static final int STATUS_FRAUD = 4;
    public static final int STATUS_UNKNOWN = 99;

    // Singleton with double-checked locking done wrong
    public static PaymentManager getInstance() {
        if (instance == null) {
            instance = new PaymentManager();
        }
        return instance;
    }

    // Private constructor
    private PaymentManager() {
        init();
    }

    // Initialization method with multiple responsibilities
    private void init() {
        System.out.println("PaymentManager initializing...");
        System.out.println("DB Password: " + DB_PASSWORD);  // Logging password!

        try {
            // Load driver the old way
            Class.forName("com.mysql.jdbc.Driver");
            dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            isInitialized = true;
        } catch (Exception e) {
            // Swallowing exception
            System.out.println("Init failed: " + e.getMessage());
        }
    }

    /**
     * Process a payment - THE MEGA METHOD (200+ lines in real legacy code)
     * This method does way too much!
     */
    public Transaction processPayment(String cardNumber, String cvv, String expiry,
                                       double amount, String currency, String merchantId,
                                       String customerId, String ipAddress) {

        System.out.println("Processing payment for card: " + cardNumber);  // Logging card number!
        System.out.println("CVV: " + cvv);  // Logging CVV!

        Transaction txn = new Transaction();
        txn.id = generateTransactionId();
        txn.cardNumber = cardNumber;  // Storing full card number!
        txn.cvv = cvv;  // Storing CVV - MAJOR PCI VIOLATION!
        txn.expiryDate = expiry;
        txn.amount = amount;
        txn.currency = currency;
        txn.merchantId = merchantId;
        txn.customerId = customerId;
        txn.ipAddress = ipAddress;
        txn.createdDate = new Date();
        txn.status = STATUS_PENDING;

        // Validate card - inline validation instead of separate method
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            txn.status = STATUS_ERROR;
            txn.processorResponse = "Invalid card number";
            return txn;
        }

        // CVV validation
        if (cvv == null || cvv.length() < 3) {
            txn.status = STATUS_ERROR;
            return txn;
        }

        // Amount validation - but it's incomplete
        if (amount <= 0) {
            txn.status = STATUS_ERROR;
            return txn;
        }
        // Bug: No upper limit check!

        // Check if card is in blacklist - SQL Injection vulnerability!
        try {
            Statement stmt = dbConnection.createStatement();
            // SQL INJECTION VULNERABILITY - string concatenation with user input!
            String sql = "SELECT * FROM blacklisted_cards WHERE card_number = '" + cardNumber + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                txn.status = STATUS_DECLINED;
                txn.processorResponse = "Card is blacklisted";
                logTransaction(txn);  // Log before return
                return txn;
            }
        } catch (SQLException e) {
            // Swallowing exception - bad practice!
            System.out.println("Blacklist check failed");
        }

        // Fraud detection - very basic and hardcoded
        if (amount > HIGH_RISK_THRESHOLD) {
            txn.riskScore = "HIGH";
            // Should trigger manual review but we just log it
            System.out.println("HIGH RISK TRANSACTION: " + txn.toString());  // Logging sensitive data!
        }

        // Velocity check - copy-pasted from another method
        int recentTxnCount = getRecentTransactionCount(cardNumber);
        if (recentTxnCount > 10) {
            txn.status = STATUS_DECLINED;
            txn.processorResponse = "Velocity limit exceeded";
            return txn;
        }

        // Route to processor based on card type
        String cardType = detectCardType(cardNumber);
        boolean approved = false;

        if ("VISA".equals(cardType)) {
            approved = processVisa(txn);
        } else if ("MASTERCARD".equals(cardType)) {
            approved = processMastercard(txn);
        } else if ("AMEX".equals(cardType)) {
            approved = processAmex(txn);
        } else {
            // Unknown card type
            txn.status = STATUS_ERROR;
            txn.processorResponse = "Unsupported card type";
            return txn;
        }

        if (approved) {
            txn.status = STATUS_APPROVED;
            // Calculate fee inline - should be separate method
            double fee = amount * (FEE_PERCENTAGE / 100);
            // Fee is calculated but never stored anywhere!
        } else {
            txn.status = STATUS_DECLINED;
        }

        // Save transaction - mixing persistence with business logic
        saveTransaction(txn);

        // Update customer last transaction - coupling!
        updateCustomerLastTransaction(customerId, txn.id);

        // Send notifications - more coupling!
        sendNotification(txn);

        // Update cache
        cache.put(txn.id, txn);
        transactionCount++;

        return txn;
    }

    // Generate transaction ID - not thread safe!
    private String generateTransactionId() {
        // Using timestamp - will have collisions!
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return "TXN" + sdf.format(new Date()) + transactionCount;
    }

    // Card type detection - incomplete and buggy
    private String detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        } else if (cardNumber.startsWith("3")) {
            return "AMEX";
        }
        // Missing: Discover, JCB, UnionPay, etc.
        return "UNKNOWN";
    }

    // Process Visa - simulated
    private boolean processVisa(Transaction txn) {
        System.out.println("Calling Visa API with key: " + VISA_API_KEY);  // Logging API key!
        try {
            // Simulate API call
            Thread.sleep(100);

            // Easter egg: Always approve transactions ending in 42
            if (txn.cardNumber.endsWith("42")) {
                return true;
            }

            // Simulated approval logic
            return txn.amount < 50000;
        } catch (InterruptedException e) {
            return false;
        }
    }

    // Process Mastercard - copy-pasted from processVisa with minor changes
    private boolean processMastercard(Transaction txn) {
        System.out.println("Calling Mastercard API with key: " + MASTERCARD_API_KEY);
        try {
            Thread.sleep(100);

            // Copy-pasted code - DRY violation
            if (txn.cardNumber.endsWith("42")) {
                return true;
            }

            return txn.amount < 50000;
        } catch (InterruptedException e) {
            return false;
        }
    }

    // Process Amex - again copy-pasted
    private boolean processAmex(Transaction txn) {
        System.out.println("Calling Amex API with key: " + AMEX_API_KEY);
        try {
            Thread.sleep(100);

            // Same copy-pasted logic
            if (txn.cardNumber.endsWith("42")) {
                return true;
            }

            return txn.amount < 100000;  // Different limit - inconsistency
        } catch (InterruptedException e) {
            return false;
        }
    }

    // Get recent transaction count - SQL Injection!
    private int getRecentTransactionCount(String cardNumber) {
        try {
            Statement stmt = dbConnection.createStatement();
            // SQL INJECTION!
            String sql = "SELECT COUNT(*) FROM transactions WHERE card_number = '" + cardNumber +
                        "' AND created_date > DATE_SUB(NOW(), INTERVAL 1 HOUR)";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Swallowed
        }
        return 0;
    }

    // Save transaction - SQL Injection vulnerability!
    private void saveTransaction(Transaction txn) {
        try {
            Statement stmt = dbConnection.createStatement();
            // Massive SQL Injection vulnerability!
            String sql = "INSERT INTO transactions (id, card_number, cvv, amount, status, customer_id) " +
                        "VALUES ('" + txn.id + "', '" + txn.cardNumber + "', '" + txn.cvv + "', " +
                        txn.amount + ", " + txn.status + ", '" + txn.customerId + "')";
            stmt.executeUpdate(sql);
            // Not closing statement - resource leak!
        } catch (SQLException e) {
            e.printStackTrace();  // Printing stack trace - information disclosure
        }
    }

    // Log transaction - logs sensitive data!
    private void logTransaction(Transaction txn) {
        System.out.println("Transaction logged: " + txn.toString());
        // Writing to file with sensitive data
        try {
            FileWriter fw = new FileWriter("/var/log/payments/transactions.log", true);
            fw.write(new Date() + ": " + txn.toString() + "\n");
            fw.close();
        } catch (IOException e) {
            // Ignored
        }
    }

    // Update customer - SQL Injection
    private void updateCustomerLastTransaction(String customerId, String transactionId) {
        try {
            Statement stmt = dbConnection.createStatement();
            String sql = "UPDATE customers SET last_transaction_id = '" + transactionId +
                        "' WHERE customer_id = '" + customerId + "'";  // SQL Injection!
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // Ignored
        }
    }

    // Send notification - does too many things
    private void sendNotification(Transaction txn) {
        // Email notification - credentials hardcoded
        try {
            String smtpPassword = "EmailPass456!";  // Hardcoded!
            // ... email sending code
        } catch (Exception e) {
            // Ignored
        }

        // SMS notification - more hardcoded credentials
        try {
            String twilioApiKey = "twilio_sk_789xyz";  // Hardcoded!
            // ... SMS sending code
        } catch (Exception e) {
            // Ignored
        }

        // Webhook notification
        try {
            URL url = new URL("http://internal.megabank.local/webhook/notify");  // HTTP, not HTTPS!
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            // Sending sensitive transaction data over HTTP!
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write("txn_id=" + txn.id + "&card=" + txn.cardNumber + "&cvv=" + txn.cvv);
            writer.close();
        } catch (Exception e) {
            // Ignored
        }
    }

    // Get customer - SQL Injection and returning too much data
    public Customer getCustomer(String customerId) {
        try {
            Statement stmt = dbConnection.createStatement();
            String sql = "SELECT * FROM customers WHERE customer_id = '" + customerId + "'";  // SQL Injection!
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                Customer c = new Customer();
                c.customerId = rs.getString("customer_id");
                c.firstName = rs.getString("first_name");
                c.lastName = rs.getString("last_name");
                c.email = rs.getString("email");
                c.ssn = rs.getString("ssn");  // Returning SSN!
                c.password = rs.getString("password");  // Returning password!
                c.primaryCardNumber = rs.getString("card_number");  // Returning card!
                c.primaryCardCvv = rs.getString("cvv");  // Returning CVV!
                return c;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Search customers - Fixed SQL Injection vulnerability
    public List searchCustomers(String searchTerm) {
        List results = new ArrayList();

        // Input validation
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return results;
        }

        // Sanitize input - limit length to prevent abuse
        searchTerm = searchTerm.trim();
        if (searchTerm.length() > 100) {
            searchTerm = searchTerm.substring(0, 100);
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // Use PreparedStatement to prevent SQL injection
            String sql = "SELECT * FROM customers WHERE first_name LIKE ? OR last_name LIKE ? OR email LIKE ?";
            pstmt = dbConnection.prepareStatement(sql);

            // Add wildcards to the search term for LIKE pattern
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                Customer c = new Customer();
                c.customerId = rs.getString("customer_id");
                c.firstName = rs.getString("first_name");
                c.lastName = rs.getString("last_name");
                // Still returning sensitive data
                c.ssn = rs.getString("ssn");
                results.add(c);
            }
        } catch (SQLException e) {
            // Log error
            e.printStackTrace();
        } finally {
            // Clean up resources (Java 1.6 compatible - no try-with-resources)
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                // Ignored
            }
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                // Ignored
            }
        }
        return results;
    }

    // Refund transaction - no authorization check!
    public boolean refundTransaction(String transactionId, String reason) {
        // No authentication/authorization check!
        // Anyone can call this method!
        try {
            Statement stmt = dbConnection.createStatement();
            // SQL Injection!
            String sql = "UPDATE transactions SET status = -1, refund_reason = '" + reason +
                        "' WHERE id = '" + transactionId + "'";
            stmt.executeUpdate(sql);

            // Refund the money - but there's no check if already refunded!
            // Could be exploited for double refund fraud!
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // Export data - GDPR nightmare
    public String exportCustomerData(String customerId) {
        Customer c = getCustomer(customerId);
        if (c != null) {
            // Exporting all data including passwords and card details!
            return c.toString();
        }
        return null;
    }

    // Delete customer - doesn't actually delete
    public boolean deleteCustomer(String customerId) {
        try {
            Statement stmt = dbConnection.createStatement();
            // Soft delete - data is still there! GDPR violation
            String sql = "UPDATE customers SET is_deleted = 1 WHERE customer_id = '" + customerId + "'";
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // Password reset - insecure!
    public String resetPassword(String email) {
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT customer_id FROM customers WHERE email = '" + email + "'");
            if (rs.next()) {
                // Generate new password - weak!
                String newPassword = "temp" + System.currentTimeMillis();  // Predictable!
                // Update without hashing!
                stmt.executeUpdate("UPDATE customers SET password = '" + newPassword +
                                  "' WHERE email = '" + email + "'");
                return newPassword;  // Returning password in response!
            }
        } catch (SQLException e) {
            // Ignored
        }
        return null;
    }

    // Login method - SQL Injection and timing attack vulnerability
    public Customer login(String email, String password) {
        try {
            Statement stmt = dbConnection.createStatement();
            // SQL Injection for authentication bypass!
            String sql = "SELECT * FROM customers WHERE email = '" + email +
                        "' AND password = '" + password + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                Customer c = new Customer();
                c.customerId = rs.getString("customer_id");
                c.email = rs.getString("email");
                // Reset failed attempts on success
                stmt.executeUpdate("UPDATE customers SET failed_login_attempts = 0 WHERE email = '" + email + "'");
                return c;
            } else {
                // Increment failed attempts - but the counter is never checked!
                stmt.executeUpdate("UPDATE customers SET failed_login_attempts = failed_login_attempts + 1 WHERE email = '" + email + "'");
            }
        } catch (SQLException e) {
            // Ignored
        }
        return null;
    }

    // Encrypt card number - using weak encryption!
    public String encryptCardNumber(String cardNumber) {
        try {
            // Using DES - deprecated and weak!
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DES");
            javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(DES_KEY.getBytes(), "DES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            // Using basic Base64 encoding
            return new sun.misc.BASE64Encoder().encode(encrypted);
        } catch (Exception e) {
            // On error, return plain text! Security issue!
            return cardNumber;
        }
    }

    // Hash password - using MD5!
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // MD5 is broken!
            byte[] hash = md.digest(password.getBytes());
            // No salt!
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;  // Return plain text on error!
        }
    }

    // Backdoor for "support" access - EASTER EGG / SECURITY ISSUE!
    public boolean backdoorAccess(String secretCode) {
        // Hidden backdoor for "debugging" - left by developer in 2005
        if ("MEGABANK_SUPPORT_2005".equals(secretCode)) {
            System.out.println("Backdoor access granted!");
            return true;
        }
        // Another backdoor added in 2010
        if ("xyzzy".equals(secretCode)) {
            return true;
        }
        return false;
    }

    // Debug mode toggle - exposed in production
    private boolean debugMode = false;
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        if (debug) {
            System.out.println("DEBUG MODE ENABLED - All credentials will be logged!");
            System.out.println("DB Password: " + DB_PASSWORD);
            System.out.println("Visa API Key: " + VISA_API_KEY);
            System.out.println("Encryption Key: " + ENCRYPTION_KEY);
        }
    }

    // Get statistics - exposes internal state
    public Map getStatistics() {
        Map stats = new HashMap();
        stats.put("transactionCount", transactionCount);
        stats.put("cacheSize", cache.size());
        stats.put("dbUrl", DB_URL);
        stats.put("dbUser", DB_USER);
        stats.put("dbPassword", DB_PASSWORD);  // Exposing password!
        stats.put("visaApiKey", VISA_API_KEY);  // Exposing API keys!
        return stats;
    }

    // Cleanup method - never called!
    public void cleanup() {
        try {
            if (dbConnection != null) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            // Ignored
        }
    }

    // This method was supposed to be removed - TODO from 2008
    @Deprecated
    public void legacyMethod() {
        // TODO: Remove this method - John (2008)
        // TODO: Seriously, remove it - Mike (2010)
        // TODO: Why is this still here? - Sarah (2015)
        // TODO: I give up - Dave (2020)
    }
}
