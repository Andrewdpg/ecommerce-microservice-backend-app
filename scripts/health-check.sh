#!/bin/bash

# Health check script for microservices
# Usage: ./health-check.sh <kubeconfig> <namespace> <timeout>

set -e

KUBECONFIG_FILE="$1"
NAMESPACE="$2"
TIMEOUT="${3:-300}"

echo "Starting health check for namespace: $NAMESPACE"
echo "Timeout: $TIMEOUT seconds"

# Function to check if all pods are running
check_pods() {
    echo "Checking pod status..."
    kubectl --kubeconfig="$KUBECONFIG_FILE" get pods -n "$NAMESPACE" -o wide
    
    # Check if all pods are running
    local not_ready=$(kubectl --kubeconfig="$KUBECONFIG_FILE" get pods -n "$NAMESPACE" --field-selector=status.phase!=Running --no-headers | wc -l)
    
    if [ "$not_ready" -gt 0 ]; then
        echo "❌ Some pods are not running:"
        kubectl --kubeconfig="$KUBECONFIG_FILE" get pods -n "$NAMESPACE" --field-selector=status.phase!=Running
        return 1
    else
        echo "✅ All pods are running"
        return 0
    fi
}

# Function to check service endpoints
check_services() {
    echo "Checking service endpoints..."
    kubectl --kubeconfig="$KUBECONFIG_FILE" get svc -n "$NAMESPACE"
    
    # Check if services have endpoints
    local services_without_endpoints=$(kubectl --kubeconfig="$KUBECONFIG_FILE" get svc -n "$NAMESPACE" -o jsonpath='{.items[?(@.spec.clusterIP!="None")].metadata.name}' | while read svc; do
        if ! kubectl --kubeconfig="$KUBECONFIG_FILE" get endpoints "$svc" -n "$NAMESPACE" -o jsonpath='{.subsets[*].addresses[*].ip}' | grep -q .; then
            echo "$svc"
        fi
    done | wc -l)
    
    if [ "$services_without_endpoints" -gt 0 ]; then
        echo "❌ Some services don't have endpoints"
        return 1
    else
        echo "✅ All services have endpoints"
        return 0
    fi
}

# Function to check health endpoints
check_health_endpoints() {
    echo "Checking health endpoints..."
    
    # Get service names and ports
    local services=("user-service:8700" "product-service:8500" "order-service:8300" "payment-service:8400" "shipping-service:8600" "favourite-service:8800")
    
    for service_port in "${services[@]}"; do
        local service_name=$(echo "$service_port" | cut -d: -f1)
        local port=$(echo "$service_port" | cut -d: -f2)
        
        echo "Checking health for $service_name..."
        
        # Port forward to service
        kubectl --kubeconfig="$KUBECONFIG_FILE" port-forward -n "$NAMESPACE" "svc/$service_name" "$port:$port" > /dev/null 2>&1 &
        local pf_pid=$!
        
        # Wait a bit for port forward to establish
        sleep 5
        
        # Check health endpoint
        if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "✅ $service_name is healthy"
        else
            echo "❌ $service_name health check failed"
            kill $pf_pid 2>/dev/null || true
            return 1
        fi
        
        # Clean up port forward
        kill $pf_pid 2>/dev/null || true
    done
    
    return 0
}

# Main health check function
main() {
    local start_time=$(date +%s)
    local end_time=$((start_time + TIMEOUT))
    
    echo "Health check started at $(date)"
    
    while [ $(date +%s) -lt $end_time ]; do
        echo "--- Health check iteration ---"
        
        if check_pods && check_services && check_health_endpoints; then
            echo "🎉 All health checks passed!"
            return 0
        fi
        
        echo "Health check failed, retrying in 30 seconds..."
        sleep 30
    done
    
    echo "❌ Health check failed after $TIMEOUT seconds"
    return 1
}

# Run main function
main "$@"
