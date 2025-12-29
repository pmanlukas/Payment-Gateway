#!/bin/bash
#
# Quick Start Script for MegaBank Payment Gateway
#
# This script sets up and starts the entire application stack using Docker
#
# Usage:
#   ./quick-start.sh            # Build and start services
#   ./quick-start.sh --prepare  # Only download/build (don't start)
#   ./quick-start.sh --destroy  # Stop and remove all containers, networks, volumes
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
DESTROY=false

if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
    echo ""
    echo "MegaBank Payment Gateway - Quick Start Script"
    echo ""
    echo "Usage:"
    echo "  ./quick-start.sh           Start the entire application stack"
    echo "  ./quick-start.sh --prepare Prepare dependencies (download & build only)"
    echo "  ./quick-start.sh --destroy Completely destroy the environment"
    echo "  ./quick-start.sh --help    Show this help message"
    echo ""
    echo "Options:"
    echo "  --prepare, -p   Download images and build containers without starting"
    echo "  --destroy, -d   Stop and remove all containers, volumes, and images"
    echo "  --help, -h      Display this help message"
    echo ""
    echo "Examples:"
    echo "  ./quick-start.sh           # Normal start"
    echo "  ./quick-start.sh -p        # Prepare for offline use"
    echo "  ./quick-start.sh --destroy # Clean up everything"
    echo ""
    exit 0
elif [[ "$1" == "--prepare" ]] || [[ "$1" == "-p" ]]; then
    PREPARE_ONLY=true
elif [[ "$1" == "--destroy" ]] || [[ "$1" == "--down" ]] || [[ "$1" == "-d" ]]; then
    DESTROY=true
fi

echo -e "${BLUE}========================================${NC}"
if [ "$PREPARE_ONLY" = true ]; then
    echo -e "${BLUE}MegaBank - Prepare Dependencies${NC}"
elif [ "$DESTROY" = true ]; then
    echo -e "${BLUE}MegaBank - Destroy Environment${NC}"
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

# Destroy mode: Stop and remove everything
if [ "$DESTROY" = true ]; then
    echo -e "${YELLOW}WARNING: This will completely remove:${NC}"
    echo -e "  - All containers (payment-gateway, mysql, prometheus, grafana, jupyter)"
    echo -e "  - All networks created by docker-compose"
    echo -e "  - All volumes (INCLUDING ALL DATA)"
    echo -e "  - All built images"
    echo ""
    echo -e "${RED}This action cannot be undone!${NC}"
    echo ""
    read -p "Are you sure you want to continue? (yes/no): " -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        echo -e "${BLUE}Destroy cancelled.${NC}"
        exit 0
    fi

    echo -e "${BLUE}Stopping and removing containers...${NC}"
    docker-compose down -v 2>/dev/null || true

    echo -e "${BLUE}Removing built images...${NC}"
    docker rmi payment-gateway_payment-gateway 2>/dev/null || echo -e "${YELLOW}  (image not found, skipping)${NC}"
    docker rmi payment-gateway_jupyter 2>/dev/null || echo -e "${YELLOW}  (image not found, skipping)${NC}"

    echo -e "${BLUE}Pruning unused Docker resources...${NC}"
    docker system prune -f

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Environment Destroyed Successfully${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}All containers, networks, and volumes have been removed.${NC}"
    echo -e "${BLUE}To start fresh, run: ./quick-start.sh${NC}"
    echo ""
    exit 0
fi

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
    if curl -s http://localhost:8080/payment-gateway/ &> /dev/null; then
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
echo ""
echo -e "  ${YELLOW}Web UI (Browser):${NC}"
echo -e "    ${GREEN}http://localhost:8080/payment-gateway/${NC}"
echo -e "    ${BLUE}└─ Interactive dashboard, payment processing, and API documentation${NC}"
echo ""
echo -e "  ${YELLOW}API Endpoints:${NC}"
echo -e "    ${GREEN}http://localhost:8080/payment-gateway/api/${NC}"
echo -e "    ${BLUE}└─ Health: /api/health | Payments: /api/payments | Transactions: /api/transactions${NC}"
echo ""
echo -e "  ${YELLOW}Monitoring & Analytics:${NC}"
echo -e "    Prometheus:  ${GREEN}http://localhost:9090${NC}"
echo -e "    Grafana:     ${GREEN}http://localhost:3000${NC} ${BLUE}(admin/admin123)${NC}"
echo -e "    Jupyter:     ${GREEN}http://localhost:8888${NC}"
echo ""
echo -e "  ${YELLOW}Database:${NC}"
echo -e "    MySQL:       ${GREEN}localhost:3306${NC} ${BLUE}(payment_admin/Payment123!)${NC}"
echo ""

# Test the health endpoint
echo -e "${BLUE}Testing health endpoint...${NC}"
HEALTH=$(curl -s -H "X-API-Key: test_key_12345" http://localhost:8080/payment-gateway/api/health)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Health check successful${NC}"
    echo "$HEALTH" | python -m json.tool 2>/dev/null || echo "$HEALTH"
else
    echo -e "${RED}✗ Health check failed${NC}"
fi

echo ""
echo -e "${BLUE}Quick Test Commands:${NC}"
echo ""
echo -e "${YELLOW}# Open the Web UI (in your browser)${NC}"
echo -e "open http://localhost:8080/payment-gateway/"
echo ""
echo -e "${YELLOW}# Test health endpoint (API)${NC}"
echo -e 'curl -H "X-API-Key: test_key_12345" http://localhost:8080/payment-gateway/api/health'
echo ""
echo -e "${YELLOW}# Test payment (API)${NC}"
echo -e 'curl -X POST http://localhost:8080/payment-gateway/api/payments \'
echo -e '  -H "X-API-Key: test_key_12345" \'
echo -e '  -H "Content-Type: application/json" \'
echo -e '  -d '"'"'{"card_number":"4111111111111111","cvv":"123","expiry":"12/25","amount":99.99}'"'"
echo ""
echo -e "${YELLOW}# View all transactions (API)${NC}"
echo -e 'curl -H "X-API-Key: test_key_12345" http://localhost:8080/payment-gateway/api/transactions'
echo ""
echo -e "${YELLOW}# View logs${NC}"
echo -e "docker-compose logs -f payment-gateway"
echo ""
echo -e "${YELLOW}# Stop services${NC}"
echo -e "docker-compose down"
echo ""
echo -e "${YELLOW}# Completely destroy environment (removes all data)${NC}"
echo -e "./scripts/quick-start.sh --destroy"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Setup complete! Happy testing!${NC}"
echo -e "${GREEN}========================================${NC}"
