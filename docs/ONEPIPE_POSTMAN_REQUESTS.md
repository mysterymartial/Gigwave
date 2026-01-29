# OnePipe – Full Postman Requests

Use these in Postman to call the same endpoints your backend uses. Replace placeholders with your real values and computed `Signature` / `auth.secure` (and `meta.bvn` if used).

---

## 1. Create Mandate

### URL & method
- **Method:** `POST`
- **URL:** `https://api.onepipe.io/v2/transact`  
  (or your `ONEPIPE_BASE_URL` if different)

### Headers

| Key             | Value |
|-----------------|--------|
| `Content-Type`  | `application/json` |
| `Authorization`  | `Bearer {{ONEPIPE_API_KEY}}` |
| `Signature`     | `{{signature}}` (see below) |

**Signature (required):** 32-character **lowercase** hex string. Per OnePipe docs: `request_ref;secret_key` (semicolon, no space).

- Input: `request_ref + ";" + ONEPIPE_SECRET_KEY`  
  Example: `REQ_1738000000000;your-secret-key-here`
- Algorithm: **MD5** of that string, then hex-encode (e.g. in Postman Pre-request Script: `CryptoJS.MD5(...).toString()` or use an online MD5 tool and paste).

Use the **same** `request_ref` in the body and in the Signature input.

### Body (raw JSON)

```json
{
  "request_ref": "REQ_1738000000000",
  "request_type": "create mandate",
  "auth": {
    "type": "bank.account",
    "secure": "{{encrypted_secure}}",
    "auth_provider": "PaywithAccount"
  },
  "transaction": {
    "mock_mode": "Live",
    "transaction_ref": "TXN_1738000000000",
    "transaction_desc": "Creating a mandate",
    "transaction_ref_parent": null,
    "amount": 0,
    "customer": {
      "customer_ref": "user-uuid-here",
      "firstname": "John",
      "surname": "Doe",
      "email": "john@example.com",
      "mobile_no": "08012345678"
    },
    "meta": {
      "amount": "500000",
      "skip_consent": "true",
      "customer_consent": "https://paywithaccount.com/consent_template.pdf",
      "activation_method": "transfer"
    },
    "details": {}
  }
}
```

**Placeholders:**

- `request_ref` – e.g. `REQ_` + current timestamp (must match value used for `Signature`).
- `transaction_ref` – e.g. `TXN_` + current timestamp.
- `{{encrypted_secure}}` – **TripleDES** (DESede/ECB/PKCS5Padding) of `accountNumber;bankCode`, then **Base64**.  
  Example plaintext: `0123456789;058` (account + semicolon + bank code).  
  Key: your `ONEPIPE_SECRET_KEY` (24 bytes; truncate or zero-pad to 24).  
  You need a small script or your backend to produce this; Postman cannot do 3DES out of the box.
- `meta.amount` – max debit in **kobo** as **string** (e.g. `"500000"` = ₦5,000).
- Optional: `meta.bvn` – if you send BVN, add one field: `"bvn": "{{encrypted_bvn}}"` (TripleDES + Base64 of raw BVN string).
- Optional: `meta.biller_code` – add `"biller_code": "YOUR_BILLER_CODE"` if your OnePipe setup requires it.

---

## 2. Collect (debit)

### URL & method
- **Method:** `POST`
- **URL:** `https://api.onepipe.io/v2/transact`  
  (same as create mandate)

### Headers

| Key             | Value |
|-----------------|--------|
| `Content-Type`  | `application/json` |
| `Authorization`  | `Bearer {{ONEPIPE_API_KEY}}` |
| `Signature`     | `{{signature}}` (same rule as create mandate) |

**Signature:** `MD5(request_ref;ONEPIPE_SECRET_KEY)` — semicolon between request_ref and secret, no space. 32-char lowercase hex. Use the same `request_ref` as in the body.

### Body (raw JSON)

```json
{
  "request_ref": "REQ_1738000000001",
  "request_type": "collect",
  "auth": {
    "type": "bank.account",
    "secure": "{{encrypted_secure}}",
    "auth_provider": "NIBBS"
  },
  "transaction": {
    "mock_mode": "Live",
    "transaction_ref": "TXN_1738000000001",
    "transaction_desc": "A nice narration",
    "transaction_ref_parent": null,
    "amount": 10800,
    "customer": {
      "customer_ref": "user-uuid-here",
      "firstname": "John",
      "surname": "Doe",
      "email": "john@example.com",
      "mobile_no": "08012345678"
    },
    "meta": {
      "biller_code": "YOUR_BILLER_CODE",
      "skip_consent": "true",
      "customer_consent": ""
    },
    "details": {}
  }
}
```

**Placeholders:**

- `request_ref` – unique per request; same value used to compute `Signature`.
- `transaction_ref` – unique per transaction.
- `{{encrypted_secure}}` – same as create mandate: TripleDES + Base64 of `accountNumber;bankCode`.
- `amount` – debit amount in **kobo** as **number** (e.g. `10800` = ₦108.00).
- Omit or set `biller_code` only if your OnePipe account requires it.

---

## 3. Copy-paste summary (headers)

Use these header names and values in Postman (replace placeholders):

**Create mandate / Collect (same headers):**

```
Content-Type: application/json
Authorization: Bearer YOUR_ONEPIPE_API_KEY
Signature: <32-char lowercase hex of MD5(request_ref;YOUR_ONEPIPE_SECRET_KEY)>
```

Example with fake values:

```
Content-Type: application/json
Authorization: Bearer sk_live_xxxxxxxxxxxx
Signature: a1b2c3d4e5f6789012345678901234ab
```

---

## 4. Getting `encrypted_secure` and `signature` for Postman

- **Option A – Backend:** Add a temporary dev-only endpoint that accepts `accountNumber`, `bankCode`, and optional `requestRef`, and returns `{ "secure": "<base64>", "signature": "<hex>" }` using your `ONEPIPE_SECRET_KEY`. Use those in Postman.
- **Option B – Script:**  
  - **Signature:** Any MD5 tool: input `REQ_1738000000000;your_secret_key` (request_ref + semicolon + secret, no space), output 32-char lowercase hex.  
  - **Secure:** Use Java (e.g. `OnePipeTripleDesUtil.encrypt("0123456789;058", secretKey)`) or another language with 3DES and paste the Base64 result into the body.  
  - **BVN (optional):** Same encryption as secure: TripleDES + Base64 of the 11-digit BVN string.

Once you have `secure` and `signature`, put them in the request body and Signature header and send the request.
