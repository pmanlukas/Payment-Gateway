# Docker Setup for MegaBank Payment Gateway

This directory contains Docker configuration files for running the MegaBank Payment Gateway in containers.

## Quick Start

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f payment-gateway

# Stop all services
docker-compose down

# Stop and remove all data
docker-compose down -v
```

## Services

The docker-compose setup includes:

1. **MySQL Database** (port 3306)
   - Pre-loaded with schema and test data
   - Persistent storage via Docker volume

2. **Payment Gateway** (port 8080)
   - Built from source
   - Connected to MySQL
   - Health checks enabled

3. **Prometheus** (port 9090) - Optional
   - Metrics collection
   - Pre-configured to scrape gateway

4. **Grafana** (port 3000) - Optional
   - Visualization dashboard
   - Default credentials: admin/admin123

## Accessing Services

After `docker-compose up -d`:

- **Payment Gateway API**: http://localhost:8080
- **Health Check**: http://localhost:8080/health
- **API Info**: http://localhost:8080/ (with X-API-Key header)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

## Test Data

The database is pre-seeded with:
- 3 test customers (CUST_DOCKER_001, CUST_DOCKER_002, CUST_DOCKER_003)
- 2 test merchants
- 3 sample transactions
- API keys for testing
- 1 blacklisted card

## API Testing

```bash
# Test health endpoint
curl http://localhost:8080/health

# Test payment (requires API key)
curl -X POST http://localhost:8080/payments \
  -H "X-API-Key: docker_test_key" \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "cvv": "123",
    "expiry": "12/25",
    "amount": 99.99,
    "currency": "USD"
  }'
```

## Volumes

Persistent data is stored in Docker volumes:
- `mysql-data`: Database files
- `batch-data`: Batch processing files
- `app-logs`: Application logs
- `prometheus-data`: Prometheus metrics
- `grafana-data`: Grafana dashboards

## Troubleshooting

### Application won't start
```bash
# Check logs
docker-compose logs payment-gateway

# Check database connectivity
docker-compose exec payment-gateway curl http://localhost:8080/health
```

### Database connection errors
```bash
# Verify MySQL is running
docker-compose ps mysql

# Check MySQL logs
docker-compose logs mysql

# Connect to MySQL
docker-compose exec mysql mysql -u payment_admin -pPayment123! payments
```

### Rebuild after code changes
```bash
# Rebuild and restart
docker-compose up -d --build payment-gateway
```

## Development Workflow

1. Make code changes
2. Rebuild: `docker-compose up -d --build payment-gateway`
3. Test: `curl http://localhost:8080/health`
4. View logs: `docker-compose logs -f payment-gateway`

## Production Considerations

⚠️ **This is a demo setup!** For production:
- Use secrets management (not hardcoded passwords)
- Enable HTTPS
- Use production-grade MySQL configuration
- Implement proper monitoring
- Add security scanning
- Use multi-stage builds (already implemented in Dockerfile)

## Files

- `init-data.sql`: Database seed data
- `prometheus.yml`: Prometheus configuration
- `README.md`: This file
