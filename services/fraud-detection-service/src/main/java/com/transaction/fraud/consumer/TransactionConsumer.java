package com.transaction.fraud.consumer;

import com.transaction.fraud.service.FraudDetectionService;
import com.transaction.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = "${kafka.topic.transactions}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Transaction transaction) {
        try {
            log.debug("Consumed transaction: {}", transaction.getTransactionId());
            fraudDetectionService.analyzeTransaction(transaction);
        } catch (Exception e) {
            log.error("Error processing transaction: {}", transaction.getTransactionId(), e);
            // In production, implement DLQ (Dead Letter Queue) here
        }
    }
}
