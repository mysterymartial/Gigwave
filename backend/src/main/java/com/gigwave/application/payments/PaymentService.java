package com.gigwave.application.payments;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.bookings.PaymentStatus;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.payments.*;
import com.gigwave.domain.payments.DebitStatus;
import com.gigwave.domain.payments.PayoutStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.payments.BankAccountRepository;
import com.gigwave.infrastructure.persistence.payments.DebitTransactionRepository;
import com.gigwave.infrastructure.persistence.payments.PaymentMandateRepository;
import com.gigwave.infrastructure.persistence.payments.PayoutRepository;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.infrastructure.payments.onepipe.OnePipeClient;
import com.gigwave.infrastructure.payments.onepipe.dto.*;
import com.gigwave.infrastructure.payments.transfer.TransferClient;
import com.gigwave.infrastructure.payments.transfer.dto.TransferRequest;
import com.gigwave.infrastructure.payments.transfer.dto.TransferResponse;
import com.gigwave.application.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final OnePipeClient onePipeClient;
    private final TransferClient transferClient;
    private final PaymentMandateRepository mandateRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DebitTransactionRepository debitRepository;
    private final PayoutRepository payoutRepository;
    private final BookingRepository bookingRepository;
    private final GigRepository gigRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${platform.fee.amount:200}")
    private BigDecimal platformFeeAmount;

    @Value("${platform.settlement.number:6977519876}")
    private String settlementAccountNumber;

    @Value("${platform.settlement.bank-code:070}")
    private String settlementBankCode;

    @Value("${platform.settlement.name:Agbaosi Bolarinwa Minasu}")
    private String settlementAccountName;

    @Value("${flutterwave.transfer.charge:10}")
    private BigDecimal flutterwaveCharge;

    @Transactional
    public MandateResponse setupMandateForOrganizer(UUID userId, UUID bankAccountId, BigDecimal maxAmount) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        if (!bankAccount.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bank account does not belong to user");
        }

        MandateRequest request = MandateRequest.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .bankCode(bankAccount.getBankCode())
                .accountName(bankAccount.getAccountName())
                .maxAmount(maxAmount)
                .callbackUrl(serverUrl + "/api/payments/webhooks/mandate")
                .build();

        MandateResponse response = onePipeClient.setupMandate(request);

        PaymentMandate mandate = PaymentMandate.builder()
                .userId(userId)
                .bankAccountId(bankAccountId)
                .provider("onepipe")
                .mandateRef(response.getMandateRef())
                .status(MandateStatus.PENDING)
                .maxAmount(maxAmount)
                .build();

        mandateRepository.save(mandate);
        return response;
    }

    @Transactional
    public MandateResponse setupMandateForMusician(UUID userId, UUID bankAccountId, BigDecimal maxAmount) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        if (!bankAccount.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bank account does not belong to user");
        }

        MandateRequest request = MandateRequest.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .bankCode(bankAccount.getBankCode())
                .accountName(bankAccount.getAccountName())
                .maxAmount(maxAmount)
                .callbackUrl(serverUrl + "/api/payments/webhooks/mandate")
                .build();

        MandateResponse response = onePipeClient.setupMandate(request);

        PaymentMandate mandate = PaymentMandate.builder()
                .userId(userId)
                .bankAccountId(bankAccountId)
                .provider("onepipe")
                .mandateRef(response.getMandateRef())
                .status(MandateStatus.PENDING)
                .maxAmount(maxAmount)
                .build();

        mandateRepository.save(mandate);
        return response;
    }

    @Transactional
    public DebitResponse initiateDebitForBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getOrganizerMandateId() == null) {
            throw new IllegalStateException("No mandate set for this booking");
        }

        PaymentMandate mandate = mandateRepository.findById(booking.getOrganizerMandateId())
                .orElseThrow(() -> new IllegalArgumentException("Mandate not found"));

        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            throw new IllegalStateException("Mandate is not active");
        }

        // Get organizer user and bank account for customer information
        User organizerUser = userRepository.findById(mandate.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Organizer user not found"));
        BankAccount organizerBankAccount = bankAccountRepository.findById(mandate.getBankAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Organizer bank account not found"));

        // Calculate total amount: acceptedAmount + platform fee (200 Naira)
        // Organizer MUST pay extra 200 - this is enforced by adding platform fee to total
        BigDecimal totalAmount = booking.getAcceptedAmount().add(platformFeeAmount);

        DebitRequest request = DebitRequest.builder()
                .mandateRef(mandate.getMandateRef())
                .amount(totalAmount)
                .narration("Payment for gig booking " + bookingId + " (Gig: ₦" + booking.getAcceptedAmount() + " + Platform fee: ₦" + platformFeeAmount + ")")
                .callbackUrl(serverUrl + "/api/payments/webhooks/debit")
                .userId(organizerUser.getId())
                .email(organizerUser.getEmail())
                .phone(organizerUser.getPhone())
                .accountName(organizerBankAccount.getAccountName())
                .build();

        DebitResponse response = onePipeClient.initiateDebit(request);

        // Handle WaitingForOTP status
        if ("WaitingForOTP".equalsIgnoreCase(response.getStatus()) || "waiting_for_otp".equalsIgnoreCase(response.getStatus())) {
            // Store OTP reference for later validation
            DebitTransaction debit = DebitTransaction.builder()
                    .bookingId(bookingId)
                    .mandateId(mandate.getId())
                    .amount(totalAmount)
                    .providerRef(response.getTransactionRef())
                    .status(DebitStatus.PENDING) // Keep as pending until OTP is validated
                    .attemptedAt(LocalDateTime.now())
                    .build();
            debitRepository.save(debit);
            booking.setPaymentStatus(PaymentStatus.DEBIT_PENDING);
            bookingRepository.save(booking);
            
            // Return response indicating OTP is required
            return response;
        }

        DebitTransaction debit = DebitTransaction.builder()
                .bookingId(bookingId)
                .mandateId(mandate.getId())
                .amount(totalAmount) // Store total amount debited
                .providerRef(response.getTransactionRef())
                .status(DebitStatus.PENDING)
                .attemptedAt(LocalDateTime.now())
                .build();

        debitRepository.save(debit);
        booking.setPaymentStatus(PaymentStatus.DEBIT_PENDING);
        bookingRepository.save(booking);

        Gig gig = gigRepository.findById(booking.getGigId())
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
        notificationService.sendPaymentInitiatedNotification(gig.getOrganizerId(), booking.getId());

        return response;
    }

    @Transactional
    public void handleDebitWebhook(String transactionRef, String status) {
        DebitTransaction debit = debitRepository.findByProviderRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Debit transaction not found"));

        if ("success".equalsIgnoreCase(status)) {
            debit.setStatus(DebitStatus.SUCCESS);
            debit.setCompletedAt(LocalDateTime.now());

            Booking booking = bookingRepository.findById(debit.getBookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            booking.setPaymentStatus(PaymentStatus.DEBIT_SUCCESS);
            bookingRepository.save(booking);

            // Trigger payout
            createPayoutOnDebitSuccess(booking);
        } else {
            debit.setStatus(DebitStatus.FAILED);
            debit.setCompletedAt(LocalDateTime.now());

            Booking booking = bookingRepository.findById(debit.getBookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            booking.setPaymentStatus(PaymentStatus.DEBIT_FAILED);
            bookingRepository.save(booking);
            
            Gig gig = gigRepository.findById(booking.getGigId())
                    .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
            notificationService.sendPaymentFailedNotification(gig.getOrganizerId(), booking.getId());
        }

        debitRepository.save(debit);
    }

    @Transactional
    public void createPayoutOnDebitSuccess(Booking booking) {
        // SETTLEMENT ACCOUNT FLOW (Settlement account IS Flutterwave account):
        // 1. Money is debited from organizer (acceptedAmount + ₦200) → Settlement account (6977519876) receives it
        // 2. From settlement account:
        //    - Transfer (acceptedAmount - Flutterwave charge) to musician
        //    - Flutterwave automatically deducts their charge (₦10-15) from settlement account balance
        //    - Platform keeps: exactly ₦200 (guaranteed)
        //
        // Calculation:
        // Settlement receives: acceptedAmount + ₦200
        // Transfer to musician: acceptedAmount - Flutterwave charge
        // Flutterwave charges settlement: Flutterwave charge (for the transfer)
        // Settlement remaining: (acceptedAmount + ₦200) - (acceptedAmount - Flutterwave charge) - Flutterwave charge = ₦200
        
        BankAccount payoutAccount = bankAccountRepository
                .findByUserIdAndIsPayoutDefaultTrue(booking.getMusicianId())
                .orElseThrow(() -> new IllegalStateException("No default payout account set"));

        User musicianUser = userRepository.findById(booking.getMusicianId())
                .orElseThrow(() -> new IllegalArgumentException("Musician user not found"));

        // Calculate amount to transfer: acceptedAmount - Flutterwave charge
        // This ensures exactly ₦200 remains in settlement account after Flutterwave takes their fee
        BigDecimal amountToTransfer = booking.getAcceptedAmount().subtract(flutterwaveCharge);
        
        if (amountToTransfer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Amount to transfer after Flutterwave charges is zero or negative. Accepted amount: " + 
                    booking.getAcceptedAmount() + ", Flutterwave charge: " + flutterwaveCharge);
        }

        TransferRequest musicianTransferRequest = TransferRequest.builder()
                .accountNumber(payoutAccount.getAccountNumber())
                .bankCode(payoutAccount.getBankCode())
                .accountName(payoutAccount.getAccountName())
                .amount(amountToTransfer) // acceptedAmount - Flutterwave charge (to guarantee exactly ₦200 remains)
                .narration("Gig payment for booking " + booking.getId() + " (from settlement account 6977519876, Flutterwave charge: ₦" + flutterwaveCharge + " deducted)")
                .callbackUrl(serverUrl + "/api/payments/webhooks/payout")
                .userId(musicianUser.getId())
                .email(musicianUser.getEmail())
                .phone(musicianUser.getPhone())
                .build();

        TransferResponse musicianResponse = transferClient.initiateTransfer(musicianTransferRequest);

        Payout musicianPayout = Payout.builder()
                .bookingId(booking.getId())
                .musicianId(booking.getMusicianId())
                .bankAccountId(payoutAccount.getId())
                .amount(amountToTransfer) // Store actual amount transferred
                .providerRef(musicianResponse.getTransactionRef())
                .status(PayoutStatus.PENDING)
                .attemptedAt(LocalDateTime.now())
                .build();

        payoutRepository.save(musicianPayout);
        
        // Platform keeps: exactly ₦200 in settlement account
        // Calculation: (acceptedAmount + ₦200) - (acceptedAmount - Flutterwave charge) - Flutterwave charge = ₦200
        
        log.info("Settlement flow initiated for booking {}: Transferring {} to musician (acceptedAmount: {} - Flutterwave charge: {}). Platform keeps exactly ₦{} in settlement account {} after Flutterwave deducts their charge", 
                booking.getId(), amountToTransfer, booking.getAcceptedAmount(), flutterwaveCharge, platformFeeAmount, settlementAccountNumber);
    }

    @Transactional
    public void handlePayoutWebhook(String transactionRef, String status) {
        Payout payout = payoutRepository.findByProviderRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found"));

        if ("success".equalsIgnoreCase(status)) {
            payout.setStatus(PayoutStatus.SUCCESS);
            payout.setCompletedAt(LocalDateTime.now());

            Booking booking = bookingRepository.findById(payout.getBookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            booking.setPaymentStatus(PaymentStatus.PAID_OUT);
            booking.setCompletedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            notificationService.sendPaymentSuccessNotification(payout.getMusicianId(), payout.getBookingId());
        } else {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setCompletedAt(LocalDateTime.now());
        }

        payoutRepository.save(payout);
    }

    @Transactional
    public void handleMandateWebhook(String mandateRef, String status) {
        PaymentMandate mandate = mandateRepository.findByMandateRef(mandateRef)
                .orElseThrow(() -> new IllegalArgumentException("Mandate not found"));

        if ("active".equalsIgnoreCase(status)) {
            mandate.setStatus(MandateStatus.ACTIVE);
        } else if ("suspended".equalsIgnoreCase(status)) {
            mandate.setStatus(MandateStatus.SUSPENDED);
        } else if ("revoked".equalsIgnoreCase(status)) {
            mandate.setStatus(MandateStatus.REVOKED);
        }

        mandateRepository.save(mandate);
    }

    public BankListResponse getSupportedBanks() {
        return onePipeClient.getSupportedBanks();
    }

    public Map<String, Object> getPlatformFeeInfo() {
        return Map.of(
                "platformFeeAmount", platformFeeAmount,
                "currency", "NGN",
                "description", "A fixed platform fee of ₦" + platformFeeAmount + " is charged per successful gig payment. This fee is added to the gig amount when the organizer confirms payment."
        );
    }
    
    public boolean verifyWebhookSignature(String payload, String signature) {
        return onePipeClient.verifyWebhookSignature(payload, signature);
    }
    
    @Transactional
    public DebitResponse validateOtpForBooking(UUID bookingId, String otp) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        // Find the pending debit transaction for this booking
        DebitTransaction debit = debitRepository.findByBookingId(bookingId)
                .stream()
                .filter(d -> d.getStatus() == DebitStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pending debit transaction found for this booking"));
        
        // Validate OTP with OnePipe
        DebitResponse response = onePipeClient.validateOtp(debit.getProviderRef(), otp);
        
        // Update transaction status based on response
        if ("Successful".equalsIgnoreCase(response.getStatus()) || "success".equalsIgnoreCase(response.getStatus())) {
            debit.setStatus(DebitStatus.SUCCESS);
            debit.setCompletedAt(LocalDateTime.now());
            booking.setPaymentStatus(PaymentStatus.DEBIT_SUCCESS);
            bookingRepository.save(booking);
            
            // Trigger payout
            createPayoutOnDebitSuccess(booking);
        } else {
            debit.setStatus(DebitStatus.FAILED);
            debit.setCompletedAt(LocalDateTime.now());
            booking.setPaymentStatus(PaymentStatus.DEBIT_FAILED);
            bookingRepository.save(booking);
        }
        
        debitRepository.save(debit);
        return response;
    }
}
