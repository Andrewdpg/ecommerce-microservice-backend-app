#!/bin/bash
# Performance Tests Runner
# Executes Locust load tests and generates reports

set -e

API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8080}"
USERS="${USERS:-50}"
SPAWN_RATE="${SPAWN_RATE:-10}"
RUN_TIME="${RUN_TIME:-300s}"

echo "=========================================="
echo "  Performance Tests with Locust"
echo "=========================================="
echo "Target: $API_GATEWAY_URL"
echo "Users: $USERS concurrent"
echo "Spawn Rate: $SPAWN_RATE users/sec"
echo "Duration: $RUN_TIME"
echo ""

# Check if locust is installed
if ! command -v locust &> /dev/null; then
    echo "Installing Locust..."
    pip install locust
fi

# Run Locust tests
echo "Running performance tests..."
locust -f locustfile.py \
    --host=$API_GATEWAY_URL \
    --users=$USERS \
    --spawn-rate=$SPAWN_RATE \
    --run-time=$RUN_TIME \
    --html=performance_report.html \
    --csv=performance_data \
    --headless

echo ""
echo "=========================================="
echo "  Performance Test Summary"
echo "=========================================="

if [ -f performance_data_stats.csv ]; then
    echo "Total Requests: $(tail -n 1 performance_data_stats.csv | cut -d',' -f2)"
    echo "Failed Requests: $(tail -n 1 performance_data_stats.csv | cut -d',' -f3)"
    echo "Average Response Time: $(tail -n 1 performance_data_stats.csv | cut -d',' -f4)ms"
    echo "Requests per Second: $(tail -n 1 performance_data_stats.csv | cut -d',' -f5)"
    echo ""
    echo "Reports generated:"
    echo "  - performance_report.html"
    echo "  - performance_data_stats.csv"
    echo "  - performance_data_failures.csv"
fi

echo "=========================================="