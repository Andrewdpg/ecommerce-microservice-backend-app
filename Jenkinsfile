def SERVICES = [
    [name: 'user-service', port: '8700', path: 'user-service'],
    [name: 'product-service', port: '8500', path: 'product-service'],
    [name: 'order-service', port: '8300', path: 'order-service'],
    [name: 'payment-service', port: '8400', path: 'payment-service'],
    [name: 'shipping-service', port: '8600', path: 'shipping-service'],
    [name: 'favourite-service', port: '8800', path: 'favourite-service']
]

def CORE_SERVICES = [
    [name: 'zipkin', port: '9411', path: 'zipkin'],
    [name: 'service-discovery', port: '8761', path: 'service-discovery'],
    [name: 'cloud-config', port: '9296', path: 'cloud-config'],
    [name: 'api-gateway', port: '8080', path: 'api-gateway']
]

pipeline {
    agent any
    
    environment {
        REGISTRY = 'docker.io/andrewdpg'
        DOCKERHUB = 'docker-hub-credentials'
        K8S_NAMESPACE_STAGING = 'microservices-staging'
        K8S_NAMESPACE_PROD = 'microservices-prod'
        KUBECONFIG_CREDENTIAL = 'kubeconfig'
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
                    def changedServices = ["user-service"]
                    
                    // If no specific changes detected, build all services
                    if (changedServices.isEmpty()) {
                        echo "No specific changes detected, building all services"
                        changedServices = SERVICES.collect { it.name }
                    }

                    env.CHANGED_SERVICES = changedServices.join(',')
                    echo "Services to build: ${env.CHANGED_SERVICES}"
                }
                stash name: 'workspace', includes: '**/*'
            }
        }

        stage('ttttt') {
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
        
        stage('Build & Test Core Services') {
            parallel {
                stage('Build Service Discovery') {
                    steps {
                        script {
                            buildService('service-discovery', '8761')
                        }
                    }
                }
                
                stage('Build Cloud Config') {
                    steps {
                        script {
                            buildService('cloud-config', '9296')
                        }
                    }
                }
                
                stage('Build API Gateway') {
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
                        def changedServices = env.CHANGED_SERVICES.split(',')
                        for (serviceName in changedServices) {
                            sh """
                                docker push ${REGISTRY}/${serviceName}:${IMAGE_TAG}
                                docker push ${REGISTRY}/${serviceName}:${LATEST_TAG}
                            """
                        }
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
        
        stage('Deploy Core Services to Staging') {
            when {
                equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
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
                equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
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
        
        stage('Health Check Staging') {
            when {
                equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
            }
            steps {
                unstash 'workspace'
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        echo "Performing health checks on staging..."
                        healthCheckEnvironment(K8S_NAMESPACE_STAGING)
                    }
                }
            }
        }
        

        
        stage('Health Check Production') {
            when {
                equals expected: 'production', actual: env.TARGET_ENVIRONMENT
            }
            steps {
                unstash 'workspace'
                withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
                    script {
                        echo "Performing health checks on production..."
                        healthCheckEnvironment(K8S_NAMESPACE_PROD)
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
                script {
                    runIntegrationTests()
                }
            }
        }
        
        stage('E2E Tests') {
            when {
                equals expected: 'production', actual: env.TARGET_ENVIRONMENT
            }
            steps {
                script {
                    runE2ETests()
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
                    echo "Staging deployment ready for testing"
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
    
    // Deploy core services in order with waits
    deployCoreService('zipkin', '9411', namespace)
    sh "sleep 30" // Wait for zipkin to be ready
    
    deployCoreService('service-discovery', '8761', namespace)
    sh "sleep 60" // Wait for eureka to be ready
    
    deployCoreService('cloud-config', '9296', namespace)
    sh "sleep 30" // Wait for cloud-config to be ready
    
    deployCoreService('api-gateway', '8080', namespace)
    sh "sleep 30" // Wait for api-gateway to be ready
}

def deployToEnvironment(environment, namespace) {
    echo "Deploying to ${environment} environment (namespace: ${namespace})..."
    
    // Define services locally
    def services = [
        [name: 'user-service', port: '8700'],
        [name: 'product-service', port: '8500'],
        [name: 'order-service', port: '8300'],
        [name: 'payment-service', port: '8400'],
        [name: 'shipping-service', port: '8600'],
        [name: 'favourite-service', port: '8800']
    ]
    
    // Deploy changed services
    def changedServices = env.CHANGED_SERVICES.split(',')
    for (serviceName in changedServices) {
        def service = services.find { it.name == serviceName }
        if (service) {
            deployService(serviceName, service.port, namespace)
        }
    }
}

def deployCoreService(serviceName, servicePort, namespace) {
    echo "Deploying core service ${serviceName} to ${namespace}..."
    
    // Apply Kubernetes manifests for core services
    sh """
        # Apply the manifest with environment variable substitution
        envsubst < k8s/base/${serviceName}.yaml | kubectl --kubeconfig="\$KCFG" apply -f -
    """
    
    echo "Successfully deployed core service ${serviceName} to ${namespace}"
}

def deployService(serviceName, servicePort, namespace) {
    echo "Deploying ${serviceName} to ${namespace}..."
    
    // Deploy to Kubernetes using registry image
    sh """
        kubectl --kubeconfig="\$KCFG" set image deployment/${serviceName} ${serviceName}=${REGISTRY}/${serviceName}:${IMAGE_TAG} -n ${namespace} || \
        kubectl --kubeconfig="\$KCFG" create deployment ${serviceName} --image=${REGISTRY}/${serviceName}:${IMAGE_TAG} -n ${namespace}
    """
    
    // Expose service
    sh "kubectl --kubeconfig=\"\$KCFG\" expose deployment ${serviceName} --port=${servicePort} --target-port=${servicePort} -n ${namespace} --dry-run=client -o yaml | kubectl --kubeconfig=\"\$KCFG\" apply -f -"
    
    echo "Successfully deployed ${serviceName} to ${namespace}"
}

def healthCheckEnvironment(namespace) {
    echo "Performing health checks on ${namespace}..."
    
    sh """
        # Verificar que todos los pods están corriendo
        kubectl --kubeconfig="\$KCFG" get pods -n ${namespace}
        
        # Verificar servicios
        kubectl --kubeconfig="\$KCFG" get svc -n ${namespace}
        
        # Health checks básicos
        if [ -f "./scripts/health-check.sh" ]; then
            chmod +x ./scripts/health-check.sh
            ./scripts/health-check.sh "\$KCFG" "${namespace}" 300
        else
            echo "Health check script not found, performing basic checks"
            # Basic health checks
            kubectl --kubeconfig="\$KCFG" get pods -n ${namespace} --field-selector=status.phase!=Running
        fi
    """
}

def runIntegrationTests() {
    echo "Running integration tests..."
    
    // Wait for all services to be ready
    sh "sleep 30"
    
    // Test service connectivity based on environment
    def namespace = env.TARGET_ENVIRONMENT == 'staging' ? K8S_NAMESPACE_STAGING : K8S_NAMESPACE_PROD
    sh """
        kubectl --kubeconfig="\$KCFG" get pods -n ${namespace}
        kubectl --kubeconfig="\$KCFG" get svc -n ${namespace}
    """
    
    // Run integration tests (you can add specific test commands here)
    echo "Integration tests completed"
}

def runE2ETests() {
    echo "Running end-to-end tests..."
    
    // Run E2E tests (you can add specific test commands here)
    echo "E2E tests completed"
}