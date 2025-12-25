# Issue #10: Upgrade outdated dependencies with known vulnerabilities

**Priority:** 🟡 Medium
**Type:** Security, Tech Debt
**Difficulty:** Hard
**Estimated Time:** 2-3 days

## Description

The project uses severely outdated dependencies with known CVEs (Common Vulnerabilities and Exposures). Dependency scanning has flagged critical vulnerabilities.

## Current Dependencies (from pom.xml)

| Dependency | Current | Latest | CVEs |
|------------|---------|--------|------|
| Spring Core | 3.2.18 | 6.1.x | Multiple RCE vulnerabilities |
| Hibernate | 3.6.10 | 6.4.x | SQL injection, deserialization |
| MySQL Connector | 5.1.6 | 8.3.x | Authentication bypass |
| Log4j | 1.2.16 | 2.23.x | **Log4Shell (CVE-2021-44228)** |
| Commons Collections | (transitive) | | Deserialization RCE |
| Xerces | 2.9.1 | 2.12.2 | XXE vulnerabilities |
| JSON | 20090211 | 20231013 | DoS vulnerabilities |

## Java Version

Current: Java 1.6 (End of Life: Feb 2013!)
Recommended: Java 17 LTS or Java 21 LTS

## Requirements

- [ ] Upgrade Java version to 17 LTS minimum
- [ ] Upgrade Spring Framework to 6.x
- [ ] Upgrade Hibernate to 6.x
- [ ] Replace Log4j 1.x with Log4j 2.x or SLF4J
- [ ] Update MySQL connector to 8.x
- [ ] Update all other dependencies to latest stable
- [ ] Run dependency vulnerability scan (OWASP Dependency Check)
- [ ] Fix any breaking API changes
- [ ] Update Maven plugins

## Demo Scenario

Ask Claude Code: *"Analyze the pom.xml and identify all outdated dependencies with security vulnerabilities. Create an upgrade plan."*

## Migration Considerations

1. **Spring 3 → 6**: Major API changes, XML config deprecated
2. **Hibernate 3 → 6**: HQL syntax changes, new Session API
3. **Java 6 → 17**: Many deprecated APIs removed
4. **Log4j migration**: Configuration format completely different

## Acceptance Criteria

1. No dependencies with known critical CVEs
2. All dependencies on supported versions
3. Java 17+ runtime
4. All tests passing after upgrade
5. Dependency check report clean
