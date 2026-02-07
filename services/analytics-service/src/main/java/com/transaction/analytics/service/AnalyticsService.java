package com.transaction.analytics.service;

import com.transaction.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOTAL_TRANSACTIONS_KEY = "analytics:total_transactions";
    private static final String TOTAL_AMOUNT_KEY = "analytics:total_amount";
    private static final String TRANSACTIONS_PER_SECOND_KEY = "analytics:transactions_per_second:";
    private static final String TOP_MERCHANTS_KEY = "analytics:top_merchants";

    public void processTransaction(Transaction transaction) {
        log.info("Processing transaction for analytics: {}", transaction.getTransactionId());

        // Update total transaction count
        redisTemplate.opsForValue().increment(TOTAL_TRANSACTIONS_KEY, 1);

        // Update total amount
        String currentAmountStr = (String) redisTemplate.opsForValue().get(TOTAL_AMOUNT_KEY);
        BigDecimal currentAmount = currentAmountStr != null
            ? new BigDecimal(currentAmountStr)
            : BigDecimal.ZERO;
        BigDecimal newAmount = currentAmount.add(transaction.getAmount());
        redisTemplate.opsForValue().set(TOTAL_AMOUNT_KEY, newAmount.toString());

        // Update transactions per second (using epoch second as key)
        long epochSecond = transaction.getTimestamp().getEpochSecond();
        String tpsKey = TRANSACTIONS_PER_SECOND_KEY + epochSecond;
        redisTemplate.opsForValue().increment(tpsKey, 1);
        // Set expiry to 1 hour for TPS keys
        redisTemplate.expire(tpsKey, java.time.Duration.ofHours(1));

        // Update average amount
        updateAverageAmount();

        // Update top merchants
        redisTemplate.opsForHash().increment(
            TOP_MERCHANTS_KEY,
            transaction.getMerchant(),
            1
        );

        log.debug("Analytics updated for transaction: {}", transaction.getTransactionId());
    }

    private void updateAverageAmount() {
        String totalTransactionsStr = (String) redisTemplate.opsForValue().get(TOTAL_TRANSACTIONS_KEY);
        String totalAmountStr = (String) redisTemplate.opsForValue().get(TOTAL_AMOUNT_KEY);

        if (totalTransactionsStr != null && totalAmountStr != null) {
            long totalTransactions = Long.parseLong(totalTransactionsStr);
            BigDecimal totalAmount = new BigDecimal(totalAmountStr);

            if (totalTransactions > 0) {
                BigDecimal averageAmount = totalAmount.divide(
                    BigDecimal.valueOf(totalTransactions),
                    2,
                    RoundingMode.HALF_UP
                );
                redisTemplate.opsForValue().set("analytics:average_amount", averageAmount.toString());
                log.debug("Updated average amount: {}", averageAmount);
            }
        }
    }

    public Long getTotalTransactions() {
        String value = (String) redisTemplate.opsForValue().get(TOTAL_TRANSACTIONS_KEY);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public BigDecimal getTotalAmount() {
        String value = (String) redisTemplate.opsForValue().get(TOTAL_AMOUNT_KEY);
        return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
    }

    public BigDecimal getAverageAmount() {
        String value = (String) redisTemplate.opsForValue().get("analytics:average_amount");
        return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
    }
}
