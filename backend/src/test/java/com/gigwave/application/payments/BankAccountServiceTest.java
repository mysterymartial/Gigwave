package com.gigwave.application.payments;

import com.gigwave.domain.payments.BankAccount;
import com.gigwave.infrastructure.persistence.payments.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {
    @Mock
    private BankAccountRepository bankAccountRepository;
    
    @InjectMocks
    private BankAccountService bankAccountService;
    
    private UUID userId;
    private UUID accountId;
    private BankAccount testAccount;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        testAccount = BankAccount.builder()
                .id(accountId)
                .userId(userId)
                .bankName("GTB")
                .bankCode("058")
                .accountNumber("1234567890")
                .accountName("Test User")
                .isPayoutDefault(false)
                .build();
    }
    
    @Test
    void testAddBankAccount_Success() {
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            account.setId(accountId);
            return account;
        });
        
        BankAccount result = bankAccountService.addBankAccount(
                userId, "GTB", "058", "1234567890", "Test User", true);
        
        assertNotNull(result);
        assertTrue(result.getIsPayoutDefault());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }
    
    @Test
    void testAddBankAccount_WithExistingDefault() {
        // Boundary: setting new default when one exists
        BankAccount existingDefault = BankAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .isPayoutDefault(true)
                .build();
        
        when(bankAccountRepository.findByUserIdAndIsPayoutDefaultTrue(userId))
                .thenReturn(Optional.of(existingDefault));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            account.setId(accountId);
            return account;
        });
        
        BankAccount result = bankAccountService.addBankAccount(
                userId, "GTB", "058", "1234567890", "Test User", true);
        
        assertTrue(result.getIsPayoutDefault());
        assertFalse(existingDefault.getIsPayoutDefault()); // Old default should be unset
        verify(bankAccountRepository, times(2)).save(any(BankAccount.class));
    }
    
    @Test
    void testSetDefaultPayoutAccount_Success() {
        BankAccount existingDefault = BankAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .isPayoutDefault(true)
                .build();
        
        when(bankAccountRepository.findByUserIdAndIsPayoutDefaultTrue(userId))
                .thenReturn(Optional.of(existingDefault));
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(testAccount);
        
        bankAccountService.setDefaultPayoutAccount(userId, accountId);
        
        assertTrue(testAccount.getIsPayoutDefault());
        assertFalse(existingDefault.getIsPayoutDefault());
        verify(bankAccountRepository, times(2)).save(any(BankAccount.class));
    }
    
    @Test
    void testSetDefaultPayoutAccount_WrongUser() {
        // Boundary: account belongs to different user
        UUID otherUserId = UUID.randomUUID();
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        
        assertThrows(IllegalArgumentException.class,
                () -> bankAccountService.setDefaultPayoutAccount(otherUserId, accountId));
    }
    
    @Test
    void testDeleteBankAccount_Success() {
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        doNothing().when(bankAccountRepository).delete(testAccount);
        
        bankAccountService.deleteBankAccount(userId, accountId);
        
        verify(bankAccountRepository).delete(testAccount);
    }
    
    @Test
    void testDeleteBankAccount_WrongUser() {
        // Boundary: account belongs to different user
        UUID otherUserId = UUID.randomUUID();
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        
        assertThrows(IllegalArgumentException.class,
                () -> bankAccountService.deleteBankAccount(otherUserId, accountId));
    }
    
    @Test
    void testGetUserBankAccounts_Empty() {
        // Boundary: no bank accounts
        when(bankAccountRepository.findByUserId(userId)).thenReturn(Arrays.asList());
        
        List<BankAccount> result = bankAccountService.getUserBankAccounts(userId);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetUserBankAccounts_Multiple() {
        BankAccount account2 = BankAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
        
        when(bankAccountRepository.findByUserId(userId))
                .thenReturn(Arrays.asList(testAccount, account2));
        
        List<BankAccount> result = bankAccountService.getUserBankAccounts(userId);
        
        assertEquals(2, result.size());
    }
}




