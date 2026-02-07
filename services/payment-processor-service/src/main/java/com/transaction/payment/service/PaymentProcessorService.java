package com.transaction.payment.service;

import com.transaction.models.PaymentConfirmation;
import com.transaction.models.Transaction;
import com.transaction.payment.entity.TransactionEntity;
import com.transaction.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessorService {

    private final TransactionRepository transactionRepository;
    private final PaymentConfirmationProducerService confirmationProducerService;

    @Transactional
    public void processPayment(Transaction transaction) {
        log.debug("Processing payment for transaction: {}", transaction.getTransactionId());

        // Convert Transaction to TransactionEntity
        TransactionEntity entity = convertToEntity(transaction);

        // Simulate payment processing (in real scenario, this would call payment gateway)
        boolean paymentSuccess = simulatePaymentProcessing(transaction);

        // Update entity status
        entity.setProcessedAt(Instant.now());
        entity.setStatus(paymentSuccess ? "COMPLETED" : "FAILED");

        // Save to database
        transactionRepository.save(entity);
        log.info("Saved transaction to database: {} with status: {}",
                entity.getTransactionId(), entity.getStatus());

        // Publish payment confirmation to Kafka
        PaymentConfirmation confirmation = buildPaymentConfirmation(transaction, entity, paymentSuccess);
        confirmationProducerService.publishConfirmation(confirmation);

        log.debug("Published payment confirmation for transaction: {}", transaction.getTransactionId());
    }

    private TransactionEntity convertToEntity(Transaction transaction) {
        return TransactionEntity.builder()
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .merchant(transaction.getMerchant())
                .merchantCategory(transaction.getMerchantCategory())
                .transactionType(transaction.getType() != null ? transaction.getType().name() : "PAYMENT")
                .country(transaction.getLocation() != null ? transaction.getLocation().getCountry() : null)
                .city(transaction.getLocation() != null ? transaction.getLocation().getCity() : null)
                .status("PROCESSING")
                .build();
    }

    private boolean simulatePaymentProcessing(Transaction transaction) {
        // Simple simulation: 95% success rate
        // In production, this would integrate with actual payment gateway
        return Math.random() < 0.95;
    }

    private PaymentConfirmation buildPaymentConfirmation(
            Transaction transaction,
            TransactionEntity entity,
            boolean success) {

        return PaymentConfirmation.builder()
                .confirmationId(UUID.randomUUID())
                .transactionId(transaction.getTransactionId())
                .userId(transaction.getUserId())
                .status(success ? Transaction.TransactionStatus.COMPLETED : Transaction.TransactionStatus.FAILED)
                .processorReferenceId(entity.getTransactionId().toString())
                .processedAt(entity.getProcessedAt())
                .failureReason(success ? null : "Payment processing failed")
                .build();
    }
}
