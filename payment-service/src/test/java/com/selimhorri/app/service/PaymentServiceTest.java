package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.impl.PaymentServiceImpl;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private PaymentDto testPaymentDto;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .paymentId(1)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderId(1)
                .build();

        testPaymentDto = PaymentDto.builder()
                .paymentId(1)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder()
                        .orderId(1)
                        .orderFee(100.0)
                        .orderDate(LocalDateTime.now())
                        .orderDesc("Test Order")
                        .build())
                        .isPayed(false)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllPayments() {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentRepository.findAll()).thenReturn(payments);

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PaymentStatus.IN_PROGRESS, result.get(0).getPaymentStatus());
        verify(paymentRepository).findAll();
    }

    @Test
    void testFindById_WhenPaymentExists_ShouldReturnPayment() {
        // Given
        Integer paymentId = 1;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        // When
        PaymentDto result = paymentService.findById(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());
        assertEquals(PaymentStatus.IN_PROGRESS, result.getPaymentStatus());
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void testFindById_WhenPaymentNotExists_ShouldThrowException() {
        // Given
        Integer paymentId = 999;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        PaymentNotFoundException exception = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.findById(paymentId));

        assertTrue(exception.getMessage().contains("Payment with id: 999 not found"));
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void testSave_ShouldReturnSavedPayment() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        PaymentDto result = paymentService.save(testPaymentDto);

        // Then
        assertNotNull(result);
        assertEquals(testPaymentDto.getPaymentId(), result.getPaymentId());
        assertEquals(testPaymentDto.getPaymentStatus(), result.getPaymentStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testUpdate_ShouldReturnUpdatedPayment() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        PaymentDto result = paymentService.update(testPaymentDto);

        // Then
        assertNotNull(result);
        assertEquals(testPaymentDto.getPaymentId(), result.getPaymentId());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testDeleteById_ShouldCallRepositoryDelete() {
        // Given
        Integer paymentId = 1;
        doNothing().when(paymentRepository).deleteById(paymentId);

        // When
        paymentService.deleteById(paymentId);

        // Then
        verify(paymentRepository).deleteById(paymentId);
    }
}
