# Contributing to MegaBank Payment Gateway

Thank you for your interest in contributing to the MegaBank Payment Gateway! This document provides guidelines and instructions for setting up your development environment and contributing to the project.

## ⚠️ Important Context

This is a **legacy codebase** with intentional security vulnerabilities for demonstration and educational purposes. While we welcome improvements, please understand the constraints and purpose of this project.

## Table of Contents

- [Development Setup](#development-setup)
- [Building the Project](#building-the-project)
- [Running Tests](#running-tests)
- [Code Style Guidelines](#code-style-guidelines)
- [Submitting Changes](#submitting-changes)
- [Working with Issues](#working-with-issues)
- [Legacy Constraints](#legacy-constraints)

## Development Setup

### Prerequisites

- **Java Development Kit (JDK)**: Java 8 or higher
  - Note: Code must remain Java 6 compatible (source/target)
  - Download from [AdoptOpenJDK](https://adoptopenjdk.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Apache Maven**: 3.x
  - Download from [Maven website](https://maven.apache.org/download.cgi)
- **MySQL**: 5.5 or higher
  - Download from [MySQL website](https://dev.mysql.com/downloads/)
- **Git**: For version control
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Clone the Repository

```bash
git clone https://github.com/pmanlukas/Payment-Gateway.git
cd Payment-Gateway
```

### Database Setup

1. **Install MySQL** (if not already installed)

2. **Create the database**:
```bash
mysql -u root -p
```

```sql
CREATE DATABASE payments;
CREATE USER 'payment_admin'@'localhost' IDENTIFIED BY 'Payment123!';
GRANT ALL PRIVILEGES ON payments.* TO 'payment_admin'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

3. **Initialize the schema**:
```bash
mysql -u payment_admin -p payments < src/main/resources/schema.sql
```

4. **Verify the setup**:
```bash
mysql -u payment_admin -p payments -e "SHOW TABLES;"
```

You should see: customers, transactions, blacklisted_cards, users, api_keys, merchants, audit_log, sessions, debug_data

### Environment Configuration

1. **Set environment variable** (optional):
```bash
export PAYMENT_ENV=DEV
# or in Windows:
set PAYMENT_ENV=DEV
```

2. **Update configuration** (if needed):
Edit `src/main/resources/application.properties` for local development settings.

**Note**: For security demonstrations, credentials are intentionally hardcoded. In a real project, use environment variables or a secrets manager.

### Build the Project

```bash
# Clean and compile
mvn clean compile

# Package (creates JAR)
mvn clean package

# Skip tests (currently disabled anyway)
mvn clean package -DskipTests
```

### Run the Application

#### Option 1: Deploy to Tomcat

1. Install Apache Tomcat 7+
2. Copy the generated JAR/WAR to Tomcat's webapps directory
3. Start Tomcat: `./bin/catalina.sh run` (or `catalina.bat run` on Windows)
4. Access at: `http://localhost:8080/payment-gateway`

#### Option 2: Maven Tomcat Plugin (if configured)

```bash
mvn tomcat7:run
```

## Building the Project

### Full Build
```bash
mvn clean install
```

### Quick Compile
```bash
mvn compile
```

### Package Only
```bash
mvn package -DskipTests
```

### Build with Dependency Check
```bash
mvn clean package
# OWASP dependency check runs via CI/CD
```

## Running Tests

### Current Status
⚠️ The test suite is currently **disabled** in `pom.xml` (surefire plugin commented out). See issue #12 for the effort to re-enable tests.

### Running Tests Locally (when enabled)

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentManagerTest

# Run specific test method
mvn test -Dtest=PaymentManagerTest#testProcessPayment

# Run with coverage (when JaCoCo is configured)
mvn test jacoco:report
```

### Writing Tests

Tests should be placed in `src/test/java` mirroring the source structure:

```
src/test/java/com/megabank/paymentgateway/
├── service/
│   └── PaymentManagerTest.java
├── util/
│   └── CryptoUtilsTest.java
└── api/
    └── RestApiControllerTest.java
```

**Test Naming Convention**:
- Test classes: `<ClassName>Test.java`
- Test methods: `test<MethodName>_<Scenario>_<ExpectedResult>()`

Example:
```java
public void testProcessPayment_ValidCard_ReturnsApproved() {
    // Arrange
    // Act
    // Assert
}
```

## Code Style Guidelines

### Java Coding Standards

This is a **legacy codebase**, so we maintain the existing style for consistency:

1. **Indentation**: 4 spaces (not tabs)
2. **Braces**: Opening brace on same line
3. **Line Length**: Try to keep under 120 characters
4. **Naming Conventions**:
   - Classes: `PascalCase`
   - Methods: `camelCase`
   - Constants: `UPPER_SNAKE_CASE`
   - Variables: `camelCase`

### What NOT to Change

To maintain the educational/demonstration value:

❌ **Don't fix these intentionally**:
- Hardcoded credentials (they're part of the demo)
- SQL injection vulnerabilities (unless working on a specific issue to fix them)
- PCI violations (unless working on issue #9)
- God class anti-pattern in PaymentManager
- Magic numbers for status codes
- Public fields in entities

✅ **Do improve**:
- New features with proper patterns
- Documentation
- Test coverage
- Build configuration
- CI/CD pipeline
- Non-security-related bugs

### Comments

- Add JavaDoc for new public methods
- Comment complex business logic
- Don't over-comment obvious code
- Keep the satirical TODO comments (they're part of the charm)

## Submitting Changes

### Workflow

1. **Create an issue** (or pick an existing one)
2. **Create a branch** from `main`:
   ```bash
   git checkout -b fix/issue-number-short-description
   # or
   git checkout -b feature/issue-number-short-description
   ```

3. **Make your changes**
4. **Test locally**:
   ```bash
   mvn clean package
   # Run tests if working on test-related issues
   ```

5. **Commit with conventional commits**:
   ```bash
   git commit -m "fix: resolve transaction endpoint 404 error"
   git commit -m "feat: add Discover card support"
   git commit -m "security: fix SQL injection in customer search"
   ```

6. **Push your branch**:
   ```bash
   git push origin your-branch-name
   ```

7. **Create a Pull Request**
   - Use the PR template (auto-populated)
   - Link to the issue: "Fixes #123"
   - Fill out all relevant sections
   - Wait for CI/CD checks to complete

### Commit Message Format

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `security`: Security fix
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `docs`: Documentation changes
- `ci`: CI/CD changes
- `chore`: Maintenance tasks

**Examples**:
```
feat: add JCB card network support

Implements detection and processing for JCB cards starting with 35.

Fixes #5
```

```
security: replace string concatenation with PreparedStatement

Fixes SQL injection vulnerability in searchCustomers() method.
This addresses the critical security issue found in recent audit.

Fixes #7
```

### Pull Request Checklist

Before submitting your PR, ensure:

- [ ] Code compiles without errors
- [ ] New code follows existing patterns
- [ ] No new security vulnerabilities introduced (check CI/CD)
- [ ] No new hardcoded secrets (unless intentional for demo)
- [ ] Tests pass (if enabled)
- [ ] Documentation updated (if needed)
- [ ] PR template fully filled out
- [ ] Linked to related issue

## Working with Issues

### Finding Issues to Work On

- **Good First Issues**: Tagged with `good first issue` - great for newcomers
- **Easy**: ⭐ Single star difficulty
- **Medium**: ⭐⭐ Two stars
- **Hard**: ⭐⭐⭐ Three stars

### Issue Labels

- `bug`: Something isn't working
- `enhancement`: New feature or request
- `security`: Security-related issue
- `compliance`: PCI-DSS or regulatory compliance
- `ci/cd`: CI/CD pipeline improvements
- `financial-impact`: Has real business/financial implications
- `good first issue`: Good for newcomers

### Before Starting Work

1. Comment on the issue: "I'd like to work on this"
2. Wait for assignment or confirmation
3. Ask questions if anything is unclear
4. Check for related issues or PRs

## Legacy Constraints

### Things You CANNOT Change

1. **Java 6 Compatibility**
   - No lambdas, streams, try-with-resources, etc.
   - Source/target must remain 1.6

2. **Mainframe Integration**
   - `LegacyPaymentBridge` fixed-length records (62 bytes)
   - Changing field lengths breaks nightly batch reconciliation

3. **Serialization Compatibility**
   - `Transaction` class is serialized to disk
   - Field changes break deserialization

4. **Dependency Versions** (mostly)
   - Spring 3.2.18 (major upgrade requires extensive refactoring)
   - Hibernate 3.6.10 (HQL queries incompatible with newer versions)
   - See `dependency-check-suppression.xml` for details

### Coordinating Breaking Changes

If your work requires breaking changes:

1. **Discuss in the issue** before implementing
2. **Coordinate with teams**:
   - Database changes → Notify DBA team
   - Mainframe integration → Notify mainframe team
   - Batch processing → Notify operations team
3. **Create migration plan**
4. **Test extensively** in staging

## Development Tips

### Useful Maven Commands

```bash
# See dependency tree
mvn dependency:tree

# Check for dependency updates
mvn versions:display-dependency-updates

# Run single test class
mvn test -Dtest=PaymentManagerTest

# Generate site documentation
mvn site
```

### Debugging

1. **Enable debug logging**:
   Edit `src/main/resources/log4j.properties`:
   ```properties
   log4j.rootLogger=DEBUG, stdout
   ```

2. **Remote debugging**:
   ```bash
   export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
   mvn tomcat7:run
   ```
   Then attach your IDE debugger to port 5005.

3. **Database debugging**:
   ```bash
   mysql -u payment_admin -p payments
   ```

### Common Issues

**Build fails with "package does not exist"**:
```bash
mvn clean install -U
```

**Tests fail with database connection error**:
- Check MySQL is running: `mysql -u root -p`
- Verify credentials in application.properties
- Check database exists: `SHOW DATABASES;`

**OutOfMemoryError during build**:
```bash
export MAVEN_OPTS="-Xmx1024m"
mvn clean package
```

## Getting Help

- **GitHub Issues**: For bugs and feature requests
- **Pull Request Comments**: For code review questions
- **CLAUDE.md**: Architecture and codebase guide
- **CI-CD.md**: CI/CD pipeline documentation
- **API.md**: API endpoint documentation

## Code of Conduct

- Be respectful and professional
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Remember this is an educational project

## License

This project is licensed under the terms specified in the LICENSE file.

---

Thank you for contributing to the MegaBank Payment Gateway! Your improvements help make this a better educational resource for understanding legacy systems and security practices.
