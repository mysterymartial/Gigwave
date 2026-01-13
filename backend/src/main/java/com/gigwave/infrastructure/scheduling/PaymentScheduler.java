package com.gigwave.infrastructure.scheduling;

import com.gigwave.application.payments.PaymentService;
import com.gigwave.domain.payments.DebitStatus;
import com.gigwave.domain.payments.DebitTransaction;
import com.gigwave.infrastructure.persistence.payments.DebitTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {
    private final DebitTransactionRepository debitTransactionRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void retryFailedDebits() {
        log.info("Starting scheduled retry of failed debits");
        
        List<DebitTransaction> failedDebits = debitTransactionRepository.findByStatus(DebitStatus.FAILED);
        
        for (DebitTransaction debit : failedDebits) {
            // Only retry if it's been at least 1 hour since last attempt
            if (debit.getAttemptedAt() != null && 
                debit.getAttemptedAt().plusHours(1).isBefore(LocalDateTime.now())) {
                try {
                    log.info("Retrying debit transaction: {}", debit.getId());
                    paymentService.initiateDebitForBooking(debit.getBookingId());
                } catch (Exception e) {
                    log.error("Error retrying debit transaction: {}", debit.getId(), e);
                }
            }
        }
        
        log.info("Completed scheduled retry of failed debits. Processed: {}", failedDebits.size());
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void processPendingPayouts() {
        log.info("Starting scheduled processing of pending payouts");
        // This would trigger payout processing for successful debits
        // Implementation depends on business rules
        log.info("Completed scheduled processing of pending payouts");
    }
}




