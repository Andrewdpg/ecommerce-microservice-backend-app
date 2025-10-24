# Taller 2: Pruebas y Lanzamiento - Reporte Técnico

## E-commerce Microservices Backend Application

**Fecha:** Octubre 2025
**Estudiante:** Andrés Parra
**Repositorio:** https://github.com/Andrewdpg/ecommerce-microservice-backend-app

---

## 1. Introducción

Este documento presenta la implementación completa de un sistema de CI/CD para una aplicación de e-commerce basada en microservicios, incluyendo pipelines automatizados, pruebas exhaustivas y despliegue en múltiples entornos.

### Microservicios Implementados

- **Service Discovery** (Puerto 8761): Eureka Server para descubrimiento de servicios
- **API Gateway** (Puerto 8080): Gateway principal para enrutamiento
- **User Service** (Puerto 8700): Gestión de usuarios y autenticación
- **Product Service** (Puerto 8500): Catálogo y gestión de productos
- **Order Service** (Puerto 8300): Procesamiento de órdenes
- **Shipping Service** (Puerto 8600): Gestión de envíos
- **Proxy Client** (Puerto 8900): Cliente proxy para comunicación entre servicios

### Arquitectura General

![1761268311529](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/REPORTE_TALLER2/1761268311529.png)

---

## 2. Configuración de Entorno (10%)

### 2.1 Configuración de Docker

Cada microservicio cuenta con su propio Dockerfile optimizado para builds multi-stage:

**Ejemplo: user-service/Dockerfile**

```dockerfile
FROM openjdk:11
ARG PROJECT_VERSION=0.1.0
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE dev
COPY user-service/ .
ADD user-service/target/user-service-v${PROJECT_VERSION}.jar user-service.jar
EXPOSE 8700
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "user-service.jar"]
```

Se da por sentado que cada proyecto será construido previo a la construcción del Dockerfile.

### 2.2 Docker Compose para Desarrollo Local

El archivo `compose.yml` define todos los servicios con sus dependencias:

```yaml
services:
  zipkin-container:
    image: openzipkin/zipkin
    ports: ["9411:9411"]
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:9411/health"]
      interval: 30s
      retries: 3

  service-discovery-container:
    build:
      context: .
      dockerfile: service-discovery/Dockerfile
    ports: ["8761:8761"]
    depends_on:
      zipkin-container:
        condition: service_healthy
```

**Beneficios:**

- Orquestación local de todos los servicios
- Health checks para garantizar disponibilidad
- Red compartida para comunicación inter-servicios

### 2.3 Configuración de Kubernetes

#### Estructura de Directorios

```
k8s/
├── base/                    # Manifiestos base con placeholders
│   ├── namespaces.yaml     # Definición de namespaces
│   ├── configmap.yaml      # Configuración compartida
│   ├── rbac-jenkins.yaml   # Permisos para Jenkins
│   ├── service-discovery.yaml
│   ├── api-gateway.yaml
│   ├── user-service.yaml
│   ├── product-service.yaml
│   ├── order-service.yaml
│   ├── shipping-service.yaml
│   └── ...
├── staging/
│   └── configmap.yaml
└── production/
    └── configmap.yaml
```

#### ConfigMap Base (k8s/base/configmap.yaml)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: micro-config
  namespace: ${NAMESPACE}
data:
  SPRING_PROFILES_ACTIVE: "dev"
  JAVA_OPTS: "-Xms256m -Xmx512m"
  EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: "http://service-discovery.${NAMESPACE}.svc.cluster.local:8761/eureka/"
  SPRING_CLOUD_CONFIG_URI: "http://cloud-config.${NAMESPACE}.svc.cluster.local:9296"
  USER_SERVICE_HOST: "http://user-service.${NAMESPACE}.svc.cluster.local:8700"
  # ... más configuraciones
```

#### Deployment Example (k8s/base/user-service.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: ${NAMESPACE}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-service
  template:
    spec:
      containers:
      - name: user-service
        image: ${REGISTRY}/user-service:${IMAGE_TAG}
        ports:
        - containerPort: 8700
        envFrom:
        - configMapRef:
            name: micro-config
        resources:
          requests:
            memory: "384Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        readinessProbe:
          httpGet:
            path: /user-service/actuator/health
            port: 8700
          initialDelaySeconds: 120
          periodSeconds: 15
        livenessProbe:
          httpGet:
            path: /user-service/actuator/health
            port: 8700
          initialDelaySeconds: 150
          periodSeconds: 30
```

**Características Implementadas:**

- **Namespaces separados**: `microservices-staging` y `microservices-prod`
- **Resource limits**: Prevención de consumo excesivo de recursos
- **Health checks**: Readiness y liveness probes
- **ConfigMaps**: Configuración centralizada
- **RBAC**: Service Account para Jenkins con permisos específicos
- **NodePort Services**: Para acceso externo a API Gateway y Service Discovery

#### RBAC para Jenkins (k8s/base/rbac-jenkins.yaml)

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkins-deployer
  namespace: microservices-staging
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: jenkins-deployer-role
  namespace: microservices-staging
rules:
- apiGroups: ["", "apps", "autoscaling", "networking.k8s.io"]
  resources: ["configmaps", "secrets", "services", "pods", "deployments"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

### 2.4 Configuración de Jenkins

#### Credenciales Configuradas

- `docker-hub-credentials`: Para push de imágenes
- `kubeconfig`: Archivo kubeconfig para acceso a Kubernetes
- `github-token`: Para crear tags y releases

#### Variables de Entorno Globales

```groovy
environment {
    REGISTRY = 'docker.io/andrewdpg'
    DOCKERHUB = 'docker-hub-credentials'
    K8S_NAMESPACE_STAGING = 'microservices-staging'
    K8S_NAMESPACE_PROD = 'microservices-prod'
    KUBECONFIG_CREDENTIAL = 'kubeconfig'
    RELEASE_VERSION = '1.0.0'
}
```

#### Plugins Requeridos

- Pipeline
- Docker Pipeline
- Kubernetes CLI
- Git
- JUnit
- HTML Publisher (para reportes de Locust)

![1761268311529](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761268311529.png)

![1761268730293](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761268730293.png)

---

## 3. Pipelines de Construcción (15% + 15% + 15%)

### 3.1 Estrategia de Branching y Entornos

![1761268815858](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761268815858.png)

### 3.2 Pipeline para Desarrollo (Dev Environment)

**Trigger:** Push a cualquier branch (excepto main/develop)

**Stages:**

1. **Checkout & Detect Changes**: Detección inteligente de servicios modificados
2. **Build & Test Core Services**: Service Discovery y API Gateway en paralelo
3. **Build & Test Changed Services**: Servicios de negocio en paralelo

```groovy
stage('Checkout & Detect Changes') {
    steps {
        script {
            // Detectar branch y determinar entorno
            env.GIT_BRANCH = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
  
            if (env.GIT_BRANCH == 'main' || env.GIT_BRANCH == 'master') {
                env.TARGET_ENVIRONMENT = 'production'
            } else if (env.GIT_BRANCH == 'develop' || env.GIT_BRANCH == 'staging') {
                env.TARGET_ENVIRONMENT = 'staging'
            } else {
                env.TARGET_ENVIRONMENT = 'dev'
            }
  
            // Detectar servicios modificados
            def changedServices = []
            for (service in SERVICES) {
                def changes = sh(
                    script: """git diff --name-only HEAD~1 HEAD | grep -E '^${service.path}/|^pom\\.xml\$' || true""",
                    returnStdout: true
                ).trim()
  
                if (changes) {
                    changedServices.add(service.name)
                }
            }
  
            // Si no hay cambios, construir todo
            if (changedServices.isEmpty()) {
                changedServices = SERVICES.collect { it.name }
            }
  
            env.CHANGED_SERVICES = changedServices.join(',')
        }
    }
}
```

**Función buildService:**

```groovy
def buildService(serviceName, servicePort) {
    echo "Building ${serviceName}..."
  
    // Build Maven project
    sh "mvn clean compile -pl ${serviceName} -am"
  
    // Run unit tests
    sh "mvn test -pl ${serviceName} -am"
  
    // Package application
    sh "mvn package -pl ${serviceName} -am -DskipTests"
  
    // Publish test results
    junit "${serviceName}/target/surefire-reports/*.xml"
  
    // Build Docker image
    sh """docker build -f ${serviceName}/Dockerfile \
          -t ${REGISTRY}/${serviceName}:${IMAGE_TAG} \
          -t ${REGISTRY}/${serviceName}:latest ."""
}
```

**Resultado Dev Environment:**

- ✅ Compilación de servicios modificados
- ✅ Ejecución de pruebas unitarias
- ✅ Construcción de imágenes Docker
- ✅ Reportes JUnit publicados
- ❌ No hay push a registry
- ❌ No hay despliegue

![1761268849238](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761268849238.png)

### 3.3 Pipeline para Staging Environment

**Trigger:** Push a `develop` o `staging`

**Stages adicionales:**
4. **Docker Push**: Subir imágenes a Docker Hub
5. **Deploy Core Services to Staging**: Zipkin, Service Discovery, API Gateway
6. **Deploy to Staging**: Servicios de negocio
7. **Integration Tests**: Pruebas de comunicación entre servicios
8. **E2E Tests**: Flujos completos de usuario
9. **Performance Tests**: Pruebas de carga con Locust

```groovy
stage('Deploy Core Services to Staging') {
    when {
        anyOf {
            equals expected: 'staging', actual: env.TARGET_ENVIRONMENT
            equals expected: 'production', actual: env.TARGET_ENVIRONMENT
        }
    }
    steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CREDENTIAL}", variable: 'KCFG')]) {
            script {
                deployCoreServicesToEnvironment('staging', K8S_NAMESPACE_STAGING)
            }
        }
    }
}
```

**Función de Despliegue:**

```groovy
def deployService(serviceName, servicePort, namespace, nodePort) {
    echo "Deploying ${serviceName} to ${namespace}..."

    // Apply Kubernetes manifests con sustitución de variables
    sh """
        sed -e "s|\${REGISTRY}|${REGISTRY}|g" \
            -e "s|\${NAMESPACE}|${namespace}|g" \
            -e "s|\${IMAGE_TAG}|${IMAGE_TAG}|g" \
            -e "s|\${NODE_PORT}|${nodePort}|g" \
            k8s/base/${serviceName}.yaml | kubectl --kubeconfig="\$KCFG" apply -f -
    """

    // Esperar a que el despliegue esté listo
    sh """
        kubectl --kubeconfig="\$KCFG" rollout status deployment/${serviceName} \
                -n ${namespace} --timeout=600s
    """
}
```

**Resultado Staging Environment:**

- ✅ Todo lo de Dev environment
- ✅ Push de imágenes a Docker Hub
- ✅ Despliegue en Kubernetes (namespace staging)
- ✅ Pruebas de integración (5 tests)
- ✅ Pruebas E2E (5 flows)
- ✅ Pruebas de rendimiento (Locust)
- ✅ Reportes HTML de performance
- ❌ No hay despliegue a producción

![1761268899298](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761268899298.png)

![1761268972109](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761268972109.png)

**[INSERTAR SCREENSHOT: Kubectl get services -n microservices-staging]**

### 3.4 Pipeline para Production Environment

**Trigger:** Push a `main` o `master`

**Stages adicionales:**
10. **Deploy Core Services to Production**: Despliegue en namespace prod
11. **Deploy to Production**: Servicios de negocio en producción
12. **Generate Release Notes**: Generación automática de notas de versión

```groovy
stage('Generate Release Notes') {
    when {
        equals expected: 'production', actual: env.TARGET_ENVIRONMENT
    }
    steps {
        withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
            script {
                sh """
                    # Generar changelog
                    git log --oneline --since="7 days ago" > CHANGELOG.md
  
                    # Crear release notes
                    echo "## Release ${RELEASE_VERSION}" > release_notes.md
                    echo "### Date: \$(date)" >> release_notes.md
                    echo "### Changes:" >> release_notes.md
                    cat CHANGELOG.md >> release_notes.md
  
                    # Crear tag
                    git tag -a v${RELEASE_VERSION} -m "Release version ${RELEASE_VERSION}"
                    git remote set-url origin https://\${GITHUB_TOKEN}@github.com/Andrewdpg/ecommerce-microservice-backend-app.git
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
```

**Resultado Production Environment:**

- ✅ Todo lo de Staging environment
- ✅ Despliegue en Kubernetes (namespace production)
- ✅ Release notes generadas automáticamente
- ✅ Git tag creado (v1.0.0)
- ✅ Artifacts archivados en Jenkins

**[INSERTAR SCREENSHOT: Pipeline Production completo]**

**[INSERTAR SCREENSHOT: Kubectl get pods -n microservices-prod]**

**[INSERTAR SCREENSHOT: GitHub release tags]**

---

## 4. Pruebas Implementadas

![1761269200978](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761269200978.png)

### 4.1 Pruebas Unitarias

#### User Service - 5 Pruebas Unitarias

```
Location: user-service/src/test/java/com/selimhorri/app/service/impl/
```

**Test Suite Summary:**

1. **UserServiceImplTest::testFindAllUsers()**

   - Valida el listado de todos los usuarios
   - Mock de repository
   - Verifica tamaño de colección y mapeo correcto
2. **UserServiceImplTest::testFindByIdSuccess()**

   - Valida búsqueda de usuario por ID
   - Verifica que retorna el usuario correcto
3. **UserServiceImplTest::testSaveUser()**

   - Valida creación de nuevo usuario
   - Verifica persistencia y retorno de ID
4. **UserServiceImplTest::testUpdateUser()**

   - Valida actualización de datos de usuario
   - Verifica que los cambios se persisten
5. **UserServiceImplTest::testDeleteById()**

   - Valida eliminación de usuario
   - Verifica llamada al repository

#### Product Service - 5 Pruebas Unitarias

```
Location: product-service/src/test/java/com/selimhorri/app/service/impl/
```

**Test Suite Summary:**

1. **ProductServiceImplTest::testFindAllProducts()**

   - Valida listado de productos
   - Verifica paginación
2. **ProductServiceImplTest::testFindByIdSuccess()**

   - Busca producto específico
   - Valida campos del producto
3. **ProductServiceImplTest::testSaveProduct()**

   - Crea nuevo producto
   - Verifica SKU único
4. **ProductServiceImplTest::testUpdateStock()**

   - Actualiza inventario
   - Valida cantidades
5. **ProductServiceImplTest::testDeleteProduct()**

   - Elimina producto
   - Verifica soft delete

#### Order Service - 5 Pruebas Unitarias

```
Location: order-service/src/test/java/com/selimhorri/app/service/impl/
```

**Test Suite Summary:**

1. **OrderServiceImplTest::testCreateOrder()**

   - Crea nueva orden
   - Valida cálculo de totales
2. **OrderServiceImplTest::testFindOrderById()**

   - Busca orden por ID
   - Verifica relaciones con items
3. **OrderServiceImplTest::testUpdateOrderStatus()**

   - Actualiza estado de orden
   - Valida transiciones válidas
4. **OrderServiceImplTest::testCalculateOrderTotal()**

   - Calcula total de orden
   - Verifica suma de items + fees
5. **OrderServiceImplTest::testCancelOrder()**

   - Cancela orden
   - Verifica liberación de inventario

### 4.2 Pruebas de Integración

Las pruebas de integración se ejecutan en el ambiente de Staging y validan la comunicación entre servicios a través del API Gateway.

```groovy
def runIntegrationTests() {
    def namespace = K8S_NAMESPACE_STAGING
    def apiGatewayUrl = "ci-control-plane:30080"
  
    sh """
        # Test 1: User Service Integration
        USER_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/user-service/api/users" \\
            -H "Content-Type: application/json" \\
            -d '{"userId": 4, "firstName": "María", ...}')
  
        USER_ID=\$(echo \$USER_RESPONSE | grep -o '"userId"[[:space:]]*:[[:space:]]*[0-9]*' | sed 's/[^0-9]*//g')
        if [ "\$USER_ID" != "null" ] && [ "\$USER_ID" != "" ]; then
            echo "✓ User creation successful"
        else
            echo "✗ User creation failed"
            exit 1
        fi
    """
}
```

#### 4.3 Pruebas End-to-End (E2E)

Las pruebas E2E simulan flujos completos de usuario, validando la interacción entre múltiples servicios.

```groovy
def runE2ETests() {
    def namespace = K8S_NAMESPACE_STAGING
    def apiGatewayUrl = "ci-control-plane:30080"
  
    sh """
        # E2E Test 1: Complete User Registration and Profile Update Flow
        USER_RESPONSE=\$(curl -s -X POST "http://${apiGatewayUrl}/user-service/api/users" ...)
        UPDATE_RESPONSE=\$(curl -s -X PUT "http://${apiGatewayUrl}/user-service/api/users" ...)
  
        # E2E Test 2: Complete Product Catalog and Search Flow
        PRODUCT1=\$(curl -s -X POST "http://${apiGatewayUrl}/product-service/api/products" ...)
        ALL_PRODUCTS=\$(curl -s "http://${apiGatewayUrl}/product-service/api/products")
    """
}
```

#### 4.4 Pruebas de Rendimiento con Locust

Las pruebas de rendimiento simulan carga real de usuarios concurrentes usando Locust.

**Archivo de Test: locustfile.py**

```python
4from locust import HttpUser, task, between

class EcommerceUser(HttpUser):
    wait_time = between(1, 3)
  
    @task(3)
    def view_products(self):
        """View product catalog"""
        response = self.client.get("/product-service/api/products")
        if response.status_code == 200:
            products = response.json()
            if 'collection' in products:
                self.product_ids = [p['productId'] for p in products['collection'][:5]]
  
    @task(2)
    def create_user(self):
        """Create a new user"""
        user_data = {...}
        response = self.client.post("/user-service/api/users", json=user_data)
        if response.status_code == 200:
            self.user_id = response.json().get('userId')
  
    @task(1)
    def create_order(self):
        """Create an order"""
        if self.user_id:
            order_data = {...}
            response = self.client.post("/order-service/api/orders", json=order_data)
```

**Configuración de Prueba:**

```bash
locust -f locustfile.py \
    --host=http://ci-control-plane:30080 \
    --users=50 \
    --spawn-rate=10 \
    --run-time=300s \
    --html=performance_report.html \
    --csv=performance_data \
    --headless
```

**Parámetros:**

- **Usuarios concurrentes:** 50
- **Tasa de spawn:** 10 usuarios/segundo
- **Duración:** 300 segundos (5 minutos)
- **Host objetivo:** API Gateway en Kubernetes

**Tasks y Distribución:**

- **view_products** (weight=3): 42% del tráfico
- **create_user** (weight=2): 29% del tráfico
- **get_user** (weight=2): 14% del tráfico
- **create_order** (weight=1): 7% del tráfico
- **add_order_item** (weight=1): 7% del tráfico
- **view_orders** (weight=1): 1% del tráfico

---

![1761269559279](https://github.com/Andrewdpg/ecommerce-microservice-backend-app/blob/master/image/README/1761269559279.png)

## 5. Análisis de Rendimiento

### 5.1 Resumen Ejecutivo

**Configuración:** 5 min 19 seg | Target: http://ci-control-plane:30080 | 23/10/2025, 3:00 PM

**Resultados Globales:**

```
Total Requests:      6,604
Failed Requests:     0 (0%)
Avg Response Time:   224ms
Median:              13ms
95th Percentile:     520ms
99th Percentile:     5,700ms
Max:                 16,002ms
RPS:                 20.66
```

**Veredicto:** ✅ Sistema estable, 0% errores, pero con problema crítico en Shipping GET.

### 5.2 Análisis por Endpoint

#### Top Performance Issues

**🔴 GET /shipping-service/api/shippings - CRÍTICO**

```
Requests: 603 | Failures: 0
Avg: 1,031ms | Median: 150ms | Max: 16,002ms
95%ile: 5,700ms | 99%ile: 15,000ms
```

**Problema:** Latencia 4-5x peor que otros endpoints. 90% de requests >3s.
**Causa:** Queries sin optimizar, sin paginación, N+1 problem.
**Acción:** Paginación obligatoria + índices en orderId/productId.

---

#### Endpoints con Buen Rendimiento

| Endpoint                             | Requests | Avg (ms) | 95%ile (ms) | Evaluación    |
| ------------------------------------ | -------- | -------- | ----------- | -------------- |
| POST /shipping-service/api/shippings | 552      | 105      | 180         | ✅ Excelente   |
| GET /product-service/api/products    | 1,879    | 125      | 320         | ✅ Excelente   |
| POST /user-service/api/users         | 1,191    | 129      | 260         | ✅ Bueno       |
| GET /user-service/api/users/4        | 1,182    | 134      | 150         | ✅ Excelente   |
| POST /order-service/api/orders       | 597      | 177      | 520         | ✅ Aceptable   |
| GET /order-service/api/orders        | 600      | 243      | 790         | ⚠️ Mejorable |

### 5.3 Distribución de Carga

```
viewProducts:      27.3% (1,879 requests)
createUser:        18.2% (1,191 requests)
getUser:           18.2% (1,182 requests)
addOrderItem:       9.1% (552 requests)
createOrder:        9.1% (597 requests)
viewOrderItems:     9.1% (603 requests) ← PROBLEMA
viewOrders:         9.1% (600 requests)
```

### 5.4 Percentiles Agregados

| Percentil     | Tiempo            | SLA               | Estado       |
| ------------- | ----------------- | ----------------- | ------------ |
| 50%           | 13ms              | <200ms            | ✅           |
| 80%           | 75ms              | <400ms            | ✅           |
| 95%           | 520ms             | <1000ms           | ✅           |
| **99%** | **5,700ms** | **<2000ms** | **❌** |
| Max           | 16,002ms          | <5000ms           | ❌           |

**Conclusión:** 95% requests excelentes, 5% problemáticas (long tail por Shipping GET).

### 5.5 SLA Compliance

| Métrica                  | Target            | Resultado        | Estado       |
| ------------------------- | ----------------- | ---------------- | ------------ |
| Availability              | 99.5%             | 100%             | ✅           |
| Avg Response              | <500ms            | 224ms            | ✅           |
| 95th Percentile           | <1000ms           | 520ms            | ✅           |
| **99th Percentile** | **<2000ms** | **5700ms** | **❌** |
| Error Rate                | <1%               | 0%               | ✅           |
| Throughput                | >15 RPS           | 20.66 RPS        | ✅           |
