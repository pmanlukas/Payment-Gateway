#!/bin/bash

###############################################################################
# MegaBank Payment Gateway - Teardown Script
#
# This script completely removes all Docker containers, networks, and volumes.
# Use this for a complete cleanup.
#
# WARNING: This will delete:
#   - All Docker containers
#   - All Docker volumes (including database data)
#   - All Docker networks
#
# Usage:
#   ./analytics/scripts/teardown.sh
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   MegaBank Payment Gateway - Complete Teardown${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

# Navigate to project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# Show what will be removed
echo -e "\n${YELLOW}The following will be removed:${NC}"
echo -e "  ${RED}✗${NC} All Docker containers (mysql, payment-gateway, jupyter, prometheus, grafana)"
echo -e "  ${RED}✗${NC} All Docker volumes (database data, logs, Jupyter data)"
echo -e "  ${RED}✗${NC} All Docker networks (megabank-network)"

# List current containers
echo -e "\n${YELLOW}Current Docker containers:${NC}"
docker-compose ps 2>/dev/null || echo "  (none running)"

# Confirm
echo -e "\n${RED}⚠  WARNING: This will permanently delete ALL data and containers!${NC}"
echo -e "${YELLOW}You will need to run setup-analytics.sh to start fresh.${NC}"

read -p "$(echo -e ${RED}Are you sure you want to proceed? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Teardown cancelled${NC}"
    exit 0
fi

# Double confirmation
read -p "$(echo -e ${RED}Type 'DESTROY' to confirm complete teardown: ${NC})" CONFIRM
if [ "$CONFIRM" != "DESTROY" ]; then
    echo -e "${YELLOW}Teardown cancelled${NC}"
    exit 0
fi

# Stop and remove containers
echo -e "\n${YELLOW}Stopping containers...${NC}"
docker-compose stop
echo -e "${GREEN}✓ Containers stopped${NC}"

# Remove containers
echo -e "\n${YELLOW}Removing containers...${NC}"
docker-compose rm -f
echo -e "${GREEN}✓ Containers removed${NC}"

# Remove volumes
echo -e "\n${YELLOW}Removing volumes...${NC}"
docker-compose down -v
echo -e "${GREEN}✓ Volumes removed${NC}"

# Remove networks
echo -e "\n${YELLOW}Removing networks...${NC}"
docker network rm megabank-network 2>/dev/null || true
echo -e "${GREEN}✓ Networks removed${NC}"

# Verify cleanup
echo -e "\n${YELLOW}Verifying cleanup...${NC}"
CONTAINERS=$(docker-compose ps -q 2>/dev/null | wc -l)

if [ "$CONTAINERS" -eq 0 ]; then
    echo -e "${GREEN}✓ All containers removed${NC}"
else
    echo -e "${RED}✗ Some containers may still exist${NC}"
fi

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Teardown Complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"

echo -e "\n${YELLOW}Environment has been completely removed.${NC}"

echo -e "\n${YELLOW}To start fresh:${NC}"
echo -e "  ${GREEN}./analytics/scripts/setup-analytics.sh${NC}"

echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════════════${NC}"
