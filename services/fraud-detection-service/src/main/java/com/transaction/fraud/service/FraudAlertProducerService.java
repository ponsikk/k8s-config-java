package com.transaction.fraud.service;

import com.transaction.models.FraudAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAlertProducerService {

    private final KafkaTemplate<String, FraudAlert> kafkaTemplate;

    @Value("${kafka.topic.fraud-alerts}")
    private String fraudAlertsTopic;

    public void sendAlert(FraudAlert alert) {
        try {
            kafkaTemplate.send(fraudAlertsTopic, alert.getTransactionId().toString(), alert)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Fraud alert sent: {} (Risk: {})", alert.getAlertId(), alert.getRiskLevel());
                        } else {
                            log.error("Failed to send fraud alert: {}", alert.getAlertId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending fraud alert to Kafka", e);
        }
    }
}
