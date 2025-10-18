package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private Cart testCart;
    private CartDto testCartDto;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(100.0)
                .build();

        testCart = Cart.builder()
                .cartId(1)
                .userId(1)
                .orders(Set.of(testOrder))
                .build();

        testOrder.setCart(testCart);

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(100.0)
                .build();

        testCartDto = CartDto.builder()
                        .cartId(1)
                        .userId(1)
                        .orderDtos(Set.of(testOrderDto))
                        .build();

        testOrderDto.setCartDto(testCartDto);
    }

    @Test
    void testFindAll_ShouldReturnAllOrders() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // When
        List<OrderDto> result = orderService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Order", result.get(0).getOrderDesc());
        verify(orderRepository).findAll();
    }

    @Test
    void testFindById_WhenOrderExists_ShouldReturnOrder() {
        // Given
        Integer orderId = 1;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        OrderDto result = orderService.findById(orderId);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("Test Order", result.getOrderDesc());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void testFindById_WhenOrderNotExists_ShouldThrowException() {
        // Given
        Integer orderId = 999;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.findById(orderId)
        );
        
        assertTrue(exception.getMessage().contains("Order with id: 999 not found"));
        verify(orderRepository).findById(orderId);
    }

    @Test
    void testSave_ShouldReturnSavedOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.save(testOrderDto);

        // Then
        assertNotNull(result);
        assertEquals(testOrderDto.getOrderId(), result.getOrderId());
        assertEquals(testOrderDto.getOrderDesc(), result.getOrderDesc());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testUpdate_ShouldReturnUpdatedOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.update(testOrderDto);

        // Then
        assertNotNull(result);
        assertEquals(testOrderDto.getOrderId(), result.getOrderId());
        verify(orderRepository).save(any(Order.class));
    }
}
