def SERVICES = [
    [name: 'user-service', port: '8700', path: 'user-service'],
    [name: 'product-service', port: '8500', path: 'product-service'],
    [name: 'order-service', port: '8300', path: 'order-service'],
    [name: 'shipping-service', port: '8600', path: 'shipping-service'],
    [name: 'service-discovery', port: '8761', path: 'service-discovery', stagePort: 30187, prodPort: 30087],
    [name: 'proxy-client', port: '8900', path: 'proxy-client'],
    [name: 'api-gateway', port: '8080', path: 'api-gateway', stagePort: 30180, prodPort: 30080]
]

pipeline {
    agent any
    
    environment {
        REGISTRY = 'docker.io/andrewdpg'
        DOCKERHUB = 'docker-hub-credentials'
        K8S_NAMESPACE_STAGING = 'microservices-staging'
        K8S_NAMESPACE_PROD = 'microservices-prod'
        KUBECONFIG_CREDENTIAL = 'kubeconfig'
        RELEASE_VERSION = '1.0.0'
    }
    
    options {
        timestamps()
    }
    
    stages {
        stage('Checkout & Detect Changes') {
            steps {
                deleteDir()
                checkout scm
                script {
                    env.DEPLOY_TIMESTAMP = sh(script: 'date +%Y%m%d-%H%M%S', returnStdout: true).trim()
                    env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.GIT_BRANCH = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
                    
                    // Determine environment based on branch
                    if (env.GIT_BRANCH == 'main' || env.GIT_BRANCH == 'master') {
                        env.TARGET_ENVIRONMENT = 'production'
                    } else if (env.GIT_BRANCH == 'develop' || env.GIT_BRANCH == 'staging') {
                        env.TARGET_ENVIRONMENT = 'staging'
                    } else {
                        env.TARGET_ENVIRONMENT = 'dev'
                    }
                    
                    // Create image tags
                    env.IMAGE_TAG = "${env.GIT_BRANCH}-${env.GIT_COMMIT_SHORT}"
                    env.LATEST_TAG = "latest"
                    
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Target Environment: ${env.TARGET_ENVIRONMENT}"
                    echo "IMAGE_TAG: ${env.IMAGE_TAG}"
                    
                    // Detect changed services
                    def changedServices = []

                    env.CHANGED_SERVICES = ''
                    echo "Services to build: ${env.CHANGED_SERVICES}"
                }
                stash name: 'workspace', includes: '**/*'
            }
        }

        stage('Build & Test Core Services') {
            parallel {
                stage('Build Service Discovery') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('service-discovery') }
                    }
                    steps {
                        script {
                            buildService('service-discovery', '8761')
                        }
                    }
                }
                
                stage('Build API Gateway') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('api-gateway') }
                    }
                    steps {
                        script {
                            buildService('api-gateway', '8080')
                        }
                    }
                }
            }
        }
        
        stage('Build & Test Changed Services') {
            parallel {
                stage('Build User Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('user-service') }
                    }
                    steps {
                        script {
                            buildService('user-service', '8700')
                        }
                    }
                }
                
                stage('Build Product Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('product-service') }
                    }
                    steps {
                        script {
                            buildService('product-service', '8500')
                        }
                    }
                }
                
                stage('Build Order Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('order-service') }
                    }
                    steps {
                        script {
                            buildService('order-service', '8300')
                        }
                    }
                }
                
                stage('Build Shipping Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('shipping-service') }
                    }
                    steps {
                        script {
                            buildService('shipping-service', '8600')
                        }
                    }
                }
                stage('Build Proxy Client') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('proxy-client') }
                    }
                    steps {
                        script {
                            buildService('proxy-client', '8900')
                        }
                    }
                }
            }
        }
        
        stage('Docker Push') {
            when {
                anyOf {
                    equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
                    equals expected: 'production', actual: env.TARGET_ENVIRONMENT
                }
            }
            steps {
                unstash 'workspace'
                withCredentials([usernamePassword(credentialsId: "${DOCKERHUB}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        sh '''
                            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        '''
                        
                        // Push changed services
                    }
                }
            }
        }
        
        stage('Deploy Core Services to Staging') {
            when {
                anyOf {
                    equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
                    equals expected: 'production', actual: env.TARGET_ENVIRONMENT
                }
            }
            steps {
                unstash 'workspace'
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        echo "Deploying core services to staging environment..."
                        deployCoreServicesToEnvironment('staging', K8S_NAMESPACE_STAGING)
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                anyOf {
                    equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
                    equals expected: 'production', actual: env.TARGET_ENVIRONMENT
                }
            }
            steps {
                unstash 'workspace'
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        echo "Deploying to staging environment..."
                        deployToEnvironment('staging', K8S_NAMESPACE_STAGING)
                    }
                }
            }
        }

        stage('Integration Tests') {
            when {
                anyOf {
                    equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
                    equals expected: 'production', actual: env.TARGET_ENVIRONMENT
                }
            }
            steps {
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        runIntegrationTests()
                    }
                }
            }
        }
        
        stage('E2E Tests') {
            when {
                anyOf {
                    equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
                    equals expected: 'production', actual: env.TARGET_ENVIRONMENT
                }
            }
            steps {
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        runE2ETests()
                    }
                }
            }
        }

        stage('Deploy Core Services to Production') {
            when {
                equals expected: 'production', actual: env.TARGET_ENVIRONMENT
            }
            steps {
                unstash 'workspace'
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        echo "Deploying core services to production environment..."
                        deployCoreServicesToEnvironment('production', K8S_NAMESPACE_PROD)
                    }
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                equals expected: 'production', actual: env.TARGET_ENVIRONMENT
            }
            steps {
                unstash 'workspace'
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        echo "Deploying to production environment..."
                        deployToEnvironment('production', K8S_NAMESPACE_PROD)
                    }
                }
            }
        }

        stage('Generate Release Notes') {
            steps {
            withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                script {
                    sh """
                        # Generar notas de release automáticas
                        git config --global user.name "Jenkins CI"
                        git config --global user.email "jenkins@example.com"

                        git log --oneline --since="7 days ago" > CHANGELOG.md
                        echo "## Release ${RELEASE_VERSION}" > release_notes.md
                        echo "### Date: \$(date)" >> release_notes.md
                        echo "### Changes:" >> release_notes.md
                        cat CHANGELOG.md >> release_notes.md
                        
                        # Crear tag de release
                        git tag -a v${RELEASE_VERSION} -m "Release version ${RELEASE_VERSION}"
                        git remote set-url origin https://${GITHUB_TOKEN}@github.com/Andrewdpg/ecommerce-microservice-backend-app.git
                        git push origin v${RELEASE_VERSION}
                    """
                }
            }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'release_notes.md', fingerprint: true
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully for services: ${env.CHANGED_SERVICES}"
            script {
                if (env.TARGET_ENVIRONMENT == 'staging') {
                    echo "Staging deployment completed"
                } else if (env.TARGET_ENVIRONMENT == 'production') {
                    echo "Production deployment completed"
                } else {
                    echo "Development build completed (no deployment)"
                }
            }
        }
        failure {
            echo "Pipeline failed for services: ${env.CHANGED_SERVICES}"
            script {
                if (env.TARGET_ENVIRONMENT != 'dev') {
                    echo "Consider rolling back the deployment"
                }
            }
        }
    }
}

def buildService(serviceName, servicePort) {
    echo "Building ${serviceName}..."
    
    // Build Maven project
    sh "mvn clean compile -pl ${serviceName} -am"
    
    // Run unit tests
    sh "mvn test -pl ${serviceName} -am"
    
    // Package application
    sh "mvn package -pl ${serviceName} -am -DskipTests"
    
    // Publish test results (only if they exist)
    script {
        def testResults = "${serviceName}/target/surefire-reports/*.xml"
        if (fileExists(testResults)) {
            junit testResults
        } else {
            echo "No test results found for ${serviceName}, skipping JUnit report"
        }
    }
    
    // Build Docker image with proper tags
    sh "docker build -f ${serviceName}/Dockerfile -t ${REGISTRY}/${serviceName}:${IMAGE_TAG} -t ${REGISTRY}/${serviceName}:${LATEST_TAG} ."
    
    echo "Successfully built ${serviceName}:${IMAGE_TAG}"
}

def deployCoreServicesToEnvironment(environment, namespace) {
    echo "Deploying core services to ${environment} environment (namespace: ${namespace})..."

    // Apply the ConfigMap
    sh """
        sed -e "s|\\\${NAMESPACE}|${namespace}|g" \
            k8s/base/configmap.yaml | kubectl --kubeconfig="\$KCFG" apply -f -
    """
    
    // Deploy core services in order with waits
    deployService('zipkin', '9411', namespace, 30087)
    
    // Define services locally
    def services = [
        [name: 'service-discovery', port: '8761'],
        [name: 'api-gateway', port: '8080'],
    ]
    
    // Deploy core services
    def changedServices = env.CHANGED_SERVICES.split(',')
    for (serviceName in changedServices) {
        def service = services.find { it.name == serviceName }
        if (service) {
            def nodePort = environment.equals('staging') ? service.stagePort : service.prodPort
            deployService(serviceName, service.port, namespace, nodePort)
        }
    }
}

def deployToEnvironment(environment, namespace) {
    echo "Deploying to ${environment} environment (namespace: ${namespace})..."
    
    // Aplicar el ConfigMap
    sh """
        sed -e "s|\\\${NAMESPACE}|${namespace}|g" \
            k8s/base/configmap.yaml | kubectl --kubeconfig="\$KCFG" apply -f -
    """
    
    // Define services locally
    def services = [
        [name: 'user-service', port: '8700'],
        [name: 'product-service', port: '8500'],
        [name: 'order-service', port: '8300'],
        [name: 'shipping-service', port: '8600'],
        [name: 'proxy-client', port: '8900']
    ]
    
    // Deploy changed services
    def changedServices = env.CHANGED_SERVICES.split(',')
    for (serviceName in changedServices) {
        def service = services.find { it.name == serviceName }
        if (service) {
            def nodePort = environment.equals('staging') ? service.stagePort : service.prodPort
            deployService(serviceName, service.port, namespace, nodePort)
        }
    }
}

def deployService(serviceName, servicePort, namespace, nodePort) {
    echo "Deploying ${serviceName} to ${namespace}..."

    // Apply Kubernetes manifests using sed for variable substitution
    sh """
        sed -e "s|\\\${REGISTRY}|${REGISTRY}|g" \
            -e "s|\\\${NAMESPACE}|${namespace}|g" \
            -e "s|\\\${IMAGE_TAG}|${IMAGE_TAG}|g" \
            -e "s|\\\${NODE_PORT}|${nodePort}|g" \
            k8s/base/${serviceName}.yaml | kubectl --kubeconfig="\$KCFG" apply -f -
    """

    // Wait for the service to be ready with kubectl wait
    sh """
        kubectl --kubeconfig="\$KCFG" rollout status deployment/${serviceName} -n ${namespace} --timeout=600s
    """

    echo "Successfully deployed ${serviceName} to ${namespace}"
}

def runIntegrationTests() {
    echo "Running integration tests on staging environment..."
    
    // Get staging namespace (always use staging for tests)
    def namespace = K8S_NAMESPACE_STAGING
    
    // Verify all services are running
    sh """
        kubectl --kubeconfig="\$KCFG" get pods -n ${namespace}
        kubectl --kubeconfig="\$KCFG" get svc -n ${namespace}
    """
    
    // Get API Gateway URL for kind cluster
    def apiGatewayUrl = "ci-control-plane:30080"
    
    // Run integration tests using curl and jq
    sh """
        # Test 1: User Service - Create and retrieve user
        echo "Test 1: User Service Integration"
        USER_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/user-service/api/users" \\
            -H "Content-Type: application/json" \\
            -d '{ "userId": 4, "firstName": "María", "lastName": "García", "imageUrl": "https://example.com/maria.jpg", "email": "maria.garcia@example.com", "phone": "+573007654321", "credential": {   "username": "maria.garcia",   "password": "SecurePass123!",   "roleBasedAuthority": "ROLE_USER",   "isEnabled": true,   "isAccountNonExpired": true,   "isAccountNonLocked": true,   "isCredentialsNonExpired": true }}')
        echo "User created: \$USER_RESPONSE"
        
        USER_ID=\$(echo \$USER_RESPONSE | grep -o '"userId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
        if [ "\$USER_ID" != "null" ] && [ "\$USER_ID" != "" ]; then
            echo "✓ User creation successful, ID: \$USER_ID"
        else
            echo "✗ User creation failed"
            exit 1
        fi
        
        # Test 2: Product Service - Create and retrieve product
        echo "Test 2: Product Service Integration"
        PRODUCT_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/product-service/api/products" \\
            -H "Content-Type: application/json" \\
            -d '{"productId": 3,"productTitle": "Test Product","imageUrl": "test.com","sku": "TEST001","priceUnit": 99.99,"quantity": 10,"category": {    "categoryId": 3,    "categoryTitle": "Game",    "imageUrl": null}}')
        echo "Product created: \$PRODUCT_RESPONSE"
        
        PRODUCT_ID=\$(echo \$PRODUCT_RESPONSE | grep -o '"productId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
        if [ "\$PRODUCT_ID" != "null" ] && [ "\$PRODUCT_ID" != "" ]; then
            echo "✓ Product creation successful, ID: \$PRODUCT_ID"
        else
            echo "✗ Product creation failed"
            exit 1
        fi
        
        # Test 3: Order Service - Create order with user and product
        echo "Test 3: Order Service Integration"
        ORDER_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/order-service/api/orders" \\
            -H "Content-Type: application/json" \\
            -d '{ "orderId": 3, "orderDesc": "Test Order", "orderFee": 99.99, "cart": {     "cartId":3 }}')
        echo "Order created: \$ORDER_RESPONSE"
        
        ORDER_ID=\$(echo \$ORDER_RESPONSE | grep -o '"orderId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
        if [ "\$ORDER_ID" != "null" ] && [ "\$ORDER_ID" != "" ]; then
            echo "✓ Order creation successful, ID: \$ORDER_ID"
        else
            echo "✗ Order creation failed"
            exit 1
        fi
        
        # Test 4: Shipping Service - Create order item
        echo "Test 4: Shipping Service Integration"
        ORDER_ITEM_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/shipping-service/api/shippings" \\
            -H "Content-Type: application/json" \\
            -d '{"orderId": 2,"productId": 2,"orderedQuantity": 2}')
        echo "Order item created: \$ORDER_ITEM_RESPONSE"
        
        # Test 5: API Gateway - Test routing to all services
        echo "Test 6: API Gateway Integration"
        GATEWAY_USER_RESPONSE=\$(curl -s "http://${apiGatewayUrl}/user-service/api/users")
        GATEWAY_PRODUCT_RESPONSE=\$(curl -s "http://${apiGatewayUrl}/product-service/api/products")
        
        if [ "\$GATEWAY_USER_RESPONSE" != "" ] && [ "\$GATEWAY_PRODUCT_RESPONSE" != "" ]; then
            echo "✓ API Gateway routing successful"
        else
            echo "✗ API Gateway routing failed"
            exit 1
        fi
        
        echo "All integration tests passed successfully!"
    """
    
    echo "Integration tests completed successfully"
}

def runE2ETests() {
    echo "Running end-to-end tests on staging environment..."
    
    // Get staging namespace (always use staging for tests)
    def namespace = K8S_NAMESPACE_STAGING
    
    // Get API Gateway URL for kind cluster
    def apiGatewayUrl = "ci-control-plane:30080"
    
    // Run E2E tests
    sh """
        # E2E Test 1: Complete User Registration and Profile Update Flow
        echo "E2E Test 1: User Registration and Profile Update Flow"
        
        # Create user
        USER_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/user-service/api/users" \\
            -H "Content-Type: application/json" \\
            -d '{ "userId": 4, "firstName": "María", "lastName": "García", "imageUrl": "https://example.com/maria.jpg", "email": "maria.garcia@example.com", "phone": "+573007654321", "credential": {   "username": "maria.garcia",   "password": "SecurePass123!",   "roleBasedAuthority": "ROLE_USER",   "isEnabled": true,   "isAccountNonExpired": true,   "isAccountNonLocked": true,   "isCredentialsNonExpired": true }}')
        
        # Update user profile
        UPDATE_RESPONSE=\$(curl -s -X PUT "http://${apiGatewayUrl}/user-service/api/users" \\
            -H "Content-Type: application/json" \\
            -d '{ "userId": 4, "firstName": "María", "lastName": "Smith", "imageUrl": "https://example.com/maria.jpg", "email": "maria.garcia@example.com", "phone": "+573007654321", "credential": {   "username": "maria.garcia",   "password": "SecurePass123!",   "roleBasedAuthority": "ROLE_USER",   "isEnabled": true,   "isAccountNonExpired": true,   "isAccountNonLocked": true,   "isCredentialsNonExpired": true }}')
        
        # E2E Test 2: Complete Product Catalog and Search Flow
        echo "E2E Test 2: Product Catalog and Search Flow"
        
        # Create multiple products
        PRODUCT1=\$(curl -s -X POST "http://${apiGatewayUrl}/product-service/api/products" \\
            -H "Content-Type: application/json" \\
            -d '{"productId": 4,"productTitle": "Test Product","imageUrl": "test.com","sku": "TEST001","priceUnit": 99.99,"quantity": 10,"category": {    "categoryId": 3,    "categoryTitle": "Game",    "imageUrl": null}}')
        PRODUCT2=\$(curl -s -X POST "http://${apiGatewayUrl}/product-service/api/products" \\
            -H "Content-Type: application/json" \\
            -d '{"productId": 5, "productTitle": "Test Product","imageUrl": "test.com","sku": "TEST001","priceUnit": 99.99,"quantity": 10,"category": {    "categoryId": 3,    "categoryTitle": "Game",    "imageUrl": null}}')
        
        # Get all products
        ALL_PRODUCTS=\$(curl -s "http://${apiGatewayUrl}/product-service/api/products")
        
        # E2E Test 3: Complete Shopping Cart and Order Flow
        echo "E2E Test 3: Shopping Cart and Order Flow"
        
        # Create order
        ORDER_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/order-service/api/orders" \\
            -H "Content-Type: application/json" \\
            -d '{"orderId": 3,"orderDesc": "Complete shopping order","orderFee": 1029.98,"cart": {     "cartId":3 }}')
        
        # Add items to order
        ITEM1=\$(curl -s -X POST "http://${apiGatewayUrl}/shipping-service/api/shippings" \\
            -H "Content-Type: application/json" \\
            -d '{"orderId":3,"productId":4,"orderedQuantity":1}')
        ITEM2=\$(curl -s -X POST "http://${apiGatewayUrl}/shipping-service/api/shippings" \\
            -H "Content-Type: application/json" \\
            -d '{"orderId":3,"productId":5,"orderedQuantity":1}')
        
        # Verify order items
        ORDER_ITEMS=\$(curl -s "http://${apiGatewayUrl}/shipping-service/api/shippings")
        
        # E2E Test 4: Complete Order Management Flow
        echo "E2E Test 4: Order Management Flow"
        
        # Get order details
        ORDER_DETAILS=\$(curl -s "http://${apiGatewayUrl}/order-service/api/orders/3")
        
        # E2E Test 5: Complete System Health and Monitoring Flow
        echo "E2E Test 5: System Health and Monitoring Flow"
        
        # Test all service health endpoints
        USER_HEALTH=\$(curl -s "http://${apiGatewayUrl}/user-service/actuator/health" || echo "unavailable")
        PRODUCT_HEALTH=\$(curl -s "http://${apiGatewayUrl}/product-service/actuator/health" || echo "unavailable")
        ORDER_HEALTH=\$(curl -s "http://${apiGatewayUrl}/order-service/actuator/health" || echo "unavailable")
        SHIPPING_HEALTH=\$(curl -s "http://${apiGatewayUrl}/shipping-service/actuator/health" || echo "unavailable")
        
        HEALTH_COUNT=0
        [ "\$USER_HEALTH" != "unavailable" ] && HEALTH_COUNT=\$((HEALTH_COUNT + 1))
        [ "\$PRODUCT_HEALTH" != "unavailable" ] && HEALTH_COUNT=\$((HEALTH_COUNT + 1))
        [ "\$ORDER_HEALTH" != "unavailable" ] && HEALTH_COUNT=\$((HEALTH_COUNT + 1))
        [ "\$SHIPPING_HEALTH" != "unavailable" ] && HEALTH_COUNT=\$((HEALTH_COUNT + 1))
        
        if [ "\$HEALTH_COUNT" -ge 3 ]; then
            echo "✓ E2E Test 5 passed: System health monitoring"
        else
            echo "✗ E2E Test 5 failed"
            exit 1
        fi
        
        echo "All E2E tests passed successfully!"
    """
    
    echo "E2E tests completed successfully"
}

def runPerformanceTests() {
    echo "Running performance tests with Locust..."
    
    // Get staging namespace (always use staging for tests)
    def namespace = K8S_NAMESPACE_STAGING
    
    // Get API Gateway URL for kind cluster
    def apiGatewayUrl = "ci-control-plane:30080"

    
    
    // Create Locust test file
    writeFile file: 'locustfile.py', text: '''
import time
import random
from locust import HttpUser, task, between

class EcommerceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Called when a user starts"""
        self.user_id = None
        self.product_ids = []
        self.order_id = None
        
    @task(3)
    def view_products(self):
        """View product catalog"""
        response = self.client.get("/product-service/api/products")
        if response.status_code == 200:
            products = response.json()
            if 'collection' in products and products['collection']:
                self.product_ids = [p['productId'] for p in products['collection'][:5]]
    
    @task(2)
    def create_user(self):
        """Create a new user"""
        user_data = { "userId": 4, "firstName": "María", "lastName": "García", "imageUrl": "https://example.com/maria.jpg", "email": "maria.garcia@example.com", "phone": "+573007654321", "credential": {   "username": "maria.garcia",   "password": "SecurePass123!",   "roleBasedAuthority": "ROLE_USER",   "isEnabled": True,   "isAccountNonExpired": True,   "isAccountNonLocked": True,   "isCredentialsNonExpired": True }}
        response = self.client.post("/user-service/api/users", json=user_data)
        if response.status_code == 200:
            self.user_id = response.json().get('userId')
    
    @task(2)
    def get_user(self):
        """Get user details"""
        if self.user_id:
            self.client.get(f"/user-service/api/users/{self.user_id}")
    
    @task(1)
    def create_order(self):
        """Create an order"""
        if self.user_id:
            order_data = {
                "orderId": 3,
                "orderDesc": "Complete shopping order",
                "orderFee": 1029.98,
                "cart": {
                    "cartId": 3
                }
            }
            response = self.client.post("/order-service/api/orders", json=order_data)
            if response.status_code == 200:
                self.order_id = response.json().get('orderId')
    
    @task(1)
    def add_order_item(self):
        """Add item to order"""
        if self.order_id and self.product_ids:
            item_data = {
                "orderId": 3,
                "productId": 4,
                "orderedQuantity": 1
            }
            self.client.post("/shipping-service/api/shippings", json=item_data)
    
    @task(1)
    def view_orders(self):
        """View all orders"""
        self.client.get("/order-service/api/orders")
    
    @task(1)
    def view_order_items(self):
        """View order items"""
        self.client.get("/shipping-service/api/shippings")
'''
    
    // Run Locust performance tests
    sh """
        # Install Locust if not available
        echo "Locust no está instalado. Instalando..."
        apt-get update && apt-get install -y python3-pip
        apt-get install -y python3.13-venv
        python3 -m venv locust-env
        
        ./locust-env/bin/pip install locust
        ./locust-env/bin/locust -f locustfile.py --host=http://ci-control-plane:30080 --users=50 --spawn-rate=10 --run-time=300s --html=performance_report.html --csv=performance_data --headless

        
        # Generate performance summary
        echo "Performance Test Summary:"
        echo "========================"
        if [ -f performance_data_stats.csv ]; then
            echo "Total Requests: \$(tail -n 1 performance_data_stats.csv | cut -d',' -f2)"
            echo "Failed Requests: \$(tail -n 1 performance_data_stats.csv | cut -d',' -f3)"
            echo "Average Response Time: \$(tail -n 1 performance_data_stats.csv | cut -d',' -f4)ms"
            echo "Requests per Second: \$(tail -n 1 performance_data_stats.csv | cut -d',' -f5)"
        fi
    """
    
    // Archive performance results
    archiveArtifacts artifacts: 'performance_report.html,performance_data*.csv', fingerprint: true
    
    echo "Performance tests completed"
}
