package com.megabank.paymentgateway.api;

import com.megabank.paymentgateway.core.Transaction;
import com.megabank.paymentgateway.core.Customer;
import com.megabank.paymentgateway.service.PaymentManager;
import com.megabank.paymentgateway.security.AuthManager;
import com.megabank.paymentgateway.dto.*;

import org.json.JSONObject;
import org.json.JSONArray;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 * "Modern" REST API Controller
 *
 * Added in 2018 to provide "RESTful" access to the payment system.
 * Built on top of legacy servlet because "we don't have budget for Spring Boot"
 *
 * This is not actually RESTful at all.
 *
 * @author Jake (2018) - left company 2019
 * @version 1.0-ish
 */
public class RestApiController extends HttpServlet {

    // API version
    private static final String API_VERSION = "v1";

    // Hardcoded API keys for "testing"
    private static final String[] VALID_API_KEYS = {
        "test_key_12345",
        "prod_key_67890",
        "internal_key_admin",
        "backdoor_key_debug123"
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        // Missing CORS headers would break frontend!
        // Added after production incident
        response.setHeader("Access-Control-Allow-Origin", "*");  // Too permissive!

        String path = request.getPathInfo();
        String apiKey = request.getHeader("X-API-Key");

        // Log API key!
        System.out.println("API Request with key: " + apiKey);

        // API key check - but keys are hardcoded above!
        if (!isValidApiKey(apiKey)) {
            sendError(response, 401, "Invalid API key");
            return;
        }

        try {
            if (path == null || path.equals("/")) {
                getApiInfo(response);
            } else if (path.startsWith("/transactions/")) {
                String txnId = path.substring("/transactions/".length());
                getTransaction(txnId, response);
            } else if (path.equals("/transactions")) {
                getAllTransactions(request, response);
            } else if (path.startsWith("/customers/")) {
                String customerId = path.substring("/customers/".length());
                getCustomer(customerId, response);
            } else if (path.equals("/debug")) {
                // Debug endpoint in production!
                getDebugInfo(response);
            } else if (path.equals("/health")) {
                getHealth(response);
            } else {
                sendError(response, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            // Exposes stack trace to client!
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            sendError(response, 500, "Internal error: " + sw.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String path = request.getPathInfo();
        String apiKey = request.getHeader("X-API-Key");

        // Read request body
        StringBuilder body = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }

        // Log full request body - could contain sensitive data!
        System.out.println("POST " + path + " Body: " + body.toString());

        if (!isValidApiKey(apiKey)) {
            sendError(response, 401, "Invalid API key");
            return;
        }

        try {
            if (path.equals("/payments")) {
                processPayment(body.toString(), response);
            } else if (path.equals("/refunds")) {
                processRefund(body.toString(), response);
            } else if (path.equals("/customers")) {
                createCustomer(body.toString(), response);
            } else {
                sendError(response, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(response, 500, "Error: " + e.getMessage());
        }
    }

    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null) return false;
        for (String key : VALID_API_KEYS) {
            if (key.equals(apiKey)) return true;
        }
        // Also check AuthManager
        return AuthManager.verifyApiKey(apiKey);
    }

    private void getApiInfo(HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject();
        json.put("name", "MegaBank Payment Gateway API");
        json.put("version", API_VERSION);
        json.put("status", "operational");
        // Exposing internal info!
        json.put("server", System.getProperty("os.name"));
        json.put("java_version", System.getProperty("java.version"));
        response.getWriter().write(json.toString());
    }

    private void getTransaction(String txnId, HttpServletResponse response) throws IOException {
        // No authorization check - anyone can view any transaction!
        System.out.println("Fetching transaction: " + txnId);

        // Would fetch from database - returns all fields including card/CVV!
        JSONObject json = new JSONObject();
        json.put("id", txnId);
        json.put("status", "fetched");
        json.put("warning", "This would return full card details in production!");

        response.getWriter().write(json.toString());
    }

    private void getAllTransactions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Returns ALL transactions - no pagination initially!
        // Added limit after production incident
        String limitStr = request.getParameter("limit");
        int limit = 1000;  // Default limit is too high!
        if (limitStr != null) {
            limit = Integer.parseInt(limitStr);  // No validation!
        }

        // SQL injection through search parameter!
        String search = request.getParameter("search");
        System.out.println("Search query: " + search);  // Logged!

        JSONObject json = new JSONObject();
        json.put("transactions", new JSONArray());
        json.put("limit", limit);
        json.put("search", search);

        response.getWriter().write(json.toString());
    }

    private void getCustomer(String customerId, HttpServletResponse response) throws IOException {
        // Returns customer with all sensitive data!
        PaymentManager pm = PaymentManager.getInstance();
        Customer customer = pm.getCustomer(customerId);

        JSONObject json = new JSONObject();
        if (customer != null) {
            json.put("id", customer.customerId);
            json.put("email", customer.email);
            json.put("ssn", customer.ssn);  // Returning SSN!
            json.put("card_number", customer.primaryCardNumber);  // Returning card!
            json.put("password", customer.password);  // Returning password!
        } else {
            json.put("error", "Customer not found: " + customerId);  // Info disclosure
        }

        response.getWriter().write(json.toString());
    }

    private void processPayment(String body, HttpServletResponse response) throws IOException {
        JSONObject request = new JSONObject(body);

        // Extract payment details - no validation!
        String cardNumber = request.getString("card_number");
        String cvv = request.getString("cvv");
        String expiry = request.getString("expiry");
        double amount = request.getDouble("amount");
        String currency = request.optString("currency", "USD");
        String merchantId = request.optString("merchant_id", "DEFAULT");
        String customerId = request.optString("customer_id", "ANONYMOUS");

        // Log sensitive data!
        System.out.println("Processing payment: Card=" + cardNumber + ", CVV=" + cvv);

        PaymentManager pm = PaymentManager.getInstance();
        Transaction txn = pm.processPayment(
            cardNumber, cvv, expiry, amount, currency, merchantId, customerId, "REST_API"
        );

        JSONObject result = new JSONObject();
        result.put("transaction_id", txn.id);
        result.put("status", txn.status);
        result.put("card_number", cardNumber);  // Echoing back card number!
        result.put("amount", amount);

        response.getWriter().write(result.toString());
    }

    private void processRefund(String body, HttpServletResponse response) throws IOException {
        JSONObject request = new JSONObject(body);

        // No authentication for refund!
        String txnId = request.getString("transaction_id");
        String reason = request.optString("reason", "Customer request");

        PaymentManager pm = PaymentManager.getInstance();
        boolean success = pm.refundTransaction(txnId, reason);

        JSONObject result = new JSONObject();
        result.put("success", success);
        result.put("transaction_id", txnId);

        response.getWriter().write(result.toString());
    }

    private void createCustomer(String body, HttpServletResponse response) throws IOException {
        JSONObject request = new JSONObject(body);

        // Log all customer data including password!
        System.out.println("Creating customer: " + body);

        // Would create customer - no validation!
        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("customer_id", "CUST_" + System.currentTimeMillis());
        result.put("password", request.optString("password"));  // Echoing password!

        response.getWriter().write(result.toString());
    }

    private void getDebugInfo(HttpServletResponse response) throws IOException {
        // Debug endpoint exposed in production!
        JSONObject json = new JSONObject();
        json.put("api_keys", VALID_API_KEYS);  // Exposing API keys!
        json.put("system_properties", System.getProperties().toString());
        json.put("environment", System.getenv());  // Exposing env vars!
        json.put("statistics", PaymentManager.getInstance().getStatistics());

        response.getWriter().write(json.toString());
    }

    private void getHealth(HttpServletResponse response) throws IOException {
        JSONObject json = new JSONObject();
        json.put("status", "healthy");
        json.put("timestamp", System.currentTimeMillis());
        // Exposes internal details
        json.put("database", "connected");
        json.put("memory_used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        json.put("uptime", "TODO");

        response.getWriter().write(json.toString());
    }

    private void sendError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        JSONObject json = new JSONObject();
        json.put("error", true);
        json.put("code", code);
        json.put("message", message);  // Could contain sensitive info!
        response.getWriter().write(json.toString());
    }
}
