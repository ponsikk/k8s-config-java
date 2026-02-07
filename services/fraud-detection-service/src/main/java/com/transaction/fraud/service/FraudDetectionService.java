package com.transaction.fraud.service;

import com.transaction.models.FraudAlert;
import com.transaction.models.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertProducerService alertProducerService;

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("1000.00");

    public void analyzeTransaction(Transaction transaction) {
        log.debug("Analyzing transaction: {}", transaction.getTransactionId());

        List<String> triggeredRules = new ArrayList<>();
        FraudAlert.RiskLevel riskLevel = FraudAlert.RiskLevel.LOW;
        FraudAlert.FraudReason primaryReason = null;

        // Rule 1: High amount check
        if (transaction.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            triggeredRules.add("HIGH_AMOUNT");
            riskLevel = FraudAlert.RiskLevel.MEDIUM;
            primaryReason = FraudAlert.FraudReason.HIGH_AMOUNT;
        }

        // Rule 2: Suspicious merchant categories
        if (isSuspiciousMerchantCategory(transaction.getMerchantCategory())) {
            triggeredRules.add("SUSPICIOUS_MERCHANT");
            riskLevel = FraudAlert.RiskLevel.HIGH;
            primaryReason = FraudAlert.FraudReason.SUSPICIOUS_MERCHANT;
        }

        // Rule 3: Geo anomaly (simplified)
        if (isGeoAnomaly(transaction.getLocation())) {
            triggeredRules.add("GEO_ANOMALY");
            riskLevel = FraudAlert.RiskLevel.HIGH;
            primaryReason = FraudAlert.FraudReason.GEO_ANOMALY;
        }

        // If fraud detected, publish alert
        if (!triggeredRules.isEmpty()) {
            FraudAlert alert = FraudAlert.builder()
                    .alertId(UUID.randomUUID())
                    .transactionId(transaction.getTransactionId())
                    .userId(transaction.getUserId())
                    .reason(primaryReason)
                    .description(String.format("Fraud detected: %s", String.join(", ", triggeredRules)))
                    .riskLevel(riskLevel)
                    .detectedAt(Instant.now())
                    .triggeredRules(triggeredRules)
                    .build();

            alertProducerService.sendAlert(alert);
            log.warn("Fraud alert generated for transaction: {} (Risk: {})",
                    transaction.getTransactionId(), riskLevel);
        } else {
            log.debug("Transaction passed fraud checks: {}", transaction.getTransactionId());
        }
    }

    private boolean isSuspiciousMerchantCategory(String category) {
        if (category == null) return false;
        // Simplified logic - can be extended
        return category.equalsIgnoreCase("GAMBLING") ||
               category.equalsIgnoreCase("CRYPTOCURRENCY");
    }

    private boolean isGeoAnomaly(Transaction.Location location) {
        if (location == null) return false;
        // Simplified logic - in reality would check against user's typical locations
        return "UNKNOWN".equals(location.getCountry());
    }
}
