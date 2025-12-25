# Issue #6: Add comprehensive input validation

**Priority:** 🟡 Medium
**Type:** Security, Quality
**Difficulty:** Easy
**Estimated Time:** 3-4 hours

## Description

The application lacks proper input validation throughout. User inputs are passed directly to business logic and database queries without sanitization or validation.

## Current Problems

### No Card Number Validation
```java
// PaymentManager.java
public void setCardNumber(String cardNumber) {
    // No validation whatsoever
    this.cardNumber = cardNumber;
}
```

### No Amount Validation
```java
// No upper limit check, negative amounts accepted
if (amount <= 0) {
    txn.status = STATUS_ERROR;
    return txn;
}
```

### Email Validation Too Permissive
```java
// StringHelper.java - accepts "a@b" as valid!
public static boolean isValidEmail(String email) {
    return email != null && email.contains("@");
}
```

### No CVV Format Validation
```java
if (cvv == null || cvv.length() < 3) {
    // Accepts "abc" as valid CVV
}
```

## Requirements

- [ ] Implement Luhn algorithm check for card numbers
- [ ] Add card number length validation (13-19 digits)
- [ ] Validate CVV format (3-4 digits only)
- [ ] Add proper email regex validation
- [ ] Validate amount ranges (positive, reasonable maximum)
- [ ] Add currency code validation (ISO 4217)
- [ ] Validate expiry date format and future date
- [ ] Create reusable ValidationUtils class
- [ ] Add unit tests for all validators

## Demo Scenario

Ask Claude Code: *"Add input validation for all payment fields - card number, CVV, expiry date, and amount"*

## Validation Rules

| Field | Rules |
|-------|-------|
| Card Number | 13-19 digits, passes Luhn check |
| CVV | 3 digits (Visa/MC) or 4 digits (Amex) |
| Expiry | MM/YY format, future date |
| Amount | Positive, max 999999.99 |
| Currency | Valid ISO 4217 code |
| Email | RFC 5322 compliant |

## Acceptance Criteria

1. All user inputs validated before processing
2. Clear error messages for validation failures
3. Unit tests for each validation rule
4. No change to valid transaction flow
