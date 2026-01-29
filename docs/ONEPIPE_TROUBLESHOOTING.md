# OnePipe Integration – Troubleshooting & Checks

## Your error

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

OnePipe returns a generic **code "01"** with no provider details. That usually means the request was rejected **before** or **at** the provider (NIBBS/PaywithAccount), so the cause has to be inferred from payload, auth, and config.

---

## 1. What could cause this 400 (code 01)

| Cause | What to check |
|-------|----------------|
| **Wrong base URL** | Production vs sandbox: `ONEPIPE_BASE_URL` must match your OnePipe dashboard (e.g. `https://api.onepipe.io/v2/transact` for live). Same URL is used for both **create mandate** and **collect**; only `request_type` changes. |
| **Signature header** | Header: `Signature: MD5(request_ref;secret_key)` as **32-char lowercase hex**. Per OnePipe docs: `request_ref` then semicolon then secret key (no space). Wrong key or wrong order will cause auth failure. |
| **Encryption of `auth.secure`** | Must be **TripleDES (DESede/ECB/PKCS5Padding)** of `accountNumber;bankCode`, Base64-encoded. Key = `ONEPIPE_SECRET_KEY` (trimmed/padded to 24 bytes). Wrong key or algorithm → provider can’t decrypt → generic error. |
| **Encryption of `meta.bvn`** (create mandate) | If you send BVN, it must be **TripleDES** with the same secret, Base64. Invalid/plain BVN → validation failure. |
| **Mandate `meta.amount`** | Create mandate sends **max amount in kobo** as **string** (e.g. `"500000"` for ₦5,000). Wrong type (number vs string) or wrong unit (Naira vs kobo) can cause rejection. |
| **Missing or invalid `biller_code`** | If your OnePipe plan requires a biller code, `meta.biller_code` must be set and match the dashboard. Empty or wrong value can trigger code 01. |
| **Customer data** | `customer_ref`, `firstname`, `surname`, `email`, `mobile_no` must be present and valid per OnePipe rules (e.g. Nigerian phone format). Empty/invalid data can cause generic failure. |
| **Environment / keys** | Using **sandbox** keys on **live** URL (or the reverse) will often return a generic error. Ensure `ONEPIPE_API_KEY`, `ONEPIPE_SECRET_KEY`, and base URL all match the same environment. |

So: **wrong URL, wrong signature, wrong encryption, wrong amount format, or wrong/missing biller/customer/env** can all surface as “Error occurred while processing request” with code 01.

---

## 1b. Logic vs OnePipe docs (create mandate)

| Doc / your request | Our implementation | Match |
|--------------------|---------------------|--------|
| `request_ref` in body = same as in Signature | Single `requestRef` in payload and in `createHeaders(requestRef)` | Yes |
| Signature: `MD5(request_ref;client_secret)` | `md5Hex(requestRef + ";" + secretKey)` | Yes |
| `auth.secure`: TripleDES(accountNumber;bankCBNCode, secretKey) | `securePlain = accountNumber + ";" + bankCode`, then `encryptSecure(securePlain)` | Yes |
| `meta.bvn`: TripleDES(BVN, secretKey) | `encryptSecure(request.getBvn())` when BVN provided | Yes |
| `meta.amount` string in kobo | `String.valueOf(maxAmountKobo)` | Yes |
| `mock_mode` | We send `"Live"` for real transactions. Doc sample shows `"Inspect"`; OnePipe docs do not define sandbox or when to use which. | Yes |

If 400 persists: set **ONEPIPE_BILLER_CODE** if required; confirm **base URL** is `https://api.onepipe.io/v2/transact`; ensure **API key and secret** match your OnePipe account.

**Why 400 code 01 can persist (even after OnePipe says it’s not on their side):**

1. **Env vars with trailing newline** – On Render/Railway/etc., pasted `ONEPIPE_SECRET_KEY` or `ONEPIPE_API_KEY` often get a trailing `\n`. That changes the Signature (MD5(request_ref;secret)) and can break decryption. **Fix:** We now trim `apiKey` and `secretKey` when building headers and encryption.
2. **Spaces in account/bank code** – Leading/trailing spaces in account number or bank code change `auth.secure` and cause the provider to reject. **Fix:** We trim account number and bank code before building `secure`.
3. **Empty optional fields** – Some gateways reject `"bvn": ""` or expect `customer_consent` to be `""` when no URL. **Fix:** We send `customer_consent: ""` when no callback URL; we still send `bvn: ""` when not provided (structure unchanged). If 400 continues, try omitting `bvn` when not provided (remove key from meta).
4. **Wrong key/URL/env** – API key or secret from a different environment (e.g. test vs live), or base URL without `/v2/transact`, still returns generic 01. Double-check dashboard vs env.
5. **Provider-side validation** – NIBBS/PaywithAccount may reject account format, BVN format, or amount limits. OnePipe returns 01 without provider details; only OnePipe (with your `request_ref`) can confirm the exact validation that failed.

---

## 2. Encryption and hashing (what we use)

- **`auth.secure`** (mandate and collect):  
  `plaintext = accountNumber + ";" + bankCode`  
  Encrypted with **OnePipeTripleDesUtil** (DESede/ECB/PKCS5Padding, 24-byte key from `ONEPIPE_SECRET_KEY`), then Base64. Same for **collect** when re-encrypting account for the debit.

- **`meta.bvn`** (create mandate only, optional):  
  If BVN is provided, it is encrypted with the **same** TripleDES util and key, then Base64.

- **Request signature**:  
  `MD5(request_ref;secret_key)` → 32-char lowercase hex in `Signature` header (semicolon between request_ref and secret_key, no space).

- **Webhook verification**:  
  `MD5(secret_key + raw_payload_body)` compared to the webhook signature header.

So encryption/hashing is **consistent** with typical OnePipe v2 usage. If code 01 persists, the most likely issues are **key/URL mismatch** or **provider-side validation** (biller_code, amount format, customer data).

---

## 3. URL check

- **Configured**: `onepipe.base-url` = `ONEPIPE_BASE_URL` defaulting to `https://api.onepipe.io/v2/transact`.
- **Confirmed**: OnePipe v2 docs ([v2.docs.onepipe.io](https://v2.docs.onepipe.io/)) use host `https://api.onepipe.io`; the transact endpoint is `https://api.onepipe.io/v2/transact` (POST). No change needed unless your dashboard shows a different URL.
- **Usage**:
  - **Create mandate**: `POST baseUrl` with `request_type: "create mandate"`.
  - **Collect**: `POST baseUrl` with `request_type: "collect"`.
  - **Validate OTP**: `POST baseUrl + "/validate"` (i.e. `.../v2/transact/validate`).

If OnePipe documents a different path for your environment (e.g. `/v2/transact` vs `/v1/...`), set `ONEPIPE_BASE_URL` to that exact base (including path). No extra path is added for mandate or collect.

---

## 4. BVN – collection and encryption

- **Create mandate**: BVN is **optional**. When the user enters BVN in the mandate setup modal, we send it in `transaction.meta.bvn` **encrypted** (same as `auth.secure`): TripleDES with `ONEPIPE_SECRET_KEY`, then Base64. Per OnePipe docs, `meta.bvn` uses the same encryption as bank account auth.
- **Collect**: We do not send BVN in collect; only create-mandate uses `meta.bvn` when provided.
- **Flow**: Frontend (Bank Accounts → Set Up Mandate) has an optional “BVN” field (11 digits). If provided, backend encrypts it and includes it in the OnePipe create-mandate payload. BVN can improve verification with the provider.

---

## 5. Payload alignment with your sample

**Create mandate** (we send):

- `request_type`: `"create mandate"`.
- `auth`: `type: "bank.account"`, `secure`: encrypted `accountNumber;bankCode`, `auth_provider`: `"PaywithAccount"`.
- `transaction.amount`: `0`; `transaction.meta.amount`: max amount in **kobo** as **string** (e.g. `"500000"` for ₦5,000).
- `transaction.meta.skip_consent`: `"true"`, `customer_consent`: URL (or default PDF).
- `transaction.meta.bvn`: encrypted BVN **only if** provided.
- `transaction.meta.biller_code`: from config when set.

**Collect** (we send):

- `request_type`: `"collect"`.
- `auth`: `type: "bank.account"`, `secure`: encrypted `accountNumber;bankCode`, `auth_provider`: `"NIBBS"`.
- `transaction.amount`: debit amount in **kobo** (number).
- `transaction.meta`: `biller_code` (if set), `skip_consent`: `"true"`, `customer_consent`: `""` (aligned with your sample).

So structure matches your samples; main variables are **env**, **keys**, **biller_code**, and **amount** (kobo, correct type).

---

## 6. Changes made in code

1. **Mandate max amount**: Frontend mandate max set to **₦5,000** (was ₦5,000,000). Backend still accepts whatever `maxAmount` the frontend sends (now 5000).
2. **Collect meta**: Added `skip_consent: "true"` and `customer_consent: ""` to collect payload to align with your sample.

---

## 7. Next steps if 400 persists

1. **Confirm env**: Same env for API key, secret key, and base URL (all sandbox or all live).
2. **Confirm biller code**: If your contract requires it, set `ONEPIPE_BILLER_CODE` and ensure it matches the OnePipe dashboard.
3. **Log request (dev only)**: Log the exact JSON payload and headers (mask secret) and compare with a working Postman/curl sample from OnePipe.
4. **Ask OnePipe**: Provide them `request_ref`, timestamp, and that you get code 01 with no provider code; they can look up the request and tell you the exact validation that failed.
