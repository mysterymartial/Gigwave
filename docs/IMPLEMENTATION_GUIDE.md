# Implementation Guide - Settlement Account, Google Maps, and Notifications

## Status Summary

### ✅ Completed:
1. **Google Maps Component** - Created `LocationMap.tsx` component
2. **Google Maps Integration** - Integrated in `GigDetailsPage.tsx`
3. **Package.json** - Added `@react-google-maps/api` dependency
4. **Configuration** - Added settlement account configuration in `application.yml`

### ⚠️ Pending Implementation:

## 1. Settlement Account Flow

### Changes Required in `PaymentService.java`:

#### Add Settlement Account Fields (around line 50-60):
```java
@Value("${platform.settlement.number:0121753572}")
private String settlementAccountNumber;

@Value("${platform.settlement.bank-code:232}")
private String settlementBankCode;

@Value("${platform.settlement.name:Agbaosi Bolarinwa Minasu}")
private String settlementAccountName;
```

#### Update `initiateDebitForBooking` (around line 148-160):
```java
// Calculate total amount: acceptedAmount + platform fee (200 Naira)
// Organizer MUST pay extra 200 - this is enforced by adding platform fee to total
BigDecimal totalAmount = booking.getAcceptedAmount().add(platformFeeAmount);

DebitRequest request = DebitRequest.builder()
        .mandateRef(mandate.getMandateRef())
        .amount(totalAmount)
        .narration("Payment for gig booking " + bookingId + " (Gig: ₦" + booking.getAcceptedAmount() + " + Platform fee: ₦" + platformFeeAmount + ")")
        // ... rest of request
```

#### Update `createPayoutOnDebitSuccess` (around line 236-304):
```java
@Transactional
public void createPayoutOnDebitSuccess(Booking booking) {
    // NEW SETTLEMENT ACCOUNT FLOW:
    // 1. Money is debited from organizer (acceptedAmount + 200) → Settlement account receives it
    // 2. From settlement account:
    //    a. Transfer 200 (platform fee) to platform account
    //    b. Transfer acceptedAmount to musician
    
    // Step 1: Transfer platform fee (200) from settlement account to platform account
    PayoutRequest platformFeePayoutRequest = PayoutRequest.builder()
            .accountNumber(platformAccountNumber)
            .bankCode(platformBankCode)
            .accountName(platformAccountName)
            .amount(platformFeeAmount)
            .narration("Platform fee from settlement for gig booking " + booking.getId())
            .callbackUrl(serverUrl + "/api/payments/webhooks/payout")
            .userId(null)
            .email("platform@gigwave.com")
            .phone("09010849782")
            .build();

    PayoutResponse platformFeeResponse = onePipeClient.initiatePayout(platformFeePayoutRequest);

    Payout platformFeePayout = Payout.builder()
            .bookingId(booking.getId())
            .musicianId(null)
            .bankAccountId(null)
            .amount(platformFeeAmount)
            .providerRef(platformFeeResponse.getTransactionRef())
            .status(PayoutStatus.PENDING)
            .attemptedAt(LocalDateTime.now())
            .build();

    payoutRepository.save(platformFeePayout);

    // Step 2: Transfer remaining amount (acceptedAmount) from settlement account to musician
    BankAccount payoutAccount = bankAccountRepository
            .findByUserIdAndIsPayoutDefaultTrue(booking.getMusicianId())
            .orElseThrow(() -> new IllegalStateException("No default payout account set"));

    User musicianUser = userRepository.findById(booking.getMusicianId())
            .orElseThrow(() -> new IllegalArgumentException("Musician user not found"));

    PayoutRequest musicianPayoutRequest = PayoutRequest.builder()
            .accountNumber(payoutAccount.getAccountNumber())
            .bankCode(payoutAccount.getBankCode())
            .accountName(payoutAccount.getAccountName())
            .amount(booking.getAcceptedAmount())
            .narration("Gig payment for booking " + booking.getId() + " (from settlement account)")
            .callbackUrl(serverUrl + "/api/payments/webhooks/payout")
            .userId(musicianUser.getId())
            .email(musicianUser.getEmail())
            .phone(musicianUser.getPhone())
            .build();

    PayoutResponse musicianResponse = onePipeClient.initiatePayout(musicianPayoutRequest);

    Payout musicianPayout = Payout.builder()
            .bookingId(booking.getId())
            .musicianId(booking.getMusicianId())
            .bankAccountId(payoutAccount.getId())
            .amount(booking.getAcceptedAmount())
            .providerRef(musicianResponse.getTransactionRef())
            .status(PayoutStatus.PENDING)
            .attemptedAt(LocalDateTime.now())
            .build();

    payoutRepository.save(musicianPayout);
    
    log.info("Settlement flow initiated for booking {}: {} to platform, {} to musician", 
            booking.getId(), platformFeeAmount, booking.getAcceptedAmount());
}
```

**NOTE**: There appears to be duplicate content in `PaymentService.java` starting at line 397. The duplicate should be removed.

---

## 2. Google Maps Integration

### ✅ Completed:
- Created `frontend/src/components/LocationMap.tsx`
- Integrated in `frontend/src/pages/gigs/GigDetailsPage.tsx`
- Added dependency to `frontend/package.json`

### Required Setup:

1. **Create `.env` file in frontend directory:**
```env
VITE_GOOGLE_MAPS_API_KEY=AIzaSyCQsOi8qGpI1xSC-dsnc2A_MBRi3XcAva8
```

2. **Install dependency:**
```bash
cd frontend
pnpm install
# or
npm install
```

3. **Restart development server** after creating `.env` file

---

## 3. Notification System

### Changes Required:

#### A. Update `NotificationService.java`:

Add methods and implement real notification sending:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    // TODO: Inject SMS/Email/WhatsApp provider (e.g., Twilio, SendGrid)
    // For now, we'll use logging but structure it for easy integration
    
    public void sendNewBidNotification(UUID organizerId, UUID bookingId, BigDecimal acceptedAmount) {
        log.info("Sending new bid notification to organizer: {}, booking: {}, amount: {}", 
                organizerId, bookingId, acceptedAmount);
        // TODO: Send SMS/Email/WhatsApp notification
        // Example: smsService.send(organizerPhone, "New bid received: ₦" + acceptedAmount);
    }
    
    public void sendGigCreatedNotification(UUID gigId, String gigTitle, String location) {
        log.info("Sending gig created notification for gig: {}, title: {}", gigId, gigTitle);
        // TODO: Notify all musicians about new gig
        // Example: pushNotificationService.notifyAllMusicians("New gig: " + gigTitle + " at " + location);
    }
    
    // Keep existing methods but add real implementation
    public void sendBookingAcceptedNotification(UUID musicianId, UUID bookingId) {
        log.info("Sending booking accepted notification to musician: {}, booking: {}", musicianId, bookingId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }
    
    // ... other methods
}
```

#### B. Add Notification When Musician Bids (`BookingService.java`):

Around line 30-48, after saving booking:

```java
@Transactional
public BookingDto musicianAcceptsGig(UUID gigId, UUID musicianId, BigDecimal acceptedAmount) {
    // ... existing code ...
    
    booking = bookingRepository.save(booking);
    
    // NEW: Notify organizer about the bid
    Gig gig = gigRepository.findById(gigId)
            .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
    notificationService.sendNewBidNotification(
        gig.getOrganizerId(), 
        booking.getId(), 
        acceptedAmount
    );
    
    return toDto(booking);
}
```

#### C. Add Notification When Gig is Created (`GigService.java`):

Around line 23-40, after saving gig:

```java
@Transactional
public GigDto createGig(GigDto dto) {
    // ... existing code ...
    
    gig = gigRepository.save(gig);
    
    // NEW: Notify musicians about new gig
    notificationService.sendGigCreatedNotification(
        gig.getId(),
        gig.getTitle(),
        gig.getLocation()
    );
    
    return toDto(gig);
}
```

---

## 4. Test Cases Required

### A. Settlement Account Flow Tests:
- Test that organizer pays acceptedAmount + 200
- Test that platform fee (200) goes to platform account
- Test that acceptedAmount goes to musician
- Test that both payouts are created from settlement account

### B. Google Maps Component Tests:
- Test component renders with valid API key
- Test component shows error when API key missing
- Test "Get Directions" link works correctly
- Test marker displays when coordinates provided

### C. Notification Service Tests:
- Test notification sent when musician bids
- Test notification sent when gig created
- Test notification sent when booking accepted
- Test notification sent when payment succeeds

---

## 5. Documentation Updates

### Files to Update:
1. **README.md** - Add Google Maps setup instructions
2. **API_INTEGRATION.md** - Update payment flow documentation
3. **ENVIRONMENT_SETUP.md** - Add Google Maps API key setup
4. **PRODUCTION_READINESS_FINAL_ASSESSMENT.md** - Update status

---

## Next Steps

1. **Fix PaymentService.java** - Remove duplicates and implement settlement flow
2. **Implement NotificationService** - Add real notification sending (SMS/Email/WhatsApp)
3. **Add notification calls** - In BookingService and GigService
4. **Create test cases** - For all new features
5. **Update documentation** - All relevant docs
6. **Create .env file** - In frontend directory with Google Maps API key

---

## Important Notes

- **PaymentService.java has duplicate content** starting at line 397 - needs to be cleaned up
- **Google Maps API key** must be added to `.env` file in frontend directory
- **Notification service** currently only logs - needs integration with real provider
- **Settlement account** configuration added to `application.yml` but PaymentService needs updates
