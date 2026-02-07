package com.transaction.payment.service;

import com.transaction.models.PaymentConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmationProducerService {

    private final KafkaTemplate<String, PaymentConfirmation> kafkaTemplate;

    @Value("${spring.kafka.producer.topics.payment-confirmations}")
    private String paymentConfirmationsTopic;

    public void publishConfirmation(PaymentConfirmation confirmation) {
        String key = confirmation.getTransactionId().toString();

        CompletableFuture<SendResult<String, PaymentConfirmation>> future =
            kafkaTemplate.send(paymentConfirmationsTopic, key, confirmation);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment confirmation for transaction: {}",
                         confirmation.getTransactionId(), ex);
            } else {
                log.debug("Successfully published payment confirmation for transaction: {} to partition: {}, offset: {}",
                         confirmation.getTransactionId(),
                         result.getRecordMetadata().partition(),
                         result.getRecordMetadata().offset());
            }
        });
    }
}
