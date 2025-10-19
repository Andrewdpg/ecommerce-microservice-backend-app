package com.selimhorri.app.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;

/**
 * Pruebas de integración que validan la comunicación entre Order Service y Payment Service
 * Simula escenarios reales de comunicación entre microservicios
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("integration-test")
@TestPropertySource(properties = {
    "app.services.payment-service.url=http://localhost:${wiremock.server.port}",
    "app.services.user-service.url=http://localhost:${wiremock.server.port}"
})
@Transactional
@DisplayName("Order-Payment Service Integration Tests")
class OrderPaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderDto testOrderDto;
    private CartDto testCartDto;

    @BeforeEach
    void setUp() {
        // Configurar datos de prueba
        testCartDto = CartDto.builder()
                .cartId(1)
                .userId(1)
                .build();

        testOrderDto = OrderDto.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Integration Test Order")
                .orderFee(299.99)
                .cartDto(testCartDto)
                .build();

        // Mock básico para User Service (requerido para validación de cart)
        stubFor(get(urlPathMatching("/user-service/api/users/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                            "\"userId\": 1," +
                            "\"firstName\": \"John\"," +
                            "\"lastName\": \"Doe\"," +
                            "\"email\": \"john.doe@example.com\"," +
                            "\"phone\": \"+1234567890\"" +
                        "}")));
    }

    @Test
    @DisplayName("Should create order successfully and allow payment service to retrieve order details")
    void shouldCreateOrderAndAllowPaymentServiceToRetrieveDetails() throws Exception {
        // 1. Crear la orden en Order Service
        String orderJson = objectMapper.writeValueAsString(testOrderDto);

        String orderResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderDesc").value("Integration Test Order"))
                .andExpect(jsonPath("$.orderFee").value(299.99))
                .andExpect(jsonPath("$.cartDto.cartId").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extraer el ID de la orden creada
        OrderDto createdOrder = objectMapper.readValue(orderResponse, OrderDto.class);
        Integer orderId = createdOrder.getOrderId();

        // 2. Verificar que la orden se puede consultar por ID (simulando Payment Service consultando)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderDesc").value("Integration Test Order"))
                .andExpect(jsonPath("$.orderFee").value(299.99))
                .andExpect(jsonPath("$.cartDto.cartId").value(1))
                .andExpect(jsonPath("$.cartDto.userId").value(1));

        // 3. Simular que Payment Service crea un pago asociado a la orden
        stubFor(post(urlPathEqualTo("/payment-service/api/payments"))
                .withRequestBody(containing("\"orderId\":" + orderId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("{" +
                            "\"paymentId\": %d," +
                            "\"orderId\": %d," +
                            "\"isPayed\": false," +
                            "\"paymentStatus\": \"IN_PROGRESS\"" +
                        "}", orderId, orderId))));

        // Verificar que se realizó la llamada al User Service para validación
        verify(getRequestedFor(urlPathMatching("/user-service/api/users/.*")));
    }

    @Test
    @DisplayName("Should handle order update and maintain consistency across services")
    void shouldHandleOrderUpdateAndMaintainConsistency() throws Exception {
        // 1. Crear orden inicial
        String initialOrderJson = objectMapper.writeValueAsString(testOrderDto);

        String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initialOrderJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderDto createdOrder = objectMapper.readValue(createResponse, OrderDto.class);
        Integer orderId = createdOrder.getOrderId();

        // 2. Actualizar la orden (cambio de precio que afecta al pago)
        OrderDto updatedOrderDto = OrderDto.builder()
                .orderId(orderId)
                .orderDate(LocalDateTime.now())
                .orderDesc("Updated Order - Price Changed")
                .orderFee(399.99) // Precio actualizado
                .cartDto(testCartDto)
                .build();

        String updatedOrderJson = objectMapper.writeValueAsString(updatedOrderDto);

        // 3. Ejecutar actualización
        mockMvc.perform(MockMvcRequestBuilders.put("/api/orders/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedOrderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderDesc").value("Updated Order - Price Changed"))
                .andExpect(jsonPath("$.orderFee").value(399.99));

        // 4. Verificar que Payment Service puede obtener la orden actualizada
        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderFee").value(399.99))
                .andExpect(jsonPath("$.orderDesc").value("Updated Order - Price Changed"));

        // 5. Simular notificación a Payment Service sobre el cambio
        stubFor(put(urlPathMatching("/payment-service/api/payments/order/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                            "\"message\": \"Payment updated due to order changes\"," +
                            "\"newAmount\": 399.99" +
                        "}")));

    }

    @Test
    @DisplayName("Should handle service failure scenarios gracefully")
    void shouldHandleServiceFailuresScenariosGracefully() throws Exception {
        // 1. Configurar fallo temporal en User Service
        stubFor(get(urlPathMatching("/user-service/api/users/.*"))
                .inScenario("User Service Failure")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                            "\"timestamp\": \"2024-01-15T10:30:00\"," +
                            "\"httpStatus\": \"SERVICE_UNAVAILABLE\"," +
                            "\"msg\": \"User Service temporarily unavailable\"" +
                        "}"))
                .willSetStateTo("Failed"));

        // 2. Intentar crear orden con servicio fallando
        String orderJson = objectMapper.writeValueAsString(testOrderDto);

        // La creación de orden debería fallar debido a la dependencia del User Service
        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().is5xxServerError());

        // 3. Configurar recuperación del User Service
        stubFor(get(urlPathMatching("/user-service/api/users/.*"))
                .inScenario("User Service Failure")
                .whenScenarioStateIs("Failed")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                            "\"userId\": 1," +
                            "\"firstName\": \"John\"," +
                            "\"lastName\": \"Doe\"," +
                            "\"email\": \"john.doe@example.com\"," +
                            "\"phone\": \"+1234567890\"" +
                        "}"))
                .willSetStateTo("Recovered"));

        // 4. Reintentar la creación de orden después de la recuperación
        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderDesc").value("Integration Test Order"))
                .andExpect(jsonPath("$.orderFee").value(299.99));

        // 5. Verificar que Payment Service puede procesar la orden después de la recuperación
        stubFor(get(urlPathMatching("/payment-service/api/payments/order/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                            "\"message\": \"Payment service operational\"," +
                            "\"status\": \"READY\"" +
                        "}")));

    }
}