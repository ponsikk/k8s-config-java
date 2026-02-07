package com.transaction.payment.consumer;

import com.transaction.models.Transaction;
import com.transaction.payment.service.PaymentProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final PaymentProcessorService paymentProcessorService;

    @KafkaListener(
        topics = "${spring.kafka.consumer.topics.transactions}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransaction(
            @Payload Transaction transaction,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Received transaction: {} from partition: {}, offset: {}",
                 transaction.getTransactionId(), partition, offset);

        try {
            paymentProcessorService.processPayment(transaction);
            log.debug("Successfully processed transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing transaction: {}", transaction.getTransactionId(), e);
            // In production, consider implementing retry logic or dead letter queue
            throw e;
        }
    }
}
