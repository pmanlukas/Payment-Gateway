# Monitoring Guide

MegaBank Payment Gateway - Monitoring and Observability

## Overview

This guide covers monitoring the Payment Gateway application in development and production environments. The Docker setup includes Prometheus and Grafana for metrics collection and visualization.

## Table of Contents

- [Quick Start](#quick-start)
- [Monitoring Stack](#monitoring-stack)
- [Health Checks](#health-checks)
- [Application Metrics](#application-metrics)
- [Database Monitoring](#database-monitoring)
- [Log Aggregation](#log-aggregation)
- [Alerting](#alerting)
- [Dashboards](#dashboards)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Using Docker Compose

```bash
# Start all services including monitoring
docker-compose up -d

# Access monitoring tools
# Grafana: http://localhost:3000 (admin/admin123)
# Prometheus: http://localhost:9090
```

### Health Check

```bash
# Application health check
curl http://localhost:8080/health

# Expected response:
# {
#   "status": "healthy",
#   "timestamp": 1640438400000,
#   "database": "connected",
#   "memory_used": 134217728
# }
```

## Monitoring Stack

### Components

1. **Application**: Built-in health endpoint
2. **Prometheus**: Metrics collection and storage
3. **Grafana**: Visualization and dashboards
4. **MySQL**: Database metrics (via queries)
5. **Docker**: Container metrics

### Architecture

```
Application Logs  ──>  stdout/files
                       │
Application Metrics ──>  /health endpoint
                       │
                       ├──> Prometheus  ──>  Grafana
                       │
Database Metrics  ──>  MySQL queries ──>  Prometheus
```

## Health Checks

### Built-in Health Endpoint

**Endpoint**: `GET /health`

```bash
curl http://localhost:8080/health
```

**Response**:
```json
{
  "status": "healthy",
  "timestamp": 1640438400000,
  "database": "connected",
  "memory_used": 134217728,
  "uptime": "TODO"
}
```

### Docker Health Checks

Health checks are configured in `docker-compose.yml`:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

Check status:
```bash
docker-compose ps
# Look for "healthy" status
```

## Application Metrics

### Current Metrics (via /health)

The application currently exposes basic metrics through the `/health` endpoint:

- **status**: Overall health status
- **timestamp**: Current server time
- **database**: Database connection status
- **memory_used**: JVM memory usage

### Accessing Metrics

```bash
# Health check with API key
curl -H "X-API-Key: docker_test_key" http://localhost:8080/health

# Without API key (if authentication is disabled)
curl http://localhost:8080/health
```

### Implementing Prometheus Metrics

⚠️ **Note**: The application doesn't have a `/metrics` endpoint yet. This needs to be implemented.

**To add Prometheus metrics**, you would:

1. Add Prometheus Java client dependency to `pom.xml`:
```xml
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient</artifactId>
    <version>0.16.0</version>
</dependency>
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient_servlet</artifactId>
    <version>0.16.0</version>
</dependency>
```

2. Create a `/metrics` endpoint in a servlet
3. Register collectors for:
   - Payment processing counters
   - Transaction success/failure rates
   - Response times (histograms)
   - Database connection pool metrics
   - JVM metrics

See Issue #XX for implementation task.

## Database Monitoring

### MySQL Metrics

Monitor MySQL performance with these queries:

```sql
-- Active connections
SHOW PROCESSLIST;

-- Connection stats
SHOW STATUS LIKE 'Connections';
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- Query performance
SHOW STATUS LIKE 'Slow_queries';
SELECT * FROM information_schema.PROCESSLIST
WHERE TIME > 5;  -- Queries running > 5 seconds

-- Table sizes
SELECT
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE table_schema = 'payments'
ORDER BY size_mb DESC;

-- InnoDB status
SHOW ENGINE INNODB STATUS;
```

### Database Health from Docker

```bash
# Connect to MySQL
docker-compose exec mysql mysql -u payment_admin -pPayment123! payments

# Check if database is accessible
docker-compose exec mysql mysqladmin ping -h localhost -u root -proot123
```

### Slow Query Log

Enable slow query logging in MySQL:

```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;  -- Log queries > 2 seconds
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow-query.log';
```

View slow queries:
```bash
docker-compose exec mysql tail -f /var/log/mysql/slow-query.log
```

## Log Aggregation

### Application Logs

Logs are written to:
- **stdout/stderr**: Captured by Docker
- **File**: `/var/log/megabank/payments.log` (if configured)

### Viewing Logs

```bash
# Real-time application logs
docker-compose logs -f payment-gateway

# Last 100 lines
docker-compose logs --tail=100 payment-gateway

# Logs for all services
docker-compose logs -f

# MySQL logs
docker-compose logs mysql

# Filter for errors
docker-compose logs payment-gateway | grep ERROR
```

### Log Patterns to Monitor

Watch for these patterns:

**Errors**:
```bash
grep "ERROR" /var/log/megabank/payments.log
grep "Exception" /var/log/megabank/payments.log
grep "SQLException" /var/log/megabank/payments.log
```

**Security Issues**:
```bash
grep "SQL Injection" /var/log/megabank/payments.log
grep "Unauthorized" /var/log/megabank/payments.log
grep "Invalid API key" /var/log/megabank/payments.log
```

**Performance**:
```bash
grep "Slow query" /var/log/megabank/payments.log
grep "Timeout" /var/log/megabank/payments.log
```

### Log Levels

Configure in `src/main/resources/log4j.properties`:

```properties
# Root logger
log4j.rootLogger=INFO, stdout, file

# For debugging
# log4j.rootLogger=DEBUG, stdout, file

# For production (less verbose)
# log4j.rootLogger=WARN, stdout, file
```

## Alerting

### Basic Alerting with Docker

Monitor container health:

```bash
# Check if container is running
docker-compose ps payment-gateway | grep -q "Up" || echo "ALERT: Container down!"

# Check health status
docker inspect megabank-payment-gateway | grep -q "\"Status\": \"healthy\"" || echo "ALERT: Unhealthy!"
```

### Prometheus Alerts (when metrics are implemented)

Example alert rules (`docker/alerts.yml`):

```yaml
groups:
  - name: payment_gateway
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(payment_errors_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} (threshold: 0.05)"

      - alert: SlowResponseTime
        expr: http_request_duration_seconds{quantile="0.95"} > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response times"

      - alert: DatabaseDown
        expr: mysql_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database is down"
```

### Email Notifications

Set up email alerts:

1. Configure Grafana SMTP settings
2. Create notification channels
3. Add alerts to dashboards

### Webhook Notifications

Send alerts to Slack, PagerDuty, etc.:

```yaml
# In Grafana notification channels
webhook_url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

## Dashboards

### Grafana Dashboards

Access Grafana at http://localhost:3000

**Default credentials**:
- Username: `admin`
- Password: `admin123`

### Creating a Dashboard

1. **Login to Grafana**
2. **Add Data Source**:
   - Type: Prometheus
   - URL: http://prometheus:9090
   - Save & Test

3. **Create Dashboard**:
   - Click "+" → Dashboard
   - Add Panel
   - Configure metrics

### Example Panels

**Payment Success Rate**:
```promql
rate(payments_successful_total[5m]) / rate(payments_total[5m])
```

**Response Time (95th percentile)**:
```promql
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
```

**Database Connections**:
```promql
mysql_global_status_threads_connected
```

### Pre-built Dashboards

You can import dashboards for:
- JVM metrics (dashboard ID: 4701)
- MySQL metrics (dashboard ID: 7362)
- Docker containers (dashboard ID: 193)

## Key Metrics to Monitor

### Application Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|----------------|
| Uptime | Time since last restart | < 5 minutes |
| Request rate | Requests per second | Sudden drops |
| Error rate | Errors per second | > 5% |
| Response time (p95) | 95th percentile latency | > 1 second |
| Transaction success rate | Successful payments / total | < 95% |

### Infrastructure Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|----------------|
| CPU usage | Container CPU % | > 80% |
| Memory usage | Container memory % | > 90% |
| Disk usage | Volume usage % | > 85% |
| Network I/O | Bytes in/out | Unusual patterns |

### Database Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|----------------|
| Connections | Active connections | > 80% of max |
| Slow queries | Queries > 2 seconds | > 10/minute |
| Lock waits | InnoDB lock waits | > 5 concurrent |
| Replication lag | Seconds behind master | > 30 seconds |

### Business Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|----------------|
| Payment volume | Transactions/hour | Unusual patterns |
| Revenue | Total $/hour | Sudden drops |
| Fraud rate | Flagged transactions % | > 2% |
| Refund rate | Refunds / transactions | > 5% |

## Troubleshooting

### Application Not Responding

```bash
# Check if container is running
docker-compose ps

# Check logs for errors
docker-compose logs --tail=50 payment-gateway

# Check health endpoint
curl http://localhost:8080/health

# Restart application
docker-compose restart payment-gateway
```

### High Memory Usage

```bash
# Check memory usage
docker stats megabank-payment-gateway

# Get JVM memory info
docker-compose exec payment-gateway \
  jmap -heap $(docker-compose exec payment-gateway pgrep java)

# Adjust JVM settings in docker-compose.yml
# JAVA_OPTS=-Xms512m -Xmx2048m
```

### High CPU Usage

```bash
# Check CPU usage
docker stats

# Get thread dump
docker-compose exec payment-gateway \
  jstack $(docker-compose exec payment-gateway pgrep java)

# Check for slow queries
docker-compose exec mysql mysql -u payment_admin -pPayment123! payments \
  -e "SHOW PROCESSLIST;"
```

### Database Connection Issues

```bash
# Test database connection
docker-compose exec payment-gateway curl http://localhost:8080/health

# Check MySQL status
docker-compose exec mysql mysqladmin status -u root -proot123

# Check connection count
docker-compose exec mysql mysql -u root -proot123 \
  -e "SHOW STATUS LIKE 'Threads_connected';"

# Check for locked tables
docker-compose exec mysql mysql -u payment_admin -pPayment123! payments \
  -e "SHOW OPEN TABLES WHERE In_use > 0;"
```

### Prometheus Not Scraping

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check Prometheus config
docker-compose exec prometheus cat /etc/prometheus/prometheus.yml

# Restart Prometheus
docker-compose restart prometheus
```

## Best Practices

1. **Set Up Baseline Metrics**: Monitor for a week to establish normal patterns
2. **Create Runbooks**: Document how to respond to common alerts
3. **Test Alerts**: Trigger test alerts to verify notification channels
4. **Regular Reviews**: Review dashboards weekly
5. **Capacity Planning**: Monitor trends for capacity planning
6. **Security Monitoring**: Watch for suspicious patterns
7. **Audit Logs**: Keep logs for compliance (90+ days)

## Security Monitoring

### Watch For

- **SQL Injection Attempts**: Unusual query patterns in logs
- **Authentication Failures**: Multiple failed API key attempts
- **Data Exfiltration**: Unusual data access patterns
- **Privilege Escalation**: Attempts to access admin endpoints
- **DDoS**: Sudden spike in requests from single IP

### Log Examples

```bash
# Failed authentication attempts
grep "Invalid API key" /var/log/megabank/payments.log

# SQL injection attempts (if logging query strings)
grep -i "union select\|drop table\|exec\|script" /var/log/megabank/payments.log

# Suspicious IP addresses
grep "127.0.0.1" /var/log/megabank/payments.log | wc -l
```

## Next Steps

1. **Implement /metrics endpoint** - Add Prometheus metrics (see Issue #XX)
2. **Configure alerts** - Set up email/Slack notifications
3. **Create dashboards** - Build Grafana dashboards for key metrics
4. **Add tracing** - Implement distributed tracing
5. **Log aggregation** - Set up ELK stack or similar
6. **APM integration** - Consider New Relic, Datadog, etc.

## References

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Docker Monitoring Best Practices](https://docs.docker.com/config/containers/runmetrics/)
- [MySQL Monitoring Guide](https://dev.mysql.com/doc/refman/5.7/en/monitoring.html)

---

**Remember**: This is demo software with intentional vulnerabilities. Production monitoring would include security event monitoring, compliance logging, and more sophisticated alerting.
