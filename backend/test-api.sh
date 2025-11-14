#!/bin/bash

# D365 Dashboard API Testing Script
# This script tests all the API endpoints

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================================="
echo "  D365 Dashboard API Testing Script"
echo "=================================================="
echo ""

# Function to test an endpoint
test_endpoint() {
    local name=$1
    local url=$2
    
    echo -e "${YELLOW}Testing: ${name}${NC}"
    echo "URL: ${url}"
    
    response=$(curl -s -w "\n%{http_code}" "${url}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ SUCCESS (HTTP ${http_code})${NC}"
        echo "Response: ${body}" | jq '.' 2>/dev/null || echo "${body}"
    else
        echo -e "${RED}✗ FAILED (HTTP ${http_code})${NC}"
        echo "Response: ${body}"
    fi
    
    echo ""
    echo "--------------------------------------------------"
    echo ""
}

# Check if jq is installed (for pretty JSON)
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Note: Install 'jq' for pretty JSON output${NC}"
    echo ""
fi

# Test 1: Health Check
test_endpoint "Health Check" "${BASE_URL}/api/health"

# Test 2: D365 Connection
test_endpoint "D365 Connection Test" "${BASE_URL}/api/health/d365-connection"

# Test 3: Configuration Info
test_endpoint "Configuration Info" "${BASE_URL}/api/health/config"

# Test 4: Get All Accounts
test_endpoint "Get All Accounts" "${BASE_URL}/api/accounts"

# Test 5: Get Top 10 Accounts
test_endpoint "Get Top 10 Accounts" "${BASE_URL}/api/accounts?top=10"

# Test 6: Get Account Count
test_endpoint "Get Account Count" "${BASE_URL}/api/accounts/count"

# Test 7: Search Accounts by City
test_endpoint "Search Accounts (City)" "${BASE_URL}/api/accounts/search?city=Seattle"

echo "=================================================="
echo "  Testing Complete!"
echo "=================================================="
echo ""
echo "If you see connection errors, make sure:"
echo "  1. The application is running (mvn spring-boot:run)"
echo "  2. Azure AD credentials are configured"
echo "  3. You have network access to D365"
echo ""
