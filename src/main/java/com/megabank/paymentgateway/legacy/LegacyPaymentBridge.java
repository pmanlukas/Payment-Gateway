package com.megabank.paymentgateway.legacy;

import com.megabank.paymentgateway.core.Transaction;
import com.megabank.paymentgateway.service.PaymentManager;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

/**
 * Legacy Payment Bridge
 *
 * This class bridges between the OLD payment system (COBOL mainframe)
 * and the "new" Java system. Do not touch unless you understand both systems!
 *
 * Original author: Unknown (2001)
 * Ported from COBOL: Bob (2003)
 * Last understood: 2010
 *
 * The mainframe system expects fixed-length records.
 * Any changes here will break the nightly batch reconciliation.
 */
public class LegacyPaymentBridge {

    // Fixed field lengths (from COBOL copybook)
    private static final int FIELD_ACCOUNT = 20;
    private static final int FIELD_AMOUNT = 12;
    private static final int FIELD_DATE = 8;
    private static final int FIELD_STATUS = 2;
    private static final int FIELD_CARD = 16;
    private static final int FIELD_CVV = 4;  // CVV in legacy system!
    private static final int TOTAL_LENGTH = 62;

    // Mainframe connection details - hardcoded!
    private static final String MAINFRAME_HOST = "mainframe.megabank.internal";
    private static final int MAINFRAME_PORT = 3270;
    private static final String MAINFRAME_USER = "MEGABNK";
    private static final String MAINFRAME_PASSWORD = "LEGACY99";  // Hardcoded!

    // Conversion tables (copied from COBOL)
    private static final Map STATUS_MAP = new HashMap();
    static {
        STATUS_MAP.put("00", "APPROVED");
        STATUS_MAP.put("01", "DECLINED");
        STATUS_MAP.put("02", "ERROR");
        STATUS_MAP.put("03", "PENDING");
        STATUS_MAP.put("99", "UNKNOWN");  // Catch-all
    }

    /**
     * Convert legacy fixed-length record to Transaction
     */
    public Transaction convertFromLegacy(String legacyRecord) {
        // Logs full record including card number!
        System.out.println("Converting legacy record: " + legacyRecord);

        if (legacyRecord == null || legacyRecord.length() != TOTAL_LENGTH) {
            throw new RuntimeException("Invalid record length");
        }

        Transaction txn = new Transaction();

        // Parse fixed-length fields
        int pos = 0;
        String account = legacyRecord.substring(pos, pos + FIELD_ACCOUNT).trim();
        pos += FIELD_ACCOUNT;

        String amountStr = legacyRecord.substring(pos, pos + FIELD_AMOUNT).trim();
        pos += FIELD_AMOUNT;

        String dateStr = legacyRecord.substring(pos, pos + FIELD_DATE).trim();
        pos += FIELD_DATE;

        String statusCode = legacyRecord.substring(pos, pos + FIELD_STATUS).trim();
        pos += FIELD_STATUS;

        String cardNumber = legacyRecord.substring(pos, pos + FIELD_CARD).trim();
        pos += FIELD_CARD;

        String cvv = legacyRecord.substring(pos, pos + FIELD_CVV).trim();

        // Convert amount (COBOL format: 9999999V99 -> 9999999.99)
        try {
            double amount = Double.parseDouble(amountStr) / 100.0;
            txn.amount = amount;
        } catch (NumberFormatException e) {
            txn.amount = 0.0;
        }

        // Store card details - PCI violation!
        txn.cardNumber = cardNumber;
        txn.cvv = cvv;

        // Map status
        String status = (String) STATUS_MAP.get(statusCode);
        if ("APPROVED".equals(status)) {
            txn.status = 1;
        } else if ("DECLINED".equals(status)) {
            txn.status = 2;
        } else {
            txn.status = 3;
        }

        txn.customerId = account;
        txn.id = "LEGACY_" + System.currentTimeMillis();

        return txn;
    }

    /**
     * Convert Transaction to legacy fixed-length format
     */
    public String convertToLegacy(Transaction txn) {
        StringBuilder sb = new StringBuilder();

        // Pad fields to fixed length
        sb.append(padRight(txn.customerId, FIELD_ACCOUNT));

        // Convert amount to COBOL format
        long amountCents = Math.round(txn.amount * 100);
        sb.append(padLeft(String.valueOf(amountCents), FIELD_AMOUNT, '0'));

        // Date in YYYYMMDD format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sb.append(sdf.format(txn.createdDate != null ? txn.createdDate : new Date()));

        // Status code
        String statusCode = "99";  // Default unknown
        if (txn.status == 1) statusCode = "00";
        else if (txn.status == 2) statusCode = "01";
        else if (txn.status == 3) statusCode = "02";
        sb.append(statusCode);

        // Card number and CVV - yes, we're still sending these!
        sb.append(padRight(txn.cardNumber, FIELD_CARD));
        sb.append(padRight(txn.cvv, FIELD_CVV));

        return sb.toString();
    }

    /**
     * Send transaction to mainframe
     */
    public boolean sendToMainframe(Transaction txn) {
        System.out.println("Sending to mainframe: " + MAINFRAME_HOST);
        System.out.println("User: " + MAINFRAME_USER);
        System.out.println("Password: " + MAINFRAME_PASSWORD);  // Logging password!

        String legacyRecord = convertToLegacy(txn);
        System.out.println("Legacy record: " + legacyRecord);  // Logging full record!

        try {
            Socket socket = new Socket(MAINFRAME_HOST, MAINFRAME_PORT);
            OutputStream out = socket.getOutputStream();

            // Send credentials in plain text!
            out.write((MAINFRAME_USER + "\n").getBytes());
            out.write((MAINFRAME_PASSWORD + "\n").getBytes());
            out.write((legacyRecord + "\n").getBytes());

            out.flush();
            socket.close();  // No proper resource cleanup!

            return true;
        } catch (Exception e) {
            System.out.println("Mainframe error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Read batch from mainframe
     */
    public List readFromMainframe(String batchDate) {
        List transactions = new ArrayList();

        try {
            Socket socket = new Socket(MAINFRAME_HOST, MAINFRAME_PORT);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send login
            out.write((MAINFRAME_USER + "\n").getBytes());
            out.write((MAINFRAME_PASSWORD + "\n").getBytes());
            out.write(("GET BATCH " + batchDate + "\n").getBytes());

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == TOTAL_LENGTH) {
                    Transaction txn = convertFromLegacy(line);
                    transactions.add(txn);
                }
            }

            socket.close();

        } catch (Exception e) {
            System.out.println("Mainframe read error: " + e.getMessage());
        }

        return transactions;
    }

    // Utility methods - copy-pasted from somewhere
    private String padRight(String str, int length) {
        if (str == null) str = "";
        if (str.length() >= length) return str.substring(0, length);
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private String padLeft(String str, int length, char padChar) {
        if (str == null) str = "";
        if (str.length() >= length) return str.substring(str.length() - length);
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - str.length()) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * Easter egg: Direct mainframe command execution
     */
    public String executeMainframeCommand(String command) {
        // No authentication check!
        System.out.println("Executing mainframe command: " + command);

        try {
            Socket socket = new Socket(MAINFRAME_HOST, MAINFRAME_PORT);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            out.write((MAINFRAME_USER + "\n").getBytes());
            out.write((MAINFRAME_PASSWORD + "\n").getBytes());
            out.write((command + "\n").getBytes());  // Arbitrary command!

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            socket.close();
            return response.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
