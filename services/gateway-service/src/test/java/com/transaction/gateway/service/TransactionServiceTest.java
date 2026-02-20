package com.transaction.gateway.service;

import com.transaction.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionProducerService producerService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Default: allow all requests
        when(rateLimitService.allowRequest(anyString())).thenReturn(true);
    }

    @Test
    void shouldProcessValidTransaction() {
        Transaction transaction = buildValidTransaction();

        Map<String, Object> result = transactionService.processTransaction(transaction);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        assertThat(result.get("transactionId")).isNotNull();

        verify(producerService, times(1)).sendTransaction(any(Transaction.class));
        verify(rateLimitService, times(1)).allowRequest(transaction.getUserId());
    }

    @Test
    void shouldGenerateTransactionIdIfMissing() {
        Transaction transaction = buildValidTransaction();
        transaction.setTransactionId(null);

        Map<String, Object> result = transactionService.processTransaction(transaction);

        assertThat(result.get("transactionId")).isNotNull();
    }

    @Test
    void shouldRejectWhenRateLimitExceeded() {
        Transaction transaction = buildValidTransaction();
        when(rateLimitService.allowRequest(transaction.getUserId())).thenReturn(false);

        Map<String, Object> result = transactionService.processTransaction(transaction);

        assertThat(result.get("status")).isEqualTo("RATE_LIMITED");
        verify(producerService, never()).sendTransaction(any());
    }

    @Test
    void shouldEnrichTransactionWithDefaultLocation() {
        Transaction transaction = buildValidTransaction();
        transaction.setLocation(null);

        Map<String, Object> result = transactionService.processTransaction(transaction);

        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        assertThat(transaction.getLocation()).isNotNull();
        assertThat(transaction.getLocation().getCountry()).isEqualTo("UNKNOWN");
    }

    @Test
    void shouldEnrichTransactionWithDefaultMerchantCategory() {
        Transaction transaction = buildValidTransaction();
        transaction.setMerchantCategory(null);

        Map<String, Object> result = transactionService.processTransaction(transaction);

        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        assertThat(transaction.getMerchantCategory()).isEqualTo("GENERAL");
    }

    private Transaction buildValidTransaction() {
        return Transaction.builder()
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
