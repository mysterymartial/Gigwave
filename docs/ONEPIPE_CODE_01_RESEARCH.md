# OnePipe Code 01 – Research & Why Create Mandate Fails

This doc summarizes research into **OnePipe API error code 01** (“Error occurred while processing request”) for **create mandate**, and what to try next.

---

## 1. Your error

```json
{
  "status": "Failed",
  "message": null,
  "data": {
    "provider_responde_code": "",
    "charge_status": null,
    "provider": "",
    "errors": [{ "code": "01", "message": "Error occurred while processing request" }],
    "error": { "code": "01", "message": "Error occurred while processing request" }
  }
}
```

**Empty `provider` and `provider_responde_code`** means the request is rejected at OnePipe’s gateway **before** it reaches PaywithAccount/NIBSS. So the cause is one of: **Signature**, **auth.secure** decryption, **Authorization**, **URL/env**, or **payload validation** (meta, customer, etc.).

---

## 2. What public OnePipe docs say

- **Endpoint:** `POST https://api.onepipe.io/v2/transact`
- **Headers:** `Authorization: Bearer <api_key>`, `Signature: <MD5 hash>`, `Content-Type: application/json`
- **Signature:** MD5 hash used for request verification; exact formula is not clearly published. Common pattern in Nigerian gateways: **MD5(request_ref;secret_key)** as 32-char hex.
- **auth.secure:** Encrypted credentials (Base64). Docs do **not** publish the exact algorithm (TripleDES mode, key derivation, encoding). You must get this from OnePipe support or a working sample.
- **Create mandate:** `request_type: "create mandate"`, `auth_provider: "PaywithAccount"`, `auth.type: "bank.account"`. Meta typically includes amount (kobo string), skip_consent, optional bvn, biller_code, customer_consent.
- **Environments:** “Inspect” (test) and “Live” only; no separate “sandbox” URL.

**Conclusion:** Public docs are not enough to be 100% sure about **encryption** (ECB vs CBC, UTF-8 vs UTF-16LE, raw key vs MD5-derived). Code 01 is generic; only OnePipe (with your `request_ref`) can give the exact reason.

---

## 3. Codebases checked (create mandate + encryption)

| Source | What it has | Create mandate? | Encryption for secure? |
|--------|-------------|-----------------|-------------------------|
| **9trocode/Onepipe-api** (GitHub) | OnePipe Node wrapper | No (v1 focus) | N/A |
| **Dr-Programmer59/Generating-Bank-account-onepipe-api** | Bank account + BVN | No mandate | N/A |
| **OnePipe v2 docs** (v2.docs.onepipe.io) | Transact structure | Not detailed | Not specified |
| **Our Postman doc** (ONEPIPE_POSTMAN_REQUESTS.md) | Create mandate body + Signature | Yes | TripleDES **ECB**, PKCS5Padding, key = 24 bytes (secret truncated/padded), **UTF-8** |
| **Your sample** (earlier in chat) | Java snippet | N/A | **CBC**, zero IV, **MD5(secret UTF-16LE)** → 24-byte key, plaintext **UTF-16LE** |

No public repo was found that implements **create mandate** with a documented, working encryption for `auth.secure`. So we have two plausible specs:

1. **ECB + UTF-8 + raw 24-byte key** (our Postman doc; common in some Nigerian APIs).
2. **CBC + UTF-16LE + MD5-derived key** (your sample; PaywithAccount-style).

If OnePipe’s gateway expects one and we use the other, decryption of `auth.secure` (and `meta.bvn`) fails → generic code 01.

---

## 4. Why your create mandate can fail (checklist)

| # | Cause | What to check / try |
|---|--------|----------------------|
| 1 | **Signature** | Same `request_ref` in body and in Signature. Formula: **MD5(request_ref + ";" + secret_key)** as **32-char lowercase hex**. No space around `;`. Secret must match exactly (no trailing newline; env vars often add `\n`). |
| 2 | **Encryption of auth.secure** | OnePipe must decrypt `accountNumber;bankCode`. We support two modes. **Try ECB:** set `onepipe.encryption=ecb` (ECB, UTF-8, raw 24-byte key). Default is `paywithaccount` (CBC, UTF-16LE, MD5 key). If 01 persists with one, switch to the other. |
| 3 | **Encryption of meta.bvn** | Same algorithm as secure. If you send BVN, it must be encrypted with the **same** key/mode as secure. Empty string `""` when not provided is OK. |
| 4 | **Authorization** | `Authorization: Bearer <api_key>`. Key must be for the **same** environment (Inspect vs Live) as the base URL. |
| 5 | **URL / environment** | Base URL e.g. `https://api.onepipe.io/v2/transact`. API key + secret + URL must all be from the same environment. |
| 6 | **mock_mode** | For testing, set `"mock_mode": "Inspect"` (capital I). If the error **changes** (e.g. different code or message), the issue may be env or key. |
| 7 | **Customer data** | `customer_ref`, `firstname`, `surname`, `email`, `mobile_no` required. `mobile_no` in Nigerian format: **234XXXXXXXXXX** (no `+`, no leading 0). We normalize in code. |
| 8 | **meta fields** | `amount` = max mandate in **kobo** as **string**. `skip_consent`, `biller_code`, `customer_consent` as per your agreement. Empty `biller_code` if you don’t use one. |
| 9 | **Empty / invalid values** | No leading/trailing spaces in account number or bank code when building `secure` (we use as-is per your request). If your DB has spaces, they will be in the encrypted value. |
| 10 | **OnePipe-side validation** | They may reject account format, BVN, or limits. Only they can confirm; give them **request_ref** + timestamp. |

---

## 5. Changes made in this codebase

1. **Response handling**  
   When OnePipe returns **HTTP 200** with **`status: "Failed"`**, we now read errors from **`body.data.errors`** (or `body.errors`) and throw with that message so we don’t treat it as success.

2. **Two encryption modes**  
   - **paywithaccount** (default): CBC, MD5-derived key (UTF-16LE), plaintext UTF-16LE (current behaviour).  
   - **ecb**: ECB, raw secret key resized to 24 bytes, UTF-8 (matches our Postman doc).  
   Set in config: **`onepipe.encryption=ecb`** to try ECB. No code change; just config.

3. **Docs**  
   This file (ONEPIPE_CODE_01_RESEARCH.md) and ONEPIPE_POSTMAN_REQUESTS.md (section 5) already describe the 01 error and the checklist.

---

## 6. What to do next (in order)

1. **Try ECB encryption**  
   Set **`onepipe.encryption=ecb`** (and restart). If create mandate starts succeeding, OnePipe expects ECB + UTF-8 + raw key for secure/BVN.

2. **Try Inspect**  
   Set **`onepipe.mock-mode=Inspect`** and send create mandate. If the error message or code changes, the problem is likely env/keys or payload validation.

3. **Verify Signature in logs**  
   Enable debug logging; confirm `request_ref` in the body matches the one used for Signature, and that the secret has no trailing newline (e.g. log secret length; compare with dashboard copy-paste).

4. **Postman with same payload**  
   Use ONEPIPE_POSTMAN_REQUESTS.md: same URL, headers (Signature = MD5(request_ref;secret) hex), and body. If Postman also gets 01, the issue is keys/env or OnePipe config. If Postman works and the app doesn’t, the issue is how we build Signature or secure.

5. **Contact OnePipe with request_ref**  
   Send them the **exact `request_ref`** (e.g. `REQ_1738...`) and timestamp of a failing create mandate. They can look up the request and tell you whether it was **signature**, **decryption of secure**, **biller_code**, or something else.

---

## 7. Summary

- **Code 01** = generic “Error occurred while processing request”; **empty provider** = rejected at gateway before PaywithAccount.
- Public **OnePipe docs** do not specify the exact **encryption** for `auth.secure` / `meta.bvn`; **no** public codebase was found that documents a working create-mandate + encryption flow.
- Two plausible encryption specs: **ECB + UTF-8 + raw 24-byte key** vs **CBC + UTF-16LE + MD5-derived key**. We support both; use **`onepipe.encryption=ecb`** to try ECB.
- Most likely causes: **Signature** (wrong formula or secret/newline), **auth.secure** (wrong algorithm/encoding), or **env/keys**. Follow the checklist above and, if 01 persists, get the exact failure reason from OnePipe using your **request_ref**.
