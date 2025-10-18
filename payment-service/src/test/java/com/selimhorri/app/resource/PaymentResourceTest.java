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
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.PaymentService;

@WebMvcTest(PaymentResource.class)
class PaymentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentDto testPaymentDto;

    @BeforeEach
    void setUp() {
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
    void testFindAll_ShouldReturnAllPayments() throws Exception {
        // Given
        List<PaymentDto> payments = Arrays.asList(testPaymentDto);
        DtoCollectionResponse<PaymentDto> response = new DtoCollectionResponse<>(payments);
        when(paymentService.findAll()).thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].paymentId").value(1))
                .andExpect(jsonPath("$.collection[0].paymentStatus").value("IN_PROGRESS"));

        verify(paymentService).findAll();
    }

    @Test
    void testFindById_ShouldReturnPayment() throws Exception {
        // Given
        Integer paymentId = 1;
        when(paymentService.findById(paymentId)).thenReturn(testPaymentDto);

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentStatus").value("IN_PROGRESS"));

        verify(paymentService).findById(paymentId);
    }

    @Test
    void testSave_ShouldCreatePayment() throws Exception {
        // Given
        when(paymentService.save(any(PaymentDto.class))).thenReturn(testPaymentDto);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPaymentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentStatus").value("IN_PROGRESS"));

        verify(paymentService).save(any(PaymentDto.class));
    }

    @Test
    void testUpdate_ShouldUpdatePayment() throws Exception {
        // Given
        when(paymentService.update(any(PaymentDto.class))).thenReturn(testPaymentDto);

        // When & Then
        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPaymentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.paymentStatus").value("IN_PROGRESS"));

        verify(paymentService).update(any(PaymentDto.class));
    }

    @Test
    void testDeleteById_ShouldDeletePayment() throws Exception {
        // Given
        Integer paymentId = 1;
        doNothing().when(paymentService).deleteById(paymentId);

        // When & Then
        mockMvc.perform(delete("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(paymentService).deleteById(paymentId);
    }

    @Test
    void testSave_WithValidPayment_ShouldSucceed() throws Exception {
        // Given
        PaymentDto newPayment = PaymentDto.builder()
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder()
                        .orderId(1)
                        .orderFee(100.0)
                        .orderDate(LocalDateTime.now())
                        .orderDesc("Test Order")
                        .build())
                        .isPayed(false)
                .build();

        PaymentDto savedPayment = PaymentDto.builder()
                .paymentId(2)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder()
                        .orderId(1)
                        .orderFee(100.0)
                        .orderDate(LocalDateTime.now())
                        .orderDesc("Test Order")
                        .build())
                        .isPayed(false)
                .build();

        when(paymentService.save(any(PaymentDto.class))).thenReturn(savedPayment);

        // When & Then
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(2))
                .andExpect(jsonPath("$.paymentStatus").value("IN_PROGRESS"));

        verify(paymentService).save(any(PaymentDto.class));
    }
}
