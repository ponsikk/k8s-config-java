package com.transaction.analytics.service;

import com.transaction.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void shouldProcessTransactionAndUpdateMetrics() {
        Transaction transaction = buildValidTransaction();

        // Mock existing values
        when(valueOperations.get("analytics:total_amount")).thenReturn("100.00");
        when(valueOperations.get("analytics:total_transactions")).thenReturn("5");

        analyticsService.processTransaction(transaction);

        // Verify total transactions incremented
        verify(valueOperations, times(1)).increment("analytics:total_transactions", 1);

        // Verify total amount updated
        verify(valueOperations, atLeastOnce()).get("analytics:total_amount");
        verify(valueOperations, times(1)).set(eq("analytics:total_amount"), anyString());

        // Verify TPS key updated
        verify(valueOperations, times(1)).increment(startsWith("analytics:transactions_per_second:"), eq(1L));
        verify(redisTemplate, times(1)).expire(startsWith("analytics:transactions_per_second:"), any(Duration.class));

        // Verify top merchants updated
        verify(hashOperations, times(1)).increment("analytics:top_merchants", transaction.getMerchant(), 1);
    }

    @Test
    void shouldGetTotalTransactions() {
        when(valueOperations.get("analytics:total_transactions")).thenReturn("42");

        Long total = analyticsService.getTotalTransactions();

        assertThat(total).isEqualTo(42L);
    }

    @Test
    void shouldReturnZeroWhenNoTransactions() {
        when(valueOperations.get("analytics:total_transactions")).thenReturn(null);

        Long total = analyticsService.getTotalTransactions();

        assertThat(total).isEqualTo(0L);
    }

    @Test
    void shouldGetTotalAmount() {
        when(valueOperations.get("analytics:total_amount")).thenReturn("12345.67");

        BigDecimal total = analyticsService.getTotalAmount();

        assertThat(total).isEqualByComparingTo(new BigDecimal("12345.67"));
    }

    @Test
    void shouldGetAverageAmount() {
        when(valueOperations.get("analytics:average_amount")).thenReturn("123.45");

        BigDecimal average = analyticsService.getAverageAmount();

        assertThat(average).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void shouldHandleFirstTransaction() {
        Transaction transaction = buildValidTransaction();

        when(valueOperations.get("analytics:total_amount")).thenReturn(null);
        when(valueOperations.get("analytics:total_transactions")).thenReturn(null);

        analyticsService.processTransaction(transaction);

        // Should handle null values gracefully
        verify(valueOperations, times(1)).increment("analytics:total_transactions", 1);
        verify(valueOperations, times(1)).set(eq("analytics:total_amount"), anyString());
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
