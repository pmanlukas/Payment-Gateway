# Issue #2: Fix SQL injection vulnerabilities in DatabaseHelper

**Priority:** 🔴 Critical
**Type:** Security, Bug
**Difficulty:** Medium
**Estimated Time:** 4-6 hours

## Description

Static code analysis identified multiple SQL injection vulnerabilities in the `DatabaseHelper` class. All methods use string concatenation to build SQL queries, making them vulnerable to SQL injection attacks.

## Vulnerability Example

```java
// Current vulnerable code in DatabaseHelper.java:
public static ResultSet findById(String table, String id) throws SQLException {
    String sql = "SELECT * FROM " + table + " WHERE id = '" + id + "'";
    return executeQuery(sql);
}
```

An attacker could input: `' OR '1'='1` to bypass authentication or `'; DROP TABLE customers; --` to delete data.

## Affected Methods

- `executeQuery(String sql)` - Line 45
- `findById(String table, String id)` - Line 78
- `findByCriteria(String table, String field, String value)` - Line 85
- `buildSelectQuery(String table, String whereClause)` - Line 52
- `buildInsertQuery(String table, Map values)` - Line 58
- `deleteRecord(String table, String id)` - Line 92
- `bulkDelete(String table, String condition)` - Line 98

## Also Affected

- `PaymentManager.java` - Multiple SQL queries with string concatenation
- `AuthManager.java` - Login query vulnerable to authentication bypass
- `ReportGenerator.java` - Report queries with user input

## Requirements

- [ ] Replace all string concatenation with PreparedStatements
- [ ] Add input validation/sanitization layer
- [ ] Add unit tests for SQL injection prevention
- [ ] Review and fix all classes using DatabaseHelper
- [ ] Document secure coding guidelines

## Demo Scenario

Ask Claude Code: *"Find and fix all SQL injection vulnerabilities in the codebase"*

## References

- OWASP SQL Injection: https://owasp.org/www-community/attacks/SQL_Injection
- CWE-89: Improper Neutralization of Special Elements in SQL Command
