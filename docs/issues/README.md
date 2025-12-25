# MegaBank Payment Gateway - Issue Backlog

This document contains the prioritized backlog of issues for the Payment Gateway modernization project.

## Issue Summary

| # | Title | Priority | Difficulty | Type |
|---|-------|----------|------------|------|
| 1 | [Replace hardcoded credentials](001-hardcoded-credentials.md) | 🔴 Critical | Easy | Security |
| 2 | [Fix SQL injection vulnerabilities](002-sql-injection.md) | 🔴 Critical | Medium | Security |
| 3 | [PCI-DSS: Remove CVV storage](003-pci-dss-cvv-storage.md) | 🔴 Critical | Medium | Compliance |
| 4 | [Refactor PaymentManager God Class](004-refactor-god-class.md) | 🟡 Medium | Hard | Tech Debt |
| 5 | [Replace deprecated crypto algorithms](005-upgrade-crypto.md) | 🔴 Critical | Medium | Security |
| 6 | [Add input validation](006-add-input-validation.md) | 🟡 Medium | Easy | Security |
| 7 | [Fix ignored tests](007-fix-ignored-tests.md) | 🟡 Medium | Easy | Quality |
| 8 | [Remove backdoors and debug endpoints](008-remove-backdoors.md) | 🔴 Critical | Easy | Security |
| 9 | [Remove sensitive data from logs](009-sensitive-data-logging.md) | 🔴 Critical | Easy | Security |
| 10 | [Upgrade outdated dependencies](010-upgrade-dependencies.md) | 🟡 Medium | Hard | Security |
| 11 | [Implement GDPR compliance](011-gdpr-compliance.md) | 🔴 Critical | Hard | Compliance |
| 12 | [Add API documentation](012-add-api-documentation.md) | 🟢 Low | Easy | Documentation |

## Issues by Difficulty

### Easy (Good for Demo Quick Wins)
- #1 - Replace hardcoded credentials
- #6 - Add input validation
- #7 - Fix ignored tests
- #8 - Remove backdoors
- #9 - Remove sensitive data from logs
- #12 - Add API documentation

### Medium
- #2 - Fix SQL injection
- #3 - PCI-DSS CVV removal
- #5 - Upgrade crypto algorithms

### Hard (Major Refactoring)
- #4 - Refactor God Class
- #10 - Upgrade dependencies
- #11 - GDPR compliance

## Demo Scenarios

### Scenario 1: Security Audit
*"Find and fix all security vulnerabilities in this codebase"*
- Covers: Issues #1, #2, #5, #8, #9

### Scenario 2: Compliance Review
*"Make this application PCI-DSS and GDPR compliant"*
- Covers: Issues #3, #9, #11

### Scenario 3: Code Quality
*"Improve code quality and test coverage"*
- Covers: Issues #4, #7, #12

### Scenario 4: Modernization
*"Modernize this legacy application"*
- Covers: Issues #4, #10, #12

## Suggested Demo Order

For a comprehensive Claude Code demo, tackle issues in this order:

1. **#8 - Remove backdoors** (Quick win, impressive security find)
2. **#1 - Hardcoded credentials** (Easy, visible improvement)
3. **#9 - Sensitive logging** (Easy, compliance relevant)
4. **#2 - SQL injection** (Medium, critical security)
5. **#5 - Crypto upgrade** (Medium, technical depth)
6. **#4 - Refactor God Class** (Hard, architecture showcase)

## How to Use

Each issue file contains:
- Description of the problem
- Current vulnerable/problematic code
- Requirements checklist
- Suggested Claude Code prompt
- Acceptance criteria

Simply copy the demo scenario prompt and paste it into Claude Code to demonstrate the capability.
