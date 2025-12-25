# Issue #5: Replace deprecated cryptographic algorithms

**Priority:** 🔴 Critical
**Type:** Security
**Difficulty:** Medium
**Estimated Time:** 4-6 hours

## Description

The application uses deprecated and insecure cryptographic algorithms that are vulnerable to attacks. Security audit requires immediate replacement.

## Current Vulnerabilities

### 1. Password Hashing - MD5 (BROKEN)
```java
// CryptoUtils.java
MessageDigest md = MessageDigest.getInstance("MD5");
```
- MD5 is cryptographically broken
- No salt used
- Vulnerable to rainbow table attacks

### 2. Encryption - DES (DEPRECATED)
```java
// CryptoUtils.java
Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
```
- DES has only 56-bit key (brute-forceable)
- ECB mode leaks patterns

### 3. Static Initialization Vector
```java
private static final String IV = "1234567890123456";
```
- Static IV defeats the purpose of CBC mode
- Enables pattern analysis attacks

### 4. Hardcoded Encryption Keys
```java
private static final String AES_KEY = "MegaBankAESKey16";
private static final String DES_KEY = "DESKey88";
```

## Requirements

- [ ] Replace MD5 with bcrypt or Argon2 for password hashing
- [ ] Replace DES with AES-256-GCM
- [ ] Generate random IV for each encryption operation
- [ ] Implement secure key management (not hardcoded)
- [ ] Add key rotation capability
- [ ] Migrate existing encrypted data to new algorithms
- [ ] Update all usages of CryptoUtils

## Demo Scenario

Ask Claude Code: *"Audit the CryptoUtils class for security issues and implement modern, secure cryptographic practices"*

## Affected Files

- `src/main/java/com/megabank/paymentgateway/util/CryptoUtils.java`
- `src/main/java/com/megabank/paymentgateway/security/AuthManager.java`
- `src/main/java/com/megabank/paymentgateway/service/PaymentManager.java`

## References

- OWASP Cryptographic Failures: https://owasp.org/Top10/A02_2021-Cryptographic_Failures/
- NIST Cryptographic Standards
