# Deployment Guide

MegaBank Payment Gateway - Deployment Procedures

⚠️ **WARNING**: This is demonstration software with intentional security vulnerabilities. Do not deploy to production!

## Table of Contents

- [Pre-Deployment Checklist](#pre-deployment-checklist)
- [Environment Setup](#environment-setup)
- [Database Migration](#database-migration)
- [Application Deployment](#application-deployment)
- [Post-Deployment Verification](#post-deployment-verification)
- [Rollback Procedures](#rollback-procedures)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Pre-Deployment Checklist

### Before Every Deployment

- [ ] **Code Review Complete**: All PRs reviewed and approved
- [ ] **CI/CD Checks Pass**: All GitHub Actions workflows green
- [ ] **Tests Pass**: Test suite passes (when enabled)
- [ ] **Security Scan Clear**: No NEW vulnerabilities introduced
- [ ] **Database Backup**: Recent backup available
- [ ] **Rollback Plan**: Documented and tested
- [ ] **Stakeholder Notification**: Teams informed of deployment
- [ ] **Maintenance Window**: Scheduled during low-traffic period

### Coordination Required

Notify these teams **24 hours before deployment**:

- **DBA Team**: For database changes
- **Operations Team**: For server access and monitoring
- **Mainframe Team**: If LegacyPaymentBridge changes
- **Customer Support**: For potential customer impact
- **Security Team**: For security-related changes

### Critical Files Review

Changes to these require extra scrutiny:

- `LegacyPaymentBridge.java` - Mainframe integration (breaks nightly batch!)
- `Transaction.java` - Serialization issues
- `schema.sql` - Database structure changes
- `application.properties` - Configuration changes
- `pom.xml` - Dependency changes

## Environment Setup

### Environments

1. **Development** (`DEV`)
   - Local developer machines
   - Uses localhost database
   - Debug logging enabled

2. **UAT** (`UAT`)
   - Testing environment
   - Mirrors production configuration
   - Uses uat-db.megabank.internal

3. **Production** (`PROD`)
   - Live environment
   - Uses prod-db.megabank.internal
   - **Default environment if not specified!** (dangerous)

### Environment Configuration

Set environment via system property:

```bash
# Development
java -Denv=DEV -jar payment-gateway.jar

# UAT
java -Denv=UAT -jar payment-gateway.jar

# Production (explicit)
java -Denv=PROD -jar payment-gateway.jar
```

### Server Requirements

**Minimum**:
- Java 8+ (targets Java 6 bytecode)
- 2GB RAM
- 10GB disk space
- MySQL 5.5+

**Recommended**:
- Java 8
- 4GB RAM
- 50GB disk space
- MySQL 5.7+

**Application Server**:
- Apache Tomcat 7.x or 8.x
- Jetty 9.x (alternative)
- Any servlet 2.4+ container

## Database Migration

### Pre-Migration Checklist

- [ ] **Backup Database**: Full backup completed
- [ ] **Test Migration**: Run on UAT first
- [ ] **Downtime Estimate**: Calculate and communicate
- [ ] **Rollback Script**: Prepared and tested
- [ ] **DBA Available**: On standby during migration

### Migration Process

1. **Backup Production Database**

```bash
# Create backup
mysqldump -u payment_admin -p payments > payments_backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup
mysql -u payment_admin -p payments < payments_backup_YYYYMMDD_HHMMSS.sql
```

2. **Apply Schema Changes**

```bash
# Review changes first
cat migration_scripts/V2.3.2__add_idempotency_table.sql

# Apply to UAT first
mysql -u payment_admin -p payments_uat < migration_scripts/V2.3.2__add_idempotency_table.sql

# Test UAT thoroughly

# Apply to Production (during maintenance window)
mysql -u payment_admin -p payments < migration_scripts/V2.3.2__add_idempotency_table.sql

# Verify
mysql -u payment_admin -p payments -e "SHOW TABLES;"
mysql -u payment_admin -p payments -e "DESCRIBE new_table_name;"
```

3. **Data Migration** (if needed)

```bash
# Example: Migrate data to new table structure
mysql -u payment_admin -p payments < migration_scripts/data_migration.sql
```

### Mainframe Coordination

⚠️ **CRITICAL**: Changes to database schema may require mainframe COBOL copybook updates!

If modifying tables used by `LegacyPaymentBridge`:
- `customers` table
- `transactions` table

**Process**:
1. Notify mainframe team 1 week in advance
2. Provide new copybook specifications
3. Coordinate deployment timing
4. Test with sample batch files
5. Monitor first batch run after deployment

### Rollback Database Changes

```bash
# Restore from backup
mysql -u payment_admin -p payments < payments_backup_YYYYMMDD_HHMMSS.sql

# Or apply rollback script
mysql -u payment_admin -p payments < migration_scripts/V2.3.2_rollback.sql
```

## Application Deployment

### Build Artifact

```bash
# On build server or local machine
git checkout main
git pull origin main
mvn clean package -DskipTests

# Artifact location
ls -lh target/payment-gateway-2.3.1-SNAPSHOT.jar
```

### Deployment to Tomcat

#### Method 1: Manual Deployment

```bash
# 1. Stop Tomcat
/opt/tomcat/bin/shutdown.sh

# 2. Backup current version
cp /opt/tomcat/webapps/payment-gateway.jar /opt/tomcat/backups/payment-gateway-$(date +%Y%m%d).jar

# 3. Deploy new version
cp target/payment-gateway-2.3.1-SNAPSHOT.jar /opt/tomcat/webapps/payment-gateway.jar

# 4. Clear work directory
rm -rf /opt/tomcat/work/Catalina/localhost/payment-gateway

# 5. Start Tomcat
/opt/tomcat/bin/startup.sh

# 6. Monitor logs
tail -f /opt/tomcat/logs/catalina.out
```

#### Method 2: Rolling Deployment (Multiple Servers)

```bash
# For each server in load balancer:

# 1. Remove from load balancer
# (via load balancer admin console)

# 2. Wait for existing connections to drain (30 seconds)
sleep 30

# 3. Deploy application (see Method 1)

# 4. Verify health
curl http://localhost:8080/health

# 5. Add back to load balancer

# 6. Wait and monitor
sleep 60

# 7. Proceed to next server
```

### Configuration Updates

If `application.properties` changed:

```bash
# Update configuration
vi /opt/tomcat/conf/payment-gateway/application.properties

# Restart Tomcat
/opt/tomcat/bin/shutdown.sh
/opt/tomcat/bin/startup.sh
```

### File System Setup

Ensure these directories exist:

```bash
# Batch processing directories
mkdir -p /var/data/megabank/incoming
mkdir -p /var/data/megabank/processed
mkdir -p /var/data/megabank/errors
mkdir -p /var/data/megabank/archive

# Set permissions
chown -R tomcat:tomcat /var/data/megabank
chmod 755 /var/data/megabank/*

# Logging directory
mkdir -p /var/log/megabank
chown tomcat:tomcat /var/log/megabank
```

## Post-Deployment Verification

### Smoke Tests

Run these tests immediately after deployment:

```bash
# 1. Health check
curl -X GET http://production-server:8080/health \
  -H "X-API-Key: prod_key_67890"
# Expected: {"status":"healthy"}

# 2. API info
curl -X GET http://production-server:8080/ \
  -H "X-API-Key: prod_key_67890"
# Expected: {"name":"MegaBank Payment Gateway API","status":"operational"}

# 3. Process test payment (use test merchant)
curl -X POST http://production-server:8080/payments \
  -H "X-API-Key: prod_key_67890" \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "cvv": "123",
    "expiry": "12/25",
    "amount": 0.01,
    "merchant_id": "TEST_MERCHANT"
  }'
# Expected: {"status":1} (approved)

# 4. Check database connectivity
mysql -u payment_admin -p payments -e "SELECT COUNT(*) FROM transactions;"
```

### Verification Checklist

- [ ] Application started successfully
- [ ] No errors in application logs
- [ ] Health endpoint responds
- [ ] Database connectivity confirmed
- [ ] Test payment processes successfully
- [ ] Batch file directories accessible
- [ ] Mainframe connectivity working (test with ping/telnet)
- [ ] No spike in error rates
- [ ] Response times normal

### Monitoring First Hour

Watch for:
- **Error logs**: `tail -f /var/log/megabank/payments.log`
- **Tomcat logs**: `tail -f /opt/tomcat/logs/catalina.out`
- **Database connections**: `SHOW PROCESSLIST;`
- **Transaction success rate**: Check monitoring dashboard
- **Response times**: Should be < 500ms

## Rollback Procedures

### When to Rollback

Rollback if:
- Critical errors in logs
- Transaction processing failures > 5%
- Database connectivity issues
- Mainframe integration broken
- Response times > 2x baseline
- Customer-reported issues spike

### Rollback Steps

```bash
# 1. STOP - Don't panic, assess the situation
# Document what's wrong

# 2. Notify stakeholders
# Send alert: "Rolling back payment gateway deployment due to [issue]"

# 3. Stop Tomcat
/opt/tomcat/bin/shutdown.sh

# 4. Restore previous version
cp /opt/tomcat/backups/payment-gateway-YYYYMMDD.jar /opt/tomcat/webapps/payment-gateway.jar

# 5. Rollback database (if needed)
mysql -u payment_admin -p payments < payments_backup_YYYYMMDD_HHMMSS.sql

# 6. Restore configuration (if changed)
cp /opt/tomcat/conf/payment-gateway/application.properties.backup \
   /opt/tomcat/conf/payment-gateway/application.properties

# 7. Clear work directory
rm -rf /opt/tomcat/work/Catalina/localhost/payment-gateway

# 8. Start Tomcat
/opt/tomcat/bin/startup.sh

# 9. Verify rollback successful
curl http://localhost:8080/health

# 10. Monitor for stability
tail -f /opt/tomcat/logs/catalina.out
```

### Post-Rollback

- Document what went wrong
- Create incident report
- Fix issues in development
- Re-test in UAT
- Plan new deployment

## Monitoring

### Key Metrics

Monitor these metrics after deployment:

1. **Application Health**
   - Process uptime
   - Memory usage
   - Thread count
   - Connection pool utilization

2. **Transaction Metrics**
   - Total transactions/minute
   - Success rate (should be > 95%)
   - Failure rate
   - Average response time (< 500ms)

3. **Database Metrics**
   - Active connections
   - Slow query log
   - Lock waits
   - Table sizes

4. **System Metrics**
   - CPU usage (< 70%)
   - Memory usage (< 80%)
   - Disk I/O
   - Network traffic

### Log Locations

```bash
# Application logs
/var/log/megabank/payments.log
/var/log/megabank/api-requests.log

# Tomcat logs
/opt/tomcat/logs/catalina.out
/opt/tomcat/logs/localhost.YYYY-MM-DD.log

# Batch processing logs
/var/data/megabank/errors/*.errors

# Database logs
/var/log/mysql/error.log
/var/log/mysql/slow-query.log
```

### Alerts

Set up alerts for:
- Error rate > 5%
- Response time > 1000ms
- Memory usage > 90%
- Disk space < 10% free
- Database connection pool exhausted
- Failed batch file processing

## Troubleshooting

### Common Issues

#### Application Won't Start

```bash
# Check logs
tail -100 /opt/tomcat/logs/catalina.out

# Common causes:
# - Port 8080 already in use
sudo netstat -tlnp | grep 8080

# - Database unreachable
mysql -h prod-db.megabank.internal -u payment_admin -p

# - Insufficient memory
# Check CATALINA_OPTS in /opt/tomcat/bin/setenv.sh
export CATALINA_OPTS="-Xms512m -Xmx2048m"
```

#### High Error Rate

```bash
# Check application logs for errors
grep ERROR /var/log/megabank/payments.log | tail -50

# Check database connectivity
mysql -u payment_admin -p payments -e "SELECT 1;"

# Check mainframe connectivity (if batch related)
telnet mainframe.megabank.internal 3270
```

#### Slow Response Times

```bash
# Check database slow queries
mysql -u payment_admin -p payments -e "SHOW PROCESSLIST;"

# Check for long-running transactions
# Check connection pool
grep "pool" /var/log/megabank/payments.log

# Check memory
free -m
top
```

#### Batch Processing Failures

```bash
# Check batch directories
ls -la /var/data/megabank/incoming/
ls -la /var/data/megabank/errors/

# Check batch log
tail -f /var/log/megabank/batch-processor.log

# Check mainframe connectivity
telnet mainframe.megabank.internal 3270

# Check FTP access (if downloading files)
ftp ftp.partnerbank.com
```

### Getting Help

1. **Check documentation**: README.md, CLAUDE.md, API.md
2. **Review logs**: Application and system logs
3. **Check GitHub issues**: Similar problems may be documented
4. **Contact teams**:
   - DBA team: Database issues
   - Operations: Server/infrastructure issues
   - Mainframe team: Integration issues
   - Development: Code-related issues

## Production Considerations

### Load Balancing

If using multiple servers:
- Deploy to one server at a time
- Monitor after each deployment
- Use health check endpoint for routing decisions

### Disaster Recovery

- **Database**: Daily automated backups
- **Application**: Version controlled in Git
- **Configuration**: Backed up separately
- **RTO**: 2 hours (Recovery Time Objective)
- **RPO**: 24 hours (Recovery Point Objective)

### Security

Even though this is demo software, in production you would:
- Use HTTPS only
- Implement WAF (Web Application Firewall)
- Regular security scanning
- Penetration testing
- Patch management
- Audit logging

## Deployment History

Keep a deployment log:

```
2024-12-25 14:00 - v2.3.1 - Added CI/CD pipeline (developer name)
2024-11-15 10:30 - v2.3.0 - Fixed SQL injection in customer search (developer name)
```

## Appendix

### Useful Commands

```bash
# Check Java version
java -version

# Check Tomcat process
ps aux | grep tomcat

# Check application port
netstat -tlnp | grep 8080

# Check disk space
df -h

# Check memory
free -m

# Database backup
mysqldump -u payment_admin -p payments > backup.sql

# Database restore
mysql -u payment_admin -p payments < backup.sql
```

### Emergency Contacts

- **On-Call Engineer**: (see team roster)
- **DBA Team**: (contact info)
- **Operations**: (contact info)
- **Security Team**: (contact info)

---

**Remember**: This is demonstration software with intentional vulnerabilities. Never deploy to actual production!
