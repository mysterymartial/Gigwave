package com.gigwave.application.payments;

import com.gigwave.domain.payments.BankAccount;
import com.gigwave.infrastructure.persistence.payments.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;

    @Transactional
    public BankAccount addBankAccount(UUID userId, String bankName, String bankCode, String accountNumber, String accountName, Boolean isPayoutDefault) {
        if (isPayoutDefault) {
            bankAccountRepository.findByUserIdAndIsPayoutDefaultTrue(userId)
                    .ifPresent(account -> {
                        account.setIsPayoutDefault(false);
                        bankAccountRepository.save(account);
                    });
        }

        BankAccount bankAccount = BankAccount.builder()
                .userId(userId)
                .bankName(bankName)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .isPayoutDefault(isPayoutDefault != null && isPayoutDefault)
                .build();

        return bankAccountRepository.save(bankAccount);
    }

    public List<BankAccount> getUserBankAccounts(UUID userId) {
        return bankAccountRepository.findByUserId(userId);
    }

    public BankAccount getBankAccount(UUID accountId) {
        return bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));
    }

    @Transactional
    public void setDefaultPayoutAccount(UUID userId, UUID accountId) {
        bankAccountRepository.findByUserIdAndIsPayoutDefaultTrue(userId)
                .ifPresent(account -> {
                    account.setIsPayoutDefault(false);
                    bankAccountRepository.save(account);
                });

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bank account does not belong to user");
        }

        account.setIsPayoutDefault(true);
        bankAccountRepository.save(account);
    }

    @Transactional
    public void deleteBankAccount(UUID userId, UUID accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bank account does not belong to user");
        }

        bankAccountRepository.delete(account);
    }
}
