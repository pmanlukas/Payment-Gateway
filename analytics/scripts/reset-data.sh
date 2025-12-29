#!/bin/bash

###############################################################################
# MegaBank Payment Gateway - Reset Data Script
#
# This script clears all sample data from the database, allowing you to
# start fresh. The base schema and structure are preserved.
#
# WARNING: This will delete ALL data from:
#   - transactions
#   - customers
#   - merchants
#   - blacklisted_cards
#   - audit_log
#
# Usage:
#   ./analytics/scripts/reset-data.sh
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   MegaBank Payment Gateway - Reset Data${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# Check if MySQL is running
if ! docker-compose ps | grep -q "megabank-mysql.*Up"; then
    echo -e "${RED}✗ MySQL container is not running${NC}"
    echo -e "${YELLOW}  Run setup first: ./analytics/scripts/setup-analytics.sh${NC}"
    exit 1
fi

# Show current data counts
echo -e "\n${YELLOW}Current database state:${NC}"
CUSTOMER_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM customers;" 2>/dev/null)
MERCHANT_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM merchants;" 2>/dev/null)
TRANSACTION_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM transactions;" 2>/dev/null)

echo -e "  Customers:     ${CUSTOMER_COUNT}"
echo -e "  Merchants:     ${MERCHANT_COUNT}"
echo -e "  Transactions:  ${TRANSACTION_COUNT}"

# Confirm deletion
echo -e "\n${RED}⚠  WARNING: This will delete ALL data from the database!${NC}"
echo -e "${YELLOW}The following tables will be cleared:${NC}"
echo -e "  • transactions"
echo -e "  • customers"
echo -e "  • merchants"
echo -e "  • blacklisted_cards"
echo -e "  • audit_log"
echo -e "  • sessions"
echo -e "  • debug_data"

read -p "$(echo -e ${RED}Are you sure you want to delete all data? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Reset cancelled${NC}"
    exit 0
fi

# Double confirmation for large datasets
if [ "$TRANSACTION_COUNT" -gt 10000 ]; then
    echo -e "\n${RED}You have $TRANSACTION_COUNT transactions. This is a significant amount of data.${NC}"
    read -p "$(echo -e ${RED}Type 'DELETE' to confirm: ${NC})" CONFIRM
    if [ "$CONFIRM" != "DELETE" ]; then
        echo -e "${YELLOW}Reset cancelled${NC}"
        exit 0
    fi
fi

# Perform deletion
echo -e "\n${YELLOW}Clearing data...${NC}"

# Disable foreign key checks, truncate tables, re-enable
docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments <<EOF
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE transactions;
TRUNCATE TABLE customers;
TRUNCATE TABLE merchants;
TRUNCATE TABLE blacklisted_cards;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE sessions;
TRUNCATE TABLE debug_data;

SET FOREIGN_KEY_CHECKS = 1;
EOF

echo -e "${GREEN}✓ Data cleared successfully${NC}"

# Verify
CUSTOMER_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM customers;" 2>/dev/null)
MERCHANT_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM merchants;" 2>/dev/null)
TRANSACTION_COUNT=$(docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments -se "SELECT COUNT(*) FROM transactions;" 2>/dev/null)

echo -e "\n${YELLOW}Database state after reset:${NC}"
echo -e "  Customers:     ${GREEN}${CUSTOMER_COUNT}${NC}"
echo -e "  Merchants:     ${GREEN}${MERCHANT_COUNT}${NC}"
echo -e "  Transactions:  ${GREEN}${TRANSACTION_COUNT}${NC}"

# Reload seed data
echo -e "\n${YELLOW}Reloading base seed data...${NC}"
docker-compose exec -T mysql mysql -u payment_admin -pPayment123! payments < docker/init-data.sql 2>/dev/null || true
echo -e "${GREEN}✓ Base seed data restored${NC}"

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Reset Complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

echo -e "\n${YELLOW}Next Steps:${NC}"
echo -e "  1. Generate new sample data:"
echo -e "     ${GREEN}./analytics/scripts/load-sample-data.sh${NC}"
echo -e "  2. Or work with base seed data (3 customers, 3 transactions)"

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
