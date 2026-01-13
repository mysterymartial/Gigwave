# Integration

## OnePipe API Integration

### API Structure

OnePipe API is integrated via `OnePipeClient` interface with implementation `OnePipeClientImpl`. The client handles all communication with OnePipe payment gateway.

**Interface Location:** `backend/src/main/java/com/gigwave/infrastructure/payments/onepipe/OnePipeClient.java`

**Implementation Location:** `backend/src/main/java/com/gigwave/infrastructure/payments/onepipe/OnePipeClientImpl.java`

**Service Usage:** `backend/src/main/java/com/gigwave/application/payments/PaymentService.java`

### API Methods

#### 1. Setup Mandate (`setupMandate`)

**Purpose:** Create direct debit mandate for user bank account authorization.

**OnePipe Request Type:** `setup_mandate`

**Request Structure:**
```json
{
  "request_ref": "REQ_timestamp",
  "request_type": "setup_mandate",
  "auth": {
    "type": "bank.account",
    "secure": "account_number",
    "auth_provider": "sandbox|production"
  },
  "transaction": {
    "mock_mode": "live",
    "transaction_ref": "TXN_timestamp",
    "transaction_desc": "Direct debit mandate setup",
    "amount": 2000000,
    "customer": {
      "customer_ref": "user_uuid",
      "firstname": "John",
      "surname": "Doe",
      "email": "user@example.com",
      "mobile_no": "08123456789"
    },
    "details": {
      "destination_account": "0123456789",
      "destination_bank_code": "058",
      "max_amount": 2000000
    },
    "meta": {
      "callback_url": "http://localhost:8080/api/payments/webhooks/mandate"
    }
  }
}
```

**Where It's Used:**
- `PaymentService.setupMandateForOrganizer()` - Called when organizer sets up payment mandate
- `PaymentService.setupMandateForMusician()` - Called when musician sets up payout mandate
- `PaymentController.setupOrganizerMandate()` - REST endpoint: `POST /api/payments/mandate/setup/organizer`
- `PaymentController.setupMusicianMandate()` - REST endpoint: `POST /api/payments/mandate/setup/musician`

**Business Flow:**
1. User adds bank account via `BankAccountController`
2. User calls mandate setup endpoint with `bankAccountId` and `maxAmount`
3. `PaymentService` fetches bank account details and user information
4. Creates `MandateRequest` with account details, user info, and callback URL
5. Calls `onePipeClient.setupMandate(request)`
6. OnePipe returns `mandateRef` and `authorizationUrl`
7. System stores mandate reference in `PaymentMandate` entity with status `PENDING`
8. User redirected to `authorizationUrl` to authorize mandate
9. OnePipe sends webhook to `/api/payments/webhooks/mandate` with mandate status
10. System updates mandate status to `ACTIVE` when authorized

**Response:** Returns `MandateResponse` with `status`, `mandateRef`, `authorizationUrl`, and `message`

---

#### 2. Initiate Debit (`initiateDebit`)

**Purpose:** Debit money from organizer's bank account using mandate reference.

**OnePipe Request Type:** `collect`

**Request Structure:**
```json
{
  "request_ref": "REQ_timestamp",
  "request_type": "collect",
  "auth": {
    "type": "bank.account",
    "secure": "mandate_reference",
    "auth_provider": "sandbox|production"
  },
  "transaction": {
    "mock_mode": "live",
    "transaction_ref": "TXN_timestamp",
    "transaction_desc": "Payment for gig booking",
    "amount": 2020000,
    "customer": {
      "customer_ref": "user_uuid",
      "firstname": "John",
      "surname": "Doe",
      "email": "user@example.com",
      "mobile_no": "08123456789"
    },
    "details": null,
    "meta": {
      "callback_url": "http://localhost:8080/api/payments/webhooks/debit"
    }
  }
}
```

**Amount Calculation:** `totalAmount = acceptedAmount + platformFeeAmount` (e.g., ₦20,000 + ₦200 = ₦20,200 = 2,020,000 kobo)

**Where It's Used:**
- `PaymentService.initiateDebitForBooking()` - Called when organizer confirms and pays for booking
- `PaymentController.initiateDebit()` - REST endpoint: `POST /api/payments/bookings/{bookingId}/debit`
- `BookingController.confirmAndPay()` - Calls payment service internally

**Business Flow:**
1. Organizer confirms booking completion via `BookingController.confirmAndPay()`
2. `PaymentService.initiateDebitForBooking()` is called with `bookingId`
3. System fetches booking, finds organizer's active mandate, calculates total amount (acceptedAmount + ₦200 platform fee)
4. Creates `DebitRequest` with mandate reference, total amount, and user details
5. Calls `onePipeClient.initiateDebit(request)`
6. OnePipe processes debit and returns `transactionRef` and status
7. If status is `WaitingForOTP`, system stores transaction and returns OTP requirement
8. Otherwise, system stores `DebitTransaction` with status `PENDING`
9. OnePipe sends webhook to `/api/payments/webhooks/debit` with transaction status
10. On success webhook, system triggers dual payouts (musician + platform fee)

**Response:** Returns `DebitResponse` with `status`, `transactionRef`, `message`. If OTP required, includes `otpReference` and `validationUrl`

**OTP Handling:** If OnePipe returns `WaitingForOTP` status, system stores transaction reference. User must call `validateOtp` endpoint with OTP code.

---

#### 3. Validate OTP (`validateOtp`)

**Purpose:** Validate OTP for debit transaction that requires OTP verification.

**OnePipe Request Type:** `validate`

**Request Structure:**
```json
{
  "request_ref": "REQ_timestamp",
  "request_type": "validate",
  "transaction": {
    "transaction_ref": "TXN_reference",
    "otp": "123456"
  }
}
```

**Where It's Used:**
- `PaymentService.validateOtpForBooking()` - Called when user submits OTP
- `PaymentController.validateOtp()` - REST endpoint: `POST /api/payments/bookings/{bookingId}/validate-otp?otp=123456`

**Business Flow:**
1. User receives OTP from bank after debit initiation
2. User calls validate OTP endpoint with `bookingId` and `otp`
3. System finds pending debit transaction for booking
4. Calls `onePipeClient.validateOtp(debit.providerRef, otp)`
5. OnePipe validates OTP and completes transaction
6. System updates debit status based on response
7. OnePipe sends webhook with final transaction status

**Response:** Returns `DebitResponse` with updated status and transaction reference

---

#### 4. Initiate Payout (`initiatePayout`)

**Purpose:** Transfer money to recipient bank account (musician or GigWave platform account).

**OnePipe Request Type:** `disburse`

**Request Structure:**
```json
{
  "request_ref": "REQ_timestamp",
  "request_type": "disburse",
  "auth": {
    "type": null,
    "secure": null,
    "auth_provider": "sandbox|production"
  },
  "transaction": {
    "mock_mode": "live",
    "transaction_ref": "TXN_timestamp",
    "transaction_desc": "Payout for gig booking",
    "amount": 2000000,
    "customer": {
      "customer_ref": "user_uuid",
      "firstname": "John",
      "surname": "Doe",
      "email": "user@example.com",
      "mobile_no": "08123456789"
    },
    "details": {
      "destination_account": "0123456789",
      "destination_bank_code": "058"
    },
    "meta": {
      "callback_url": "http://localhost:8080/api/payments/webhooks/payout"
    }
  }
}
```

**Where It's Used:**
- `PaymentService.createPayoutOnDebitSuccess()` - Called automatically when debit succeeds
- Two payouts are created: one for musician (acceptedAmount) and one for GigWave platform fee (₦200)

**Musician Payout Flow:**
1. Debit webhook confirms successful debit
2. System fetches musician's default payout bank account
3. Creates `PayoutRequest` with musician's account details, `acceptedAmount`, and user info
4. Calls `onePipeClient.initiatePayout(musicianPayoutRequest)`
5. System stores `Payout` entity with status `PENDING`

**Platform Fee Payout Flow:**
1. After musician payout, system creates second payout request
2. Uses platform account details: `0121753572` (Sterling Bank - 232), name: `Agbaosi Bolarinwa Minasu`
3. Amount is ₦200 (20,000 kobo)
4. Uses platform email `platform@gigwave.com` and phone `09010849782`
5. Calls `onePipeClient.initiatePayout(platformPayoutRequest)`
6. System stores second `Payout` entity for platform fee

**Business Flow:**
1. Organizer confirms payment → Debit initiated
2. Debit succeeds → Webhook received
3. `PaymentService.createPayoutOnDebitSuccess()` called
4. Two payouts created simultaneously:
   - Musician: Full `acceptedAmount` (e.g., ₦20,000)
   - GigWave: Platform fee (₦200)
5. Both payouts sent to OnePipe
6. OnePipe processes transfers and sends webhooks
7. System updates payout statuses based on webhook responses

**Response:** Returns `PayoutResponse` with `status`, `transactionRef`, and `message`

---

#### 5. Get Supported Banks (`getSupportedBanks`)

**Purpose:** Fetch list of Nigerian banks supported by OnePipe for account selection.

**OnePipe Endpoint:** `GET https://api.onepipe.io/v2/banks`

**Where It's Used:**
- `PaymentService.getSupportedBanks()` - Returns bank list for frontend
- `PaymentController.getSupportedBanks()` - REST endpoint: `GET /api/payments/banks` (public, no auth required)
- `BankAccountsPage` - Frontend displays bank list when adding bank account

**Business Flow:**
1. User navigates to bank accounts page
2. Frontend calls `GET /api/payments/banks`
3. System calls `onePipeClient.getSupportedBanks()`
4. OnePipe returns bank list with codes and names
5. If API call fails, system returns fallback list of common Nigerian banks
6. Frontend displays banks in dropdown for user selection

**Response:** Returns `BankListResponse` with array of banks containing `code` and `name`

**Fallback Banks:** If OnePipe API unavailable, returns: GTB (058), First Bank (011), Access Bank (014), Sterling Bank (232), UBA (033), Ecobank (050), Fidelity Bank (070), Zenith Bank (057)

---

#### 6. Verify Webhook Signature (`verifyWebhookSignature`)

**Purpose:** Verify webhook requests are authentic from OnePipe using HMAC-SHA256 signature.

**Verification Method:** HMAC-SHA256 using `ONEPIPE_SECRET_KEY`

**Where It's Used:**
- `PaymentController.handleMandateWebhook()` - Verifies mandate webhook signature
- `PaymentController.handleDebitWebhook()` - Verifies debit webhook signature
- `PaymentController.handlePayoutWebhook()` - Verifies payout webhook signature
- All webhook endpoints verify signature before processing

**Business Flow:**
1. OnePipe sends webhook POST request to configured callback URL
2. Request includes raw JSON body and `X-OnePipe-Signature` header
3. System reads raw request body as string
4. Calls `onePipeClient.verifyWebhookSignature(requestBody, signature)`
5. System generates HMAC-SHA256 signature from request body using `secretKey`
6. Compares computed signature with provided signature (constant-time comparison)
7. If match, processes webhook; if not, returns 401 Unauthorized

**Implementation:** `OnePipeClientImpl.verifyWebhookSignature()` uses constant-time comparison to prevent timing attacks

---

### Webhook Integration

#### Mandate Webhook

**Endpoint:** `POST /api/payments/webhooks/mandate`

**Payload:**
```json
{
  "mandate_ref": "MANDATE_123456",
  "status": "active|suspended|revoked"
}
```

**Processing:** `PaymentService.handleMandateWebhook()` updates `PaymentMandate` status based on webhook status

**Where It's Used:** Called automatically by OnePipe when mandate status changes (authorized, suspended, or revoked)

---

#### Debit Webhook

**Endpoint:** `POST /api/payments/webhooks/debit`

**Payload:**
```json
{
  "transaction_ref": "TXN_123456",
  "status": "success|failed|pending"
}
```

**Processing:** 
- `PaymentService.handleDebitWebhook()` updates `DebitTransaction` status
- On success, triggers `createPayoutOnDebitSuccess()` to initiate dual payouts
- On failure, updates booking payment status to `DEBIT_FAILED` and sends notification

**Where It's Used:** Called automatically by OnePipe when debit transaction completes

---

#### Payout Webhook

**Endpoint:** `POST /api/payments/webhooks/payout`

**Payload:**
```json
{
  "transaction_ref": "TXN_123456",
  "status": "success|failed|pending"
}
```

**Processing:** `PaymentService.handlePayoutWebhook()` updates `Payout` status based on webhook response

**Where It's Used:** Called automatically by OnePipe for each payout transaction (musician payout and platform fee payout)

---

### Payment Flow Integration

**Complete Gig Payment Flow:**

1. **Mandate Setup** (Organizer & Musician):
   - User adds bank account
   - User calls `POST /api/payments/mandate/setup/organizer` or `/mandate/setup/musician`
   - System calls `onePipeClient.setupMandate()`
   - OnePipe returns authorization URL
   - User authorizes mandate
   - OnePipe webhook updates mandate status to `ACTIVE`

2. **Booking Payment** (Organizer confirms):
   - Organizer calls `POST /api/bookings/{bookingId}/confirm-pay`
   - System calls `onePipeClient.initiateDebit()` with total amount (acceptedAmount + ₦200)
   - OnePipe debits organizer account
   - If OTP required, user validates via `POST /api/payments/bookings/{bookingId}/validate-otp`
   - OnePipe webhook confirms debit success

3. **Dual Payout** (On debit success):
   - System automatically calls `onePipeClient.initiatePayout()` twice:
     - First payout: Musician receives `acceptedAmount`
     - Second payout: GigWave receives ₦200 platform fee
   - Both payouts sent simultaneously
   - OnePipe processes transfers
   - Separate webhooks confirm each payout status

---

### Error Handling

**OnePipe API Error Structure:**
```json
{
  "status": "failed",
  "errors": [
    {
      "message": "Error description",
      "code": "ERROR_CODE"
    }
  ]
}
```

**Error Handling:**
- All errors checked in response `errors` array
- First error message extracted and logged
- Thrown as `RuntimeException` with descriptive message
- Business logic catches exceptions and updates transaction status accordingly

**Common Error Scenarios:**
- Invalid mandate reference → Debit fails, booking marked as `DEBIT_FAILED`
- Insufficient funds → Debit fails, webhook updates status
- Invalid bank account → Payout fails, retry mechanism can be implemented
- OTP timeout → User must re-initiate debit

---

### Integration Points Summary

**Backend Services:**
- `PaymentService` - Orchestrates all OnePipe API calls for payment flows
- `OnePipeClientImpl` - Direct HTTP communication with OnePipe API
- `PaymentController` - Exposes REST endpoints for frontend integration

**Frontend Integration:**
- Bank account selection uses `GET /api/payments/banks`
- Mandate setup triggers `POST /api/payments/mandate/setup/*`
- Payment confirmation triggers `POST /api/payments/bookings/{bookingId}/debit`
- OTP validation triggers `POST /api/payments/bookings/{bookingId}/validate-otp`

**Webhook Integration:**
- All webhooks are publicly accessible (no authentication)
- Signature verification ensures authenticity
- Webhooks update transaction statuses and trigger subsequent actions

**Database Integration:**
- `PaymentMandate` - Stores mandate references and status
- `DebitTransaction` - Tracks debit attempts and status
- `Payout` - Tracks payout attempts for both musician and platform fee
- `Booking` - Payment status updated based on transaction outcomes
