# OnePipe 400 Code 01 – Deep Research & Fix Hypotheses

**No code was changed.** This document is analysis and recommendations only.

---

## 1. Your error

```json
400 Bad Request: {
  "status": "Failed",
  "message": null,
  "data": {
    "provider_responde_code": "",
    "charge_status": null,
    "provider": "",
    "errors": [{"code": "01", "message": "Error occurred while processing request"}],
    "error": {"code": "01", "message": "Error occurred while processing request"}
  }
}
```

OnePipe returns **code "01"** with no provider code and no field-level detail. The failure happens at or before the provider (PaywithAccount/NIBBS).

---

## 2. OnePipe API (from docs + your code)

- **Base URL:** `https://api.onepipe.io`; transact endpoint: `https://api.onepipe.io/v2/transact`.
- **Headers:** `Content-Type: application/json`, `Authorization: Bearer {api_key}`, `Signature: MD5(request_ref;client_secret)` (32-char lowercase hex). Same `request_ref` must be in the **body** and used to compute the Signature.
- **Create mandate:** `request_type: "create mandate"`, `auth_provider: "PaywithAccount"`, `auth.secure` = TripleDES(accountNumber;bankCBNCode, secretKey) Base64, optional `meta.bvn` = TripleDES(BVN, secretKey) Base64.
- **Collect:** `request_type: "collect"`, `auth_provider: "NIBBS"`, same `auth.secure` format.
- **Encryption:** TripleDES (DESede/ECB/PKCS5Padding), 24-byte key from secret, then Base64.

Your implementation matches this: same `request_ref` in body and Signature, correct payload shape, TripleDES + Base64 for `secure` and `bvn`.

---

## 3. End-to-end flow in your codebase

1. **Frontend** → POST `/payments/mandate/setup/organizer` or `.../musician` with query params: `bankAccountId`, `maxAmount`, optional `bvn`, header `Idempotency-Key`, and **Bearer token**.
2. **PaymentController** → Reads `userId` from `@CurrentUser` (JWT). If null → 401. Otherwise calls `PaymentService.setupMandateForOrganizer/Musician(userId, bankAccountId, maxAmount, bvn)`.
3. **PaymentService** → Loads `BankAccount` by `bankAccountId`, checks ownership, loads `User`, builds `MandateRequest` with:
   - `accountNumber`, `bankCode`, `accountName` from **BankAccount**
   - `maxAmount`, `bvn` from params
   - **`callbackUrl` = `serverUrl + "/api/payments/webhooks/onepipe"`** (always set)
   - `userId`, `email`, `phone` from **User**
4. **OnePipeClientImpl.setupMandate** → Builds payload:
   - `request_ref`, `transaction_ref` (new refs each time)
   - `auth.secure` = TripleDES(trim(accountNumber) + ";" + trim(bankCode), secretKey)
   - `transaction.customer`: customer_ref = userId, firstname/surname from accountName, **email**, **phone** from User
   - **`meta.customer_consent` = `request.getCallbackUrl().trim()`** → so it is **always your webhook URL** (e.g. `https://yourapp.com/api/payments/webhooks/onepipe`)
   - `meta.amount` (string kobo), `meta.skip_consent`, `meta.bvn` (encrypted or ""), `meta.biller_code`
   - Headers: `Authorization: Bearer` + trimmed apiKey, **Signature: MD5(request_ref + ";" + trimmed secretKey)**
5. **RestTemplate** → POST to `baseUrl` (e.g. `https://api.onepipe.io/v2/transact`). OnePipe responds 400 with the JSON above.

---

## 4. Strongest hypotheses for 400 code 01

### Hypothesis A (highest): **`customer_consent` is set to your webhook URL**

- **What you do:** `PaymentService` always sets `callbackUrl = serverUrl + "/api/payments/webhooks/onepipe"`. In `OnePipeClientImpl` you set `meta.customer_consent = request.getCallbackUrl().trim()`, so **`customer_consent` is always your webhook URL** (e.g. `https://yourapp.onrender.com/api/payments/webhooks/onepipe`).
- **What it likely means in OnePipe/PaywithAccount:** In create-mandate, **`customer_consent`** is almost certainly the **URL of the consent document (e.g. PDF) shown to the customer** when they link their account — i.e. “where to find the mandate terms,” not “where to send webhook notifications.” Your codebase even has `DEFAULT_CUSTOMER_CONSENT_URL = "https://paywithaccount.com/consent_template.pdf"` but that value is **no longer used** for create mandate (you use callbackUrl when present).
- **Why it could cause 01:** PaywithAccount may validate `customer_consent` (e.g. must be empty, or a known/HTTPS consent doc URL). Sending your **backend webhook path** there could fail validation and result in a generic 01.
- **Fix (recommendation, no code change here):** Do **not** use your webhook URL as `customer_consent`. Either:
  - Send **empty string** for `customer_consent` when you don’t have a consent document URL, or
  - Send a **real consent document URL** (e.g. `https://paywithaccount.com/consent_template.pdf` or a URL OnePipe/PaywithAccount doc specifies).  
  Use `callbackUrl` / webhook URL only for **webhook configuration** in the OnePipe dashboard, not inside `meta.customer_consent`.

### Hypothesis B: **Secret used for Signature vs encryption is inconsistent**

- **What you do:** In `createHeaders()` you use **trimmed** `secretKey` for the Signature. In `encryptSecure()` you pass **raw** `secretKey` (no trim) to `OnePipeTripleDesUtil.encrypt()`.
- **Why it could cause 01:** If `ONEPIPE_SECRET_KEY` in env has a **trailing newline** (common when pasting in Render/Railway):
  - Signature = MD5(request_ref; **trimmed** secret) → OnePipe verifies with their stored secret (no newline) → **match**.
  - `auth.secure` = encrypted with **untrimmed** secret (with newline) → OnePipe decrypts with their secret (no newline) → **decrypt fails or garbage** → provider rejects → 01.
- **Fix (recommendation):** Use the **same** secret value for both Signature and encryption. If you trim for Signature, trim before passing the secret into `encryptSecure` / `OnePipeTripleDesUtil.encrypt` as well.

### Hypothesis C: **`biller_code` required but missing or wrong**

- **What you do:** You send `meta.biller_code` only when `billerCode` is non-blank (from `ONEPIPE_BILLER_CODE`). Otherwise you send `""`.
- **Why it could cause 01:** Some OnePipe/PaywithAccount setups **require** a valid `biller_code`. If the contract requires it and you send empty or wrong value, the gateway can return 01.
- **Fix (recommendation):** Confirm with OnePipe whether your product **requires** `biller_code` for create mandate. If yes, set `ONEPIPE_BILLER_CODE` in env to the exact value from the dashboard and ensure it’s sent in `meta.biller_code` (non-empty).

### Hypothesis D: **Customer data (email, phone) invalid or empty**

- **What you do:** You send `email` and `mobile_no` from the **User** entity. No format validation before sending to OnePipe.
- **Why it could cause 01:** PaywithAccount/NIBBS may require non-empty, valid email and Nigerian phone format (e.g. `234...`). If User has null/empty email or phone stored without `234`, provider can reject with 01.
- **Fix (recommendation):** In DB (or via a one-off check), ensure the user who sets up the mandate has:
  - Non-empty, valid **email**
  - **Phone** in Nigerian format (e.g. 234 + 10 digits; no leading 0).  
  Optionally normalize/validate before calling the mandate API.

### Hypothesis E: **Bank account format (account number, bank code)**

- **What you do:** You trim `accountNumber` and `bankCode` and send `accountNumber;bankCode` in `auth.secure`. No length or format checks.
- **Why it could cause 01:** Nigerian NUBAN is **10 digits**. Bank code is often **3 digits** (e.g. 058, 011). If the stored account number has spaces/dashes or wrong length, or bank code is stored as "58" instead of "058", provider validation could fail with 01.
- **Fix (recommendation):** Ensure in DB (and when saving from frontend) that:
  - **Account number** is exactly 10 digits, no spaces/dashes.
  - **Bank code** is 3-digit CBN code (zero-padded if needed, e.g. 058 not 58).  
  Validate or normalize before building the mandate request if needed.

### Hypothesis F: **Base URL, API key, or secret for wrong environment**

- **What you do:** You use `ONEPIPE_BASE_URL`, `ONEPIPE_API_KEY`, `ONEPIPE_SECRET_KEY` from env.
- **Why it could cause 01:** Using **sandbox** keys on **live** URL (or the reverse), or a base URL that doesn’t end with `/v2/transact`, can yield generic 01.
- **Fix (recommendation):** Confirm in the OnePipe dashboard: mode (inspect vs Live), correct base URL (`https://api.onepipe.io/v2/transact` for transact), and that the keys in your env match that environment. Re-paste keys without trailing newlines/spaces.

---

## 5. Summary: what to try first (no code edits in this doc)

| Priority | Hypothesis | What to do |
|----------|------------|------------|
| 1 | **A. customer_consent = webhook URL** | Stop sending your webhook URL in `meta.customer_consent`. Send `""` or the consent document URL (e.g. PaywithAccount consent PDF). Keep webhook URL only in dashboard/config for callbacks. |
| 2 | **B. Secret trim for encryption** | Use the same trimmed secret for both Signature and encryption (trim before calling `encryptSecure` / TripleDES). |
| 3 | **C. biller_code** | Confirm with OnePipe if `biller_code` is required; if yes, set `ONEPIPE_BILLER_CODE` and send it non-empty. |
| 4 | **D. Customer email/phone** | Ensure user has valid email and Nigerian-format phone (234...) in DB. |
| 5 | **E. Account / bank code format** | Ensure account number 10 digits, bank code 3 digits (zero-padded), no spaces. |
| 6 | **F. Env / URL** | Confirm base URL, API key, and secret match the same OnePipe mode (inspect or Live; no sandbox); re-paste keys without newlines. |

---

## 6. Getting a definitive answer from OnePipe

Because 01 has no field-level detail, the only way to be sure is server-side logs:

- In your next failing request, **log the exact `request_ref`** you send (you already use it in the payload and Signature).
- Ask OnePipe support: “We get 400 with code 01 on create mandate. Please check server-side logs for **request_ref = REQ_&lt;timestamp&gt;** and tell us whether the failure was: signature verification, decryption of auth.secure, validation of a specific field (e.g. customer_consent, biller_code, customer data), or provider rejection and for which reason.”

That will tell you whether the error is coming from your frontend/backend (wrong payload or key) or from provider rules (format/consent/biller_code), and which field to fix.
