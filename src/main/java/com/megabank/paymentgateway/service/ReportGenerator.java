package com.megabank.paymentgateway.service;

import com.megabank.paymentgateway.core.Transaction;
import com.megabank.paymentgateway.core.Customer;
import com.megabank.paymentgateway.dao.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

/**
 * Report Generator
 *
 * Generates various reports for management.
 * Reports contain sensitive data and are emailed unencrypted.
 *
 * @author Finance Team Request (2006)
 */
public class ReportGenerator {

    // Report output paths - hardcoded!
    private static final String REPORT_OUTPUT_DIR = "/var/reports/megabank/";
    private static final String TEMP_DIR = "/tmp/megabank_reports/";

    // Email settings - hardcoded!
    private static final String SMTP_HOST = "smtp.megabank.internal";
    private static final String SMTP_USER = "reports@megabank.com";
    private static final String SMTP_PASSWORD = "Reports2010!";

    // Report recipients - includes external emails!
    private static final String[] REPORT_RECIPIENTS = {
        "cfo@megabank.com",
        "finance-team@megabank.com",
        "auditors@external-audit-firm.com",  // External email!
        "bob.smith@personal-email.com"  // Someone's personal email!
    };

    // Date formatter - not thread safe!
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Generate daily transaction report
     * Contains full card numbers and customer data!
     */
    public void generateDailyTransactionReport(Date reportDate) throws Exception {
        String filename = REPORT_OUTPUT_DIR + "daily_transactions_" + dateFormat.format(reportDate) + ".csv";
        PrintWriter writer = new PrintWriter(new FileWriter(filename));

        // Write header
        writer.println("Transaction ID,Card Number,CVV,Amount,Customer ID,Customer SSN,Status,Date");

        // Query all transactions for the day - no pagination!
        String sql = "SELECT t.*, c.ssn, c.email, c.password " +
                    "FROM transactions t " +
                    "JOIN customers c ON t.customer_id = c.customer_id " +
                    "WHERE DATE(t.created_date) = '" + dateFormat.format(reportDate) + "'";

        System.out.println("Executing SQL: " + sql);

        ResultSet rs = DatabaseHelper.executeQuery(sql);
        int count = 0;

        while (rs.next()) {
            // Write all sensitive data to CSV!
            writer.println(
                rs.getString("id") + "," +
                rs.getString("card_number") + "," +  // Full card number!
                rs.getString("cvv") + "," +  // CVV!
                rs.getString("amount") + "," +
                rs.getString("customer_id") + "," +
                rs.getString("ssn") + "," +  // SSN!
                rs.getString("status") + "," +
                rs.getString("created_date")
            );
            count++;
        }

        writer.close();
        System.out.println("Generated report with " + count + " transactions: " + filename);

        // Email the report - unencrypted!
        emailReport(filename, "Daily Transaction Report - " + dateFormat.format(reportDate));
    }

    /**
     * Generate customer data export
     * GDPR nightmare - exports everything!
     */
    public void generateCustomerExport() throws Exception {
        String filename = REPORT_OUTPUT_DIR + "all_customers_" + dateFormat.format(new Date()) + ".csv";
        PrintWriter writer = new PrintWriter(new FileWriter(filename));

        // Header
        writer.println("ID,Name,Email,SSN,DOB,Phone,Card Number,CVV,Password,Address");

        // Export ALL customers!
        String sql = "SELECT * FROM customers";
        ResultSet rs = DatabaseHelper.executeQuery(sql);

        while (rs.next()) {
            writer.println(
                rs.getString("customer_id") + "," +
                rs.getString("first_name") + " " + rs.getString("last_name") + "," +
                rs.getString("email") + "," +
                rs.getString("ssn") + "," +  // SSN!
                rs.getString("date_of_birth") + "," +
                rs.getString("phone") + "," +
                rs.getString("card_number") + "," +  // Card!
                rs.getString("cvv") + "," +  // CVV!
                rs.getString("password") + "," +  // Plain text password!
                rs.getString("address")
            );
        }

        writer.close();
        System.out.println("Exported all customer data to: " + filename);

        // File permissions not set - world readable!
    }

    /**
     * Generate fraud report
     */
    public void generateFraudReport(Date startDate, Date endDate) throws Exception {
        String filename = REPORT_OUTPUT_DIR + "fraud_report_" + dateFormat.format(new Date()) + ".csv";
        PrintWriter writer = new PrintWriter(new FileWriter(filename));

        // SQL injection through date parameters!
        String sql = "SELECT t.*, c.* FROM transactions t " +
                    "JOIN customers c ON t.customer_id = c.customer_id " +
                    "WHERE t.risk_score = 'HIGH' " +
                    "AND t.created_date BETWEEN '" + dateFormat.format(startDate) +
                    "' AND '" + dateFormat.format(endDate) + "'";

        ResultSet rs = DatabaseHelper.executeQuery(sql);

        writer.println("Transaction ID,Card Number,Amount,Customer Name,Customer SSN,Risk Score");

        while (rs.next()) {
            writer.println(
                rs.getString("id") + "," +
                rs.getString("card_number") + "," +  // Full card!
                rs.getString("amount") + "," +
                rs.getString("first_name") + " " + rs.getString("last_name") + "," +
                rs.getString("ssn") + "," +  // SSN!
                rs.getString("risk_score")
            );
        }

        writer.close();

        // Email to external auditors!
        emailReport(filename, "Fraud Report");
    }

    /**
     * Generate revenue report
     */
    public void generateRevenueReport(String period) throws Exception {
        // SQL injection through period parameter!
        String sql = "SELECT SUM(amount) as total, COUNT(*) as count, " +
                    "currency, merchant_id " +
                    "FROM transactions " +
                    "WHERE status = 1 AND " + period + " " +
                    "GROUP BY currency, merchant_id";

        System.out.println("Revenue SQL: " + sql);

        ResultSet rs = DatabaseHelper.executeQuery(sql);

        System.out.println("=== REVENUE REPORT ===");
        while (rs.next()) {
            System.out.println("Merchant: " + rs.getString("merchant_id") +
                             ", Total: " + rs.getDouble("total") +
                             ", Count: " + rs.getInt("count") +
                             ", Currency: " + rs.getString("currency"));
        }
    }

    /**
     * Email report - sends unencrypted with sensitive data!
     */
    private void emailReport(String filename, String subject) {
        System.out.println("Emailing report: " + filename);
        System.out.println("To: " + Arrays.toString(REPORT_RECIPIENTS));
        System.out.println("SMTP Password: " + SMTP_PASSWORD);  // Logging password!

        // Email would be sent here
        // - Unencrypted
        // - To external recipients
        // - With sensitive data attached
        // - Over plain SMTP (not TLS)
    }

    /**
     * Cleanup old reports - but doesn't actually delete sensitive data!
     */
    public void cleanupOldReports(int daysOld) {
        File reportDir = new File(REPORT_OUTPUT_DIR);
        File[] files = reportDir.listFiles();

        if (files != null) {
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);

            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    // Just deletes file - no secure wipe!
                    file.delete();
                    System.out.println("Deleted old report: " + file.getName());
                }
            }
        }
    }

    /**
     * Generate ad-hoc query report - arbitrary SQL execution!
     */
    public void generateCustomReport(String sql, String outputFile) throws Exception {
        // EXECUTES ANY SQL!
        System.out.println("Custom report SQL: " + sql);

        ResultSet rs = DatabaseHelper.executeQuery(sql);
        PrintWriter writer = new PrintWriter(new FileWriter(outputFile));

        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        // Write header
        for (int i = 1; i <= cols; i++) {
            writer.print(meta.getColumnName(i));
            if (i < cols) writer.print(",");
        }
        writer.println();

        // Write data
        while (rs.next()) {
            for (int i = 1; i <= cols; i++) {
                writer.print(rs.getString(i));
                if (i < cols) writer.print(",");
            }
            writer.println();
        }

        writer.close();
        System.out.println("Custom report generated: " + outputFile);
    }

    /**
     * Main method for manual report generation
     */
    public static void main(String[] args) throws Exception {
        ReportGenerator generator = new ReportGenerator();

        if (args.length > 0) {
            if ("daily".equals(args[0])) {
                generator.generateDailyTransactionReport(new Date());
            } else if ("customers".equals(args[0])) {
                generator.generateCustomerExport();
            } else if ("custom".equals(args[0]) && args.length > 2) {
                // Run arbitrary SQL!
                generator.generateCustomReport(args[1], args[2]);
            }
        } else {
            System.out.println("Usage: ReportGenerator <daily|customers|custom>");
        }
    }
}
