package com.transaction.payment.service;

import com.transaction.models.PaymentConfirmation;
import com.transaction.models.Transaction;
import com.transaction.payment.entity.TransactionEntity;
import com.transaction.payment.repository.TransactionRepository;
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
class PaymentProcessorServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentConfirmationProducerService confirmationProducerService;

    @InjectMocks
    private PaymentProcessorService paymentProcessorService;

    @Test
    void shouldProcessPaymentSuccessfully() {
        Transaction transaction = buildValidTransaction();

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentProcessorService.processPayment(transaction);

        ArgumentCaptor<TransactionEntity> entityCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(entityCaptor.capture());

        TransactionEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getTransactionId()).isEqualTo(transaction.getTransactionId());
        assertThat(savedEntity.getUserId()).isEqualTo(transaction.getUserId());
        assertThat(savedEntity.getAmount()).isEqualByComparingTo(transaction.getAmount());
        assertThat(savedEntity.getStatus()).isIn("COMPLETED", "FAILED");
        assertThat(savedEntity.getProcessedAt()).isNotNull();

        verify(confirmationProducerService, times(1)).publishConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldPublishPaymentConfirmation() {
        Transaction transaction = buildValidTransaction();

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentProcessorService.processPayment(transaction);

        ArgumentCaptor<PaymentConfirmation> confirmationCaptor = ArgumentCaptor.forClass(PaymentConfirmation.class);
        verify(confirmationProducerService, times(1)).publishConfirmation(confirmationCaptor.capture());

        PaymentConfirmation confirmation = confirmationCaptor.getValue();
        assertThat(confirmation.getTransactionId()).isEqualTo(transaction.getTransactionId());
        assertThat(confirmation.getUserId()).isEqualTo(transaction.getUserId());
        assertThat(confirmation.getConfirmationId()).isNotNull();
        assertThat(confirmation.getProcessedAt()).isNotNull();
        assertThat(confirmation.getStatus()).isIn(
                Transaction.TransactionStatus.COMPLETED,
                Transaction.TransactionStatus.FAILED
        );
    }

    @Test
    void shouldConvertTransactionToEntity() {
        Transaction transaction = buildValidTransaction();

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentProcessorService.processPayment(transaction);

        ArgumentCaptor<TransactionEntity> entityCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(entityCaptor.capture());

        TransactionEntity entity = entityCaptor.getValue();
        assertThat(entity.getTransactionId()).isEqualTo(transaction.getTransactionId());
        assertThat(entity.getUserId()).isEqualTo(transaction.getUserId());
        assertThat(entity.getAmount()).isEqualByComparingTo(transaction.getAmount());
        assertThat(entity.getCurrency()).isEqualTo(transaction.getCurrency());
        assertThat(entity.getMerchant()).isEqualTo(transaction.getMerchant());
        assertThat(entity.getMerchantCategory()).isEqualTo(transaction.getMerchantCategory());
        assertThat(entity.getCountry()).isEqualTo(transaction.getLocation().getCountry());
        assertThat(entity.getCity()).isEqualTo(transaction.getLocation().getCity());
    }

    @Test
    void shouldHandleTransactionWithNullLocation() {
        Transaction transaction = buildValidTransaction();
        transaction.setLocation(null);

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentProcessorService.processPayment(transaction);

        ArgumentCaptor<TransactionEntity> entityCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(1)).save(entityCaptor.capture());

        TransactionEntity entity = entityCaptor.getValue();
        assertThat(entity.getCountry()).isNull();
        assertThat(entity.getCity()).isNull();
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
