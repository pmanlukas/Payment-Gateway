# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MegaBank Payment Gateway is an enterprise Java payment processing system that has been in operation since 2003. The system was originally developed as a rapid prototype to quickly enter the payment processing market and has evolved over two decades to serve multiple merchant clients processing millions of transactions annually.

**Current State**: The codebase is undergoing a comprehensive modernization and security enhancement initiative. The system currently operates in production but has accumulated significant technical debt over its 20+ year history. Multiple development teams have worked on the system over the years, each adding features while working under tight deadlines and legacy constraints.

**Known Technical Debt Areas**:
The engineering team has identified several areas requiring attention as part of the ongoing modernization effort:
- Legacy credential management practices requiring migration to secure vault systems
- Database access patterns needing updates to modern parameterized query standards
- Data storage practices requiring review for PCI-DSS compliance alignment
- Authentication mechanisms scheduled for enhancement
- Cryptographic implementations targeted for upgrade to current standards
- Input validation and authorization framework improvements in progress
- Logging practices under review for sensitive data handling

These items are tracked in the issue management system and prioritized as part of the technical debt reduction roadmap.

## Documentation

This repository has comprehensive documentation:

- **[README.md](README.md)** - Project overview, quick start, features
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development setup, code style, contribution workflow
- **[API.md](API.md)** - Complete REST API documentation with examples
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment procedures, rollback, monitoring
- **[MONITORING.md](MONITORING.md)** - Monitoring guide, health checks, metrics, alerting
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

### Running with Docker (Recommended)
```bash
# Quick start - runs entire stack (MySQL, app, Prometheus, Grafana)
docker-compose up -d

# Or use the quick start script
./scripts/quick-start.sh

# Access the application
curl http://localhost:8080/health
```

**See [docker/README.md](docker/README.md) for Docker setup details.**

### Running the Application (Manual)
The application is designed to run on Tomcat (servlet container). Deploy the JAR to a servlet container.

**See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment procedures.**

### Database Setup
```bash
# Initialize database schema
mysql -u root -p < src/main/resources/schema.sql
```

Note: Database credentials are currently configured in application.properties. Migration to a secure credential management system is planned as part of the infrastructure modernization.

**See [CONTRIBUTING.md](CONTRIBUTING.md) for complete database setup instructions.**

## Architecture Overview

### Core Components

**PaymentManager** (`service/PaymentManager.java`)
- Central payment processing engine
- Approximately 1500 lines, 80+ methods
- Implements Singleton pattern for centralized payment orchestration
- Main entry point: `processPayment()` method handles the entire payment flow
- Coordinates business logic, persistence, notification, and validation
- Note: Scheduled for refactoring into smaller, more focused service classes

**API Layer**
- **RestApiController** (`api/RestApiController.java`): REST API layer (added 2018)
  - Servlet-based implementation
  - Endpoints: `/payments`, `/transactions`, `/customers`, `/refunds`, `/debug`, `/health`
  - API key-based authentication via `X-API-Key` header
  - Provides JSON responses for transaction data
- **PaymentServlet** (`api/PaymentServlet.java`): Legacy servlet interface

**Data Access**
- **DatabaseHelper** (`dao/DatabaseHelper.java`): JDBC data access layer
  - Direct JDBC operations for database interaction
  - Static connection management
  - Environment-based configuration (DEV/UAT/PROD)
  - Targeted for migration to PreparedStatement-based queries
- **Schema** (`resources/schema.sql`): MySQL database schema
  - Tables: customers, transactions, blacklisted_cards, users, api_keys, merchants, debug_data
  - Supports transaction processing, customer management, and merchant operations

**Legacy Integration**
- **LegacyPaymentBridge** (`legacy/LegacyPaymentBridge.java`): Mainframe integration layer
  - Fixed-length record format (62 bytes total) for compatibility with COBOL systems
  - TCP/IP socket communication on port 3270
  - **CRITICAL**: Record format must remain unchanged to maintain batch reconciliation compatibility
  - Any changes require coordination with mainframe team and thorough testing

**Batch Processing**
- **TransactionBatchProcessor** (`batch/TransactionBatchProcessor.java`): Nightly reconciliation job
  - Processes CSV/DAT files from `/var/data/megabank/incoming/`
  - FTP-based file retrieval (SFTP migration planned)
  - Transaction reconciliation and reporting
  - Runs with system privileges for file system access
  - Supports export and reporting functions

**Utilities**
- **CryptoUtils** (`util/CryptoUtils.java`): Cryptographic utility functions
  - Legacy encryption implementation (DES, MD5, SHA1)
  - Planned migration to modern algorithms (AES-256, SHA-256)
  - Card number masking functionality
  - Part of security modernization initiative
- **StringHelper** (`util/StringHelper.java`): String manipulation utilities

**Security**
- **AuthManager** (`security/AuthManager.java`): Authentication management
  - User authentication and session handling
  - Scheduled for security enhancements

**Domain Models**
- **Transaction** (`core/Transaction.java`): Transaction entity
  - Stores transaction details and payment information
  - Serializable for batch processing compatibility
  - Uses double precision for monetary values (BigDecimal migration planned)
- **Customer** (`core/Customer.java`): Customer entity

### Configuration Files

**application.properties** (`resources/application.properties`)
- Central configuration for database, SMTP, FTP, and mainframe connectivity
- Contains environment-specific settings
- Scheduled for migration to externalized configuration management
- Part of the infrastructure modernization initiative

**pom.xml**
- Java 1.6 source/target compatibility (runs on Java 8+)
- Spring Framework 3.2.18
- Hibernate ORM 3.6.10
- MySQL Connector 5.1.6
- Logging: Log4j 1.2.16, commons-logging
- JSON processing library
- Servlet API 2.4
- Note: Dependency upgrade initiative in progress to address CVEs and move to LTS versions

## Key Patterns & Architectural Considerations

### Technical Debt Items
The following areas have been identified for modernization:

1. **Database Access Patterns**
   - Current: Direct JDBC with string concatenation
   - Planned: Migration to parameterized queries and ORM best practices
   - Priority: High (security and maintainability)

2. **Credential Management**
   - Current: Configuration file-based credentials
   - Planned: Migration to secure credential vault (HashiCorp Vault or AWS Secrets Manager)
   - Priority: High (security)

3. **Data Handling**
   - Current: Legacy data storage patterns
   - Planned: Compliance review and implementation of PCI-DSS recommended practices
   - Priority: High (compliance)

4. **Debugging and Diagnostics**
   - Current: Debug endpoints for troubleshooting
   - Planned: Transition to proper observability stack (Prometheus/Grafana already implemented)
   - Priority: Medium (operational excellence)

5. **Cryptography**
   - Current: Legacy algorithm implementations
   - Planned: Migration to modern cryptographic standards
   - Priority: High (security)

6. **Authorization Framework**
   - Current: Limited authorization checks in some operations
   - Planned: Comprehensive role-based access control implementation
   - Priority: High (security)

7. **Logging Standards**
   - Current: Legacy logging practices
   - Planned: Review and update logging to follow security best practices
   - Priority: Medium (security and compliance)

### Code Patterns
The codebase exhibits several common patterns found in long-lived enterprise systems:

1. **Centralized Processing**: PaymentManager serves as the central orchestrator
   - Refactoring planned to distribute responsibilities across multiple service classes

2. **Singleton Pattern**: Used for PaymentManager singleton
   - Thread safety improvements scheduled

3. **Status Code Management**: Integer-based status codes
   - Migration to enum-based status system under consideration

4. **Legacy Instantiation**: Direct object instantiation patterns
   - Dependency injection framework evaluation in progress

5. **Domain Model**: Public field access in some entity classes
   - Encapsulation improvements planned

6. **Card Processing**: Separate methods for different card types
   - Consolidation and refactoring under evaluation

7. **Error Handling**: Variable exception handling patterns
   - Standardization initiative planned

8. **Resource Management**: Some areas need improved resource cleanup
   - Try-with-resources migration planned

### Critical Constraints
When working with the codebase, be aware of these critical constraints:

1. **COBOL Integration**: LegacyPaymentBridge uses fixed 62-byte records
   - **DO NOT** change field lengths or ordering
   - Breaking changes will disrupt nightly batch reconciliation
   - Coordinate any changes with mainframe operations team

2. **Serialization**: Transaction class is serialized for batch processing
   - Maintain serialization compatibility
   - Test thoroughly with batch processing system

3. **Nightly Batch**: Schema changes must maintain batch compatibility
   - Coordinate database changes with batch processing team
   - Test against batch reconciliation procedures

4. **Java 1.6 Compatibility**: Source code must compile with Java 1.6
   - No Java 7+ language features (lambdas, try-with-resources, etc.)
   - Can run on Java 8+ but must maintain 1.6 source compatibility

## Working with This Codebase

### Development Guidelines
1. **Understand Legacy Constraints**: Review critical constraints section above before making changes
2. **Maintain Compatibility**: Coordinate changes to LegacyPaymentBridge or Transaction serialization with operations team
3. **Database Operations**: Follow established patterns when adding new database operations
4. **Fixed-Length Records**: Mainframe integration requires exact 62-byte format (see LegacyPaymentBridge)

### Payment Processing Flow
1. Request received via RestApiController or PaymentServlet
2. PaymentManager.processPayment() validates and processes the transaction
3. Card type detected by first digit (4=Visa, 5=Mastercard, 3=Amex)
4. Routed to appropriate card-specific processing method
5. Transaction persisted to database
6. Customer record updated
7. Notifications dispatched (email, SMS, webhook)
8. Result returned to caller

### Batch Processing Flow
1. Files downloaded from FTP server to `/var/data/megabank/incoming/`
2. TransactionBatchProcessor reads CSV/DAT files
3. Each line parsed: card_number,cvv,expiry,amount,merchant_id,customer_id
4. Transactions processed via PaymentManager.processPayment()
5. Errors logged to `/var/data/megabank/errors/`
6. Successfully processed files archived to `/var/data/megabank/archive/`
7. Summary report generated and distributed

### Areas Identified for Security Review
As part of the ongoing security audit, the following areas have been identified for review and remediation:

- **Data Access Layer**: DatabaseHelper, PaymentManager, AuthManager database operations
- **Configuration Management**: Credential storage in application.properties and related configuration files
- **Data Handling**: Transaction and customer data fields, database schema
- **Command Processing**: BatchProcessor command-line argument handling
- **Debug Features**: Debug endpoints and diagnostic functions

These items are tracked in the issue management system and prioritized based on risk assessment.

## Testing

The test suite is currently undergoing refactoring as part of the modernization initiative. See issue #12 for details on the testing framework upgrade.

Available test classes:
- `src/test/java/com/megabank/paymentgateway/PaymentManagerTest.java`
- `src/test/java/com/megabank/paymentgateway/CryptoUtilsTest.java`

Run with: `mvn test`

**Note**: The Maven Surefire plugin requires re-enablement in pom.xml as part of the test framework upgrade initiative.

## Environment Detection

The application supports multiple deployment environments via the `env` system property:
- **DEV**: Development environment (localhost:3306)
- **UAT**: User acceptance testing (uat-db.megabank.internal)
- **PROD**: Production environment (prod-db.megabank.internal)

Configure environment:
```bash
java -Denv=DEV -jar payment-gateway.war
```

**Default Behavior**: If no environment is specified, the system defaults to PROD configuration. Always explicitly set the environment property for non-production deployments.

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

Due to the legacy nature of the codebase, CI/CD scans will report known technical debt items:
- ✅ Build should succeed
- ⚠️ Tests currently skipped (testing framework upgrade in progress)
- ⚠️ 50+ dependency vulnerabilities (legacy versions - upgrade initiative in progress)
- ⚠️ Configuration management findings (credential vault migration planned)
- ⚠️ Data access pattern findings (parameterized query migration scheduled)
- ⚠️ Data storage findings (PCI compliance alignment in progress)

**PR Review Focus**: Pull requests should not introduce NEW technical debt items. Existing legacy items are tracked separately in the backlog and addressed through planned initiatives.

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
