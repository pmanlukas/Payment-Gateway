#!/bin/bash

###############################################################################
# MegaBank Payment Gateway - Load Sample Data Script
#
# This script generates and loads realistic banking data into the database.
#
# Usage:
#   ./analytics/scripts/load-sample-data.sh [preset]
#
# Presets:
#   demo    - 50 customers, 200 transactions (quick test)
#   small   - 500 customers, 2K transactions
#   medium  - 2K customers, 20K transactions (default, recommended)
#   large   - 10K customers, 100K transactions
#   xlarge  - 50K customers, 500K transactions (slow!)
#
# Examples:
#   ./analytics/scripts/load-sample-data.sh
#   ./analytics/scripts/load-sample-data.sh demo
#   ./analytics/scripts/load-sample-data.sh large
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Default preset
PRESET="${1:-medium}"

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   MegaBank Payment Gateway - Load Sample Data${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# Check if Docker services are running
if ! docker-compose ps | grep -q "megabank-mysql.*Up"; then
    echo -e "${RED}✗ MySQL container is not running${NC}"
    echo -e "${YELLOW}  Run setup first: ./analytics/scripts/setup-analytics.sh${NC}"
    exit 1
fi

if ! docker-compose ps | grep -q "megabank-jupyter.*Up"; then
    echo -e "${RED}✗ Jupyter container is not running${NC}"
    echo -e "${YELLOW}  Run setup first: ./analytics/scripts/setup-analytics.sh${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker services are running${NC}"

# Show available presets
echo -e "\n${YELLOW}Available presets:${NC}"
echo -e "  demo    - 50 customers, 200 transactions (quick test)"
echo -e "  small   - 500 customers, 2K transactions"
echo -e "  medium  - 2K customers, 20K transactions ${GREEN}(recommended)${NC}"
echo -e "  large   - 10K customers, 100K transactions"
echo -e "  xlarge  - 50K customers, 500K transactions (slow!)"

echo -e "\n${BLUE}Selected preset: ${GREEN}${PRESET}${NC}"

# Confirm with user
read -p "$(echo -e ${YELLOW}Proceed with data generation? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Data generation cancelled${NC}"
    exit 0
fi

# Run data generation inside Jupyter container
echo -e "\n${YELLOW}Generating sample data (this may take a few minutes)...${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

docker-compose exec -T jupyter python /home/jovyan/work/data_generation/generate_all.py \
    --preset "$PRESET" \
    --yes

# Check final counts
echo -e "\n${YELLOW}Verifying data...${NC}"
CUSTOMER_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM customers;" 2>/dev/null)
MERCHANT_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM merchants;" 2>/dev/null)
TRANSACTION_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM transactions;" 2>/dev/null)

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Data Generation Complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

echo -e "\n${YELLOW}Database contents:${NC}"
echo -e "  Customers:     ${GREEN}${CUSTOMER_COUNT}${NC}"
echo -e "  Merchants:     ${GREEN}${MERCHANT_COUNT}${NC}"
echo -e "  Transactions:  ${GREEN}${TRANSACTION_COUNT}${NC}"

echo -e "\n${YELLOW}Next Steps:${NC}"
echo -e "  1. Open Jupyter Lab: ${GREEN}http://localhost:8888${NC}"
echo -e "  2. Navigate to: ${GREEN}/notebooks/${NC}"
echo -e "  3. Start with: ${GREEN}01_transaction_analysis.ipynb${NC}"

echo -e "\n${YELLOW}Available notebooks:${NC}"
echo -e "  • Transaction Analysis - Volume, trends, revenue analytics"
echo -e "  • Fraud Detection - Identify suspicious patterns"
echo -e "  • Customer Segmentation - RFM analysis, cohorts"
echo -e "  • Regulatory Reporting - GDPR compliance, PII handling"
echo -e "  • Data Quality Analysis - Profiling, validation"

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
