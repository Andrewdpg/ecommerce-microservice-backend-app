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
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderItemService;

@WebMvcTest(OrderItemResource.class)
class OrderItemResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderItemService orderItemService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderItemDto testOrderItemDto;
    private ProductDto testProductDto;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testProductDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .imageUrl("http://example.com/image.jpg")
                .sku("SKU123")
                .priceUnit(99.99)
                .quantity(10)
                .build();

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(199.98)
                .build();

        testOrderItemDto = OrderItemDto.builder()
                .productId(1)
                .orderId(1)
                .orderedQuantity(2)
                .productDto(testProductDto)
                .orderDto(testOrderDto)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllOrderItems() throws Exception {
        // Given
        List<OrderItemDto> orderItems = Arrays.asList(testOrderItemDto);
        DtoCollectionResponse<OrderItemDto> response = new DtoCollectionResponse<>(orderItems);
        when(orderItemService.findAll()).thenReturn(orderItems);

        // When & Then
        mockMvc.perform(get("/api/shippings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].orderedQuantity").value(2))
                .andExpect(jsonPath("$.collection[0].productId").value(1));

        verify(orderItemService).findAll();
    }

    @Test
    void testFindById_ShouldReturnOrderItem() throws Exception {
        // Given
        when(orderItemService.findById(any(OrderItemId.class))).thenReturn(testOrderItemDto);

        // When & Then
        mockMvc.perform(get("/api/shippings/{orderId}/{productId}", "1", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(2))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.orderId").value(1));

        verify(orderItemService).findById(any(OrderItemId.class));
    }

    @Test
    void testSave_ShouldCreateOrderItem() throws Exception {
        // Given
        when(orderItemService.save(any(OrderItemDto.class))).thenReturn(testOrderItemDto);

        // When & Then
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testOrderItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(2))
                .andExpect(jsonPath("$.productId").value(1));

        verify(orderItemService).save(any(OrderItemDto.class));
    }

    @Test
    void testUpdate_ShouldUpdateOrderItem() throws Exception {
        // Given
        when(orderItemService.update(any(OrderItemDto.class))).thenReturn(testOrderItemDto);

        // When & Then
        mockMvc.perform(put("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testOrderItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(2))
                .andExpect(jsonPath("$.productId").value(1));

        verify(orderItemService).update(any(OrderItemDto.class));
    }

    @Test
    void testDeleteById_ShouldDeleteOrderItem() throws Exception {
        // Given
        doNothing().when(orderItemService).deleteById(any(OrderItemId.class));

        // When & Then
        mockMvc.perform(delete("/api/shippings/{orderId}/{productId}", "1", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(orderItemService).deleteById(any(OrderItemId.class));
    }

    @Test
    void testSave_WithValidOrderItem_ShouldSucceed() throws Exception {
        // Given
        OrderItemDto newOrderItem = OrderItemDto.builder()
                .productId(2)
                .orderId(2)
                .orderedQuantity(3)
                .productDto(testProductDto)
                .orderDto(testOrderDto)
                .build();

        OrderItemDto savedOrderItem = OrderItemDto.builder()
                .productId(2)
                .orderId(2)
                .orderedQuantity(3)
                .productDto(testProductDto)
                .orderDto(testOrderDto)
                .build();

        when(orderItemService.save(any(OrderItemDto.class))).thenReturn(savedOrderItem);

        // When & Then
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrderItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(3))
                .andExpect(jsonPath("$.productId").value(2));

        verify(orderItemService).save(any(OrderItemDto.class));
    }
}
