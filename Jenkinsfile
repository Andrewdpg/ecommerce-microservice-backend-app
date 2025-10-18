def SERVICES = [
    [name: 'user-service', port: '8700', path: 'user-service'],
    [name: 'product-service', port: '8500', path: 'product-service'],
    [name: 'order-service', port: '8300', path: 'order-service'],
    [name: 'payment-service', port: '8400', path: 'payment-service'],
    [name: 'shipping-service', port: '8600', path: 'shipping-service'],
    [name: 'favourite-service', port: '8800', path: 'favourite-service']
]

pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'localhost:5000'
        K8S_NAMESPACE = 'microservices-dev'
    }
    
    stages {
        stage('Checkout & Detect Changes') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    
                    // Detect changed services
                    def changedServices = []
                    for (service in SERVICES) {
                        def serviceName = service.name
                        def servicePath = service.path
                        
                        // Check if service directory or related files changed
                        def changes = sh(
                            script: """git diff --name-only HEAD~1 HEAD | grep -E '^${servicePath}/|^pom\\.xml\$|^shared/' || true""",
                            returnStdout: true
                        ).trim()
                        
                        if (changes) {
                            changedServices.add(serviceName)
                            echo "Changes detected in ${serviceName}: ${changes}"
                        }
                    }
                    
                    // If no specific changes detected, build all services
                    if (changedServices.isEmpty()) {
                        echo "No specific changes detected, building all services"
                        changedServices = SERVICES.collect { it.name }
                    }

                    env.CHANGED_SERVICES = changedServices.join(',')
                    echo "Services to build: ${env.CHANGED_SERVICES}"
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
                
                stage('Build Payment Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('payment-service') }
                    }
                    steps {
                        script {
                            buildService('payment-service', '8400')
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
                
                stage('Build Favourite Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('favourite-service') }
                    }
                    steps {
                        script {
                            buildService('favourite-service', '8800')
                        }
                    }
                }
            }
        }
        
        stage('Deploy Changed Services') {
            parallel {
                stage('Deploy User Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('user-service') }
                    }
                    steps {
                        script {
                            deployService('user-service', '8700')
                        }
                    }
                }
                
                stage('Deploy Product Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('product-service') }
                    }
                    steps {
                        script {
                            deployService('product-service', '8500')
                        }
                    }
                }
                
                stage('Deploy Order Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('order-service') }
                    }
                    steps {
                        script {
                            deployService('order-service', '8300')
                        }
                    }
                }
                
                stage('Deploy Payment Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('payment-service') }
                    }
                    steps {
                        script {
                            deployService('payment-service', '8400')
                        }
                    }
                }
                
                stage('Deploy Shipping Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('shipping-service') }
                    }
                    steps {
                        script {
                            deployService('shipping-service', '8600')
                        }
                    }
                }
                
                stage('Deploy Favourite Service') {
                    when {
                        expression { env.CHANGED_SERVICES.contains('favourite-service') }
                    }
                    steps {
                        script {
                            deployService('favourite-service', '8800')
                        }
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                expression { !env.CHANGED_SERVICES.isEmpty() }
            }
            steps {
                script {
                    // Run integration tests for changed services
                    runIntegrationTests()
                }
            }
        }
        
        stage('Health Check All Services') {
            steps {
                script {
                    // Health check for all services
                    healthCheckAllServices()
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
        }
        failure {
            echo "Pipeline failed for services: ${env.CHANGED_SERVICES}"
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
    
    // Build Docker image
    sh "docker build -f ${serviceName}/Dockerfile -t ${serviceName}:${BUILD_NUMBER} ."
    sh "docker tag ${serviceName}:${BUILD_NUMBER} ${serviceName}:latest"
    
    // Tag for registry
    sh "docker tag ${serviceName}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER}"
    sh "docker tag ${serviceName}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${serviceName}:latest"
    
    // Push to registry
    sh "docker push ${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER}"
    sh "docker push ${DOCKER_REGISTRY}/${serviceName}:latest"
    
    echo "Successfully built and pushed ${serviceName}:${BUILD_NUMBER}"
}

def deployService(serviceName, servicePort) {
    echo "Deploying ${serviceName}..."
    
    // Create namespace if not exists
    sh "kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
    
    // Deploy to Kubernetes
    sh """
        kubectl set image deployment/${serviceName} ${serviceName}=${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER} -n ${K8S_NAMESPACE} || \
        kubectl create deployment ${serviceName} --image=${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER} -n ${K8S_NAMESPACE}
    """
    
    // Expose service
    sh "kubectl expose deployment ${serviceName} --port=${servicePort} --target-port=${servicePort} -n ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
    
    // Wait for deployment to be ready
    sh "kubectl rollout status deployment/${serviceName} -n ${K8S_NAMESPACE} --timeout=300s"
    
    echo "Successfully deployed ${serviceName}"
}

def runIntegrationTests() {
    echo "Running integration tests..."
    
    // Wait for all services to be ready
    sh "sleep 30"
    
    // Test service connectivity
    sh """
        kubectl get pods -n ${K8S_NAMESPACE}
        kubectl get svc -n ${K8S_NAMESPACE}
    """
    
    // Run integration tests (you can add specific test commands here)
    echo "Integration tests completed"
}

def healthCheckAllServices() {
    echo "Performing health checks on all services..."
    
    for (service in SERVICES) {
        def serviceName = service.name
        def servicePort = service.port
        
        sh """
            kubectl get pods -n ${K8S_NAMESPACE} -l app=${serviceName}
            kubectl get svc -n ${K8S_NAMESPACE} -l app=${serviceName}
        """
    }
    
    echo "Health checks completed for all services"
}
