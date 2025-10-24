#!/bin/bash
# Master test runner - Executes all test suites

set -e

API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8080}"

echo "=========================================="
echo "  E-commerce Microservices - Test Suite"
echo "=========================================="
echo ""

# Check if services are running
echo "Checking if services are available..."
if ! curl -s "$API_GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
    echo "❌ ERROR: API Gateway is not responding at $API_GATEWAY_URL"
    echo "Please ensure services are running:"
    echo "  docker-compose up -d"
    echo "  OR"
    echo "  kubectl get pods -n microservices-staging"
    exit 1
fi
echo "✓ Services are running"
echo ""

# Run Integration Tests
echo "Running Integration Tests..."
./integration-tests.sh
echo ""

# Run E2E Tests
echo "Running E2E Tests..."
./e2e-tests.sh
echo ""

# Run Performance Tests
echo "Running Performance Tests..."
./performance-tests.sh
echo ""

echo "=========================================="
echo "  All Test Suites Completed Successfully!"
echo "=========================================="