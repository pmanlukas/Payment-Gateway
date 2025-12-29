# MegaBank Payment Gateway - Grafana Dashboards

This directory contains pre-configured Grafana dashboards for monitoring the MegaBank Payment Gateway.

## Available Dashboards

### 1. System Overview (`system-overview.json`)
**Purpose**: Monitor Prometheus and system health

**Metrics Displayed**:
- Prometheus uptime
- Services status (Payment Gateway, MySQL)
- Request rates
- Time series samples
- Memory usage

**Data Source**: Prometheus

**Refresh Rate**: 10 seconds

**Use Cases**:
- Verify monitoring stack is operational
- Check service health status
- Monitor Prometheus performance

---

### 2. Business Metrics (`business-metrics.json`)
**Purpose**: Real-time business KPIs and transaction analytics

**Metrics Displayed**:
- **Key Stats (Last Hour)**:
  - Total transactions
  - Total revenue
  - Approval rate
  - Average transaction value
  - Declined transactions

- **Time Series**:
  - Transaction volume over time (approved vs declined)
  - Revenue over time

- **Analysis**:
  - Transaction status distribution (pie chart)
  - Top 10 merchants by revenue
  - Recent transactions table

**Data Source**: MySQL (Direct database queries)

**Refresh Rate**: 30 seconds

**Default Time Range**: Last 6 hours

**Use Cases**:
- Monitor real-time payment processing
- Track revenue and approval rates
- Identify top-performing merchants
- Detect transaction anomalies

---

### 3. Customer & Risk Analytics (`customer-analytics.json`)
**Purpose**: Customer segmentation and risk monitoring

**Metrics Displayed**:
- **Customer Statistics**:
  - Total customers
  - VIP customers
  - Active customers (24h)
  - Average risk score

- **Segmentation**:
  - VIP vs Regular customers (pie chart)
  - Customer risk level distribution

- **Risk Monitoring**:
  - Average risk score trend over time
  - High-risk transactions table (risk score >= 5)

- **Customer Insights**:
  - Top 20 customers by spend (30 days)
  - Transaction counts and averages per customer

**Data Source**: MySQL (Direct database queries)

**Refresh Rate**: 1 minute

**Default Time Range**: Last 24 hours

**Use Cases**:
- Monitor customer behavior
- Identify VIP customers
- Track fraud/risk indicators
- Customer lifetime value analysis

---

## Data Sources

### Prometheus
- **URL**: `http://prometheus:9090`
- **Type**: Prometheus
- **Purpose**: System metrics, service health
- **Auto-configured**: Yes

### MySQL
- **URL**: `mysql:3306`
- **Database**: `payments`
- **Type**: MySQL
- **Purpose**: Business metrics, transaction data
- **Auto-configured**: Yes
- **Credentials**: Configured via datasources.yml

---

## Accessing Dashboards

1. **Open Grafana**: http://localhost:3000
2. **Login**:
   - Username: `admin`
   - Password: `admin123`
3. **Navigate**: Click "Dashboards" → "Browse"
4. **Select**: Choose any of the three dashboards

---

## Dashboard Features

### Common Features
- **Auto-refresh**: All dashboards refresh automatically
- **Time range selector**: Adjust the time window in the top-right corner
- **Drill-down**: Click on chart elements for details
- **Panel editing**: Click panel title → Edit to modify queries
- **Export**: Dashboard menu → Share → Export

### Query Examples

#### Business Metrics Queries
```sql
-- Total transactions in last hour
SELECT COUNT(*) as value
FROM transactions
WHERE created_date >= NOW() - INTERVAL 1 HOUR

-- Revenue (approved transactions only)
SELECT COALESCE(SUM(amount), 0) as value
FROM transactions
WHERE status = 1
  AND created_date >= NOW() - INTERVAL 1 HOUR

-- Approval rate
SELECT ROUND(
  SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*),
  2
) as value
FROM transactions
WHERE created_date >= NOW() - INTERVAL 1 HOUR
```

#### Customer Analytics Queries
```sql
-- Top customers by spend
SELECT
  c.customer_id,
  c.email,
  COUNT(t.id) AS transaction_count,
  SUM(CASE WHEN t.status = 1 THEN t.amount ELSE 0 END) AS total_spent
FROM customers c
LEFT JOIN transactions t ON c.customer_id = t.customer_id
WHERE t.created_date >= NOW() - INTERVAL 30 DAY
GROUP BY c.customer_id, c.email
ORDER BY total_spent DESC
LIMIT 20
```

---

## Customization

### Adding New Panels

1. Open dashboard
2. Click "Add panel" (top-right)
3. Select visualization type
4. Configure data source and query
5. Save dashboard

### Modifying Queries

1. Click panel title → Edit
2. Modify SQL query or PromQL expression
3. Click "Apply" to save changes

### Creating Alerts (Future Enhancement)

Grafana supports alerting on panel queries. To enable:
1. Edit panel
2. Go to "Alert" tab
3. Configure alert conditions
4. Set notification channels

---

## Troubleshooting

### Dashboard Not Loading
- Check Grafana logs: `docker-compose logs grafana`
- Verify datasource connectivity: Configuration → Data sources
- Ensure MySQL is healthy: `docker-compose ps`

### No Data Displayed
- **MySQL panels**: Verify sample data exists in database
- **Prometheus panels**: Check Prometheus targets at http://localhost:9090/targets
- Check time range (top-right corner)

### Permission Errors
- Ensure Grafana has read access to dashboard files
- Check docker volume mounts in docker-compose.yml

---

## Performance Considerations

### Query Optimization
- Dashboards query MySQL directly - heavy usage may impact database performance
- Consider adding indexes for frequently queried columns:
  ```sql
  CREATE INDEX idx_txn_created ON transactions(created_date);
  CREATE INDEX idx_txn_status ON transactions(status);
  CREATE INDEX idx_txn_customer ON transactions(customer_id);
  ```

### Refresh Rates
- Business Metrics: 30s (suitable for real-time monitoring)
- Customer Analytics: 1m (less time-sensitive)
- System Overview: 10s (critical for infrastructure monitoring)

Adjust refresh rates based on your needs:
- Dashboard settings → Time options → Auto refresh

---

## Future Enhancements

### Planned Additions
1. **Application Metrics Dashboard**: When `/metrics` endpoint is implemented
   - Request latency histograms
   - HTTP status code distribution
   - JVM metrics (heap, GC)

2. **Fraud Detection Dashboard**:
   - Real-time fraud alerts
   - Suspicious pattern detection
   - Geolocation-based anomalies

3. **Merchant Performance Dashboard**:
   - Per-merchant approval rates
   - Chargeback monitoring
   - Settlement tracking

### Integration Opportunities
- Alerting to Slack/PagerDuty
- Anomaly detection with machine learning
- Historical trend analysis
- Compliance reporting

---

## Additional Resources

- [Grafana Documentation](https://grafana.com/docs/)
- [MySQL Data Source](https://grafana.com/docs/grafana/latest/datasources/mysql/)
- [Prometheus Data Source](https://grafana.com/docs/grafana/latest/datasources/prometheus/)
- [Dashboard JSON Model](https://grafana.com/docs/grafana/latest/dashboards/json-model/)

---

## Support

For issues or questions:
1. Check Grafana logs: `docker-compose logs grafana`
2. Review datasource configuration: Grafana UI → Configuration → Data sources
3. Verify database connectivity: `docker exec megabank-mysql mysql -uroot -proot123 payments -e "SELECT COUNT(*) FROM transactions"`
