package com.transaction.gateway.service;

import com.transaction.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProducerService {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    @Value("${kafka.topic.transactions}")
    private String transactionsTopic;

    public void sendTransaction(Transaction transaction) {
        try {
            kafkaTemplate.send(transactionsTopic, transaction.getTransactionId().toString(), transaction)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Transaction sent to Kafka: {}, partition: {}, offset: {}",
                                    transaction.getTransactionId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send transaction to Kafka: {}", transaction.getTransactionId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending transaction to Kafka", e);
            throw new RuntimeException("Failed to publish transaction", e);
        }
    }
}
