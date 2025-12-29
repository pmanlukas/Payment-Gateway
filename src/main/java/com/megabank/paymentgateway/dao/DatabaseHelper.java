package com.megabank.paymentgateway.dao;

import java.sql.*;
import java.util.*;
import java.io.*;

/**
 * Database Helper Utility Class
 *
 * This class provides database operations "the old way"
 *
 * Last updated: 2007
 * Status: Works, don't touch
 */
public class DatabaseHelper {

    // Hardcoded database credentials - multiple environments mixed!
    // Check environment variable for Docker support
    private static final String DEV_DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    private static final String DEV_DB_URL = "jdbc:mysql://" + DEV_DB_HOST + ":3306/payments";
    private static final String PROD_DB_URL = "jdbc:mysql://prod-db.megabank.internal:3306/payments";
    private static final String UAT_DB_URL = "jdbc:mysql://uat-db.megabank.internal:3306/payments";

    // Same password for all environments! Security issue!
    private static final String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";  // Using root!
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "root123";

    // Static connection - not thread safe!
    private static Connection connection = null;

    // Environment detection - unreliable!
    private static String getCurrentEnvironment() {
        String env = System.getProperty("env");
        if (env == null) {
            // Default to PROD! Dangerous!
            return "PROD";
        }
        return env.toUpperCase();
    }

    // Get connection - connection leak potential
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.jdbc.Driver");

                String dbUrl;
                String env = getCurrentEnvironment();
                if ("DEV".equals(env)) {
                    dbUrl = DEV_DB_URL;
                } else if ("UAT".equals(env)) {
                    dbUrl = UAT_DB_URL;
                } else {
                    dbUrl = PROD_DB_URL;
                }

                connection = DriverManager.getConnection(dbUrl, DB_USER, DB_PASSWORD);
                System.out.println("Connected to database: " + dbUrl + " with user: " + DB_USER);
                System.out.println("Password used: " + DB_PASSWORD);  // Logging password!

            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL driver not found", e);
            }
        }
        return connection;
    }

    // Execute query - SQL Injection vulnerable
    public static ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        System.out.println("Executing SQL: " + sql);  // Logging SQL - could contain sensitive data
        return stmt.executeQuery(sql);
    }

    // Execute update - SQL Injection vulnerable
    public static int executeUpdate(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        System.out.println("Executing SQL: " + sql);
        return stmt.executeUpdate(sql);
    }

    // Build query helper - SQL Injection!
    public static String buildSelectQuery(String table, String whereClause) {
        // Direct string concatenation!
        return "SELECT * FROM " + table + " WHERE " + whereClause;
    }

    // Insert helper - SQL Injection!
    public static String buildInsertQuery(String table, Map values) {
        StringBuilder columns = new StringBuilder();
        StringBuilder vals = new StringBuilder();

        Iterator it = values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            columns.append(entry.getKey());
            vals.append("'").append(entry.getValue()).append("'");  // No escaping!
            if (it.hasNext()) {
                columns.append(", ");
                vals.append(", ");
            }
        }

        return "INSERT INTO " + table + " (" + columns + ") VALUES (" + vals + ")";
    }

    // Find by ID - SQL Injection
    public static ResultSet findById(String table, String id) throws SQLException {
        // SQL Injection vulnerability!
        String sql = "SELECT * FROM " + table + " WHERE id = '" + id + "'";
        return executeQuery(sql);
    }

    // Find by criteria - SQL Injection
    public static ResultSet findByCriteria(String table, String field, String value) throws SQLException {
        // SQL Injection vulnerability!
        String sql = "SELECT * FROM " + table + " WHERE " + field + " = '" + value + "'";
        return executeQuery(sql);
    }

    // Delete record - SQL Injection
    public static int deleteRecord(String table, String id) throws SQLException {
        // SQL Injection vulnerability!
        String sql = "DELETE FROM " + table + " WHERE id = '" + id + "'";
        return executeUpdate(sql);
    }

    // Bulk delete - extremely dangerous!
    public static int bulkDelete(String table, String condition) throws SQLException {
        // No confirmation, no limit, SQL Injection!
        String sql = "DELETE FROM " + table + " WHERE " + condition;
        return executeUpdate(sql);
    }

    // Export table to file - exposes all data!
    public static void exportTable(String table, String filePath) throws SQLException, IOException {
        ResultSet rs = executeQuery("SELECT * FROM " + table);
        FileWriter fw = new FileWriter(filePath);

        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        // Write headers
        for (int i = 1; i <= columnCount; i++) {
            fw.write(meta.getColumnName(i));
            if (i < columnCount) fw.write(",");
        }
        fw.write("\n");

        // Write data - including sensitive columns!
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                fw.write(rs.getString(i) != null ? rs.getString(i) : "");
                if (i < columnCount) fw.write(",");
            }
            fw.write("\n");
        }

        fw.close();
        System.out.println("Exported " + table + " to " + filePath);  // Path disclosure
    }

    // Import from file - no validation!
    public static void importFromFile(String table, String filePath) throws SQLException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        String headers = br.readLine();  // Skip header

        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            // Direct insertion without validation!
            StringBuilder sql = new StringBuilder("INSERT INTO " + table + " VALUES (");
            for (int i = 0; i < values.length; i++) {
                sql.append("'").append(values[i]).append("'");  // No escaping!
                if (i < values.length - 1) sql.append(",");
            }
            sql.append(")");
            executeUpdate(sql.toString());
        }
        br.close();
    }

    // Run arbitrary SQL - extremely dangerous!
    public static void runScript(String sqlScript) throws SQLException {
        // Runs any SQL without validation!
        String[] statements = sqlScript.split(";");
        for (String stmt : statements) {
            if (stmt.trim().length() > 0) {
                executeUpdate(stmt);
            }
        }
    }

    // Close connection - but static connection means this affects everyone
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            // Swallowed
        }
    }

    // Debug method that dumps schema
    public static void dumpSchema() throws SQLException {
        ResultSet rs = getConnection().getMetaData().getTables(null, null, "%", null);
        System.out.println("=== DATABASE SCHEMA ===");
        while (rs.next()) {
            System.out.println("Table: " + rs.getString("TABLE_NAME"));
        }
    }
}
