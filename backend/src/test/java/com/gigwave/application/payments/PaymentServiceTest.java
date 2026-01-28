package com.gigwave.application.payments;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.bookings.PaymentStatus;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.payments.*;
import com.gigwave.infrastructure.persistence.payments.*;
import com.gigwave.infrastructure.payments.onepipe.OnePipeClient;
import com.gigwave.infrastructure.payments.onepipe.dto.DebitResponse;
import com.gigwave.infrastructure.payments.onepipe.dto.MandateResponse;
import com.gigwave.infrastructure.payments.transfer.TransferClient;
import com.gigwave.infrastructure.payments.transfer.dto.TransferRequest;
import com.gigwave.infrastructure.payments.transfer.dto.TransferResponse;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.application.notifications.NotificationService;
import com.gigwave.domain.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private OnePipeClient onePipeClient;
    
    @Mock
    private TransferClient transferClient;
    
    @Mock
    private PaymentMandateRepository mandateRepository;
    
    @Mock
    private BankAccountRepository bankAccountRepository;
    
    @Mock
    private DebitTransactionRepository debitRepository;
    
    @Mock
    private PayoutRepository payoutRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private GigRepository gigRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private BankAccount testBankAccount;
    private PaymentMandate testMandate;
    private Booking testBooking;
    private UUID userId;
    private UUID musicianId;
    private UUID bankAccountId;
    private UUID mandateId;
    private UUID bookingId;
    private UUID gigId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        musicianId = UUID.randomUUID();
        bankAccountId = UUID.randomUUID();
        mandateId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        gigId = UUID.randomUUID();
        
        // Set platform fee configuration using reflection
        ReflectionTestUtils.setField(paymentService, "platformFeeAmount", new BigDecimal("200"));
        ReflectionTestUtils.setField(paymentService, "settlementAccountNumber", "0000000000");
        ReflectionTestUtils.setField(paymentService, "settlementBankCode", "000");
        ReflectionTestUtils.setField(paymentService, "settlementAccountName", "Test Settlement");
        ReflectionTestUtils.setField(paymentService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(paymentService, "flutterwaveCharge", new BigDecimal("10"));
        
        testBankAccount = BankAccount.builder()
                .id(bankAccountId)
                .userId(userId)
                .bankName("GTB")
                .bankCode("058")
                .accountNumber("1234567890")
                .accountName("Test User")
                .isPayoutDefault(true)
                .build();
        
        testMandate = PaymentMandate.builder()
                .id(mandateId)
                .userId(userId)
                .bankAccountId(bankAccountId)
                .mandateRef("MANDATE_123")
                .status(MandateStatus.ACTIVE)
                .maxAmount(new BigDecimal("1000000"))
                .build();
        
        testBooking = Booking.builder()
                .id(bookingId)
                .gigId(gigId)
                .musicianId(musicianId)
                .organizerMandateId(mandateId)
                .acceptedAmount(new BigDecimal("75000"))
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .build();
    }
    
    @Test
    void testSetupMandateForOrganizer_Success() {
        BigDecimal maxAmount = new BigDecimal("1000000");
        User user = User.builder().id(userId).email("organizer@test.com").phone("08012345678").build();

        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(onePipeClient.setupMandate(any())).thenReturn(
                MandateResponse.builder()
                        .status("pending")
                        .mandateRef("MANDATE_123")
                        .authorizationUrl("https://authorize.url")
                        .build()
        );
        when(mandateRepository.save(any(PaymentMandate.class))).thenAnswer(invocation -> {
            PaymentMandate mandate = invocation.getArgument(0);
            mandate.setId(mandateId);
            return mandate;
        });

        var result = paymentService.setupMandateForOrganizer(userId, bankAccountId, maxAmount, null);

        assertNotNull(result);
        assertEquals("MANDATE_123", result.getMandateRef());
        verify(bankAccountRepository).findById(bankAccountId);
        verify(userRepository).findById(userId);
        verify(onePipeClient).setupMandate(any());
        verify(mandateRepository).save(any(PaymentMandate.class));
    }
    
    @Test
    void testSetupMandateForOrganizer_BankAccountNotFound() {
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.setupMandateForOrganizer(userId, bankAccountId, new BigDecimal("1000000"), null));
    }

    @Test
    void testSetupMandateForOrganizer_WrongUser() {
        UUID otherUserId = UUID.randomUUID();
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.setupMandateForOrganizer(otherUserId, bankAccountId, new BigDecimal("1000000"), null));
    }

    @Test
    void testSetupMandateForOrganizer_WithZeroMaxAmount() {
        User user = User.builder().id(userId).email("o@test.com").phone("08012345678").build();
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(onePipeClient.setupMandate(any())).thenReturn(
                MandateResponse.builder().mandateRef("MANDATE_123").build()
        );
        when(mandateRepository.save(any(PaymentMandate.class))).thenAnswer(invocation -> {
            PaymentMandate mandate = invocation.getArgument(0);
            mandate.setId(mandateId);
            return mandate;
        });

        var result = paymentService.setupMandateForOrganizer(userId, bankAccountId, BigDecimal.ZERO, null);

        assertNotNull(result);
    }
    
    @Test
    void testInitiateDebitForBooking_Success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(testMandate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).email("organizer@test.com").phone("08012345678").build()));
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
        when(debitRepository.findByBookingId(bookingId)).thenReturn(List.of());
        when(debitRepository.findByMandateId(mandateId)).thenReturn(List.of());
        when(onePipeClient.initiateDebit(any())).thenReturn(
                DebitResponse.builder()
                        .status("pending")
                        .transactionRef("DEBIT_123")
                        .build()
        );
        when(debitRepository.save(any(DebitTransaction.class))).thenAnswer(invocation -> {
            DebitTransaction debit = invocation.getArgument(0);
            debit.setId(UUID.randomUUID());
            return debit;
        });
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(Gig.builder().id(gigId).organizerId(userId).build()));
        doNothing().when(notificationService).sendPaymentInitiatedNotification(any(UUID.class), any(UUID.class));
        
        var result = paymentService.initiateDebitForBooking(bookingId);
        
        assertNotNull(result);
        assertEquals("DEBIT_123", result.getTransactionRef());
        verify(onePipeClient).initiateDebit(argThat(request -> {
            // Verify that platform fee (200) is added to accepted amount (75000) = 75200
            return request.getAmount().compareTo(new BigDecimal("75200")) == 0;
        }));
        verify(debitRepository).save(any(DebitTransaction.class));
    }
    
    @Test
    void testInitiateDebitForBooking_NoMandate() {
        // Boundary: booking without mandate
        testBooking.setOrganizerMandateId(null);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        
        assertThrows(IllegalStateException.class,
                () -> paymentService.initiateDebitForBooking(bookingId));
    }
    
    @Test
    void testInitiateDebitForBooking_MandateNotActive() {
        // Boundary: mandate not active
        testMandate.setStatus(MandateStatus.PENDING);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(testMandate));
        
        assertThrows(IllegalStateException.class,
                () -> paymentService.initiateDebitForBooking(bookingId));
    }
    
    @Test
    void testHandleDebitWebhook_Success() {
        // Arrange
        String transactionRef = "DEBIT_123";
        DebitTransaction debit = DebitTransaction.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .status(DebitStatus.PENDING)
                .providerRef(transactionRef)
                .build();
        
        when(debitRepository.findByProviderRef(transactionRef)).thenReturn(Optional.of(debit));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(debitRepository.save(any(DebitTransaction.class))).thenReturn(debit);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        // Create a separate bank account for musician
        BankAccount musicianBankAccount = BankAccount.builder()
                .id(UUID.randomUUID())
                .userId(musicianId)
                .bankName("GTB")
                .bankCode("058")
                .accountNumber("9876543210")
                .accountName("Musician User")
                .isPayoutDefault(true)
                .build();
        
        when(bankAccountRepository.findByUserIdAndIsPayoutDefaultTrue(musicianId)).thenReturn(Optional.of(musicianBankAccount));
        when(userRepository.findById(musicianId)).thenReturn(Optional.of(User.builder().id(musicianId).email("musician@test.com").phone("08098765432").build()));
        when(payoutRepository.findByBookingId(bookingId)).thenReturn(List.of());
        when(transferClient.initiateTransfer(any(TransferRequest.class))).thenReturn(
                TransferResponse.builder()
                        .status("pending")
                        .transactionRef("TRANSFER_123")
                        .message("Transfer initiated")
                        .provider("flutterwave")
                        .build()
        );
        when(payoutRepository.save(any(Payout.class))).thenAnswer(invocation -> {
            Payout payout = invocation.getArgument(0);
            payout.setId(UUID.randomUUID());
            return payout;
        });
        
        // Act
        paymentService.handleDebitWebhook(transactionRef, "success");
        
        // Assert
        verify(debitRepository).findByProviderRef(transactionRef);
        verify(bookingRepository).save(any(Booking.class));
        // Verify one transfer: to musician (platform fee stays in settlement account)
        verify(transferClient, times(1)).initiateTransfer(any(TransferRequest.class));
        assertEquals(DebitStatus.SUCCESS, debit.getStatus());
        assertEquals(PaymentStatus.DEBIT_SUCCESS, testBooking.getPaymentStatus());
    }
    
    @Test
    void testHandleDebitWebhook_Failed() {
        // Boundary: failed debit
        String transactionRef = "DEBIT_123";
        DebitTransaction debit = DebitTransaction.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .status(DebitStatus.PENDING)
                .providerRef(transactionRef)
                .build();
        
        when(debitRepository.findByProviderRef(transactionRef)).thenReturn(Optional.of(debit));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(debitRepository.save(any(DebitTransaction.class))).thenReturn(debit);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(Gig.builder().id(gigId).organizerId(userId).build()));
        doNothing().when(notificationService).sendPaymentFailedNotification(any(UUID.class), any(UUID.class));
        
        paymentService.handleDebitWebhook(transactionRef, "failed");
        
        assertEquals(DebitStatus.FAILED, debit.getStatus());
        assertEquals(PaymentStatus.DEBIT_FAILED, testBooking.getPaymentStatus());
    }
    
    @Test
    void testHandleDebitWebhook_InvalidTransactionRef() {
        // Boundary: invalid transaction reference
        String invalidRef = "INVALID_123";
        when(debitRepository.findByProviderRef(invalidRef)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.handleDebitWebhook(invalidRef, "success"));
    }
    
    @Test
    void testSetupMandateForMusician_Success() {
        BigDecimal maxAmount = new BigDecimal("1000000");
        User user = User.builder().id(userId).email("musician@test.com").phone("08087654321").build();

        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(onePipeClient.setupMandate(any())).thenReturn(
                MandateResponse.builder()
                        .status("pending")
                        .mandateRef("MANDATE_MUSICIAN_123")
                        .authorizationUrl("https://authorize.url")
                        .build()
        );
        when(mandateRepository.save(any(PaymentMandate.class))).thenAnswer(invocation -> {
            PaymentMandate mandate = invocation.getArgument(0);
            mandate.setId(mandateId);
            return mandate;
        });

        var result = paymentService.setupMandateForMusician(userId, bankAccountId, maxAmount, null);

        assertNotNull(result);
        assertEquals("MANDATE_MUSICIAN_123", result.getMandateRef());
        verify(onePipeClient).setupMandate(any());
        verify(mandateRepository).save(any(PaymentMandate.class));
    }
    
    @Test
    void testGetPlatformFeeInfo() {
        var result = paymentService.getPlatformFeeInfo();
        
        assertNotNull(result);
        assertEquals(new BigDecimal("200"), result.get("platformFeeAmount"));
        assertEquals("NGN", result.get("currency"));
        assertTrue(result.get("description").toString().contains("200"));
    }
}
