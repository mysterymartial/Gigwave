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
Set via env only; no defaults in production. Use `PLATFORM_SETTLEMENT_ACCOUNT`, `PLATFORM_SETTLEMENT_BANK_CODE`, `PLATFORM_SETTLEMENT_ACCOUNT_NAME` (and `platform.account.*` equivalents). See `application.yml`.

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

#### `createPayoutOnDebitSuccess` (PaymentService)

- Single transfer to musician: `acceptedAmount - Flutterwave charge`. Platform fee remains in settlement.
- Settlement account details come from `PLATFORM_SETTLEMENT_*` env only (no defaults in production).
- Do not hardcode account numbers, bank codes, or contact details in code or config.

---

## 2. Google Maps Integration

### ✅ Completed:
- Created `frontend/src/components/LocationMap.tsx`
- Integrated in `frontend/src/pages/gigs/GigDetailsPage.tsx`
- Added dependency to `frontend/package.json`

### Required Setup:

1. **Create `.env` file in frontend directory:**
```env
VITE_GOOGLE_MAPS_API_KEY=your-google-maps-api-key
```
Get a key from [Google Cloud Console](https://console.cloud.google.com/) (Maps JavaScript API). Never commit real keys.

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
6. **Create .env file** - In frontend directory with `VITE_GOOGLE_MAPS_API_KEY` (use a placeholder in docs; never commit real keys)

---

## Important Notes

- **PaymentService.java has duplicate content** starting at line 397 - needs to be cleaned up
- **Google Maps API key** must be in `frontend/.env` as `VITE_GOOGLE_MAPS_API_KEY` (never commit real keys)
- **Notification service** currently only logs - needs integration with real provider
- **Settlement account** configuration added to `application.yml` but PaymentService needs updates
