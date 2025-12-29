#!/bin/bash
#
# Quick Start Script for MegaBank Payment Gateway
#
# This script sets up and starts the entire application stack using Docker
#
# Usage:
#   ./quick-start.sh           # Build and start services
#   ./quick-start.sh --prepare # Only download/build (don't start)
#

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parse arguments
PREPARE_ONLY=false
if [[ "$1" == "--prepare" ]] || [[ "$1" == "-p" ]]; then
    PREPARE_ONLY=true
fi

echo -e "${BLUE}========================================${NC}"
if [ "$PREPARE_ONLY" = true ]; then
    echo -e "${BLUE}MegaBank - Prepare Dependencies${NC}"
else
    echo -e "${BLUE}MegaBank Payment Gateway - Quick Start${NC}"
fi
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    echo "Please install Docker from https://www.docker.com/get-started"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed${NC}"
    echo "Please install Docker Compose from https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✓ Docker is installed${NC}"
echo -e "${GREEN}✓ Docker Compose is installed${NC}"
echo ""

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo -e "${RED}Error: Docker daemon is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi

echo -e "${GREEN}✓ Docker daemon is running${NC}"
echo ""

# Stop any existing containers (unless preparing)
if [ "$PREPARE_ONLY" = false ]; then
    echo -e "${BLUE}Stopping any existing containers...${NC}"
    docker-compose down 2>/dev/null || true
fi

# Prepare mode: Only pull and build
if [ "$PREPARE_ONLY" = true ]; then
    echo -e "${BLUE}Preparing environment (download and build)...${NC}"
    echo -e "${YELLOW}This will download all images and build containers${NC}"
    echo -e "${YELLOW}but will NOT start any services.${NC}"
    echo ""

    # Pull all images
    echo -e "${BLUE}Step 1/2: Pulling Docker images...${NC}"
    docker-compose pull mysql prometheus grafana 2>/dev/null || true

    # Build custom images (payment-gateway and jupyter)
    echo ""
    echo -e "${BLUE}Step 2/2: Building custom images...${NC}"
    echo -e "${YELLOW}This downloads Maven dependencies and Python packages...${NC}"
    docker-compose build --no-cache

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Preparation Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}All dependencies downloaded and images built.${NC}"
    echo -e "${BLUE}Next time you run './quick-start.sh' it will be much faster!${NC}"
    echo ""
    echo -e "${YELLOW}What was downloaded:${NC}"
    echo -e "  ✓ MySQL 5.7 image"
    echo -e "  ✓ Prometheus image"
    echo -e "  ✓ Grafana image"
    echo -e "  ✓ Jupyter scipy-notebook image"
    echo -e "  ✓ Maven dependencies (cached)"
    echo -e "  ✓ Python packages (cached)"
    echo -e "  ✓ Payment Gateway built"
    echo ""
    exit 0
fi

# Normal mode: Build and start services
echo -e "${BLUE}Building and starting services...${NC}"
echo -e "${YELLOW}This may take a few minutes on first run...${NC}"
echo ""

docker-compose up -d --build

# Wait for services to be healthy
echo ""
echo -e "${BLUE}Waiting for services to be healthy...${NC}"

# Wait for MySQL
echo -n "MySQL"
for i in {1..30}; do
    if docker-compose exec -T mysql mysqladmin ping -h localhost -u root -proot123 &> /dev/null; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# Wait for Application
echo -n "Payment Gateway"
for i in {1..60}; do
    if curl -s http://localhost:8080/health &> /dev/null; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Services are ready!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Display access information
echo -e "${BLUE}Access URLs:${NC}"
echo -e "  Payment Gateway API: ${GREEN}http://localhost:8080${NC}"
echo -e "  Health Check:        ${GREEN}http://localhost:8080/health${NC}"
echo -e "  Prometheus:          ${GREEN}http://localhost:9090${NC}"
echo -e "  Grafana:             ${GREEN}http://localhost:3000${NC} (admin/admin123)"
echo -e "  MySQL:               ${GREEN}localhost:3306${NC} (payment_admin/Payment123!)"
echo ""

# Test the health endpoint
echo -e "${BLUE}Testing health endpoint...${NC}"
HEALTH=$(curl -s http://localhost:8080/health)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Health check successful${NC}"
    echo "$HEALTH" | python -m json.tool 2>/dev/null || echo "$HEALTH"
else
    echo -e "${RED}✗ Health check failed${NC}"
fi

echo ""
echo -e "${BLUE}Quick Test Commands:${NC}"
echo ""
echo -e "${YELLOW}# Test health endpoint${NC}"
echo -e "curl http://localhost:8080/health"
echo ""
echo -e "${YELLOW}# Test payment (requires API key)${NC}"
echo -e 'curl -X POST http://localhost:8080/payments \'
echo -e '  -H "X-API-Key: docker_test_key" \'
echo -e '  -H "Content-Type: application/json" \'
echo -e '  -d '"'"'{"card_number":"4111111111111111","cvv":"123","expiry":"12/25","amount":99.99}'"'"
echo ""
echo -e "${YELLOW}# View logs${NC}"
echo -e "docker-compose logs -f payment-gateway"
echo ""
echo -e "${YELLOW}# Stop services${NC}"
echo -e "docker-compose down"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Setup complete! Happy testing!${NC}"
echo -e "${GREEN}========================================${NC}"
