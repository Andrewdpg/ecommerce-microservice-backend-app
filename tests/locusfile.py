"""
Performance Tests - E-commerce Microservices
Locust load testing script
Usage: locust -f locustfile.py --host=http://localhost:8080
"""

import time
import random
from locust import HttpUser, task, between


class EcommerceUser(HttpUser):
    """
    Simulates an e-commerce user performing various operations
    """
    wait_time = between(1, 3)
    
    def on_start(self):
        """Called when a user starts"""
        self.user_id = None
        self.product_ids = []
        self.order_id = None
        
    @task(3)
    def view_products(self):
        """View product catalog - Most frequent operation (27.3%)"""
        response = self.client.get("/product-service/api/products")
        if response.status_code == 200:
            try:
                products = response.json()
                if 'collection' in products and products['collection']:
                    self.product_ids = [p['productId'] for p in products['collection'][:5]]
            except:
                pass
    
    @task(2)
    def create_user(self):
        """Create a new user (18.2%)"""
        user_data = {
            "userId": random.randint(1000, 9999),
            "firstName": "Test",
            "lastName": "User",
            "imageUrl": "https://example.com/avatar.jpg",
            "email": f"test.user{random.randint(1000, 9999)}@example.com",
            "phone": f"+57300{random.randint(1000000, 9999999)}",
            "credential": {
                "username": f"testuser{random.randint(1000, 9999)}",
                "password": "SecurePass123!",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True
            }
        }
        response = self.client.post("/user-service/api/users", json=user_data)
        if response.status_code == 200:
            try:
                self.user_id = response.json().get('userId')
            except:
                pass
    
    @task(2)
    def get_user(self):
        """Get user details (18.2%)"""
        if self.user_id:
            self.client.get(f"/user-service/api/users/{self.user_id}")
        else:
            # Use a default user ID if we haven't created one yet
            self.client.get("/user-service/api/users/4")
    
    @task(1)
    def create_order(self):
        """Create an order (9.1%)"""
        order_data = {
            "orderId": random.randint(1000, 9999),
            "orderDesc": f"Order {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(10.0, 500.0), 2),
            "cart": {
                "cartId": random.randint(1, 100)
            }
        }
        response = self.client.post("/order-service/api/orders", json=order_data)
        if response.status_code == 200:
            try:
                self.order_id = response.json().get('orderId')
            except:
                pass
    
    @task(1)
    def add_order_item(self):
        """Add item to order (9.1%)"""
        if self.product_ids:
            item_data = {
                "orderId": self.order_id or random.randint(1, 10),
                "productId": random.choice(self.product_ids) if self.product_ids else random.randint(1, 10),
                "orderedQuantity": random.randint(1, 5)
            }
            self.client.post("/shipping-service/api/shippings", json=item_data)
    
    @task(1)
    def view_orders(self):
        """View all orders (9.1%)"""
        self.client.get("/order-service/api/orders")
    
    @task(1)
    def view_order_items(self):
        """View order items / shippings (9.1%)"""
        self.client.get("/shipping-service/api/shippings")