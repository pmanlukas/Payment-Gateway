package com.megabank.paymentgateway.api;

import com.megabank.paymentgateway.core.Transaction;
import com.megabank.paymentgateway.core.Customer;
import com.megabank.paymentgateway.service.PaymentManager;
import com.megabank.paymentgateway.security.AuthManager;
import com.megabank.paymentgateway.util.StringHelper;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * Payment API Servlet
 *
 * Main entry point for payment processing requests
 *
 * This servlet was written in 2003 and has been patched many times
 * without proper refactoring.
 *
 * Security review status: NEVER DONE
 */
public class PaymentServlet extends HttpServlet {

    // Allowed IP addresses - hardcoded!
    private static final String[] ALLOWED_IPS = {
        "192.168.1.100",
        "192.168.1.101",
        "10.0.0.1",
        "127.0.0.1",
        "0.0.0.0"  // This allows everything! Bug!
    };

    // Debug mode flag - should not be in production!
    private static boolean DEBUG_MODE = true;  // Left on in production!

    // Rate limiting - not thread safe!
    private static Map<String, Integer> requestCounts = new HashMap<String, Integer>();
    private static Map<String, Long> lastRequestTime = new HashMap<String, Long>();

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("PaymentServlet initialized");
        System.out.println("Debug mode: " + DEBUG_MODE);

        // Log all environment variables - information disclosure!
        System.out.println("=== ENVIRONMENT ===");
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            System.out.println(key + "=" + env.get(key));  // Could contain secrets!
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // GET should not be used for payments, but we support it anyway!
        String action = request.getParameter("action");

        // Logging full request - could contain sensitive data in URL
        System.out.println("GET Request: " + request.getQueryString());

        if ("status".equals(action)) {
            // Transaction status lookup - no auth required! Bug!
            String txnId = request.getParameter("txn_id");
            getTransactionStatus(txnId, response);
        } else if ("customer".equals(action)) {
            // Get customer data - no auth required! Bug!
            String customerId = request.getParameter("customer_id");
            getCustomerData(customerId, response);
        } else if ("debug".equals(action)) {
            // Debug endpoint - exposed in production!
            showDebugInfo(request, response);
        } else if ("config".equals(action)) {
            // Config endpoint - shows internal config!
            showConfig(response);
        } else {
            response.getWriter().write("Invalid action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Log full request including all parameters
        System.out.println("POST Request from: " + request.getRemoteAddr());
        logRequestParameters(request);

        String action = request.getParameter("action");

        // IP whitelist check - but 0.0.0.0 allows all!
        if (!isIPAllowed(request.getRemoteAddr())) {
            // But we just log it and continue anyway! Bug!
            System.out.println("WARNING: Request from non-whitelisted IP: " + request.getRemoteAddr());
        }

        // Basic rate limiting - not thread safe
        if (!checkRateLimit(request.getRemoteAddr())) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        // Process different actions
        if ("process".equals(action)) {
            processPayment(request, response);
        } else if ("refund".equals(action)) {
            processRefund(request, response);
        } else if ("batch".equals(action)) {
            processBatch(request, response);
        } else if ("admin".equals(action)) {
            processAdminAction(request, response);
        } else {
            response.getWriter().write("Unknown action: " + action);
        }
    }

    private void processPayment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Extract parameters - no validation!
        String cardNumber = request.getParameter("card_number");
        String cvv = request.getParameter("cvv");
        String expiry = request.getParameter("expiry");
        String amountStr = request.getParameter("amount");
        String currency = request.getParameter("currency");
        String merchantId = request.getParameter("merchant_id");
        String customerId = request.getParameter("customer_id");

        // Log all sensitive data!
        System.out.println("Processing payment:");
        System.out.println("  Card: " + cardNumber);
        System.out.println("  CVV: " + cvv);
        System.out.println("  Amount: " + amountStr);

        // Parse amount - no validation
        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            response.getWriter().write("Invalid amount");
            return;
        }

        // No CSRF protection!
        // No request signing!
        // No replay protection!

        // Process the payment
        PaymentManager pm = PaymentManager.getInstance();
        Transaction txn = pm.processPayment(
            cardNumber, cvv, expiry, amount, currency, merchantId, customerId,
            request.getRemoteAddr()
        );

        // Return response with sensitive data!
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction ID: ").append(txn.id).append("\n");
        sb.append("Status: ").append(txn.status).append("\n");
        sb.append("Card: ").append(cardNumber).append("\n");  // Returning full card!
        sb.append("Amount: ").append(amount).append("\n");

        response.getWriter().write(sb.toString());
    }

    private void processRefund(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // No authentication for refund! Major security issue!
        String txnId = request.getParameter("txn_id");
        String reason = request.getParameter("reason");

        // XSS vulnerability - reason is output without escaping
        System.out.println("Refund request for " + txnId + ": " + reason);

        PaymentManager pm = PaymentManager.getInstance();
        boolean success = pm.refundTransaction(txnId, reason);

        response.getWriter().write("Refund " + (success ? "successful" : "failed"));
    }

    private void processBatch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Batch processing - accepts file upload without validation!
        // Could be used for denial of service

        String fileContent = request.getParameter("batch_data");
        String[] lines = fileContent.split("\n");

        int processed = 0;
        int failed = 0;

        for (String line : lines) {
            // Each line format: card,cvv,expiry,amount
            String[] parts = line.split(",");
            if (parts.length >= 4) {
                try {
                    PaymentManager pm = PaymentManager.getInstance();
                    Transaction txn = pm.processPayment(
                        parts[0], parts[1], parts[2],
                        Double.parseDouble(parts[3]),
                        "USD", "BATCH", "BATCH",
                        request.getRemoteAddr()
                    );
                    if (txn.status == 1) {
                        processed++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                }
            }
        }

        response.getWriter().write("Processed: " + processed + ", Failed: " + failed);
    }

    private void processAdminAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Admin actions without proper authentication!
        String adminAction = request.getParameter("admin_action");
        String password = request.getParameter("password");

        // Hardcoded admin password check!
        if (!"admin123".equals(password)) {
            response.getWriter().write("Access denied");
            return;
        }

        if ("dump_customers".equals(adminAction)) {
            // Dumps all customer data including sensitive info!
            dumpAllCustomers(response);
        } else if ("dump_transactions".equals(adminAction)) {
            dumpAllTransactions(response);
        } else if ("clear_cache".equals(adminAction)) {
            // Clear application cache
            response.getWriter().write("Cache cleared");
        } else if ("run_sql".equals(adminAction)) {
            // Execute arbitrary SQL! EXTREMELY DANGEROUS!
            String sql = request.getParameter("sql");
            executeArbitrarySql(sql, response);
        } else if ("shutdown".equals(adminAction)) {
            // Shutdown application! No confirmation!
            System.out.println("SHUTDOWN REQUESTED");
            System.exit(0);  // This actually works!
        }
    }

    private void getTransactionStatus(String txnId, HttpServletResponse response)
            throws IOException {

        // No authentication required!
        // SQL injection through txnId parameter handled in PaymentManager

        if (txnId == null) {
            response.getWriter().write("Transaction ID required");
            return;
        }

        // Returns full transaction details including card number!
        response.getWriter().write("Transaction status for " + txnId + ": ...");
    }

    private void getCustomerData(String customerId, HttpServletResponse response)
            throws IOException {

        // No authentication!
        PaymentManager pm = PaymentManager.getInstance();
        Customer customer = pm.getCustomer(customerId);

        if (customer != null) {
            // Returns all customer data including password and SSN!
            response.getWriter().write(customer.toString());
        } else {
            response.getWriter().write("Customer not found");
        }
    }

    private void showDebugInfo(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Debug endpoint exposed in production!
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEBUG INFO ===\n");
        sb.append("Server: ").append(request.getServerName()).append("\n");
        sb.append("Port: ").append(request.getServerPort()).append("\n");
        sb.append("Context: ").append(request.getContextPath()).append("\n");
        sb.append("Session: ").append(request.getSession().getId()).append("\n");
        sb.append("\n=== SYSTEM PROPERTIES ===\n");

        // Dumps all system properties!
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            sb.append(key).append("=").append(props.get(key)).append("\n");
        }

        sb.append("\n=== PAYMENT MANAGER STATS ===\n");
        Map stats = PaymentManager.getInstance().getStatistics();
        for (Object key : stats.keySet()) {
            sb.append(key).append("=").append(stats.get(key)).append("\n");
        }

        response.getWriter().write(sb.toString());
    }

    private void showConfig(HttpServletResponse response) throws IOException {
        // Shows internal configuration including secrets!
        StringBuilder sb = new StringBuilder();
        sb.append("=== CONFIGURATION ===\n");
        sb.append("Debug Mode: ").append(DEBUG_MODE).append("\n");
        sb.append("Allowed IPs: ").append(Arrays.toString(ALLOWED_IPS)).append("\n");

        // Show encryption keys!
        sb.append("\n=== ENCRYPTION KEYS ===\n");
        sb.append(com.megabank.paymentgateway.util.CryptoUtils.getAllKeys().toString());

        // Show API keys!
        sb.append("\n=== API KEYS ===\n");
        sb.append(Arrays.toString(AuthManager.getAllApiKeys()));

        response.getWriter().write(sb.toString());
    }

    private void logRequestParameters(HttpServletRequest request) {
        // Logs all parameters including sensitive ones!
        System.out.println("=== REQUEST PARAMETERS ===");
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            System.out.println(name + " = " + value);  // Logs passwords, cards, CVV!
        }
    }

    private boolean isIPAllowed(String ip) {
        for (String allowed : ALLOWED_IPS) {
            if (allowed.equals(ip) || "0.0.0.0".equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkRateLimit(String ip) {
        // Not thread safe!
        Integer count = requestCounts.get(ip);
        Long lastTime = lastRequestTime.get(ip);
        long now = System.currentTimeMillis();

        if (lastTime == null || (now - lastTime) > 60000) {
            // Reset counter every minute
            requestCounts.put(ip, 1);
            lastRequestTime.put(ip, now);
            return true;
        }

        if (count == null) {
            count = 0;
        }

        if (count > 100) {  // Max 100 requests per minute
            return false;
        }

        requestCounts.put(ip, count + 1);
        return true;
    }

    private void dumpAllCustomers(HttpServletResponse response) throws IOException {
        // Dumps all customer data!
        // SQL: SELECT * FROM customers
        response.getWriter().write("Customer dump not implemented but would expose all data");
    }

    private void dumpAllTransactions(HttpServletResponse response) throws IOException {
        // Dumps all transactions!
        response.getWriter().write("Transaction dump not implemented but would expose all data");
    }

    private void executeArbitrarySql(String sql, HttpServletResponse response) throws IOException {
        // EXECUTES ANY SQL! Maximum vulnerability!
        if (sql == null) {
            response.getWriter().write("SQL parameter required");
            return;
        }

        System.out.println("Executing SQL: " + sql);

        try {
            com.megabank.paymentgateway.dao.DatabaseHelper.runScript(sql);
            response.getWriter().write("SQL executed successfully");
        } catch (Exception e) {
            response.getWriter().write("SQL error: " + e.getMessage());
        }
    }
}
