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
import com.gigwave.infrastructure.payments.onepipe.dto.PayoutResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private OnePipeClient onePipeClient;
    
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
    
    @InjectMocks
    private PaymentService paymentService;
    
    private BankAccount testBankAccount;
    private PaymentMandate testMandate;
    private Booking testBooking;
    private UUID userId;
    private UUID bankAccountId;
    private UUID mandateId;
    private UUID bookingId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bankAccountId = UUID.randomUUID();
        mandateId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        
        // Set platform fee configuration using reflection
        ReflectionTestUtils.setField(paymentService, "platformFeeAmount", new BigDecimal("200"));
        ReflectionTestUtils.setField(paymentService, "platformAccountNumber", "0121753572");
        ReflectionTestUtils.setField(paymentService, "platformBankCode", "232");
        ReflectionTestUtils.setField(paymentService, "platformAccountName", "Agbaosi Bolarinwa Minasu");
        ReflectionTestUtils.setField(paymentService, "serverUrl", "http://localhost:8080");
        
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
                .organizerMandateId(mandateId)
                .acceptedAmount(new BigDecimal("75000"))
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .build();
    }
    
    @Test
    void testSetupMandateForOrganizer_Success() {
        // Arrange
        BigDecimal maxAmount = new BigDecimal("1000000");
        
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
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
        
        // Act
        var result = paymentService.setupMandateForOrganizer(userId, bankAccountId, maxAmount);
        
        // Assert
        assertNotNull(result);
        assertEquals("MANDATE_123", result.getMandateRef());
        verify(bankAccountRepository).findById(bankAccountId);
        verify(onePipeClient).setupMandate(any());
        verify(mandateRepository).save(any(PaymentMandate.class));
    }
    
    @Test
    void testSetupMandateForOrganizer_BankAccountNotFound() {
        // Boundary: non-existent bank account
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.setupMandateForOrganizer(userId, bankAccountId, new BigDecimal("1000000")));
    }
    
    @Test
    void testSetupMandateForOrganizer_WrongUser() {
        // Boundary: bank account belongs to different user
        UUID otherUserId = UUID.randomUUID();
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
        
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.setupMandateForOrganizer(otherUserId, bankAccountId, new BigDecimal("1000000")));
    }
    
    @Test
    void testSetupMandateForOrganizer_WithZeroMaxAmount() {
        // Boundary: zero max amount
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
        when(onePipeClient.setupMandate(any())).thenReturn(
                MandateResponse.builder().mandateRef("MANDATE_123").build()
        );
        when(mandateRepository.save(any(PaymentMandate.class))).thenAnswer(invocation -> {
            PaymentMandate mandate = invocation.getArgument(0);
            mandate.setId(mandateId);
            return mandate;
        });
        
        var result = paymentService.setupMandateForOrganizer(userId, bankAccountId, BigDecimal.ZERO);
        
        assertNotNull(result);
    }
    
    @Test
    void testInitiateDebitForBooking_Success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(testMandate));
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
        when(gigRepository.findById(any())).thenReturn(Optional.of(Gig.builder().organizerId(userId).build()));
        
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
        when(bankAccountRepository.findByUserIdAndIsPayoutDefaultTrue(any())).thenReturn(Optional.of(testBankAccount));
        when(onePipeClient.initiatePayout(any())).thenReturn(
                PayoutResponse.builder().transactionRef("PAYOUT_123").build()
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
        // Verify two payouts: one for musician, one for platform
        verify(onePipeClient, times(2)).initiatePayout(any());
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
        // Test musician mandate setup
        BigDecimal maxAmount = new BigDecimal("1000000");
        
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(testBankAccount));
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
        
        var result = paymentService.setupMandateForMusician(userId, bankAccountId, maxAmount);
        
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


