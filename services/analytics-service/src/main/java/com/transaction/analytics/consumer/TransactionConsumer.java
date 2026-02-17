Аpackage com.transaction.analytics.consumer;

import com.transaction.analytics.service.AnalyticsService;
import com.transaction.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(topics = "transactions", groupId = "analytics-group")
    public void consumeTransaction(Transaction transaction) {
        log.info("Received transaction for analytics: {}", transaction.getTransactionId());

        try {
            analyticsService.processTransaction(transaction);
            log.debug("Successfully processed transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing transaction {}: {}", transaction.getTransactionId(), e.getMessage(), e);
        }
    }
}
