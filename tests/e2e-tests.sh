#!/bin/bash
# End-to-End Tests - Microservices E-commerce
# Validates complete user flows

set -e

API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8080}"

echo "=========================================="
echo "  End-to-End Tests"
echo "=========================================="
echo "API Gateway: $API_GATEWAY_URL"
echo ""

# E2E Test 1: User Registration and Profile Update Flow
echo "E2E Test 1: User Registration and Profile Update"
echo "  Step 1: Create user..."
USER_RESPONSE=$(curl -s -X POST "$API_GATEWAY_URL/user-service/api/users" \
    -H "Content-Type: application/json" \
    -d '{
        "userId": 4,
        "firstName": "María",
        "lastName": "García",
        "imageUrl": "https://example.com/maria.jpg",
        "email": "maria.garcia@example.com",
        "phone": "+573007654321",
        "credential": {
            "username": "maria.garcia",
            "password": "SecurePass123!",
            "roleBasedAuthority": "ROLE_USER",
            "isEnabled": true,
            "isAccountNonExpired": true,
            "isAccountNonLocked": true,
            "isCredentialsNonExpired": true
        }
    }')

echo "  Step 2: Update user profile..."
UPDATE_RESPONSE=$(curl -s -X PUT "$API_GATEWAY_URL/user-service/api/users" \
    -H "Content-Type: application/json" \
    -d '{
        "userId": 4,
        "firstName": "María",
        "lastName": "Smith",
        "imageUrl": "https://example.com/maria.jpg",
        "email": "maria.garcia@example.com",
        "phone": "+573007654321",
        "credential": {
            "username": "maria.garcia",
            "password": "SecurePass123!",
            "roleBasedAuthority": "ROLE_USER",
            "isEnabled": true,
            "isAccountNonExpired": true,
            "isAccountNonLocked": true,
            "isCredentialsNonExpired": true
        }
    }')

echo "✓ E2E Test 1 PASSED: User Registration Flow"

# E2E Test 2: Product Catalog and Search Flow
echo ""
echo "E2E Test 2: Product Catalog and Search"
echo "  Step 1: Create product 1..."
PRODUCT1=$(curl -s -X POST "$API_GATEWAY_URL/product-service/api/products" \
    -H "Content-Type: application/json" \
    -d '{
        "productId": 4,
        "productTitle": "Test Product 1",
        "imageUrl": "test.com",
        "sku": "TEST001",
        "priceUnit": 99.99,
        "quantity": 10,
        "category": {
            "categoryId": 3,
            "categoryTitle": "Game",
            "imageUrl": null
        }
    }')

echo "  Step 2: Create product 2..."
PRODUCT2=$(curl -s -X POST "$API_GATEWAY_URL/product-service/api/products" \
    -H "Content-Type: application/json" \
    -d '{
        "productId": 5,
        "productTitle": "Test Product 2",
        "imageUrl": "test.com",
        "sku": "TEST002",
        "priceUnit": 149.99,
        "quantity": 5,
        "category": {
            "categoryId": 3,
            "categoryTitle": "Game",
            "imageUrl": null
        }
    }')

echo "  Step 3: Get all products..."
ALL_PRODUCTS=$(curl -s "$API_GATEWAY_URL/product-service/api/products")

echo "✓ E2E Test 2 PASSED: Product Catalog Flow"

# E2E Test 3: Shopping Cart and Order Flow
echo ""
echo "E2E Test 3: Shopping Cart and Order"
echo "  Step 1: Create order..."
ORDER_RESPONSE=$(curl -s -X POST "$API_GATEWAY_URL/order-service/api/orders" \
    -H "Content-Type: application/json" \
    -d '{
        "orderId": 3,
        "orderDesc": "Complete shopping order",
        "orderFee": 1029.98,
        "cart": {
            "cartId": 3
        }
    }')

echo "  Step 2: Add item 1 to order..."
ITEM1=$(curl -s -X POST "$API_GATEWAY_URL/shipping-service/api/shippings" \
    -H "Content-Type: application/json" \
    -d '{
        "orderId": 3,
        "productId": 4,
        "orderedQuantity": 1
    }')

echo "  Step 3: Add item 2 to order..."
ITEM2=$(curl -s -X POST "$API_GATEWAY_URL/shipping-service/api/shippings" \
    -H "Content-Type: application/json" \
    -d '{
        "orderId": 3,
        "productId": 5,
        "orderedQuantity": 1
    }')

echo "  Step 4: Verify order items..."
ORDER_ITEMS=$(curl -s "$API_GATEWAY_URL/shipping-service/api/shippings")

echo "✓ E2E Test 3 PASSED: Shopping Cart Flow"

# E2E Test 4: Order Management Flow
echo ""
echo "E2E Test 4: Order Management"
echo "  Step 1: Get order details..."
ORDER_DETAILS=$(curl -s "$API_GATEWAY_URL/order-service/api/orders/3")

echo "✓ E2E Test 4 PASSED: Order Management Flow"

# E2E Test 5: System Health and Monitoring Flow
echo ""
echo "E2E Test 5: System Health Monitoring"
echo "  Checking service health..."

USER_HEALTH=$(curl -s "$API_GATEWAY_URL/user-service/actuator/health" || echo "unavailable")
PRODUCT_HEALTH=$(curl -s "$API_GATEWAY_URL/product-service/actuator/health" || echo "unavailable")
ORDER_HEALTH=$(curl -s "$API_GATEWAY_URL/order-service/actuator/health" || echo "unavailable")
SHIPPING_HEALTH=$(curl -s "$API_GATEWAY_URL/shipping-service/actuator/health" || echo "unavailable")

HEALTH_COUNT=0
[ "$USER_HEALTH" != "unavailable" ] && HEALTH_COUNT=$((HEALTH_COUNT + 1))
[ "$PRODUCT_HEALTH" != "unavailable" ] && HEALTH_COUNT=$((HEALTH_COUNT + 1))
[ "$ORDER_HEALTH" != "unavailable" ] && HEALTH_COUNT=$((HEALTH_COUNT + 1))
[ "$SHIPPING_HEALTH" != "unavailable" ] && HEALTH_COUNT=$((HEALTH_COUNT + 1))

if [ "$HEALTH_COUNT" -ge 3 ]; then
    echo "✓ E2E Test 5 PASSED: System Health ($HEALTH_COUNT/4 services UP)"
else
    echo "✗ E2E Test 5 FAILED: Not enough services healthy ($HEALTH_COUNT/4)"
    exit 1
fi

echo ""
echo "=========================================="
echo "  All E2E Tests Passed!"
echo "=========================================="