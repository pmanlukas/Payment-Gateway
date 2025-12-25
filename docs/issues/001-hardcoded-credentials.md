# Issue #1: Replace hardcoded database credentials with environment variables

**Priority:** 🔴 Critical
**Type:** Security, Tech Debt
**Difficulty:** Easy
**Estimated Time:** 2-4 hours

## Description

The security team flagged that database credentials are hardcoded in multiple files throughout the codebase. This is a critical security issue that needs to be addressed.

## Affected Files

- `src/main/java/com/megabank/paymentgateway/service/PaymentManager.java`
- `src/main/java/com/megabank/paymentgateway/dao/DatabaseHelper.java`
- `src/main/java/com/megabank/paymentgateway/config/DatabaseConfig.java`
- `src/main/resources/application.properties`

## Current State

```java
// PaymentManager.java
private static final String DB_URL = "jdbc:mysql://prod-db.megabank.internal:3306/payments";
private static final String DB_USER = "payment_admin";
private static final String DB_PASSWORD = "Payment123!";
```

## Requirements

- [ ] Remove all hardcoded credentials from source code
- [ ] Implement environment variable based configuration
- [ ] Update documentation on how to set required environment variables
- [ ] Ensure backward compatibility with existing deployments

## Demo Scenario

Ask Claude Code: *"Find all hardcoded credentials in the codebase and replace them with environment variables"*

## Acceptance Criteria

1. No passwords, API keys, or secrets in source code
2. Application reads credentials from environment variables
3. Fallback to properties file for non-sensitive defaults
4. README updated with environment variable documentation
