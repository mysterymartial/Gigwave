# OnePipe API Flow – Frontend to Backend to OnePipe (and back)

This doc describes **how Gigwave uses the OnePipe API** from the browser to the backend to OnePipe, and how OnePipe talks back. It also notes how this aligns with OnePipe’s expected flow.

---

## 1. High-level flow

```
[User in browser] → [Gigwave Frontend] → [Gigwave Backend] → [OnePipe API]
                                                                    ↓
[User authorizes on OnePipe / bank] ← [authorization_url] ← [OnePipe]
                                                                    ↓
[OnePipe] → [Gigwave webhook] → [Backend updates mandate status] → [Frontend sees “active”]
```

- **Create mandate:** User clicks “Set up mandate” → backend calls OnePipe “create mandate” → backend returns `authorizationUrl` → frontend redirects user to OnePipe → user authorizes → OnePipe calls our webhook → we mark mandate active.
- **Collect (debit):** User confirms payment → backend calls OnePipe “collect” → if OTP required, user enters OTP → we call “validate” → OnePipe calls our webhook with success/failure.

---

## 2. Frontend → Backend (how the UI talks to our API)

### 2.1 Where it starts

- **Page:** `BankAccountsPage.tsx` (Payments / Bank accounts).
- **User action:** User adds a bank account, then clicks “Set up mandate” (or is prompted). They can optionally enter BVN.
- **Max amount:** Frontend uses a constant `MANDATE_MAX_AMOUNT_NGN` (e.g. 5,000,000 NGN) and sends it to the backend.

### 2.2 What the frontend sends

**Mandate setup (organizer or musician):**

- **Endpoint:**  
  - Organizer: `POST /api/payments/mandate/setup/organizer`  
  - Musician: `POST /api/payments/mandate/setup/musician`
- **Query params:**  
  - `bankAccountId` (UUID of the bank account to link to the mandate)  
  - `maxAmount` (number, in Naira)  
  - `bvn` (optional string)
- **Headers:**  
  - `Authorization: Bearer <JWT>` (from `localStorage.token`)  
  - `Content-Type: application/json`  
  - Optional: `Idempotency-Key: mandate-{accountId}-{timestamp}`

**Implementation:**

- `frontend/src/lib/api.ts`: `paymentApi.setupOrganizerMandate()` / `setupMusicianMandate()` build params and call the above endpoints.
- `frontend/src/hooks/usePayments.ts`: `useSetupOrganizerMandate` / `useSetupMusicianMandate` wrap those calls (e.g. for React Query).

So: **frontend only talks to Gigwave backend.** It never calls OnePipe directly; it never sees API key, secret, or Signature. All OnePipe communication is backend-only.

### 2.3 What the frontend gets back

- **Success:**  
  - `status`, `mandateRef`, `authorizationUrl`  
  - If `authorizationUrl` is present, the app redirects: `window.location.href = response.authorizationUrl` so the user completes authorization on OnePipe’s page.  
  - After the user authorizes, OnePipe redirects them (per their config); our backend learns about “active” via webhook (see below).
- **Error:**  
  - Backend returns 4xx/5xx; frontend shows a generic message (e.g. “Failed to set up mandate”). The actual OnePipe error (e.g. code 01) is in backend logs / response body.

So the flow “from header to how it communicate to frontend” is: **frontend sends JWT + query params to our backend; backend returns JSON with `authorizationUrl` (and optionally mandateRef/status); frontend only redirects or shows error.**

---

## 3. Backend → OnePipe (headers and body)

All OnePipe calls are in **`OnePipeClientImpl`**. Base URL is from config (e.g. `https://api.onepipe.io/v2/transact`).

### 3.1 Headers we send to OnePipe

| Header           | Value |
|------------------|--------|
| `Content-Type`   | `application/json` |
| `Authorization`  | `Bearer <ONEPIPE_API_KEY>` (key trimmed, no trailing newline) |
| `Signature`      | `MD5(request_ref + ";" + ONEPIPE_SECRET_KEY)` as **32-character lowercase hex** |

Rules:

- **Same `request_ref`** is used in (1) the JSON body as `request_ref` and (2) the Signature calculation. If they differ, OnePipe can reject the request.
- **Secret key** is trimmed before use (same key for Signature and for TripleDES below).

Implementation: `createHeaders(requestRef)` in `OnePipeClientImpl`:

- Sets `Authorization: Bearer {apiKey}` (trimmed).
- For non-null `requestRef`, sets `Signature: md5Hex(requestRef + ";" + secret)` (trimmed secret).

So: **header-wise we do exactly what OnePipe expects:** Bearer API key + Signature from `MD5(request_ref;client_secret)`.

### 3.2 Create mandate – body we send

- **Top level:**  
  - `request_ref`: e.g. `REQ_<timestamp>` (same value as in Signature).  
  - `request_type`: `"create mandate"`.
- **auth:**  
  - `type`: `"bank.account"`  
  - `secure`: TripleDES(Base64) of `accountNumber + ";" + bankCode`, key = trimmed `ONEPIPE_SECRET_KEY`  
  - `auth_provider`: `"PaywithAccount"`
- **transaction:**  
  - `mock_mode`: `"Live"`  
  - `transaction_ref`, `transaction_desc`, `amount` (0 for mandate), `customer` (customer_ref, firstname, surname, email, mobile_no)  
  - **meta:**  
    - `amount`: max mandate amount in **kobo** as **string**  
    - `skip_consent`: `"true"`  
    - `bvn`: encrypted BVN if provided, else `""`  
    - `biller_code`: from config (trimmed)  
    - `customer_consent`: `"https://paywithaccount.com/consent_template.pdf"`

Data for `customer` and `auth.secure` comes from **PaymentService**: bank account + user (email, phone) from DB; callback URL for webhook is our backend URL (e.g. `serverUrl + "/api/payments/webhooks/onepipe"`). Frontend does not send these; backend builds them.

### 3.3 Collect (debit) – body we send

- **request_type:** `"collect"`  
- **auth:** Same idea: `secure` = TripleDES(accountNumber;bankCode). `auth_provider`: `"NIBBS"` for collect.  
- **transaction:** amount in kobo, narration, customer, etc.  
- **meta:** `biller_code`, `skip_consent`, `customer_consent` (empty for collect).

So: **create mandate vs collect** differ by `request_type`, auth_provider, and meta (e.g. mandate has amount/customer_consent/bvn; collect does not). Headers (Bearer + Signature) are the same pattern.

### 3.4 How backend uses the OnePipe response (create mandate)

- OnePipe returns JSON with e.g. `status`, `transaction.mandate_ref`, `transaction.authorization_url`, `message`.  
- **PaymentService** maps that to **MandateResponse** (status, mandateRef, authorizationUrl, message), saves a **PaymentMandate** row (PENDING), and returns MandateResponse to the controller.  
- **PaymentController** returns that to the frontend; frontend redirects to `authorizationUrl` if present.

So the “communication to frontend” is: **backend gets OnePipe JSON → maps to our DTO → returns to frontend → frontend only sees status/mandateRef/authorizationUrl and redirects or shows error.**

---

## 4. OnePipe → Backend (webhooks)

OnePipe does **not** tell the frontend directly that the mandate is active. They call **our** webhook.

### 4.1 Webhook URL

- Configured in our backend as `serverUrl + "/api/payments/webhooks/onepipe"` (e.g. `https://your-app.com/api/payments/webhooks/onepipe`).  
- This same URL is used for **mandate** and **debit** callbacks (we distinguish by payload).

### 4.2 Webhook request from OnePipe

- **Method:** POST  
- **Headers:** OnePipe sends `X-OnePipe-Signature` (or similar; we read it in **PaymentController** as `X-OnePipe-Signature`).  
- **Body:** JSON with e.g. `status`, and either `mandate_ref` (mandate flow) or `transaction_ref` (debit flow).

### 4.3 What our backend does

- **PaymentController.handleOnePipeWebhook:**  
  - Verifies signature using `paymentService.verifyWebhookSignature(requestBody, signature)` (we use e.g. `MD5(secret + payload)` per our implementation; must match OnePipe’s doc).  
  - If valid:  
    - If payload has `mandate_ref`: `paymentService.handleMandateWebhook(mandateRef, status)` → we update **PaymentMandate** to ACTIVE/SUSPENDED/REVOKED.  
    - If payload has `transaction_ref`: `paymentService.handleDebitWebhook(transactionRef, status)` → we update **DebitTransaction** and booking payment status (and trigger payout on success).  
  - Frontend never receives this webhook; frontend can only “see” mandate active by refetching data (e.g. bank accounts / mandate status) or by being told via our own API that the mandate is active.

So: **OnePipe communicates to the frontend only indirectly** – via our backend (webhook handler updates DB; frontend gets updated state when it calls our APIs again).

---

## 5. Is this how it ought to work? (Research / docs)

- **Frontend ↔ Backend:** Yes. The frontend should **not** call OnePipe directly or hold API keys. Our pattern (frontend → backend → OnePipe, backend returns authorization URL and mandateRef) is correct.
- **Headers:** OnePipe docs describe **Authorization: Bearer &lt;api_key&gt;** and **Signature: MD5(request_ref;client_secret)** (same value in body and in Signature). Our implementation matches (same request_ref, semicolon, trimmed secret).
- **Body:**  
  - `request_ref` in body must match the one used in Signature – we do that.  
  - `auth.secure` must be TripleDES(accountNumber;bankCode) with client secret – we do that.  
  - Create mandate: meta amount in kobo as string, optional BVN encrypted, customer_consent URL – we align with that.  
- **Webhook:** OnePipe calls a **server-side URL** (our backend) with mandate_ref or transaction_ref and status. We are supposed to update our state and return 2xx. We do that; the frontend is then updated when it next loads data from our API.

So end-to-end: **from header (Bearer + Signature) to how we communicate with the frontend (JSON with authorizationUrl, no direct OnePipe calls from frontend) and how OnePipe communicates back (webhook to backend only), this is how OnePipe is intended to be used.** If you still get 400/code 01, the next place to check is exact payload (customer phone format, biller_code, env keys, and OnePipe dashboard config) and their support with a specific `request_ref` so they can confirm server-side why that request was rejected.
