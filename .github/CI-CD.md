# CI/CD Pipeline Documentation

This document describes the GitHub Actions CI/CD pipeline for the MegaBank Payment Gateway.

## Overview

The CI/CD pipeline consists of multiple workflows designed to catch security issues, build/test the application, and provide visibility into code changes. Given the legacy nature of this codebase, many checks are configured to warn rather than fail.

## Workflows

### 1. CI Pipeline (`ci.yml`)
**Triggers**: Push to `main`/`develop`, Pull Requests

**Jobs**:
- **Build and Test**: Compiles code, runs tests (if enabled), packages JAR
- **Code Quality Check**: Static analysis and secret scanning
- **Dependency Check**: OWASP vulnerability scanning

**Key Features**:
- Caches Maven dependencies for faster builds
- Test failures don't block pipeline (tests are disabled in pom.xml)
- Generates dependency vulnerability report
- Uploads build artifacts for 30 days
- Comments on PRs with vulnerability summary

**Artifacts Produced**:
- `payment-gateway-jar` - Packaged JAR file
- `dependency-check-report` - HTML/JSON vulnerability report

### 2. Security Scanning (`security-scan.yml`)
**Triggers**: Push to `main`, Pull Requests, Weekly schedule (Mondays 9am UTC), Manual

**Jobs**:
- **Secret Scanning**: Detects hardcoded credentials using Gitleaks + custom patterns
- **SQL Injection Scan**: Finds string concatenation in SQL queries
- **PCI Compliance Check**: Scans for CVV storage, card number handling, SSN storage
- **CodeQL Analysis**: GitHub's semantic code analysis for Java

**Key Features**:
- Scans for multiple secret patterns (DB passwords, API keys, encryption keys)
- Identifies SQL injection vulnerabilities (string concatenation)
- Checks PCI-DSS compliance (CVV storage violations)
- Comments on PRs with PCI compliance status
- Runs weekly to catch any new issues

**Why Some Checks Continue-on-Error**:
This codebase is **intentionally** full of security issues for demonstration purposes. Checks are informational but don't block PRs.

**Artifacts Produced**:
- `security-scan-report` - Hardcoded secrets found
- `sql-injection-scan-report` - SQL injection vulnerabilities
- `pci-compliance-report` - PCI-DSS violations

### 3. Pull Request Checks (`pr-checks.yml`)
**Triggers**: Pull Request opened/updated

**Jobs**:
- **PR Validation**: Validates PR title format (semantic commits)
- **Changed Files Report**: Analyzes impact of file changes
- **Test Coverage Check**: Reports on test coverage status

**Key Features**:
- Enforces semantic PR titles (feat:, fix:, security:, etc.)
- Checks for large files accidentally committed
- Detects sensitive file patterns (.env, .pem, credentials)
- **Delta analysis**: Only reports NEW issues in the PR
  - New hardcoded passwords/API keys
  - New SQL injection risks
  - New CVV storage violations
- Categorizes changes by impact:
  - ⚠️ Core Payment Processing (HIGH impact)
  - 🗄️ Database Layer (MEDIUM impact)
  - 🏛️ Legacy Integration (CRITICAL - may break mainframe!)
  - 🔒 Security Files (HIGH - requires security review)
- Comments on PR with impact analysis

**Example PR Comment**:
```markdown
## Files Changed in this PR

### ⚠️ Core Payment Processing Files
These changes affect critical payment processing logic:
- src/main/java/.../PaymentManager.java

**Impact**: HIGH - Requires thorough testing
```

### 4. Dependency Review (`dependency-review.yml`)
**Triggers**: PRs that modify `pom.xml`

**Jobs**:
- **Dependency Review**: GitHub's dependency review action
- **Check for Dependency Changes**: Analyzes what changed in pom.xml

**Key Features**:
- Fails on HIGH severity vulnerabilities (but allows through for legacy deps)
- Warns about dependency changes and their implications
- Reminds developers about legacy constraints:
  - Java 1.6 compatibility
  - Mainframe integration dependencies
  - Serialized Transaction object compatibility
- Comments on PR with upgrade warnings

## Dependabot Configuration

**File**: `.github/dependabot.yml`

Dependabot is configured but heavily constrained:
- **Maven dependencies**: Checked monthly (most upgrades blocked)
- **GitHub Actions**: Checked weekly
- **Ignored**: Major/minor version updates for Spring, Hibernate, MySQL

**Why?** Legacy dependencies are pinned due to:
- Java 6 compatibility requirements
- COBOL mainframe integration
- Deprecated HQL queries in Hibernate 3.6
- Serialization compatibility

## Pull Request Template

**File**: `.github/PULL_REQUEST_TEMPLATE.md`

Comprehensive PR template with:
- Description and issue linking
- Type of change classification
- Impact assessment checkboxes
- Testing checklist
- Security checklist (SQL injection, hardcoded secrets, PCI compliance)
- Deployment considerations
- Rollback plan section
- Reviewer checklist

## OWASP Dependency Check Suppression

**File**: `dependency-check-suppression.xml`

Suppresses known CVEs in legacy dependencies that cannot be upgraded:
- Spring 3.2.18 (CVE-2016-1000027, CVE-2014-0054, etc.)
- Hibernate 3.6.10
- MySQL Connector 5.1.6
- Log4j 1.2.16 (CVE-2019-17571, CVE-2020-9488, etc.)
- Apache Commons libraries
- org.json (2009 version)

Each suppression includes notes explaining why upgrade is blocked.

## Running Workflows Manually

### Trigger Security Scan Manually
```bash
gh workflow run security-scan.yml
```

### View Workflow Runs
```bash
gh run list --workflow=ci.yml
gh run list --workflow=security-scan.yml
```

### Download Artifacts
```bash
gh run download <run-id>
```

## Understanding Check Results

### Expected Failures/Warnings
The following are EXPECTED in this legacy codebase:
- ✅ Build succeeds
- ⚠️ Tests skipped (surefire plugin disabled)
- ⚠️ 50+ dependency vulnerabilities (legacy versions)
- ⚠️ Hundreds of hardcoded secrets detected
- ⚠️ 20+ SQL injection vulnerabilities
- ⚠️ Multiple PCI-DSS violations (CVV storage)
- ⚠️ CodeQL findings (high severity)

### What Should Fail a PR
Despite the legacy code, NEW issues should be questioned:
- New hardcoded passwords/API keys in PR diff
- New SQL string concatenation (use PreparedStatement!)
- New CVV storage (PCI-DSS violation)
- Breaking changes to LegacyPaymentBridge (mainframe integration)
- Dependencies that break Java 6 compatibility

## CI/CD Roadmap

### Phase 1 (Current)
- ✅ Automated builds
- ✅ Security scanning (informational)
- ✅ PR validation
- ✅ Artifact generation

### Phase 2 (Planned)
- [ ] Enable test suite (after fixing failing tests)
- [ ] Add code coverage reporting (JaCoCo)
- [ ] Staging environment deployment
- [ ] Integration test execution

### Phase 3 (Future)
- [ ] Production deployment automation
- [ ] Smoke tests after deployment
- [ ] Rollback automation
- [ ] Performance regression testing

## Troubleshooting

### Build Failures

**Maven compile fails**:
```bash
# Local testing
mvn clean compile
```

**Java version mismatch**:
- Pipeline uses Java 8 (to support Java 6 source/target)
- Ensure local environment matches

### Secret Scanning False Positives

If Gitleaks flags legitimate code:
1. Review the finding carefully
2. If truly a false positive, add to `.gitleaksignore`
3. Document why it's safe in PR description

### Dependency Check Timeouts

OWASP Dependency Check can be slow (5-10 minutes):
- Downloads CVE database on first run
- Subsequent runs are faster (cached)
- Can be disabled locally with suppression file

## Integration with Development Workflow

### Recommended Workflow
1. Create branch from `main`
2. Make changes
3. Push to GitHub
4. Create Pull Request
5. Review CI/CD check results
6. Address any NEW security issues flagged
7. Get approval from reviewers
8. Merge to `main`

### Local Testing Before Push
```bash
# Build
mvn clean package

# Check for obvious issues
grep -r "password.*=.*\"" src/
grep -r "SELECT.*+" src/

# Run tests (if enabled)
mvn test
```

## Security Considerations

### Secrets in CI/CD
- No production secrets stored in GitHub Secrets (code has them hardcoded already!)
- GitHub token auto-provided for API access
- Future: Move secrets to GitHub Secrets + environment variables

### Branch Protection
Recommended settings:
- Require PR reviews before merge
- Require status checks to pass (when tests are enabled)
- Require branches to be up to date
- Restrict who can push to `main`

## Metrics & Monitoring

### Workflow Success Rate
Track in GitHub Actions dashboard:
- Build success rate
- Test pass rate (when enabled)
- Average build time

### Security Trends
Monitor over time:
- Number of dependency vulnerabilities (should decrease)
- New secrets introduced (should be zero)
- PCI compliance violations (should decrease)

## Related Documentation
- [README.md](../README.md) - Project overview
- [CLAUDE.md](../CLAUDE.md) - Development guide
- Issues tagged `ci/cd` - CI/CD improvements
