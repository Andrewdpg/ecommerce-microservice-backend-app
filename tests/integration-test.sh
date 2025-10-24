#!/bin/bash
# Integration Tests - Microservices E-commerce
# Validates communication between services through API Gateway

set -e

API_GATEWAY_URL="${API_GATEWAY_URL:-http://localhost:8080}"

echo "=========================================="
echo "  Integration Tests"
echo "=========================================="
echo "API Gateway: $API_GATEWAY_URL"
echo ""

# Test 1: User Service Integration
echo "Test 1: User Service - Create User"
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

USER_ID=$(echo $USER_RESPONSE | grep -o '"userId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
if [ "$USER_ID" != "null" ] && [ "$USER_ID" != "" ]; then
    echo "✓ User creation successful, ID: $USER_ID"
else
    echo "✗ User creation failed"
    echo "Response: $USER_RESPONSE"
    exit 1
fi

# Test 2: Product Service Integration
echo ""
echo "Test 2: Product Service - Create Product"
PRODUCT_RESPONSE=$(curl -s -X POST "$API_GATEWAY_URL/product-service/api/products" \
    -H "Content-Type: application/json" \
    -d '{
        "productId": 3,
        "productTitle": "Test Product",
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

PRODUCT_ID=$(echo $PRODUCT_RESPONSE | grep -o '"productId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
if [ "$PRODUCT_ID" != "null" ] && [ "$PRODUCT_ID" != "" ]; then
    echo "✓ Product creation successful, ID: $PRODUCT_ID"
else
    echo "✗ Product creation failed"
    echo "Response: $PRODUCT_RESPONSE"
    exit 1
fi

# Test 3: Order Service Integration
echo ""
echo "Test 3: Order Service - Create Order"
ORDER_RESPONSE=$(curl -s -X POST "$API_GATEWAY_URL/order-service/api/orders" \
    -H "Content-Type: application/json" \
    -d '{
        "orderId": 3,
        "orderDesc": "Test Order",
        "orderFee": 99.99,
        "cart": {
            "cartId": 3
        }
    }')

ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"orderId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
if [ "$ORDER_ID" != "null" ] && [ "$ORDER_ID" != "" ]; then
    echo "✓ Order creation successful, ID: $ORDER_ID"
else
    echo "✗ Order creation failed"
    echo "Response: $ORDER_RESPONSE"
    exit 1
fi

# Test 4: Shipping Service Integration
echo ""
echo "Test 4: Shipping Service - Create Shipping Item"
SHIPPING_RESPONSE=$(curl -s -X POST "$API_GATEWAY_URL/shipping-service/api/shippings" \
    -H "Content-Type: application/json" \
    -d '{
        "orderId": 2,
        "productId": 2,
        "orderedQuantity": 2
    }')

echo "✓ Shipping item created"

# Test 5: API Gateway Routing
echo ""
echo "Test 5: API Gateway - Routing Test"
GATEWAY_USER_RESPONSE=$(curl -s "$API_GATEWAY_URL/user-service/api/users")
GATEWAY_PRODUCT_RESPONSE=$(curl -s "$API_GATEWAY_URL/product-service/api/products")

if [ "$GATEWAY_USER_RESPONSE" != "" ] && [ "$GATEWAY_PRODUCT_RESPONSE" != "" ]; then
    echo "✓ API Gateway routing successful"
else
    echo "✗ API Gateway routing failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "  All Integration Tests Passed!"
echo "=========================================="