# MegaBank Payment Gateway

[![CI Pipeline](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/ci.yml/badge.svg)](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/ci.yml)
[![Security Scan](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/security-scan.yml/badge.svg)](https://github.com/pmanlukas/Payment-Gateway/actions/workflows/security-scan.yml)

Enterprise Payment Processing System - Est. 2003

## Overview

The MegaBank Payment Gateway is a Java-based payment processing system that has been serving our enterprise clients since 2003. Originally built as a rapid prototype to capture market share, the system has evolved over two decades through multiple development teams and business requirements.

**Current Technical Landscape:**

The codebase reflects its long history and the various teams that have worked on it:
- Original architecture from 2003 using Java 1.6 and Servlet 2.4
- Integration with legacy COBOL mainframe systems (still operational)
- REST API layer added in 2018 for modern integrations
- Batch processing infrastructure for nightly reconciliation
- MySQL database for transaction storage and customer management

**Note**: The system is currently undergoing a modernization effort. Several legacy patterns and technical debt items have been identified and are being addressed as part of our ongoing security and compliance initiatives.

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
- ⏳ Discover and JCB support (planned - see issue #5)
- Transaction processing with mainframe integration
- Batch reconciliation infrastructure
- Multiple merchant support

### API
- RESTful API interface (added in 2018)
- API key-based authentication
- Transaction creation and retrieval
- Customer management
- Merchant operations
- See [API.md](API.md) for complete endpoint documentation

### Architecture
- Core payment processing engine (PaymentManager)
- Servlet-based web layer
- JDBC data access layer
- Legacy mainframe integration via TCP/IP
- Batch processing subsystem
- See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation

### Monitoring & Analytics
- Prometheus metrics collection
- Grafana dashboards for business and system metrics
- Jupyter notebooks for data analysis
- Real-time transaction monitoring
- See [MONITORING.md](MONITORING.md) and [GRAFANA_QUICKSTART.md](GRAFANA_QUICKSTART.md)

## Documentation

- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development setup and guidelines
- **[API.md](API.md)** - REST API documentation
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment procedures
- **[MONITORING.md](MONITORING.md)** - Monitoring and observability guide
- **[CLAUDE.md](CLAUDE.md)** - Architecture and development guide
- **[.github/CI-CD.md](.github/CI-CD.md)** - CI/CD pipeline documentation

## API Endpoints

```bash
# Process a payment
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

# Get transaction details
curl -X GET http://localhost:8080/transactions/TXN123 \
  -H "X-API-Key: test_key_12345"

# Debug endpoint
curl -X GET http://localhost:8080/debug \
  -H "X-API-Key: test_key_12345"
```

See [API.md](API.md) for complete API documentation.

## Testing

```bash
mvn test
```

**Note**: The test suite is currently undergoing refactoring. See issue #12 for details on the testing modernization initiative.

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

- Java 1.6 (source/target compatibility, runs on Java 8+)
- Spring Framework 3.2.18
- Hibernate ORM 3.6.10
- MySQL 5.5+
- Apache Log4j 1.2.16
- Servlet API 2.4
- Apache Maven 3.x
- Docker & Docker Compose

**Note**: The technology stack reflects the system's original 2003 architecture. A modernization initiative is planned to upgrade to current LTS versions while maintaining backward compatibility with legacy integrations.

## Security & Compliance

### Security Scanning

The CI/CD pipeline includes automated security scanning:
- Secret detection (Gitleaks)
- SQL injection pattern analysis
- PCI-DSS compliance validation
- OWASP dependency vulnerability scanning
- GitHub CodeQL semantic analysis

### Known Security Considerations

The codebase is currently undergoing a security audit. Several areas have been identified for improvement:

1. **Data Access Patterns** - Migration to parameterized queries planned
2. **Credential Management** - Transition to secure credential storage in progress
3. **PCI-DSS Compliance** - Audit findings being addressed (see issue #9)
4. **Cryptography Modernization** - Upgrade to current algorithms scheduled
5. **Authentication & Authorization** - Enhanced security controls under development
6. **Logging Practices** - Review of sensitive data handling in logs

These items are tracked in our issue tracker and scheduled for resolution as part of the ongoing modernization effort.

## Contributing

We welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development environment setup
- Code style guidelines
- Pull request process
- Legacy constraints you must respect

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for deployment procedures including:
- Pre-deployment checklist
- Database migration procedures
- Rollback procedures
- Production deployment considerations
- Environment configuration

## Version History

- **2.3.1** - Current (December 2024)
  - Added CI/CD pipeline with automated security scanning
  - Created comprehensive documentation
  - Integrated monitoring and analytics capabilities
  - Added Grafana dashboards for operational insights
- **2.3** - Security updates (2020)
  - Applied critical security patches
  - Updated dependency versions
- **2.2** - REST API (2018)
  - Added RESTful API layer
  - Modernized authentication mechanism
- **2.1** - Maintenance releases (2009-2015)
  - Various bug fixes and minor enhancements
- **2.0** - Architecture refactor (2008)
  - Major refactoring initiative
  - Enhanced mainframe integration
- **1.2** - Stabilization (2005)
  - Performance optimizations
  - Bug fixes
- **1.1** - Early improvements (2004)
  - Initial bug fixes and enhancements
- **1.0** - Initial release (2003)
  - Core payment processing capabilities
  - Mainframe integration
  - Basic transaction management

## Team

Development and maintenance is handled by the MegaBank engineering team with contributions from various teams over the years.

## License

See [LICENSE](LICENSE) file for details.

## Support

- 📖 Documentation: Comprehensive guides available in the docs directory
- 🐛 Bug Reports: [GitHub Issues](https://github.com/pmanlukas/Payment-Gateway/issues)
- 💬 Questions: Open an issue or consult the documentation
- 🔒 Security: Report security concerns through the issue tracker with "security" label

---

For operational support and questions, please consult the [CONTRIBUTING.md](CONTRIBUTING.md) and [MONITORING.md](MONITORING.md) guides.
