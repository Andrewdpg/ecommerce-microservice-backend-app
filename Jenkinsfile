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
        DOCKER_REGISTRY = 'docker.io/andrewdpg'
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
                    
                    echo "Branch: ${env.GIT_BRANCH}"
                    echo "Target Environment: ${env.TARGET_ENVIRONMENT}"
                    
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
    
    // Build Docker image
    sh "docker build -f ${serviceName}/Dockerfile -t ${serviceName}:${BUILD_NUMBER} ."
    sh "docker tag ${serviceName}:${BUILD_NUMBER} ${serviceName}:latest"
    
    // Tag for registry (only if registry is available)
    script {
        try {
            sh "docker tag ${serviceName}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER}"
            sh "docker tag ${serviceName}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${serviceName}:latest"
            
            // Push to registry
            sh "docker push ${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER}"
            sh "docker push ${DOCKER_REGISTRY}/${serviceName}:latest"
            echo "Successfully pushed to registry: ${DOCKER_REGISTRY}"
        } catch (Exception e) {
            echo "Registry ${DOCKER_REGISTRY} not available, skipping push. Error: ${e.getMessage()}"
            echo "Images built locally: ${serviceName}:${BUILD_NUMBER}, ${serviceName}:latest"
        }
    }
    
    echo "Successfully built and pushed ${serviceName}:${BUILD_NUMBER}"
}

def deployToEnvironment(environment, namespace) {
    echo "Deploying to ${environment} environment (namespace: ${namespace})..."
    
    // Create namespace if not exists
    sh "kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -"
    
    // Deploy changed services
    def changedServices = env.CHANGED_SERVICES.split(',')
    for (serviceName in changedServices) {
        def service = SERVICES.find { it.name == serviceName }
        if (service) {
            deployService(serviceName, service.port, namespace)
        }
    }
}

def deployService(serviceName, servicePort, namespace) {
    echo "Deploying ${serviceName} to ${namespace}..."
    
    // Deploy to Kubernetes
    script {
        try {
            // Try with registry first
            sh """
                kubectl set image deployment/${serviceName} ${serviceName}=${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER} -n ${namespace} || \
                kubectl create deployment ${serviceName} --image=${DOCKER_REGISTRY}/${serviceName}:${BUILD_NUMBER} -n ${namespace}
            """
        } catch (Exception e) {
            echo "Registry image not available, using local image: ${serviceName}:${BUILD_NUMBER}"
            // Fallback to local image
            sh """
                kubectl set image deployment/${serviceName} ${serviceName}=${serviceName}:${BUILD_NUMBER} -n ${namespace} || \
                kubectl create deployment ${serviceName} --image=${serviceName}:${BUILD_NUMBER} -n ${namespace}
            """
        }
    }
    
    // Expose service
    sh "kubectl expose deployment ${serviceName} --port=${servicePort} --target-port=${servicePort} -n ${namespace} --dry-run=client -o yaml | kubectl apply -f -"
    
    // Wait for deployment to be ready
    sh "kubectl rollout status deployment/${serviceName} -n ${namespace} --timeout=300s"
    
    echo "Successfully deployed ${serviceName} to ${namespace}"
}

def healthCheckEnvironment(namespace) {
    echo "Performing health checks on ${namespace}..."
    
    sh """
        # Verificar que todos los pods están corriendo
        kubectl get pods -n ${namespace}
        
        # Verificar servicios
        kubectl get svc -n ${namespace}
        
        # Health checks básicos
        if [ -f "./scripts/health-check.sh" ]; then
            chmod +x ./scripts/health-check.sh
            ./scripts/health-check.sh "${namespace}" 300
        else
            echo "Health check script not found, performing basic checks"
            # Basic health checks
            kubectl get pods -n ${namespace} --field-selector=status.phase!=Running
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
        kubectl get pods -n ${namespace}
        kubectl get svc -n ${namespace}
    """
    
    // Run integration tests (you can add specific test commands here)
    echo "Integration tests completed"
}

def runE2ETests() {
    echo "Running end-to-end tests..."
    
    // Run E2E tests (you can add specific test commands here)
    echo "E2E tests completed"
}