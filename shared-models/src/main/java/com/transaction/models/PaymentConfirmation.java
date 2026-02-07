package com.transaction.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmation {

    private UUID confirmationId;
    private UUID transactionId;
    private String userId;
    private Transaction.TransactionStatus status;
    private String processorReferenceId;
    private Instant processedAt;
    private String failureReason;
}
