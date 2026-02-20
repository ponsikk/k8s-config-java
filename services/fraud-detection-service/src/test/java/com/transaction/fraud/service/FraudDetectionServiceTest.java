package com.transaction.fraud.service;

import com.transaction.models.FraudAlert;
import com.transaction.models.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudAlertProducerService alertProducerService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Test
    void shouldNotDetectFraudForValidTransaction() {
        Transaction transaction = buildValidTransaction();

        fraudDetectionService.analyzeTransaction(transaction);

        verify(alertProducerService, never()).sendAlert(any());
    }

    @Test
    void shouldDetectHighAmountFraud() {
        Transaction transaction = buildValidTransaction();
        transaction.setAmount(new BigDecimal("5000.00"));

        fraudDetectionService.analyzeTransaction(transaction);

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(alertProducerService, times(1)).sendAlert(alertCaptor.capture());

        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getReason()).isEqualTo(FraudAlert.FraudReason.HIGH_AMOUNT);
        assertThat(alert.getRiskLevel()).isEqualTo(FraudAlert.RiskLevel.MEDIUM);
        assertThat(alert.getTriggeredRules()).contains("HIGH_AMOUNT");
    }

    @Test
    void shouldDetectSuspiciousMerchant() {
        Transaction transaction = buildValidTransaction();
        transaction.setMerchantCategory("GAMBLING");

        fraudDetectionService.analyzeTransaction(transaction);

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(alertProducerService, times(1)).sendAlert(alertCaptor.capture());

        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getReason()).isEqualTo(FraudAlert.FraudReason.SUSPICIOUS_MERCHANT);
        assertThat(alert.getRiskLevel()).isEqualTo(FraudAlert.RiskLevel.HIGH);
        assertThat(alert.getTriggeredRules()).contains("SUSPICIOUS_MERCHANT");
    }

    @Test
    void shouldDetectGeoAnomaly() {
        Transaction transaction = buildValidTransaction();
        transaction.setLocation(Transaction.Location.builder()
                .country("UNKNOWN")
                .city("UNKNOWN")
                .build());

        fraudDetectionService.analyzeTransaction(transaction);

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(alertProducerService, times(1)).sendAlert(alertCaptor.capture());

        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getReason()).isEqualTo(FraudAlert.FraudReason.GEO_ANOMALY);
        assertThat(alert.getRiskLevel()).isEqualTo(FraudAlert.RiskLevel.HIGH);
        assertThat(alert.getTriggeredRules()).contains("GEO_ANOMALY");
    }

    @Test
    void shouldDetectMultipleRules() {
        Transaction transaction = buildValidTransaction();
        transaction.setAmount(new BigDecimal("5000.00"));
        transaction.setMerchantCategory("CRYPTOCURRENCY");

        fraudDetectionService.analyzeTransaction(transaction);

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(alertProducerService, times(1)).sendAlert(alertCaptor.capture());

        FraudAlert alert = alertCaptor.getValue();
        assertThat(alert.getRiskLevel()).isEqualTo(FraudAlert.RiskLevel.HIGH);
        assertThat(alert.getTriggeredRules()).hasSize(2);
        assertThat(alert.getTriggeredRules()).contains("HIGH_AMOUNT", "SUSPICIOUS_MERCHANT");
    }

    private Transaction buildValidTransaction() {
        return Transaction.builder()
                .transactionId(UUID.randomUUID())
                .userId("user_123")
                .amount(new BigDecimal("149.99"))
                .currency("USD")
                .merchant("Test Merchant")
                .merchantCategory("RETAIL")
                .type(Transaction.TransactionType.PAYMENT)
                .location(Transaction.Location.builder()
                        .country("US")
                        .city("New York")
                        .build())
                .timestamp(Instant.now())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }
}
