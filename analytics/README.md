# MegaBank Payment Gateway - Analytics Platform

## Overview

This analytics platform provides comprehensive data analysis, machine learning, and compliance capabilities for the MegaBank Payment Gateway. Built specifically for demonstrating Claude Code's capabilities to Chief Data Officers and data professionals.

## Features

### 📊 **Analytics Notebooks**
- **Transaction Analysis** - Revenue trends, merchant performance, approval rates
- **Fraud Detection** - ML-based pattern detection, risk scoring
- **Customer Segmentation** - RFM analysis, CLV calculation, churn prediction
- **GDPR Compliance** - Data subject rights, privacy assessments, PII management
- **Data Quality** - Profiling, validation, integrity checks

### 🐍 **Python Data Middleware**
- Database connection pooling (SQLAlchemy)
- ETL utilities and data transformers
- Data quality checkers
- Transaction analytics helpers
- Customer segmentation algorithms

### 🎲 **Synthetic Data Generation**
- Realistic customer data (Faker library)
- Transaction patterns with seasonality
- Merchant/business records
- Fraud indicators (~2% fraud rate)
- Configurable presets (demo to xlarge)

### 🚀 **Easy Setup**
- One-command Docker setup
- Automated database seeding
- Pre-configured Jupyter Lab environment
- Sample data presets

## Quick Start

### Prerequisites
- Docker Desktop installed and running
- 8GB+ RAM available
- 10GB+ disk space

### 1. Start the Analytics Platform

```bash
# From project root
./analytics/scripts/setup-analytics.sh
```

This will:
- Start MySQL, Payment Gateway, Jupyter, Prometheus, Grafana
- Wait for health checks
- Display access URLs

**Access Jupyter Lab**: http://localhost:8888 (no password required)

### 2. Generate Sample Data

```bash
# Generate realistic banking data (medium preset = 2K customers, 20K transactions)
./analytics/scripts/load-sample-data.sh medium
```

**Available presets:**
- `demo` - 50 customers, 200 transactions (~30 seconds)
- `small` - 500 customers, 2K transactions (~1 minute)
- `medium` - 2K customers, 20K transactions (~3 minutes) **← Recommended**
- `large` - 10K customers, 100K transactions (~15 minutes)
- `xlarge` - 50K customers, 500K transactions (~60 minutes)

### 3. Explore the Notebooks

Open Jupyter Lab (http://localhost:8888) and navigate to `/notebooks/`:

1. **01_transaction_analysis.ipynb** - Start here for business analytics
2. **02_fraud_detection.ipynb** - ML-based fraud detection
3. **03_customer_segmentation.ipynb** - Customer insights
4. **04_regulatory_gdpr.ipynb** - Compliance and privacy
5. **05_data_quality.ipynb** - Data quality assessment

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Environment                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │   MySQL     │  │   Payment    │  │   Jupyter Lab   │   │
│  │   (3306)    │◄─┤   Gateway    │  │     (8888)      │   │
│  │             │  │    (8080)    │  │                 │   │
│  └─────────────┘  └──────────────┘  └─────────────────┘   │
│         ▲                                     │            │
│         │                                     │            │
│         └─────────────────────────────────────┘            │
│                                                             │
│  ┌─────────────┐  ┌──────────────┐                        │
│  │ Prometheus  │  │   Grafana    │                        │
│  │   (9090)    │  │    (3000)    │                        │
│  └─────────────┘  └──────────────┘                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
analytics/
├── notebooks/                 # Jupyter notebooks
│   ├── 01_transaction_analysis.ipynb
│   ├── 02_fraud_detection.ipynb
│   ├── 03_customer_segmentation.ipynb
│   ├── 04_regulatory_gdpr.ipynb
│   └── 05_data_quality.ipynb
│
├── middleware/                # Python data access layer
│   ├── __init__.py
│   ├── db_connector.py       # Database connections
│   └── etl_utils.py          # Data transformations
│
├── data_generation/           # Synthetic data generators
│   ├── generate_all.py       # Master orchestrator
│   ├── generate_customers.py
│   ├── generate_merchants.py
│   └── generate_transactions.py
│
├── scripts/                   # Shell utilities
│   ├── setup-analytics.sh    # Start Docker stack
│   ├── load-sample-data.sh   # Generate sample data
│   ├── reset-data.sh         # Clear all data
│   └── teardown.sh           # Remove everything
│
├── data/                      # Generated datasets (gitignored)
├── Dockerfile.jupyter         # Jupyter container
├── requirements.txt           # Python dependencies
└── README.md                  # This file
```

## Notebook Descriptions

### 01. Transaction Analysis

**Purpose**: Executive-level business analytics and KPIs

**What it demonstrates:**
- Time-series analysis (daily/hourly patterns)
- Revenue and volume trends
- Merchant performance comparison
- Approval/decline rate analysis
- Business insights and recommendations

**Target Audience**: CFO, COO, Business Analysts

**Key Outputs:**
- Transaction volume charts
- Revenue trends
- Merchant rankings
- Time-based patterns
- Executive summary dashboard

---

### 02. Fraud Detection

**Purpose**: Risk management and fraud prevention using ML

**What it demonstrates:**
- Pattern analysis (velocity, amounts, timing)
- Random Forest classifier (80%+ accuracy)
- Isolation Forest anomaly detection
- ROC curves and model evaluation
- Suspicious transaction identification

**Target Audience**: Risk Management, Fraud Team, CISO

**Key Outputs:**
- Fraud detection model
- Feature importance analysis
- Suspicious cards list
- Risk scoring system
- Model performance metrics

---

### 03. Customer Segmentation

**Purpose**: Customer intelligence and marketing analytics

**What it demonstrates:**
- RFM (Recency, Frequency, Monetary) segmentation
- Customer Lifetime Value (CLV) calculation
- K-means clustering
- Churn risk identification
- Actionable customer lists

**Target Audience**: CMO, Product Management, Customer Success

**Key Outputs:**
- Customer segments (Champions, Loyal, At-Risk, Lost)
- CLV distribution
- Churn risk analysis
- Marketing campaign lists
- Behavioral clusters

---

### 04. Regulatory & GDPR Compliance

**Purpose**: Data privacy, compliance, and governance

**What it demonstrates:**
- PII discovery and classification
- Data Subject Access Request (DSAR) automation
- Right to be Forgotten (RTBF) implementation
- Data retention policy analysis
- Privacy Impact Assessment (PIA)
- GDPR Article 30 compliance

**Target Audience**: Chief Data Officer, Legal, Compliance, DPO

**Key Outputs:**
- PII inventory
- DSAR report generator
- RTBF analysis tool
- Compliance gap assessment
- Risk mitigation recommendations

**⚠️ Compliance Issues Demonstrated:**
- CVV storage (PCI-DSS violation)
- Weak encryption
- Plain text sensitive data
- Missing data minimization

---

### 05. Data Quality Analysis

**Purpose**: Data quality assessment and improvement

**What it demonstrates:**
- Completeness analysis (null/missing values)
- Duplicate detection (exact and fuzzy)
- Referential integrity checks
- Data validation (formats, business rules)
- Quality scorecard (90-100 scale)

**Target Audience**: Chief Data Officer, Data Engineering, Data Governance

**Key Outputs:**
- Data quality scorecard
- Issue prioritization matrix
- Validation rule recommendations
- Quality improvement roadmap
- Automated quality checks

**Quality Dimensions:**
- Completeness: 85-95%
- Validity: 90-98%
- Consistency: 80-95%
- Uniqueness: 95-99%

## Database Connection

All notebooks connect to MySQL using environment variables set by Docker:

```python
from middleware import get_db, query

# Connect to database
db = get_db()

# Execute query
transactions = query("SELECT * FROM transactions LIMIT 100")
```

**Connection Details:**
- Host: `mysql` (Docker service) or `localhost:3306` (external)
- Database: `payments`
- User: `payment_admin`
- Password: `Payment123!`

## Data Generation

### Generating Data Programmatically

```bash
# Python script
docker-compose exec jupyter python /home/jovyan/work/data_generation/generate_all.py \
    --preset medium \
    --yes

# Custom configuration
docker-compose exec jupyter python /home/jovyan/work/data_generation/generate_all.py \
    --merchants 150 \
    --customers 5000 \
    --transactions 50000 \
    --months 18 \
    --fraud-rate 0.03
```

### Data Generation Features

**Realistic Patterns:**
- Time-based (business hours, weekdays weighted)
- Amount distributions (log-normal)
- Card types (Visa, Mastercard, Amex)
- Approval/decline logic (88% approval)
- Fraud indicators (2% base rate)

**Generated Fields:**
- Customer: Name, email, SSN, address, card details
- Merchant: Business names, categories, fee structures
- Transaction: Amount, status, timestamps, risk scores

## Utility Scripts

### Setup Script
```bash
./analytics/scripts/setup-analytics.sh
```
- Starts all Docker services
- Waits for health checks
- Displays access information

### Load Data Script
```bash
./analytics/scripts/load-sample-data.sh [preset]
```
- Generates sample data
- Loads into database
- Verifies data integrity

### Reset Data Script
```bash
./analytics/scripts/reset-data.sh
```
- Clears all transaction/customer/merchant data
- Reloads base seed data
- Preserves schema and configuration

### Teardown Script
```bash
./analytics/scripts/teardown.sh
```
- Stops all containers
- Removes volumes
- Complete cleanup

## Demo Scenarios

### Scenario 1: CDO Executive Review (15 minutes)

**Goal**: Show strategic data capabilities

1. Start with **01_transaction_analysis.ipynb**
   - Show business KPIs and trends
   - Demonstrate executive dashboard

2. Jump to **04_regulatory_gdpr.ipynb**
   - Show GDPR compliance automation
   - Demonstrate DSAR workflow

3. End with **05_data_quality.ipynb**
   - Show data quality scorecard
   - Discuss governance approach

### Scenario 2: Data Science Demo (20 minutes)

**Goal**: Show ML and analytics capabilities

1. **02_fraud_detection.ipynb**
   - Build fraud detection model
   - Evaluate performance
   - Deploy predictions

2. **03_customer_segmentation.ipynb**
   - RFM analysis
   - CLV calculation
   - K-means clustering

### Scenario 3: Compliance Focus (25 minutes)

**Goal**: Show privacy and regulatory capabilities

1. **04_regulatory_gdpr.ipynb** (full walkthrough)
   - PII inventory
   - DSAR execution
   - RTBF analysis
   - Privacy impact assessment

2. **05_data_quality.ipynb**
   - Data validation
   - Quality monitoring
   - Issue remediation

## Troubleshooting

### Jupyter Not Starting
```bash
# Check logs
docker-compose logs jupyter

# Restart service
docker-compose restart jupyter
```

### Database Connection Errors
```bash
# Verify MySQL is running
docker-compose ps mysql

# Check connection
docker-compose exec jupyter python -c "from middleware import get_db; db = get_db(); print('OK')"
```

### Data Generation Fails
```bash
# Check if customers and merchants exist
docker-compose exec mysql mysql -u payment_admin -pPayment123! payments -e "SELECT COUNT(*) FROM customers;"

# Regenerate from scratch
./analytics/scripts/reset-data.sh
./analytics/scripts/load-sample-data.sh demo
```

### Out of Memory
```bash
# For large datasets, increase Docker memory
# Docker Desktop → Settings → Resources → Memory: 8GB+

# Or use smaller preset
./analytics/scripts/load-sample-data.sh small
```

## Development

### Adding New Notebooks

1. Create notebook in `analytics/notebooks/`
2. Import middleware: `from middleware import get_db, query`
3. Follow existing notebook structure
4. Add to this README

### Extending Data Generation

1. Edit `analytics/data_generation/generate_*.py`
2. Add new fields to generator classes
3. Update database schema if needed
4. Test with demo preset first

### Custom Analysis

```python
# In any notebook
from middleware import query

# Your custom query
results = query("""
    SELECT
        DATE(created_date) as date,
        COUNT(*) as txn_count,
        SUM(amount) as revenue
    FROM transactions
    WHERE status = 1
    GROUP BY DATE(created_date)
    ORDER BY date DESC
    LIMIT 30
""")

results.plot(x='date', y='revenue', kind='line', figsize=(12, 6))
```

## Performance

**Recommended Configurations:**

| Use Case | Customers | Transactions | Time | Memory |
|----------|-----------|--------------|------|--------|
| Quick Demo | 100 | 500 | 30s | 2GB |
| Full Demo | 2,000 | 20,000 | 3min | 4GB |
| Realistic | 10,000 | 100,000 | 15min | 6GB |
| Production-like | 50,000 | 500,000 | 60min | 8GB |

## Security Notes

⚠️ **This is a DEMONSTRATION environment with intentional vulnerabilities:**

- Hardcoded credentials
- No authentication on Jupyter
- Sample data only
- Not production-ready
- For educational purposes

**Do NOT:**
- Deploy to production
- Use real customer data
- Expose to public internet
- Use production credentials

## Support

For issues or questions:
- Check [CLAUDE.md](../CLAUDE.md) for codebase documentation
- Review [CONTRIBUTING.md](../CONTRIBUTING.md) for development setup
- See main [README.md](../README.md) for project overview

## License

This is a demonstration project. See main project LICENSE file.

---

**Built with Claude Code** - Demonstrating AI-assisted data engineering and analytics
