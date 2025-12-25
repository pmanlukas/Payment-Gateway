# Issue #7: Fix ignored unit tests and improve coverage

**Priority:** 🟡 Medium
**Type:** Quality, Testing
**Difficulty:** Easy
**Estimated Time:** 2-3 hours

## Description

Multiple unit tests are marked with `@Ignore` annotation and have been disabled for years. The test suite only covers approximately 15% of the codebase.

## Current State

### PaymentManagerTest.java
```java
@Ignore("Fails with NullPointerException - need to fix code first")
public void testProcessPayment_nullCard()

@Ignore("Need to discuss expected behavior with BA")
public void testProcessPayment_zeroAmount()

@Ignore("Negative amounts cause weird behavior")
public void testProcessPayment_negativeAmount()

@Ignore("Requires database connection")
public void testLogin()

@Ignore("Fails intermittently due to race conditions")
public void testConcurrentPayments()

@Ignore("Takes too long")
public void testPerformance()
```

### CryptoUtilsTest.java
```java
@Ignore("Throws NumberFormatException")
public void testLuhnWithNonNumeric()
```

## Requirements

- [ ] Review each ignored test and determine root cause
- [ ] Fix underlying code issues causing test failures
- [ ] Re-enable tests or delete if no longer relevant
- [ ] Add missing test cases for edge cases
- [ ] Achieve minimum 50% code coverage
- [ ] Add integration test for payment flow
- [ ] Set up test database configuration

## Demo Scenario

Ask Claude Code: *"Review the ignored tests in the test suite, fix the underlying issues, and re-enable them"*

## Test Coverage Targets

| Package | Current | Target |
|---------|---------|--------|
| service | ~10% | 60% |
| util | ~20% | 80% |
| security | 0% | 50% |
| dao | 0% | 40% |

## Acceptance Criteria

1. No tests with `@Ignore` annotation without documented reason
2. All previously ignored tests either fixed or removed
3. Test coverage report generated
4. CI pipeline runs all tests
