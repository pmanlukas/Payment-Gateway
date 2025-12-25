# Issue #9: Remove sensitive data from log statements

**Priority:** 🔴 Critical
**Type:** Security, Compliance
**Difficulty:** Easy
**Estimated Time:** 2-3 hours

## Description

The application logs sensitive information including passwords, credit card numbers, CVV codes, and API keys. This violates PCI-DSS, GDPR, and basic security practices.

## Examples Found

### Credit Card Numbers
```java
// PaymentManager.java
System.out.println("Processing payment for card: " + cardNumber);
System.out.println("CVV: " + cvv);
```

### Passwords
```java
// AuthManager.java
System.out.println("Login attempt - User: " + username + ", Password: " + password);

// DatabaseHelper.java
System.out.println("Password used: " + DB_PASSWORD);
```

### API Keys
```java
// PaymentManager.java
System.out.println("Calling Visa API with key: " + VISA_API_KEY);
```

### SSN and PII
```java
// StringHelper.java
System.out.println("Validating SSN: " + ssn);
```

### toString() Methods
```java
// Transaction.java
@Override
public String toString() {
    return "Transaction{" +
            "cardNumber='" + cardNumber + '\'' +
            ", cvv='" + cvv + '\'' +
            // ...
}
```

## Requirements

- [ ] Remove all `System.out.println` containing sensitive data
- [ ] Implement proper logging framework (SLF4J + Logback)
- [ ] Create masking utility for card numbers (show last 4 only)
- [ ] Never log passwords, CVV, or full SSN
- [ ] Update toString() methods to mask sensitive fields
- [ ] Add log sanitization filter
- [ ] Review log4j.properties for appropriate levels

## Demo Scenario

Ask Claude Code: *"Find all places where sensitive data like passwords, card numbers, or SSN are logged and fix them"*

## Masking Rules

| Data Type | Masking |
|-----------|---------|
| Card Number | ************1234 |
| CVV | *** |
| Password | [REDACTED] |
| SSN | ***-**-1234 |
| API Key | ****...last4 |

## Acceptance Criteria

1. No sensitive data in any log output
2. Proper logging framework implemented
3. Masking utilities for all PII
4. Log audit passes security review
