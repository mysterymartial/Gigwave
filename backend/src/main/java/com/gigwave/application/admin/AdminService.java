package com.gigwave.application.admin;

import com.gigwave.domain.payments.*;
import com.gigwave.domain.users.User;
import com.gigwave.domain.users.UserRole;
import com.gigwave.infrastructure.persistence.payments.BankAccountRepository;
import com.gigwave.infrastructure.persistence.payments.DebitTransactionRepository;
import com.gigwave.infrastructure.persistence.payments.PaymentMandateRepository;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.infrastructure.payments.onepipe.OnePipeClient;
import com.gigwave.infrastructure.payments.onepipe.dto.DebitRequest;
import com.gigwave.infrastructure.payments.onepipe.dto.DebitResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final UserRepository userRepository;
    private final PaymentMandateRepository mandateRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DebitTransactionRepository debitRepository;
    private final OnePipeClient onePipeClient;

    /**
     * Get all customers (musicians and event owners, excluding admins)
     * @return List of customer information with mandate status
     */
    @Transactional(readOnly = true)
    public List<CustomerInfo> getAllCustomers() {
        List<User> musicians = userRepository.findByRole(UserRole.MUSICIAN);
        List<User> eventOwners = userRepository.findByRole(UserRole.EVENT_OWNER);

        return Stream.concat(musicians.stream(), eventOwners.stream())
                .map(user -> {
                    List<PaymentMandate> mandates = mandateRepository.findByUserId(user.getId());
                    PaymentMandate activeMandate = mandates.stream()
                            .filter(m -> m.getStatus() == MandateStatus.ACTIVE)
                            .findFirst()
                            .orElse(null);

                    return CustomerInfo.builder()
                            .id(user.getId())
                            .phone(user.getPhone())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .isDisabled(user.getIsDisabled())
                            .createdAt(user.getCreatedAt())
                            .hasActiveMandate(activeMandate != null)
                            .mandateRef(activeMandate != null ? activeMandate.getMandateRef() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get customer information by ID
     * @param customerId Customer user ID
     * @return Customer information
     * @throws IllegalArgumentException if customer not found or is an admin
     */
    @Transactional(readOnly = true)
    public CustomerInfo getCustomerById(UUID customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot retrieve admin as customer");
        }

        List<PaymentMandate> mandates = mandateRepository.findByUserId(customerId);
        PaymentMandate activeMandate = mandates.stream()
                .filter(m -> m.getStatus() == MandateStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        return CustomerInfo.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .isDisabled(user.getIsDisabled())
                .createdAt(user.getCreatedAt())
                .hasActiveMandate(activeMandate != null)
                .mandateRef(activeMandate != null ? activeMandate.getMandateRef() : null)
                .build();
    }

    /**
     * Debit a customer (musician or event owner) using their active mandate
     * @param customerId Customer user ID
     * @param amount Amount to debit
     * @param reason Reason for debit (e.g., "Fraudulent activity", "Chargeback")
     * @param adminId Admin user ID performing the debit
     * @return Debit result with transaction reference
     * @throws IllegalArgumentException if customer not found, is admin, or has no active mandate
     * @throws IllegalStateException if account is disabled
     */
    @Transactional
    public AdminDebitResult debitCustomer(UUID customerId, BigDecimal amount, String reason, UUID adminId) {
        // Verify customer exists and is not an admin
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (customer.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot debit admin accounts");
        }

        if (Boolean.TRUE.equals(customer.getIsDisabled())) {
            throw new IllegalStateException("Cannot debit disabled accounts");
        }

        // Find active mandate
        List<PaymentMandate> mandates = mandateRepository.findByUserId(customerId);
        PaymentMandate activeMandate = mandates.stream()
                .filter(m -> m.getStatus() == MandateStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Customer has no active mandate"));

        // Get bank account
        BankAccount bankAccount = bankAccountRepository.findById(activeMandate.getBankAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        // Create debit request
        String requestRef = "ADMIN_DEBIT_" + System.currentTimeMillis();
        DebitRequest debitRequest = DebitRequest.builder()
                .mandateRef(activeMandate.getMandateRef())
                .amount(amount)
                .narration("Admin debit: " + reason)
                .callbackUrl(null) // Admin debits don't need callbacks
                .userId(customerId)
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .accountName(bankAccount.getAccountName())
                .build();

        // Initiate debit
        DebitResponse response = onePipeClient.initiateDebit(debitRequest);

        // Save debit transaction (without bookingId since this is admin-initiated)
        DebitTransaction debitTransaction = DebitTransaction.builder()
                .mandateId(activeMandate.getId())
                .amount(amount)
                .providerRef(response.getTransactionRef())
                .status(DebitStatus.PENDING)
                .attemptedAt(LocalDateTime.now())
                .build();

        debitRepository.save(debitTransaction);

        log.info("Admin {} debited {} from customer {} ({}). Transaction: {}", 
                adminId, amount, customerId, reason, response.getTransactionRef());

        return AdminDebitResult.builder()
                .customerId(customerId)
                .amount(amount)
                .status(response.getStatus())
                .transactionRef(response.getTransactionRef())
                .message(response.getMessage())
                .build();
    }

    @Data
    @lombok.Builder
    public static class CustomerInfo {
        private UUID id;
        private String phone;
        private String email;
        private UserRole role;
        private Boolean isDisabled;
        private java.time.LocalDateTime createdAt;
        private Boolean hasActiveMandate;
        private String mandateRef;
    }

    @Data
    @lombok.Builder
    public static class AdminDebitResult {
        private UUID customerId;
        private BigDecimal amount;
        private String status;
        private String transactionRef;
        private String message;
    }
}
