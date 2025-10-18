package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.impl.OrderItemServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private OrderItem testOrderItem;
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

        testOrderItem = OrderItem.builder()
                .productId(1)
                .orderId(1)
                .orderedQuantity(2)
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
    void testFindAll_ShouldReturnAllOrderItems() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(testOrderItem);
        when(orderItemRepository.findAll()).thenReturn(orderItems);
        when(restTemplate.getForObject(anyString(), eq(ProductDto.class))).thenReturn(testProductDto);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(testOrderDto);

        // When
        List<OrderItemDto> result = orderItemService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(2), result.get(0).getOrderedQuantity());
        verify(orderItemRepository).findAll();
    }

    @Test
    void testSave_ShouldReturnSavedOrderItem() {
        // Given
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // When
        OrderItemDto result = orderItemService.save(testOrderItemDto);

        // Then
        assertNotNull(result);
        assertEquals(testOrderItemDto.getOrderedQuantity(), result.getOrderedQuantity());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void testUpdate_ShouldReturnUpdatedOrderItem() {
        // Given
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // When
        OrderItemDto result = orderItemService.update(testOrderItemDto);

        // Then
        assertNotNull(result);
        assertEquals(testOrderItemDto.getOrderedQuantity(), result.getOrderedQuantity());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void testDeleteById_ShouldCallRepositoryDelete() {
        // Given
        OrderItemId orderItemId = new OrderItemId(1, 1);
        doNothing().when(orderItemRepository).deleteById(orderItemId);

        // When
        orderItemService.deleteById(orderItemId);

        // Then
        verify(orderItemRepository).deleteById(orderItemId);
    }

    @Test
    void testSave_WithValidData_ShouldSucceed() {
        // Given
        OrderItemDto newOrderItem = OrderItemDto.builder()
                .productId(2)
                .orderId(2)
                .orderedQuantity(3)
                .productDto(testProductDto)
                .orderDto(testOrderDto)
                .build();

        OrderItem savedOrderItem = OrderItem.builder()
                .productId(2)
                .orderId(2)
                .orderedQuantity(3)
                .build();

        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedOrderItem);

        // When
        OrderItemDto result = orderItemService.save(newOrderItem);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(3), result.getOrderedQuantity());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void testUpdate_WithValidData_ShouldSucceed() {
        // Given
        OrderItemDto updatedOrderItem = OrderItemDto.builder()
                .productId(1)
                .orderId(1)
                .orderedQuantity(4)
                .productDto(testProductDto)
                .orderDto(testOrderDto)
                .build();

        OrderItem savedOrderItem = OrderItem.builder()
                .productId(1)
                .orderId(1)
                .orderedQuantity(4)
                .build();

        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedOrderItem);

        // When
        OrderItemDto result = orderItemService.update(updatedOrderItem);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(4), result.getOrderedQuantity());
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void testFindAll_WithRestTemplateCalls_ShouldSucceed() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(testOrderItem);
        when(orderItemRepository.findAll()).thenReturn(orderItems);
        when(restTemplate.getForObject(contains("PRODUCT-SERVICE"), eq(ProductDto.class))).thenReturn(testProductDto);
        when(restTemplate.getForObject(contains("ORDER-SERVICE"), eq(OrderDto.class))).thenReturn(testOrderDto);

        // When
        List<OrderItemDto> result = orderItemService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getProductDto());
        assertNotNull(result.get(0).getOrderDto());
        verify(restTemplate, times(1)).getForObject(contains("PRODUCT-SERVICE"), eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(contains("ORDER-SERVICE"), eq(OrderDto.class));
    }
}