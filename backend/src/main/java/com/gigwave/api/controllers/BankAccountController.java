package com.gigwave.api.controllers;

import com.gigwave.api.dto.payments.BankAccountDto;
import com.gigwave.application.payments.BankAccountService;
import com.gigwave.domain.payments.BankAccount;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {
    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<BankAccountDto> addBankAccount(
            @Valid @RequestBody BankAccountDto dto,
            @CurrentUser UUID userId
    ) {
        BankAccount account = bankAccountService.addBankAccount(
                userId,
                dto.getBankName(),
                dto.getBankCode(),
                dto.getAccountNumber(),
                dto.getAccountName(),
                dto.getIsPayoutDefault()
        );
        return ResponseEntity.ok(toDto(account));
    }

    @GetMapping
    public ResponseEntity<List<BankAccountDto>> getUserBankAccounts(@CurrentUser UUID userId) {
        List<BankAccount> accounts = bankAccountService.getUserBankAccounts(userId);
        return ResponseEntity.ok(accounts.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccountDto> getBankAccount(@PathVariable UUID accountId) {
        BankAccount account = bankAccountService.getBankAccount(accountId);
        return ResponseEntity.ok(toDto(account));
    }

    @PutMapping("/{accountId}/set-default")
    public ResponseEntity<Void> setDefaultPayoutAccount(
            @PathVariable UUID accountId,
            @CurrentUser UUID userId
    ) {
        bankAccountService.setDefaultPayoutAccount(userId, accountId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteBankAccount(
            @PathVariable UUID accountId,
            @CurrentUser UUID userId
    ) {
        bankAccountService.deleteBankAccount(userId, accountId);
        return ResponseEntity.noContent().build();
    }

    private BankAccountDto toDto(BankAccount account) {
        return BankAccountDto.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .bankName(account.getBankName())
                .bankCode(account.getBankCode())
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .isPayoutDefault(account.getIsPayoutDefault())
                .build();
    }
}
