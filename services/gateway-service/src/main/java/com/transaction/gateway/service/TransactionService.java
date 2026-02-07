package com.transaction.gateway.service;

import com.transaction.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionProducerService producerService;
    private final RateLimitService rateLimitService;

    public Map<String, Object> processTransaction(Transaction transaction) {
        try {
            // Generate transaction ID if not provided
            if (transaction.getTransactionId() == null) {
                transaction.setTransactionId(UUID.randomUUID());
            }

            // Set initial status
            if (transaction.getStatus() == null) {
                transaction.setStatus(Transaction.TransactionStatus.PENDING);
            }

            // Check rate limit
            if (!rateLimitService.allowRequest(transaction.getUserId())) {
                log.warn("Rate limit exceeded for user: {}", transaction.getUserId());
                Map<String, Object> response = new HashMap<>();
                response.put("transactionId", transaction.getTransactionId());
                response.put("status", "RATE_LIMITED");
                response.put("message", "Rate limit exceeded. Please try again later.");
                return response;
            }

            // Enrich and validate transaction
            enrichTransaction(transaction);

            // Publish to Kafka
            producerService.sendTransaction(transaction);

            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", transaction.getTransactionId());
            response.put("status", "ACCEPTED");
            response.put("message", "Transaction submitted for processing");

            log.info("Transaction accepted: {}", transaction.getTransactionId());
            return response;

        } catch (Exception e) {
            log.error("Error processing transaction: {}", transaction.getTransactionId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to submit transaction: " + e.getMessage());
            return errorResponse;
        }
    }

    private void enrichTransaction(Transaction transaction) {
        // Add enrichment logic here (e.g., geo lookup, merchant validation)
        if (transaction.getLocation() == null) {
            // Default location if not provided
            transaction.setLocation(Transaction.Location.builder()
                    .country("UNKNOWN")
                    .city("UNKNOWN")
                    .build());
        }

        if (transaction.getMerchantCategory() == null) {
            transaction.setMerchantCategory("GENERAL");
        }
    }
}
