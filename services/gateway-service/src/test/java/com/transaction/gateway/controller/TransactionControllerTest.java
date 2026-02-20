package com.transaction.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.gateway.service.TransactionService;
import com.transaction.models.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("gateway-service"));
    }

    @Test
    void shouldAcceptValidTransaction() throws Exception {
        Transaction transaction = buildValidTransaction();

        Map<String, Object> mockResponse = Map.of(
                "transactionId", UUID.randomUUID().toString(),
                "status", "ACCEPTED",
                "message", "Transaction submitted for processing"
        );

        when(transactionService.processTransaction(any(Transaction.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldRejectInvalidTransaction() throws Exception {
        // Transaction with missing required fields
        Transaction invalidTransaction = Transaction.builder().build();

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }

    private Transaction buildValidTransaction() {
        return Transaction.builder()
                .transactionId(UUID.randomUUID())
                .userId("user_123")
                .amount(new BigDecimal("149.99"))
                .currency("USD")
                .merchant("Test Merchant")
                .merchantCategory("RETAIL")
                .type(Transaction.TransactionType.PAYMENT)
                .location(Transaction.Location.builder()
                        .country("US")
                        .city("New York")
                        .build())
                .timestamp(Instant.now())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }
}
