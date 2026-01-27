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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMandateRepository mandateRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private DebitTransactionRepository debitRepository;

    @Mock
    private OnePipeClient onePipeClient;

    @InjectMocks
    private AdminService adminService;

    private UUID adminId;
    private UUID musicianId;
    private UUID eventOwnerId;
    private User musician;
    private User eventOwner;
    private PaymentMandate activeMandate;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        musicianId = UUID.randomUUID();
        eventOwnerId = UUID.randomUUID();

        musician = User.builder()
                .id(musicianId)
                .phone("08012345678")
                .email("musician@test.com")
                .role(UserRole.MUSICIAN)
                .isDisabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        eventOwner = User.builder()
                .id(eventOwnerId)
                .phone("08087654321")
                .email("organizer@test.com")
                .role(UserRole.EVENT_OWNER)
                .isDisabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        bankAccount = BankAccount.builder()
                .id(UUID.randomUUID())
                .userId(musicianId)
                .bankName("GTBank")
                .bankCode("058")
                .accountNumber("0123456789")
                .accountName("John Doe")
                .build();

        activeMandate = PaymentMandate.builder()
                .id(UUID.randomUUID())
                .userId(musicianId)
                .bankAccountId(bankAccount.getId())
                .provider("onepipe")
                .mandateRef("MANDATE_REF_123")
                .status(MandateStatus.ACTIVE)
                .maxAmount(new BigDecimal("100000"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetAllCustomers_ReturnsMusiciansAndEventOwners() {
        // Given
        when(userRepository.findByRole(UserRole.MUSICIAN))
                .thenReturn(Collections.singletonList(musician));
        when(userRepository.findByRole(UserRole.EVENT_OWNER))
                .thenReturn(Collections.singletonList(eventOwner));
        when(mandateRepository.findByUserId(musicianId))
                .thenReturn(Collections.singletonList(activeMandate));
        when(mandateRepository.findByUserId(eventOwnerId))
                .thenReturn(Collections.emptyList());

        // When
        List<AdminService.CustomerInfo> customers = adminService.getAllCustomers();

        // Then
        assertEquals(2, customers.size());
        assertTrue(customers.stream().anyMatch(c -> c.getId().equals(musicianId)));
        assertTrue(customers.stream().anyMatch(c -> c.getId().equals(eventOwnerId)));
        verify(userRepository).findByRole(UserRole.MUSICIAN);
        verify(userRepository).findByRole(UserRole.EVENT_OWNER);
    }

    @Test
    void testGetAllCustomers_ExcludesAdminUsers() {
        // Given
        User admin = User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findByRole(UserRole.MUSICIAN))
                .thenReturn(Collections.singletonList(musician));
        when(userRepository.findByRole(UserRole.EVENT_OWNER))
                .thenReturn(Collections.singletonList(eventOwner));
        when(mandateRepository.findByUserId(any(UUID.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<AdminService.CustomerInfo> customers = adminService.getAllCustomers();

        // Then
        assertFalse(customers.stream().anyMatch(c -> c.getRole() == UserRole.ADMIN));
    }

    @Test
    void testGetCustomerById_ReturnsCustomerInfo() {
        // Given
        when(userRepository.findById(musicianId))
                .thenReturn(Optional.of(musician));
        when(mandateRepository.findByUserId(musicianId))
                .thenReturn(Collections.singletonList(activeMandate));

        // When
        AdminService.CustomerInfo customer = adminService.getCustomerById(musicianId);

        // Then
        assertNotNull(customer);
        assertEquals(musicianId, customer.getId());
        assertEquals(UserRole.MUSICIAN, customer.getRole());
        assertTrue(customer.getHasActiveMandate());
        assertEquals("MANDATE_REF_123", customer.getMandateRef());
    }

    @Test
    void testGetCustomerById_ThrowsException_WhenCustomerNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.getCustomerById(nonExistentId);
        });
    }

    @Test
    void testGetCustomerById_ThrowsException_WhenUserIsAdmin() {
        // Given
        User admin = User.builder()
                .id(adminId)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(admin));

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.getCustomerById(adminId);
        });
    }

    @Test
    void testDebitCustomer_Success() {
        // Given
        BigDecimal amount = new BigDecimal("5000");
        String reason = "Fraudulent activity detected";
        DebitResponse debitResponse = DebitResponse.builder()
                .status("pending")
                .transactionRef("TXN_123")
                .message("Debit initiated")
                .build();

        when(userRepository.findById(musicianId))
                .thenReturn(Optional.of(musician));
        when(mandateRepository.findByUserId(musicianId))
                .thenReturn(Collections.singletonList(activeMandate));
        when(bankAccountRepository.findById(bankAccount.getId()))
                .thenReturn(Optional.of(bankAccount));
        when(onePipeClient.initiateDebit(any(DebitRequest.class)))
                .thenReturn(debitResponse);
        when(debitRepository.save(any(DebitTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AdminService.AdminDebitResult result = adminService.debitCustomer(
                musicianId, amount, reason, adminId);

        // Then
        assertNotNull(result);
        assertEquals(musicianId, result.getCustomerId());
        assertEquals(amount, result.getAmount());
        assertEquals("pending", result.getStatus());
        assertEquals("TXN_123", result.getTransactionRef());

        verify(onePipeClient).initiateDebit(any(DebitRequest.class));
        verify(debitRepository).save(any(DebitTransaction.class));
    }

    @Test
    void testDebitCustomer_ThrowsException_WhenCustomerNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.debitCustomer(nonExistentId, new BigDecimal("1000"), "Test", adminId);
        });
    }

    @Test
    void testDebitCustomer_ThrowsException_WhenCustomerIsAdmin() {
        // Given
        User admin = User.builder()
                .id(adminId)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(admin));

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.debitCustomer(adminId, new BigDecimal("1000"), "Test", adminId);
        });
    }

    @Test
    void testDebitCustomer_ThrowsException_WhenAccountIsDisabled() {
        // Given
        musician.setIsDisabled(true);
        when(userRepository.findById(musicianId))
                .thenReturn(Optional.of(musician));

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            adminService.debitCustomer(musicianId, new BigDecimal("1000"), "Test", adminId);
        });
    }

    @Test
    void testDebitCustomer_ThrowsException_WhenNoActiveMandate() {
        // Given
        when(userRepository.findById(musicianId))
                .thenReturn(Optional.of(musician));
        when(mandateRepository.findByUserId(musicianId))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            adminService.debitCustomer(musicianId, new BigDecimal("1000"), "Test", adminId);
        });
    }

    @Test
    void testDebitCustomer_ThrowsException_WhenBankAccountNotFound() {
        // Given
        when(userRepository.findById(musicianId))
                .thenReturn(Optional.of(musician));
        when(mandateRepository.findByUserId(musicianId))
                .thenReturn(Collections.singletonList(activeMandate));
        when(bankAccountRepository.findById(bankAccount.getId()))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            adminService.debitCustomer(musicianId, new BigDecimal("1000"), "Test", adminId);
        });
    }

    @Test
    void testDebitCustomer_CreatesDebitTransactionWithoutBookingId() {
        // Given
        BigDecimal amount = new BigDecimal("3000");
        String reason = "Chargeback";
        DebitResponse debitResponse = DebitResponse.builder()
                .status("pending")
                .transactionRef("TXN_456")
                .message("Debit initiated")
                .build();

        when(userRepository.findById(eventOwnerId))
                .thenReturn(Optional.of(eventOwner));
        when(mandateRepository.findByUserId(eventOwnerId))
                .thenReturn(Collections.singletonList(activeMandate));
        when(bankAccountRepository.findById(bankAccount.getId()))
                .thenReturn(Optional.of(bankAccount));
        when(onePipeClient.initiateDebit(any(DebitRequest.class)))
                .thenReturn(debitResponse);
        when(debitRepository.save(any(DebitTransaction.class)))
                .thenAnswer(invocation -> {
                    DebitTransaction debit = invocation.getArgument(0);
                    assertNull(debit.getBookingId(), "Admin debits should not have booking ID");
                    return debit;
                });

        // When
        adminService.debitCustomer(eventOwnerId, amount, reason, adminId);

        // Then
        verify(debitRepository).save(argThat(debit ->
                debit.getBookingId() == null &&
                debit.getAmount().equals(amount) &&
                debit.getMandateId().equals(activeMandate.getId())
        ));
    }

    @Test
    void testDebitCustomer_WorksForBothMusicianAndEventOwner() {
        // Given
        BigDecimal amount = new BigDecimal("2000");
        String reason = "Fraudulent activity";
        DebitResponse debitResponse = DebitResponse.builder()
                .status("pending")
                .transactionRef("TXN_789")
                .message("Debit initiated")
                .build();

        // Test with musician
        when(userRepository.findById(musicianId))
                .thenReturn(Optional.of(musician));
        when(mandateRepository.findByUserId(musicianId))
                .thenReturn(Collections.singletonList(activeMandate));
        when(bankAccountRepository.findById(bankAccount.getId()))
                .thenReturn(Optional.of(bankAccount));
        when(onePipeClient.initiateDebit(any(DebitRequest.class)))
                .thenReturn(debitResponse);
        when(debitRepository.save(any(DebitTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AdminService.AdminDebitResult musicianResult = adminService.debitCustomer(
                musicianId, amount, reason, adminId);

        // Then
        assertNotNull(musicianResult);
        assertEquals(musicianId, musicianResult.getCustomerId());

        // Test with event owner
        PaymentMandate eventOwnerMandate = PaymentMandate.builder()
                .id(UUID.randomUUID())
                .userId(eventOwnerId)
                .bankAccountId(bankAccount.getId())
                .provider("onepipe")
                .mandateRef("MANDATE_REF_456")
                .status(MandateStatus.ACTIVE)
                .maxAmount(new BigDecimal("50000"))
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(eventOwnerId))
                .thenReturn(Optional.of(eventOwner));
        when(mandateRepository.findByUserId(eventOwnerId))
                .thenReturn(Collections.singletonList(eventOwnerMandate));

        AdminService.AdminDebitResult eventOwnerResult = adminService.debitCustomer(
                eventOwnerId, amount, reason, adminId);

        // Then
        assertNotNull(eventOwnerResult);
        assertEquals(eventOwnerId, eventOwnerResult.getCustomerId());
    }
}
