# OnePipe – Full Postman Request (Headers + Body)

Use these in Postman to test **create mandate** and **collect** against `https://api.onepipe.io/v2/transact`. Replace placeholders with your real values and computed **Signature** / **auth.secure** (and **meta.bvn** if used).

---

## Environment variables (Postman)

Set these in Postman **Environment** (or use raw values):

| Variable | Example | Description |
|----------|---------|-------------|
| `base_url` | `https://api.onepipe.io/v2/transact` | OnePipe transact URL |
| `api_key` | Your OnePipe API key | Bearer token |
| `secret_key` | Your OnePipe secret key | For Signature + TripleDES (no trailing newline) |
| `request_ref` | `REQ_1738000000000` | Unique per request; **must match body** and **Signature** input |
| `transaction_ref` | `TXN_1738000000000` | Unique per transaction |
| `encrypted_secure` | Base64 string | TripleDES of `accountNumber;bankCode` (see below) |
| `encrypted_bvn` | Base64 string or leave empty | TripleDES of BVN (create mandate only, optional) |
| `biller_code` | `000019` or `` | From OnePipe dashboard (optional) |

---

## Headers (same for both create mandate and collect)

| Key | Value |
|-----|--------|
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer {{api_key}}` |
| `Signature` | `{{signature}}` |

**Signature:** 32-character **lowercase hex** of **MD5(**`request_ref`**;**`secret_key`**)**.  
Use the **same** `request_ref` as in the body.

**Postman Pre-request Script (to set `signature`):**

```javascript
const requestRef = pm.environment.get("request_ref") || "REQ_" + Date.now();
const secret = pm.environment.get("secret_key") || "";
const toHash = requestRef + ";" + secret;
const signature = CryptoJS.MD5(toHash).toString(CryptoJS.enc.Hex).toLowerCase();
pm.environment.set("signature", signature);
// Optional: ensure body uses same request_ref
pm.environment.set("request_ref", requestRef);
pm.environment.set("transaction_ref", "TXN_" + Date.now());
```

(Requires Postman’s built-in CryptoJS or a small script that does MD5 and hex.)

**Manual Signature:**  
Input: `REQ_1738000000000;your_secret_key_here` (no space around `;`)  
Output: MD5 → 32-char lowercase hex. Put that in the `Signature` header.

---

## 1. Create mandate – full request

**Method:** `POST`  
**URL:** `{{base_url}}` (e.g. `https://api.onepipe.io/v2/transact`)

### Headers

```
Content-Type: application/json
Authorization: Bearer {{api_key}}
Signature: {{signature}}
```

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
      "customer_ref": "2348000021412",
      "firstname": "Koko",
      "surname": "Below",
      "email": "kokobelow@gmail.com",
      "mobile_no": "2348000021412"
    },
    "meta": {
      "amount": "100000",
      "skip_consent": "true",
      "bvn": "",
      "biller_code": "000019",
      "customer_consent": "https://paywithaccount.com/consent_template.pdf"
    },
    "details": {}
  }
}
```

**Replace before send:**

- `request_ref` – same value used to compute `Signature` (e.g. `REQ_` + timestamp).
- `transaction_ref` – e.g. `TXN_` + timestamp.
- `secure` – **TripleDES** (DESede/ECB/PKCS5Padding) of **`accountNumber;bankCode`** (e.g. `1234567890;058`), key = `secret_key` (24 bytes), output **Base64**.
- `customer_ref`, `firstname`, `surname`, `email`, `mobile_no` – your test customer; `mobile_no` must be **234XXXXXXXXXX** (no + or leading 0).
- `meta.amount` – max mandate amount in **kobo** as **string** (e.g. `"100000"` = ₦1,000).
- `meta.bvn` – if you send BVN: **TripleDES + Base64** of the 11-digit BVN string; otherwise `""`.
- `meta.biller_code` – your biller code or `""`.
- `meta.customer_consent` – `"https://paywithaccount.com/consent_template.pdf"` or `""`.

**Test / Inspect:** use `"mock_mode": "Inspect"` and same structure to test without hitting live bank.

---

## 2. Collect – full request

**Method:** `POST`  
**URL:** `{{base_url}}` (same as create mandate)

### Headers

```
Content-Type: application/json
Authorization: Bearer {{api_key}}
Signature: {{signature}}
```

### Body (raw JSON)

```json
{
  "request_ref": "REQ_1738000000001",
  "request_type": "collect",
  "auth": {
    "type": "bank.account",
    "secure": "{{encrypted_secure}}",
    "auth_provider": "NIBSS"
  },
  "transaction": {
    "mock_mode": "Live",
    "transaction_ref": "TXN_1738000000001",
    "transaction_desc": "A nice narration",
    "transaction_ref_parent": null,
    "amount": 10800,
    "customer": {
      "customer_ref": "2348000021412",
      "firstname": "Koko",
      "surname": "Below",
      "email": "kokobelow@gmail.com",
      "mobile_no": "2348000021412"
    },
    "meta": {
      "biller_code": "000019",
      "skip_consent": "true",
      "customer_consent": ""
    },
    "details": {}
  }
}
```

**Replace before send:**

- `request_ref` – unique per request; **same** value used to compute `Signature`.
- `transaction_ref` – unique per transaction.
- `secure` – **same** as create mandate: TripleDES + Base64 of **`accountNumber;bankCode`**.
- `amount` – debit amount in **kobo** as **number** (e.g. `10800` = ₦108.00).
- `customer` – same format as create mandate; `mobile_no` = **234XXXXXXXXXX**.
- `meta.biller_code` – your biller code or `""`.

---

## 3. Getting `encrypted_secure` (and `encrypted_bvn`) for Postman

Postman cannot do TripleDES natively. Use one of:

1. **Backend:** Call your app (or a small dev endpoint) that returns `{ "secure": "<base64>", "signature": "<hex>" }` for given `accountNumber`, `bankCode`, and `request_ref` using `ONEPIPE_SECRET_KEY`.
2. **Java:** Use your `OnePipeTripleDesUtil.encrypt(plaintext, secretKey)`:
   - **secure:** `encrypt("1234567890;058", secretKey)` → paste Base64 into body.
   - **bvn:** `encrypt("22334455667", secretKey)` → paste into `meta.bvn` (create mandate only).
3. **Online/script:** Any TripleDES (DESede) ECB, PKCS5Padding, key = 24 bytes (secret truncated or zero-padded to 24), output Base64.

**Key:** Trim `secret_key`; then use first 24 bytes (or pad to 24 with zeros). Same key for Signature input and for encryption.

---

## 4. Copy-paste summary

**URL (both):** `https://api.onepipe.io/v2/transact`  
**Method:** POST  
**Headers:**

```
Content-Type: application/json
Authorization: Bearer YOUR_API_KEY
Signature: <MD5(request_ref;YOUR_SECRET_KEY) as 32-char lowercase hex>
```

**Create mandate body:** use JSON from section 1; set `request_ref`, `transaction_ref`, `secure`, customer, meta (amount string, skip_consent, bvn, biller_code, customer_consent).  
**Collect body:** use JSON from section 2; set `request_ref`, `transaction_ref`, `secure`, `amount` (number kobo), customer, meta (biller_code, skip_consent, customer_consent).

Use the **same** `request_ref` in the body and in the Signature calculation.

---

## 5. Fix Postman 01 Error (status Failed, code "01")

When you get:

```json
{
  "status": "Failed",
  "message": null,
  "data": {
    "provider_responde_code": "",
    "provider": "",
    "errors": [{ "code": "01", "message": "Error occurred while processing request" }]
  }
}
```

**Empty `provider` and `provider_responde_code`** means the request is rejected at OnePipe’s gateway (before PaywithAccount). Fix these in order:

### 5.1 Signature header (most common)

1. **Header must be present:** In Postman → **Headers** tab, add:
   - Key: `Signature`
   - Value: 32-character **lowercase hex** string (no spaces).

2. **Correct calculation:**
   - Input string: **exactly** `request_ref` + `;` + `secret_key` (no space).
   - Example for your body: `REQ_1738000000000` + `;` + your secret key →  
     `REQ_1738000000000;your_actual_secret_key_here`
   - Algorithm: **MD5** of that string.
   - Output: **32-char lowercase hex** (e.g. `a1b2c3d4e5f6789012345678901234ab`).

3. **Same `request_ref`:** The value in the **body** (`"request_ref": "REQ_1738000000000"`) must be **identical** to the one you use in the Signature input. If the body has `REQ_1738000000000`, the Signature must be computed from `REQ_1738000000000;secret_key`.

4. **No newline in secret:** Copy the secret from your OnePipe dashboard again; paste into a plain text editor and ensure there is no newline or space at the end. Use that trimmed value for both Signature and for generating `secure`.

**Quick test:** Use an online MD5 tool. Input: `REQ_1738000000000;YOUR_SECRET`. Mode: string (not file). Output: 32-char hex → copy and paste into the `Signature` header (all lowercase).

### 5.2 Authorization header

- Key: `Authorization`  
- Value: `Bearer YOUR_API_KEY` (space after `Bearer`, no quotes in the value).
- API key must be the one from the same OnePipe environment (Live vs Inspect) as the base URL.

### 5.3 Secure and secret key

- `auth.secure` must be **TripleDES + Base64** of **`accountNumber;bankCode`** (e.g. `1234567890;058`).
- Use the **same** secret key (trimmed, no newline) for:
  1. Computing **Signature** (MD5 of `request_ref;secret_key`).
  2. **Encrypting** the string `accountNumber;bankCode` to get `secure`.
- If the key has a trailing newline when you encrypt but not when you sign (or the opposite), OnePipe will reject the request → 01.

### 5.4 URL and environment

- **URL:** `https://api.onepipe.io/v2/transact` (or the exact URL from your OnePipe dashboard).
- **API key and secret** must be for the **same** environment (Live or Inspect) as the URL.

### 5.5 Try Inspect mode

- In the body, set `"mock_mode": "Inspect"` (capital I) and send again.
- If you get a **different** error or a more specific message, the issue is likely environment or key related.
- If you still get 01, the issue is likely **Signature** or **secure** (key/format).

### 5.6 Biller code

- If your OnePipe account **does not** use a biller code, try sending `"biller_code": ""` in `meta`.
- If it **does** use one, `"000019"` must be the value shown in your OnePipe dashboard for that account.

### 5.7 Get the exact reason from OnePipe

- In your request body you use a fixed `request_ref` (e.g. `REQ_1738000000000`). Use a **unique** one per test (e.g. `REQ_` + current timestamp).
- Send the request, then contact OnePipe support with:
  - The **exact `request_ref`** you sent (e.g. `REQ_1738123456789`).
  - Timestamp (date/time) of the request.
- They can look up that request on their side and tell you whether the failure was due to **signature**, **decryption of secure**, **biller_code**, or something else.

### Checklist (copy and tick)

- [ ] Headers: `Content-Type: application/json`, `Authorization: Bearer <api_key>`, `Signature: <32-char hex>`
- [ ] Signature = MD5(`request_ref` + `;` + `secret_key`) as **lowercase hex**; `request_ref` = value in body
- [ ] Secret key has **no trailing newline**; same key used for Signature and for `secure`
- [ ] `secure` = TripleDES(Base64) of `accountNumber;bankCode` (e.g. `1234567890;058`)
- [ ] URL = `https://api.onepipe.io/v2/transact` (or your dashboard URL)
- [ ] API key and secret match the environment (Live vs Inspect)
- [ ] Tried `"mock_mode": "Inspect"` once to see if error changes
- [ ] If still 01: contact OnePipe with your `request_ref` and timestamp
