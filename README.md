# MegaBank Payment Gateway

> ⚠️ **DISCLAIMER**: This is a deliberately vulnerable legacy codebase created for educational and demonstration purposes. It contains intentional security flaws, anti-patterns, and legacy practices. **DO NOT use in production.**

[![CI Pipeline](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/ci.yml/badge.svg)](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/ci.yml)
[![Security Scan](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/security-scan.yml/badge.svg)](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/security-scan.yml)

Enterprise Payment Processing System - Est. 2003

## Overview

The MegaBank Payment Gateway is a "modern" Java-based payment processing system that demonstrates what happens when a codebase accumulates 20 years of technical debt, changing developers, and zero security reviews.

**This codebase intentionally contains:**
- 🔓 Hardcoded credentials and API keys
- 💉 SQL injection vulnerabilities
- 💳 PCI-DSS violations (CVV storage!)
- 🔑 Plain text password storage
- 🔐 Broken cryptography (DES, MD5, SHA1)
- 🚪 Backdoor access methods
- 📝 Sensitive data logging

**Purpose**: Educational demonstration of legacy code patterns, security vulnerabilities, and technical debt.

## Quick Start

### Option 1: Docker (Recommended - 5 minutes)

```bash
# Clone the repository
git clone https://github.com/pmanlukas/Payment-Gateway.git
cd Payment-Gateway

# Start everything with Docker Compose
docker-compose up -d

# Wait for services to start (~60 seconds)
# Access at http://localhost:8080
```

**Or use the quick start script:**
```bash
./scripts/quick-start.sh
```

**Services available:**
- Payment Gateway API: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin123)
- MySQL: localhost:3306

See [docker/README.md](docker/README.md) for details.

### Option 2: Manual Setup

**Prerequisites:**
- Java 8+ (code targets Java 6 for... historical reasons)
- Apache Maven 3.x
- MySQL 5.5+
- A strong stomach for legacy code

```bash
# Setup database
mysql -u root -p < src/main/resources/schema.sql

# Build
mvn clean package

# Deploy to Tomcat
# (or your favorite servlet container from 2005)
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed setup instructions.

## Features

### Payment Processing
- ✅ Visa, Mastercard, and Amex support
- ⏳ Discover and JCB coming soon (see issue #5)
- ❌ No idempotency (duplicate charges are a feature!)
- ❌ No fraud detection (trust everyone!)
- ⚠️ Stores CVV in plain text (PCI auditors hate this one trick!)

### API
- REST-ish API (added in 2018, very modern!)
- Authentication via hardcoded API keys
- Returns full card numbers in responses (what could go wrong?)
- Debug endpoint that exposes all secrets
- See [API.md](API.md) for details

### Architecture
- God class pattern (PaymentManager does EVERYTHING)
- Singleton with broken double-checked locking
- SQL queries via string concatenation
- COBOL mainframe integration (don't touch it!)
- See [CLAUDE.md](CLAUDE.md) for architecture details

## Documentation

- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development setup and guidelines
- **[API.md](API.md)** - REST API documentation
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment procedures
- **[MONITORING.md](MONITORING.md)** - Monitoring and observability guide
- **[CLAUDE.md](CLAUDE.md)** - Architecture and development guide
- **[.github/CI-CD.md](.github/CI-CD.md)** - CI/CD pipeline documentation

## API Endpoints

```bash
# Process a payment (no idempotency, yolo!)
curl -X POST http://localhost:8080/payments \
  -H "X-API-Key: test_key_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "cvv": "123",
    "expiry": "12/25",
    "amount": 99.99,
    "currency": "USD"
  }'

# Get transaction details (returns CVV and everything!)
curl -X GET http://localhost:8080/transactions/TXN123 \
  -H "X-API-Key: test_key_12345"

# Debug endpoint (exposes all credentials!)
curl -X GET http://localhost:8080/debug \
  -H "X-API-Key: test_key_12345"
```

See [API.md](API.md) for complete API documentation.

## Testing

```bash
mvn test
```

**Note**: Tests are currently disabled (surefire plugin commented out since 2012). See issue #12 to help fix this!

## CI/CD Pipeline

Automated workflows for:
- ✅ Build and compile
- 🔍 Security scanning (Gitleaks, SQL injection, PCI compliance)
- 📊 Dependency vulnerability scanning (OWASP)
- 🎯 PR validation with delta analysis
- 📈 Code quality checks

See [.github/CI-CD.md](.github/CI-CD.md) for details.

## Known Issues

We have many! Check out our [Issues page](https://github.com/pmanlukas/Payment-Gateway/issues).

**Priority Issues**:
- Issue #9: Remove CVV storage (PCI compliance blocker)
- Issue #7: Fix SQL injection in customer search (critical)
- Issue #6: Implement payment idempotency (financial impact)
- Issue #12: Enable test suite

## Technology Stack

- Java 1.6 (source/target, runs on Java 8+)
- Spring 3.2.18 (end of life since 2016)
- Hibernate 3.6.10 (vintage!)
- MySQL 5.5+
- Log4j 1.2.16 (the old one, not Log4Shell affected but still old)
- Servlet API 2.4
- Maven 3.x

**Yes, these are all ancient. That's the point.**

## Security

### Intentional Vulnerabilities

This codebase contains **intentional** security vulnerabilities for educational purposes:

1. **SQL Injection** - All database operations use string concatenation
2. **Hardcoded Secrets** - Credentials throughout the code
3. **PCI-DSS Violations** - CVV stored in database
4. **Broken Cryptography** - DES, MD5, SHA1
5. **Authentication Issues** - SQL injection in login
6. **Authorization Issues** - No checks on sensitive operations
7. **Information Disclosure** - Debug endpoints, excessive logging

### Security Scanning

The CI/CD pipeline includes:
- Gitleaks for secret detection
- Custom SQL injection pattern scanning
- PCI-DSS compliance checks
- OWASP dependency vulnerability scanning
- GitHub CodeQL analysis

## Contributing

We welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development environment setup
- Code style guidelines
- Pull request process
- Legacy constraints you must respect

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment procedures including:
- Pre-deployment checklist
- Database migrations
- Rollback procedures
- Production considerations

**Warning**: Please don't actually deploy this to production. It's for demonstration only!

## Version History

- **2.3.1** - Current (December 2024)
  - Added CI/CD pipeline
  - Created comprehensive documentation
  - Added 10 issues for demonstration
- **2.3** - Security patches (2020, maybe)
- **2.2** - Added "modern" REST API (2018)
- **2.1** - Hotfixes (2009-2015)
- **2.0** - Major rewrite attempt (abandoned 2008)
- **1.2** - More fixes (2005)
- **1.1** - Fixed something (2004)
- **1.0** - Initial release (2003)

## Team

- **Original Author**: Unknown (left before Git was adopted)
- **Early Maintainer**: Bob (left 2007)
- **Current Maintainer**: You? (We're hiring!)
- **Emergency Contact**: The ops team (if they still exist)

## License

See [LICENSE](LICENSE) file for details.

## Acknowledgments

This codebase was created to demonstrate:
- Legacy system patterns
- Common security vulnerabilities
- Technical debt accumulation
- Real-world enterprise constraints

Special thanks to every developer who has ever inherited a legacy codebase and thought "how did this happen?"

---

**Remember**: This is intentionally bad code for educational purposes. Don't let your production code end up like this!

## Support

- 📖 Documentation: See docs above
- 🐛 Bug Reports: [GitHub Issues](https://github.com/pmanlukas/Payment-Gateway/issues)
- 💬 Questions: Open an issue
- 🔒 Security: See intentional vulnerabilities above (everything is a "security issue" here!)

---

*"It works in production... we think." - Anonymous Developer, 2015*
