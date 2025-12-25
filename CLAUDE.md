# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MegaBank Payment Gateway is a deliberately vulnerable legacy Java application (circa 2003-2023) designed to demonstrate security vulnerabilities and legacy code patterns. This is **NOT** production-ready code - it intentionally contains security flaws, anti-patterns, and legacy practices for educational/demonstration purposes.

**CRITICAL**: This codebase contains intentional security vulnerabilities including:
- Hardcoded credentials and API keys throughout the code
- SQL injection vulnerabilities in all database operations
- PCI-DSS violations (storing CVV, full card numbers in plain text)
- Plain text password storage
- Weak/broken cryptography (DES, MD5, SHA1)
- Missing input validation and authorization checks
- Sensitive data logging
- Backdoor access methods

## Documentation

This repository has comprehensive documentation:

- **[README.md](README.md)** - Project overview, quick start, features
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development setup, code style, contribution workflow
- **[API.md](API.md)** - Complete REST API documentation with examples
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment procedures, rollback, monitoring
- **[.github/CI-CD.md](.github/CI-CD.md)** - CI/CD pipeline documentation
- **[openapi.yaml](openapi.yaml)** - OpenAPI 3.0 specification
- **[CLAUDE.md](CLAUDE.md)** - This file - Architecture and development guide

## Build & Development Commands

### Building the Project
```bash
# Compile and package (Maven)
mvn clean package

# Run tests (note: surefire plugin is disabled in pom.xml)
mvn test

# Run specific test
mvn test -Dtest=PaymentManagerTest
```

**See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed development setup instructions.**

### Running the Application
The application is designed to run on Tomcat (servlet container). Deploy the JAR to a servlet container.

**See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment procedures.**

### Database Setup
```bash
# Initialize database schema
mysql -u root -p < src/main/resources/schema.sql
```

Note: Database credentials are hardcoded in multiple locations - see application.properties, DatabaseHelper.java, and PaymentManager.java.

**See [CONTRIBUTING.md](CONTRIBUTING.md) for complete database setup instructions.**

## Architecture Overview

### Core Components

**PaymentManager** (`service/PaymentManager.java`)
- The "god class" - handles everything payment-related
- 1500+ lines, 80+ methods
- Singleton pattern with mutable state (thread-safety issues)
- Contains hardcoded credentials, API keys, and encryption keys
- Main entry point: `processPayment()` method handles the entire payment flow
- Mixes business logic, persistence, notification, and validation

**API Layer**
- **RestApiController** (`api/RestApiController.java`): "Modern" REST API added in 2018
  - Servlet-based (not Spring Boot)
  - Endpoints: `/payments`, `/transactions`, `/customers`, `/refunds`, `/debug`, `/health`
  - Authentication via hardcoded API keys in `X-API-Key` header
  - Returns sensitive data (full card numbers, CVV, passwords) in responses
- **PaymentServlet** (`api/PaymentServlet.java`): Legacy servlet interface

**Data Access**
- **DatabaseHelper** (`dao/DatabaseHelper.java`): Raw JDBC operations with SQL injection vulnerabilities
  - All queries use string concatenation (no PreparedStatements)
  - Static connection (not thread-safe)
  - Defaults to PROD environment if not specified
- **Schema** (`resources/schema.sql`): MySQL schema with plain text sensitive data storage
  - Tables: customers, transactions, blacklisted_cards, users, api_keys, merchants, debug_data
  - Default admin accounts with weak passwords inserted on schema creation

**Legacy Integration**
- **LegacyPaymentBridge** (`legacy/LegacyPaymentBridge.java`): COBOL mainframe integration
  - Fixed-length record format (62 bytes total)
  - Communicates via raw TCP socket on port 3270
  - Sends credentials in plain text
  - Critical: Changes break nightly batch reconciliation

**Batch Processing**
- **TransactionBatchProcessor** (`batch/TransactionBatchProcessor.java`): Nightly batch job
  - Processes CSV/DAT files from `/var/data/megabank/incoming/`
  - Downloads from FTP (not SFTP) using hardcoded credentials
  - Logs full card numbers and CVVs
  - Runs with elevated privileges
  - Main method accepts dangerous arguments: `export` or `sql` for arbitrary SQL execution

**Utilities**
- **CryptoUtils** (`util/CryptoUtils.java`): Cryptography with hardcoded keys
  - Uses deprecated algorithms: DES (ECB mode), MD5, SHA1
  - Static IV for AES (security issue)
  - Global salt for all users
  - XOR "obfuscation" (not real encryption)
  - maskCardNumber() shows first 6 + last 4 (PCI requires last 4 only)
- **StringHelper** (`util/StringHelper.java`): String manipulation utilities

**Security**
- **AuthManager** (`security/AuthManager.java`): Authentication with SQL injection vulnerabilities

**Domain Models**
- **Transaction** (`core/Transaction.java`): Entity with public fields
  - Stores CVV and full card numbers (PCI violation)
  - Uses `double` for money (precision issues)
  - toString() logs sensitive data
  - Missing serialVersionUID
- **Customer** (`core/Customer.java`): Customer entity

### Configuration Files

**application.properties** (`resources/application.properties`)
- Contains production credentials for database, SMTP, FTP, mainframe
- All security features disabled (`security.enabled=false`, etc.)
- Debug mode enabled in production
- Sensitive data logging enabled
- Admin backdoor codes exposed

**pom.xml**
- Java 1.6 / Maven 2.x (legacy versions)
- Spring 3.2.18, Hibernate 3.6.10 (old versions with known vulnerabilities)
- MySQL Connector 5.1.6
- Multiple logging frameworks (Log4j 1.2.16, commons-logging)
- Old JSON library (2009)
- Servlet API 2.4

## Key Patterns & Anti-Patterns

### Security Issues to Be Aware Of
1. **SQL Injection Everywhere**: All database queries use string concatenation
2. **Hardcoded Credentials**: Database, API keys, FTP, mainframe passwords in source code
3. **PCI Violations**: CVV stored in database and logged; full card numbers everywhere
4. **Backdoor Access**: `backdoorAccess()` method in PaymentManager accepts secret codes
5. **Debug Endpoints**: `/debug` endpoint exposes all keys, environment variables, statistics
6. **No Authorization**: Refund and customer operations have no auth checks
7. **Sensitive Data Logging**: Card numbers, CVVs, passwords logged to stdout and files

### Code Patterns
1. **God Class Pattern**: PaymentManager does everything
2. **Singleton Anti-Pattern**: PaymentManager uses broken double-checked locking
3. **Magic Numbers**: Status codes as integers (0=pending, 1=approved, etc.)
4. **No Dependency Injection**: Direct instantiation and static methods throughout
5. **Public Fields**: Transaction and Customer use public fields instead of encapsulation
6. **Copy-Paste Code**: processVisa(), processMastercard(), processAmex() nearly identical
7. **Swallowed Exceptions**: Try-catch blocks that ignore exceptions
8. **Resource Leaks**: SQL statements not closed, file handles not properly managed

### Legacy Constraints
1. **COBOL Integration**: LegacyPaymentBridge uses fixed 62-byte records - do not change field lengths
2. **Serialization**: Transaction class is serialized to disk for batch processing
3. **Nightly Batch**: Schema changes must be compatible with batch reconciliation job
4. **Java 1.6**: Code must compile with Java 6 (no lambdas, try-with-resources, etc.)

## Working with This Codebase

### When Making Changes
1. **Understand the Security Context**: This is intentionally vulnerable code for demonstration
2. **Maintain Legacy Compatibility**: Changes to LegacyPaymentBridge or Transaction serialization break integrations
3. **Database Operations**: Be aware all queries are vulnerable to SQL injection
4. **Fixed-Length Records**: Mainframe integration requires exact 62-byte format (see LegacyPaymentBridge)

### Payment Processing Flow
1. Request comes via RestApiController or PaymentServlet
2. PaymentManager.processPayment() validates and processes
3. Card type detected by first digit (4=Visa, 5=MC, 3=Amex)
4. Routed to processVisa()/processMastercard()/processAmex()
5. Transaction saved to database via SQL concatenation
6. Customer record updated
7. Notifications sent (email, SMS, webhook)
8. Result returned with full card details in response

### Batch Processing Flow
1. Files downloaded from FTP to `/var/data/megabank/incoming/`
2. TransactionBatchProcessor reads CSV/DAT files
3. Each line parsed: card_number,cvv,expiry,amount,merchant_id,customer_id
4. Processed via PaymentManager.processPayment()
5. Errors written to `/var/data/megabank/errors/`
6. Successful files archived to `/var/data/megabank/archive/`
7. Summary email sent with statistics (includes FTP password!)

### Common Vulnerability Locations
- **SQL Injection**: DatabaseHelper, PaymentManager, AuthManager - all executeQuery/executeUpdate calls
- **Hardcoded Secrets**: PaymentManager (lines 43-54), application.properties, DatabaseHelper, LegacyPaymentBridge, TransactionBatchProcessor
- **PCI Violations**: Transaction.cardNumber/cvv fields, database schema (transactions.cvv column), all toString() methods
- **Command Injection**: TransactionBatchProcessor.main() accepts arbitrary SQL via args
- **Backdoors**: PaymentManager.backdoorAccess() (line 549), setDebugMode() (line 564)

## Testing

The test suite is minimal and many tests are disabled. The surefire plugin is commented out in pom.xml with a note: "Disabled by John in 2012 because tests were failing."

Available tests:
- `src/test/java/com/megabank/paymentgateway/PaymentManagerTest.java`
- `src/test/java/com/megabank/paymentgateway/CryptoUtilsTest.java`

Run with: `mvn test` (may need to enable surefire plugin first)

## Environment Detection

The application detects environment via `System.getProperty("env")`:
- DEV: connects to localhost:3306
- UAT: connects to uat-db.megabank.internal
- **Default: PROD** (dangerous - defaults to production!)

Set via: `java -Denv=DEV ...`

## CI/CD Pipeline

The project has a comprehensive GitHub Actions pipeline for automated builds, testing, and security scanning.

### GitHub Actions Workflows

**Main CI Pipeline** (`.github/workflows/ci.yml`)
- Builds and packages the application
- Runs tests (currently disabled in pom.xml)
- Performs OWASP dependency vulnerability scanning
- Generates and uploads build artifacts
- Comments on PRs with vulnerability summary

**Security Scanning** (`.github/workflows/security-scan.yml`)
- Runs on push, PR, weekly schedule, and manual trigger
- Detects hardcoded secrets (Gitleaks + custom patterns)
- Scans for SQL injection vulnerabilities (string concatenation)
- PCI-DSS compliance checks (CVV storage, card handling)
- GitHub CodeQL semantic analysis
- Comments on PRs with security findings

**Pull Request Checks** (`.github/workflows/pr-checks.yml`)
- Validates PR title format (semantic commits)
- Analyzes impact of changed files (core, database, legacy, security)
- Detects NEW issues in PR (delta analysis):
  - New hardcoded passwords/API keys
  - New SQL injection risks
  - New CVV storage violations
- Reports test coverage status
- Comments on PR with change impact analysis

**Dependency Review** (`.github/workflows/dependency-review.yml`)
- Triggered when pom.xml changes
- Reviews dependency changes for vulnerabilities
- Warns about legacy constraints (Java 6, mainframe compatibility)
- Comments on PR with upgrade warnings

### Dependabot Configuration

Located at `.github/dependabot.yml`:
- Maven dependencies checked monthly (most upgrades blocked due to legacy constraints)
- GitHub Actions checked weekly
- Ignores major/minor updates for Spring, Hibernate, MySQL

### Pull Request Template

Comprehensive template at `.github/PULL_REQUEST_TEMPLATE.md` includes:
- Impact assessment (core, database, legacy integration)
- Security checklist (SQL injection, hardcoded secrets, PCI compliance)
- Testing checklist
- Deployment considerations
- Rollback plan

### Expected CI/CD Results

Given the intentional vulnerabilities in this codebase:
- ✅ Build should succeed
- ⚠️ Tests skipped (surefire plugin disabled)
- ⚠️ 50+ dependency vulnerabilities (expected - legacy versions)
- ⚠️ Hundreds of hardcoded secrets detected (expected)
- ⚠️ 20+ SQL injection vulnerabilities (expected)
- ⚠️ Multiple PCI-DSS violations (expected - CVV storage)

**What should fail a PR**: NEW issues introduced (not existing legacy issues)

### Running Workflows

```bash
# Trigger security scan manually
gh workflow run security-scan.yml

# View workflow runs
gh run list --workflow=ci.yml

# Download artifacts
gh run download <run-id>
```

See `.github/CI-CD.md` for detailed CI/CD documentation.
