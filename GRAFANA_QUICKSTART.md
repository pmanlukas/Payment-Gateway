# Grafana Dashboards - Quick Start Guide

## 🚀 Access Grafana

**URL**: http://localhost:3000

**Login Credentials**:
- Username: `admin`
- Password: `admin123`

---

## 📊 Available Dashboards

### 1. Business Metrics
**Best for**: Real-time payment processing monitoring

**URL**: http://localhost:3000/d/megabank-business

**What you'll see**:
- 💰 Real-time revenue tracking
- 📈 Transaction volume trends
- ✅ Approval/decline rates
- 🏪 Top performing merchants
- 📋 Recent transaction list

**Use this when**:
- Monitoring daily operations
- Checking payment processing health
- Analyzing merchant performance
- Tracking revenue in real-time

---

### 2. Customer & Risk Analytics
**Best for**: Customer insights and fraud monitoring

**URL**: http://localhost:3000/d/megabank-customers

**What you'll see**:
- 👥 Customer segmentation (VIP vs Regular)
- ⚠️ Risk score monitoring
- 🎯 Top customers by spend
- 🚨 High-risk transaction alerts
- 📊 Customer lifetime value metrics

**Use this when**:
- Identifying VIP customers
- Monitoring fraud/risk indicators
- Analyzing customer behavior
- Planning customer retention strategies

---

### 3. System Overview
**Best for**: Infrastructure monitoring

**URL**: http://localhost:3000/d/megabank-system

**What you'll see**:
- 🟢 Service health status
- ⏱️ System uptime
- 📡 Request rates
- 💾 Memory usage
- 📈 Prometheus metrics

**Use this when**:
- Verifying system health
- Troubleshooting infrastructure issues
- Monitoring Prometheus performance

---

## ⚡ Quick Access

After logging in to Grafana:

1. Click **"Dashboards"** in the left sidebar (or the 4-square icon)
2. Click **"Browse"**
3. Select any of the three dashboards:
   - MegaBank - Business Metrics
   - MegaBank - Customer & Risk Analytics
   - MegaBank - System Overview

---

## 🎯 Key Features

### Auto-Refresh
All dashboards refresh automatically:
- **Business Metrics**: Every 30 seconds
- **Customer Analytics**: Every 1 minute
- **System Overview**: Every 10 seconds

You can change this using the refresh dropdown (top-right corner).

### Time Range Selection
- Default ranges are optimized for each dashboard
- Adjust time range using the time picker (top-right)
- Common shortcuts: Last 5m, 15m, 1h, 6h, 24h, 7d

### Interactive Charts
- **Hover**: See detailed values
- **Click**: Drill down into data
- **Zoom**: Click and drag on time series charts
- **Legend**: Click to show/hide series

---

## 📈 Sample Queries

### Business Metrics
The Business Metrics dashboard uses direct MySQL queries like:

```sql
-- Total transactions in last hour
SELECT COUNT(*) FROM transactions
WHERE created_date >= NOW() - INTERVAL 1 HOUR

-- Revenue (approved only)
SELECT SUM(amount) FROM transactions
WHERE status = 1 AND created_date >= NOW() - INTERVAL 1 HOUR

-- Approval rate
SELECT ROUND(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2)
FROM transactions
WHERE created_date >= NOW() - INTERVAL 1 HOUR
```

### Customer Analytics
```sql
-- Top customers by spend (last 30 days)
SELECT
  c.email,
  COUNT(t.id) AS transactions,
  SUM(CASE WHEN t.status = 1 THEN t.amount ELSE 0 END) AS total_spent
FROM customers c
LEFT JOIN transactions t ON c.customer_id = t.customer_id
WHERE t.created_date >= NOW() - INTERVAL 30 DAY
GROUP BY c.email
ORDER BY total_spent DESC
LIMIT 20
```

---

## 🔧 Customization

### Editing Dashboards
1. Open any dashboard
2. Click panel title → **"Edit"**
3. Modify query or visualization
4. Click **"Apply"** to save

### Adding Panels
1. Click **"Add panel"** button (top-right)
2. Select visualization type
3. Choose data source (MySQL or Prometheus)
4. Write your query
5. Click **"Apply"**

### Saving Changes
- Dashboards are persistent
- Changes are saved to the Grafana database
- Click the save icon (💾) to update

---

## 🔍 Troubleshooting

### No Data Showing
**Check**:
1. Time range (top-right) - ensure it covers your data
2. Database has data: `docker exec megabank-mysql mysql -uroot -proot123 payments -e "SELECT COUNT(*) FROM transactions"`
3. MySQL datasource is working: Grafana → Configuration → Data Sources → MySQL → "Test"

### Dashboard Not Loading
**Solutions**:
1. Refresh the page (Ctrl+R / Cmd+R)
2. Check Grafana logs: `docker-compose logs grafana`
3. Restart Grafana: `docker-compose restart grafana`

### Connection Errors
**Check**:
1. MySQL container is running: `docker ps | grep mysql`
2. Prometheus is running: `docker ps | grep prometheus`
3. Network connectivity: `docker exec megabank-grafana ping mysql`

---

## 📱 Mobile Access

Grafana dashboards are mobile-responsive:
- Access from your phone/tablet
- Same URL: http://localhost:3000 (if on same network)
- Swipe charts to see tooltips
- Pinch to zoom on time series

---

## 🎨 Dashboard Panels Overview

### Business Metrics Dashboard

**Row 1 - Key Metrics (Last Hour)**:
- Total Transactions
- Total Revenue
- Approval Rate (%)
- Average Transaction Value
- Declined Transactions

**Row 2 - Trends**:
- Transaction Volume Over Time (line chart)
- Revenue Over Time (bar chart)

**Row 3 - Analysis**:
- Transaction Status Distribution (pie chart)
- Top 10 Merchants by Revenue (bar chart)
- Recent Transactions (table)

### Customer Analytics Dashboard

**Row 1 - Summary Stats**:
- Total Customers
- VIP Customers
- Active Customers (24h)
- Average Risk Score

**Row 2 - Segmentation**:
- Customer Type Distribution (pie chart)
- Average Risk Score Trend (line chart)
- Risk Level Distribution (donut chart)

**Row 3 - Deep Dive**:
- Top 20 Customers by Spend (table)
- High Risk Transactions (table with color coding)

### System Overview Dashboard

**Row 1**:
- Prometheus Uptime
- Payment Gateway Status
- Prometheus Request Rate (line chart)

**Row 2**:
- Time Series Samples
- Memory Usage

---

## 🚦 Status Indicators

### Color Coding
- 🟢 **Green**: Healthy, within normal range
- 🟡 **Yellow**: Warning, approaching threshold
- 🔴 **Red**: Critical, requires attention

### Thresholds
- **Approval Rate**: >90% green, 70-90% yellow, <70% red
- **Risk Score**: <5 green, 5-7 yellow, >7 red
- **Memory Usage**: <50MB green, 50-100MB yellow, >100MB red

---

## 💡 Pro Tips

1. **Use Variables**: Create dashboard variables for dynamic filtering (merchant, customer, date range)

2. **Set up Alerts**: Configure alerts for critical metrics (low approval rate, high risk scores)

3. **Export Data**: Click panel menu → Inspect → Data → Download CSV

4. **Share Dashboards**: Dashboard menu → Share → Copy link or export JSON

5. **Dark/Light Mode**: User menu (bottom-left) → Preferences → UI Theme

6. **Keyboard Shortcuts**:
   - `?` - Show all shortcuts
   - `Ctrl+S` / `Cmd+S` - Save dashboard
   - `d+k` - Toggle kiosk mode
   - `Esc` - Exit panel edit mode

---

## 📚 Additional Resources

- **Grafana Docs**: https://grafana.com/docs/
- **Dashboard JSON**: See `docker/grafana-dashboards/` directory
- **MySQL Datasource**: https://grafana.com/docs/grafana/latest/datasources/mysql/
- **Prometheus Queries**: http://localhost:9090/graph

---

## 🆘 Support

**Check logs**:
```bash
# Grafana logs
docker-compose logs grafana

# All services
docker-compose logs -f
```

**Verify data**:
```bash
# Check transaction count
docker exec megabank-mysql mysql -uroot -proot123 payments \
  -e "SELECT COUNT(*) as total_transactions FROM transactions"

# Check recent transactions
docker exec megabank-mysql mysql -uroot -proot123 payments \
  -e "SELECT id, amount, status, created_date FROM transactions ORDER BY created_date DESC LIMIT 5"
```

**Restart services**:
```bash
# Restart Grafana only
docker-compose restart grafana

# Restart all monitoring stack
docker-compose restart grafana prometheus
```

---

## 🎉 You're All Set!

Your Grafana dashboards are ready to use. Start exploring your payment gateway metrics at:

**🌐 http://localhost:3000**

Login with `admin` / `admin123` and select a dashboard from the Browse menu!
