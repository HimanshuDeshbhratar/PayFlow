package com.payflow.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.exception.GlobalExceptionHandler;
import com.payflow.payment.controller.PaymentController;
import com.payflow.payment.dto.CreatePaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock PaymentService paymentService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPayment() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId, merchantId, UUID.randomUUID(), "pay_abc", 5000, "USD",
                PaymentStatus.COMPLETED, "test", "cs_1", 10, null, UUID.randomUUID(),
                null, Instant.now(), Instant.now()
        );
        when(paymentService.createPayment(any(CreatePaymentRequest.class), nullable(String.class))).thenReturn(response);

        CreatePaymentRequest request = new CreatePaymentRequest(
                merchantId, UUID.randomUUID(), 5000L, "USD", "test",
                null, null, null, null, null, false, null
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-key-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("pay_abc"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
