package com.selimhorri.app.resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderService;

@WebMvcTest(OrderResource.class)
class OrderResourceTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private OrderService orderService;

        @Autowired
        private ObjectMapper objectMapper;

        private OrderDto testOrderDto;
        private CartDto testCartDto;

        @BeforeEach
        void setUp() {
                testOrderDto = OrderDto.builder()
                                .orderId(1)
                                .orderDate(LocalDateTime.now())
                                .orderDesc("Test Order")
                                .orderFee(100.0)
                                .build();

                testCartDto = CartDto.builder()
                                .cartId(1)
                                .userId(1)
                                .orderDtos(null)
                                .build();

                testOrderDto.setCartDto(testCartDto);
        }

        @Test
        void testFindAll_ShouldReturnAllOrders() throws Exception {
                // Given
                List<OrderDto> orders = Arrays.asList(testOrderDto);
                DtoCollectionResponse<OrderDto> response = new DtoCollectionResponse<>(orders);
                when(orderService.findAll()).thenReturn(orders);

                // When & Then
                mockMvc.perform(get("/api/orders"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.collection").isArray())
                                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                                .andExpect(jsonPath("$.collection[0].orderDesc").value("Test Order"));

                verify(orderService).findAll();
        }

        @Test
        void testFindById_ShouldReturnOrder() throws Exception {
                // Given
                Integer orderId = 1;
                when(orderService.findById(orderId)).thenReturn(testOrderDto);

                // When & Then
                mockMvc.perform(get("/api/orders/{orderId}", orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(1))
                                .andExpect(jsonPath("$.orderDesc").value("Test Order"));

                verify(orderService).findById(orderId);
        }

        @Test
        void testSave_ShouldCreateOrder() throws Exception {
                // Given
                when(orderService.save(any(OrderDto.class))).thenReturn(testOrderDto);

                // When & Then
                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testOrderDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(1))
                                .andExpect(jsonPath("$.orderDesc").value("Test Order"));

                verify(orderService).save(any(OrderDto.class));
        }

        @Test
        void testUpdate_ShouldUpdateOrder() throws Exception {
                // Given
                when(orderService.update(any(OrderDto.class))).thenReturn(testOrderDto);

                // When & Then
                mockMvc.perform(put("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testOrderDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(1))
                                .andExpect(jsonPath("$.orderDesc").value("Test Order"));

                verify(orderService).update(any(OrderDto.class));
        }

        @Test
        void testUpdateById_ShouldUpdateOrder() throws Exception {
                // Given
                Integer orderId = 1;
                when(orderService.update(eq(orderId), any(OrderDto.class))).thenReturn(testOrderDto);

                // When & Then
                mockMvc.perform(put("/api/orders/{orderId}", orderId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(testOrderDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.orderId").value(1))
                                .andExpect(jsonPath("$.orderDesc").value("Test Order"));

                verify(orderService).update(eq(orderId), any(OrderDto.class));
        }

        @Test
        void testDeleteById_ShouldDeleteOrder() throws Exception {
                // Given
                Integer orderId = 1;
                doNothing().when(orderService).deleteById(orderId);

                // When & Then
                mockMvc.perform(delete("/api/orders/{orderId}", orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(true));

                verify(orderService).deleteById(orderId);
        }
}
