package com.transaction.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

    private UUID alertId;
    private UUID transactionId;
    private String userId;
    private FraudReason reason;
    private String description;
    private RiskLevel riskLevel;
    private Instant detectedAt;
    private List<String> triggeredRules;

    public enum FraudReason {
        HIGH_AMOUNT,
        HIGH_FREQUENCY,
        GEO_ANOMALY,
        SUSPICIOUS_MERCHANT,
        UNUSUAL_TIME
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
