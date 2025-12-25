# Issue #3: PCI-DSS Compliance - Remove CVV storage from database

**Priority:** 🔴 Critical
**Type:** Compliance, Security
**Difficulty:** Medium
**Estimated Time:** 6-8 hours

## Description

Our PCI-DSS audit identified that we are storing CVV (Card Verification Value) in the database, which is a **direct violation of PCI-DSS Requirement 3.2**.

> "Do not store the card verification code or value (three-digit or four-digit number printed on the front or back of a payment card used to verify card-not-present transactions) after authorization."

## Affected Areas

### Database Schema (`schema.sql`)
- `transactions.cvv` column
- `customers.cvv` column
- `debug_data.cvv` column

### Java Classes
- `Transaction.java` - has `public String cvv` field
- `Customer.java` - has `public String primaryCardCvv` field
- `PaymentManager.java` - stores CVV in database insert
- `LegacyPaymentBridge.java` - transmits CVV to mainframe
- `PaymentRequest.java` - CVV in DTO

### Logging (also a violation)
- `PaymentManager.java` logs CVV to console
- `PaymentServlet.java` logs request parameters including CVV
- Error logs may contain CVV data

## Requirements

- [ ] Remove CVV columns from database schema
- [ ] Remove CVV fields from entity classes (or mark as transient)
- [ ] Update payment processing to not persist CVV after authorization
- [ ] Remove CVV from all log statements
- [ ] Add validation to reject any attempt to store CVV
- [ ] Audit and clean existing data containing CVV
- [ ] Update `LegacyPaymentBridge` to not transmit CVV

## Demo Scenario

Ask Claude Code: *"We need to become PCI-DSS compliant. Find all places where we store or log CVV data and fix them"*

## Compliance Deadline

This must be resolved before our next PCI audit.

## References

- PCI-DSS v4.0 Requirement 3.2
- PCI Security Standards Council: https://www.pcisecuritystandards.org/
