package com.megabank.paymentgateway.batch;

import com.megabank.paymentgateway.core.Transaction;
import com.megabank.paymentgateway.dao.DatabaseHelper;
import com.megabank.paymentgateway.service.PaymentManager;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.*;

/**
 * Batch Transaction Processor
 *
 * Runs nightly to process batch files from partner banks
 * This class is scheduled via cron and should not be modified
 * without consulting the Operations team.
 *
 * CRITICAL: This job runs with elevated privileges!
 *
 * @author Night Operations Team (2005)
 * @since v1.0
 *
 * TODO: Add proper error handling - Mike 2007
 * TODO: Still no error handling - Dave 2012
 * TODO: We really need to fix this - Sarah 2018
 * TODO: One day... - Bob 2023
 */
public class TransactionBatchProcessor {

    // Hardcoded file paths - environment specific!
    private static final String INPUT_DIR = "/var/data/megabank/incoming/";
    private static final String OUTPUT_DIR = "/var/data/megabank/processed/";
    private static final String ERROR_DIR = "/var/data/megabank/errors/";
    private static final String ARCHIVE_DIR = "/var/data/megabank/archive/";

    // FTP credentials hardcoded!
    private static final String FTP_HOST = "ftp.partnerbank.com";
    private static final String FTP_USER = "megabank_batch";
    private static final String FTP_PASSWORD = "BatchFTP2010!";

    // Processing limits
    private static final int MAX_BATCH_SIZE = 100000;
    private static final int MAX_RETRIES = 3;

    // Date formatter - not thread safe!
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    // Processing statistics
    private int totalProcessed = 0;
    private int totalSuccess = 0;
    private int totalFailed = 0;
    private double totalAmount = 0.0;

    /**
     * Main entry point for batch processing
     */
    public void processBatchFiles() {
        System.out.println("=== BATCH PROCESSING STARTED ===");
        System.out.println("Time: " + new Date());
        System.out.println("FTP Password: " + FTP_PASSWORD);  // Logging password!

        // Download files from FTP
        downloadFilesFromFTP();

        // Process each file in input directory
        File inputDir = new File(INPUT_DIR);
        File[] files = inputDir.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No files to process");
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith(".csv") || file.getName().endsWith(".dat")) {
                processFile(file);
            }
        }

        // Send summary report
        sendSummaryReport();

        System.out.println("=== BATCH PROCESSING COMPLETED ===");
        System.out.println("Total: " + totalProcessed);
        System.out.println("Success: " + totalSuccess);
        System.out.println("Failed: " + totalFailed);
        System.out.println("Amount: $" + totalAmount);
    }

    /**
     * Download files from partner FTP - plain FTP, not SFTP!
     */
    private void downloadFilesFromFTP() {
        System.out.println("Connecting to FTP: " + FTP_HOST);
        System.out.println("User: " + FTP_USER);
        System.out.println("Password: " + FTP_PASSWORD);

        // Simulated FTP download
        // In reality this would use plain FTP (not SFTP) - security issue!
        try {
            // Connection code would be here
            System.out.println("Downloaded files from FTP");
        } catch (Exception e) {
            System.out.println("FTP error: " + e.getMessage());
            // Continues anyway! No proper error handling
        }
    }

    /**
     * Process a single batch file
     */
    private void processFile(File file) {
        System.out.println("Processing file: " + file.getName());

        BufferedReader reader = null;
        PrintWriter errorWriter = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String errorFile = ERROR_DIR + file.getName() + ".errors";
            errorWriter = new PrintWriter(new FileWriter(errorFile));

            String line;
            int lineNumber = 0;

            // Skip header
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber > MAX_BATCH_SIZE) {
                    System.out.println("WARNING: Batch size exceeded, stopping at " + MAX_BATCH_SIZE);
                    break;
                }

                try {
                    processBatchLine(line, lineNumber, errorWriter);
                } catch (Exception e) {
                    // Log error with full line - could contain card numbers!
                    System.out.println("Error on line " + lineNumber + ": " + line);
                    errorWriter.println("Line " + lineNumber + ": " + line + " - Error: " + e.getMessage());
                    totalFailed++;
                }
            }

            // Move file to archive
            moveToArchive(file);

        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
            // File is left in input directory - could be processed again!
        } finally {
            // Resource cleanup - but done incorrectly!
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                // Ignored
            }
            if (errorWriter != null) errorWriter.close();
        }
    }

    /**
     * Process a single line from batch file
     * Format: card_number,cvv,expiry,amount,merchant_id,customer_id
     */
    private void processBatchLine(String line, int lineNumber, PrintWriter errorWriter) {
        String[] parts = line.split(",");

        if (parts.length < 6) {
            throw new RuntimeException("Invalid format");
        }

        String cardNumber = parts[0].trim();
        String cvv = parts[1].trim();
        String expiry = parts[2].trim();
        double amount = Double.parseDouble(parts[3].trim());  // No validation!
        String merchantId = parts[4].trim();
        String customerId = parts[5].trim();

        // Log full card details!
        System.out.println("Processing: Card=" + cardNumber + ", CVV=" + cvv + ", Amount=" + amount);

        // Process through payment manager
        PaymentManager pm = PaymentManager.getInstance();
        Transaction txn = pm.processPayment(
            cardNumber, cvv, expiry, amount, "USD", merchantId, customerId, "BATCH"
        );

        totalProcessed++;

        if (txn.status == 1) {
            totalSuccess++;
            totalAmount += amount;
        } else {
            totalFailed++;
            // Write to error file with sensitive data!
            errorWriter.println("DECLINED: " + cardNumber + "," + cvv + "," + amount);
        }
    }

    /**
     * Move processed file to archive
     */
    private void moveToArchive(File file) {
        String archiveName = ARCHIVE_DIR + file.getName() + "." + TIMESTAMP_FORMAT.format(new Date());
        File archiveFile = new File(archiveName);

        if (file.renameTo(archiveFile)) {
            System.out.println("Archived: " + archiveName);
        } else {
            // Copy instead - but original is not deleted!
            try {
                copyFile(file, archiveFile);
                // Original file not deleted - will be processed again!
            } catch (IOException e) {
                System.out.println("Archive failed: " + e.getMessage());
            }
        }
    }

    /**
     * Copy file
     */
    private void copyFile(File source, File dest) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(dest);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }

        // Not using try-finally properly!
        fis.close();
        fos.close();
    }

    /**
     * Send summary report via email
     */
    private void sendSummaryReport() {
        // Email credentials hardcoded!
        String smtpHost = "smtp.megabank.internal";
        String smtpUser = "batch_reports";
        String smtpPassword = "ReportPass123!";

        String subject = "Batch Processing Report - " + DATE_FORMAT.format(new Date());
        String body = "Batch Processing Summary\n\n" +
                     "Total Processed: " + totalProcessed + "\n" +
                     "Successful: " + totalSuccess + "\n" +
                     "Failed: " + totalFailed + "\n" +
                     "Total Amount: $" + totalAmount + "\n\n" +
                     "FTP Password: " + FTP_PASSWORD + "\n";  // Including password in report!

        System.out.println("Sending report:");
        System.out.println(body);

        // Email sending code would be here
    }

    /**
     * Manual reprocessing - no authentication!
     */
    public void reprocessFile(String filename) {
        // No authorization check!
        File file = new File(ARCHIVE_DIR + filename);
        if (file.exists()) {
            // Copies back to input and processes again
            try {
                copyFile(file, new File(INPUT_DIR + filename));
                processBatchFiles();
            } catch (IOException e) {
                System.out.println("Reprocess failed: " + e.getMessage());
            }
        }
    }

    /**
     * Direct database update - bypasses all validation!
     */
    public void directDatabaseUpdate(String sql) {
        // No authorization! Executes any SQL!
        try {
            DatabaseHelper.executeUpdate(sql);
            System.out.println("Direct SQL executed: " + sql);
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
    }

    /**
     * Export all transactions to file - data leak potential!
     */
    public void exportAllTransactions(String outputPath) {
        try {
            DatabaseHelper.exportTable("transactions", outputPath);
            System.out.println("Exported all transactions to: " + outputPath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Main method for manual testing
     */
    public static void main(String[] args) {
        System.out.println("Starting batch processor...");
        System.out.println("Arguments: " + Arrays.toString(args));

        TransactionBatchProcessor processor = new TransactionBatchProcessor();

        if (args.length > 0 && "export".equals(args[0])) {
            // Export all data!
            processor.exportAllTransactions("/tmp/all_transactions.csv");
        } else if (args.length > 1 && "sql".equals(args[0])) {
            // Execute arbitrary SQL!
            processor.directDatabaseUpdate(args[1]);
        } else {
            processor.processBatchFiles();
        }
    }
}
