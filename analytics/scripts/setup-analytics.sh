#!/bin/bash

###############################################################################
# MegaBank Payment Gateway - Analytics Setup Script
#
# This script:
# 1. Starts Docker Compose services (MySQL, Gateway, Jupyter, Monitoring)
# 2. Waits for services to be healthy
# 3. Displays access information
#
# Usage:
#   ./analytics/scripts/setup-analytics.sh           # Build and start
#   ./analytics/scripts/setup-analytics.sh --prepare # Only download/build
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
PREPARE_ONLY=false
if [[ "$1" == "--prepare" ]] || [[ "$1" == "-p" ]]; then
    PREPARE_ONLY=true
fi

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
if [ "$PREPARE_ONLY" = true ]; then
    echo -e "${BLUE}   MegaBank - Prepare Analytics Dependencies${NC}"
else
    echo -e "${BLUE}   MegaBank Payment Gateway - Analytics Setup${NC}"
fi
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}✗ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker is running${NC}"

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}Working directory: ${PROJECT_ROOT}${NC}"

# Prepare mode: Only pull and build
if [ "$PREPARE_ONLY" = true ]; then
    echo -e "\n${BLUE}Preparing analytics environment...${NC}"
    echo -e "${YELLOW}This will download all images and build containers${NC}"
    echo -e "${YELLOW}but will NOT start any services.${NC}"
    echo ""

    # Pull all images
    echo -e "${BLUE}Step 1/2: Pulling Docker images...${NC}"
    docker-compose pull mysql prometheus grafana 2>/dev/null || true

    # Build custom images (payment-gateway and jupyter)
    echo ""
    echo -e "${BLUE}Step 2/2: Building custom images (includes Maven + Python deps)...${NC}"
    docker-compose build --no-cache

    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ Analytics Preparation Complete!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BLUE}All dependencies downloaded and cached.${NC}"
    echo -e "${BLUE}Next startup will be much faster!${NC}"
    echo ""
    echo -e "${YELLOW}What was prepared:${NC}"
    echo -e "  ✓ MySQL 5.7 image (x86_64)"
    echo -e "  ✓ Prometheus + Grafana images"
    echo -e "  ✓ Jupyter scipy-notebook (native ARM64)"
    echo -e "  ✓ Maven dependencies (Java build cached)"
    echo -e "  ✓ Python packages (pandas, sklearn, etc.)"
    echo ""
    echo -e "${YELLOW}To start services:${NC}"
    echo -e "  ${GREEN}./analytics/scripts/setup-analytics.sh${NC}"
    echo ""
    exit 0
fi

# Normal mode: Stop any existing containers
echo -e "\n${YELLOW}Stopping existing containers...${NC}"
docker-compose down 2>/dev/null || true

# Build and start services
echo -e "\n${YELLOW}Building and starting services...${NC}"
docker-compose up -d --build

# Wait for MySQL to be healthy
echo -e "\n${YELLOW}Waiting for MySQL to be ready...${NC}"
for i in {1..30}; do
    if docker-compose exec -T mysql mysqladmin ping -h localhost -u root -proot123 &> /dev/null; then
        echo -e "${GREEN}✓ MySQL is ready${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# Wait for Payment Gateway to be healthy
echo -e "\n${YELLOW}Waiting for Payment Gateway to be ready...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Payment Gateway is ready${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# Wait for Jupyter to be ready
echo -e "\n${YELLOW}Waiting for Jupyter to be ready...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8888 > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Jupyter is ready${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# Check database table counts
echo -e "\n${YELLOW}Checking database state...${NC}"
CUSTOMER_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM customers;" 2>/dev/null || echo "0")
MERCHANT_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM merchants;" 2>/dev/null || echo "0")
TRANSACTION_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM transactions;" 2>/dev/null || echo "0")

echo -e "${BLUE}Current database state:${NC}"
echo -e "  Customers:     ${CUSTOMER_COUNT}"
echo -e "  Merchants:     ${MERCHANT_COUNT}"
echo -e "  Transactions:  ${TRANSACTION_COUNT}"

# Display access information
echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Setup Complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

echo -e "\n${YELLOW}Access URLs:${NC}"
echo -e "  Jupyter Lab:       ${GREEN}http://localhost:8888${NC}"
echo -e "  Payment Gateway:   ${GREEN}http://localhost:8080${NC}"
echo -e "  Prometheus:        ${GREEN}http://localhost:9090${NC}"
echo -e "  Grafana:           ${GREEN}http://localhost:3000${NC} (admin/admin123)"

echo -e "\n${YELLOW}Database Connection:${NC}"
echo -e "  Host:     ${GREEN}localhost:3306${NC}"
echo -e "  Database: ${GREEN}payments${NC}"
echo -e "  User:     ${GREEN}payment_admin${NC}"
echo -e "  Password: ${GREEN}Payment123!${NC}"

if [ "$TRANSACTION_COUNT" -lt 100 ]; then
    echo -e "\n${YELLOW}⚠  Limited sample data detected.${NC}"
    echo -e "${YELLOW}   To generate realistic banking data, run:${NC}"
    echo -e "     ${GREEN}./analytics/scripts/load-sample-data.sh${NC}"
fi

echo -e "\n${YELLOW}Next Steps:${NC}"
echo -e "  1. Generate sample data:"
echo -e "     ${GREEN}./analytics/scripts/load-sample-data.sh${NC}"
echo -e "  2. Open Jupyter Lab:"
echo -e "     ${GREEN}http://localhost:8888${NC}"
echo -e "  3. Explore notebooks in:"
echo -e "     ${GREEN}/notebooks/${NC}"

echo -e "\n${YELLOW}Useful Commands:${NC}"
echo -e "  View logs:         ${GREEN}docker-compose logs -f${NC}"
echo -e "  Stop services:     ${GREEN}docker-compose stop${NC}"
echo -e "  Restart services:  ${GREEN}docker-compose restart${NC}"
echo -e "  Reset data:        ${GREEN}./analytics/scripts/reset-data.sh${NC}"
echo -e "  Teardown:          ${GREEN}./analytics/scripts/teardown.sh${NC}"

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
