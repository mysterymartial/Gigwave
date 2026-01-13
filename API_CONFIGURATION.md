# Configuration

## OnePipe API Configuration

### Environment Variables

Configure OnePipe credentials in `application.yml` or via environment variables:

```yaml
onepipe:
  base-url: ${ONEPIPE_BASE_URL:https://api.onepipe.io/v2/transact}
  api-key: ${ONEPIPE_API_KEY:your-onepipe-api-key}
  secret-key: ${ONEPIPE_SECRET_KEY:your-onepipe-secret-key}
  environment: ${ONEPIPE_ENV:sandbox}
```

**Required Environment Variables:**
- `ONEPIPE_BASE_URL`: OnePipe API base URL (default: `https://api.onepipe.io/v2/transact`)
- `ONEPIPE_API_KEY`: Your OnePipe API key (required)
- `ONEPIPE_SECRET_KEY`: Your OnePipe secret key for signature generation (required)
- `ONEPIPE_ENV`: Environment - `sandbox` or `production` (default: `sandbox`)

**Configuration File Location:** `backend/src/main/resources/application.yml`

### API Headers

All OnePipe API requests require the following headers:

```
Authorization: Bearer {ONEPIPE_API_KEY}
Signature: {HMAC-SHA256-Signature}
Content-Type: application/json
```

**Header Generation:**
- `Authorization`: Uses format `Bearer {apiKey}`
- `Signature`: HMAC-SHA256 signature generated from `apiKey + timestamp` using `secretKey`
- Signature is Base64 encoded

**Implementation Location:** `backend/src/main/java/com/gigwave/infrastructure/payments/onepipe/OnePipeClientImpl.java` → `createHeaders()` method

### Base URL Configuration

**Default Base URL:** `https://api.onepipe.io/v2/transact`

**Supported Endpoints:**
- Transactions: `{baseUrl}` (POST)
- Banks List: `https://api.onepipe.io/v2/banks` (GET)
- OTP Validation: `{baseUrl}/validate` (POST)

### Amount Conversion

All amounts must be converted to **kobo** (1 Naira = 100 kobo) before sending to OnePipe API:

```java
long amountKobo = amount.multiply(new BigDecimal("100")).longValue();
```

**Example:** ₦20,000 = 2,000,000 kobo

### Customer Information

All requests must include customer object with:
- `customer_ref`: User ID as string
- `firstname`: First name extracted from account name
- `surname`: Surname extracted from account name
- `email`: User email address
- `mobile_no`: User phone number

**Name Parsing:** Account name is parsed to extract firstname and surname. If only one word, surname is empty string.

### Platform Account Configuration

GigWave platform fee account details configured in `application.yml`:

```yaml
platform:
  account:
    number: 0121753572
    bank-code: 232  # Sterling Bank
    name: Agbaosi Bolarinwa Minasu
```

**Phone Numbers:**
- Primary: `09010849782`
- Backup: `08159089791`

**Email:** `platform@gigwave.com`

### Webhook Configuration

OnePipe webhooks require signature verification using HMAC-SHA256:

**Webhook Endpoints:**
- Mandate: `POST /api/payments/webhooks/mandate`
- Debit: `POST /api/payments/webhooks/debit`
- Payout: `POST /api/payments/webhooks/payout`

**Required Header:** `X-OnePipe-Signature`

**Verification:** Raw request body is verified against signature using `ONEPIPE_SECRET_KEY`

**Implementation Location:** `backend/src/main/java/com/gigwave/infrastructure/payments/onepipe/OnePipeClientImpl.java` → `verifyWebhookSignature()` method

### Callback URLs

Configure server URL for webhook callbacks:

```yaml
server:
  url: ${SERVER_URL:http://localhost:8080}
```

**Callback URLs:**
- Mandate: `{serverUrl}/api/payments/webhooks/mandate`
- Debit: `{serverUrl}/api/payments/webhooks/debit`
- Payout: `{serverUrl}/api/payments/webhooks/payout`

### Error Handling

OnePipe API errors are returned in `errors` array within response body:

```json
{
  "status": "failed",
  "errors": [
    {
      "message": "Error description"
    }
  ]
}
```

All errors are logged and thrown as `RuntimeException` with descriptive messages.

### Signature Generation

Signature is generated using HMAC-SHA256:

1. Create message: `apiKey + timestamp` (timestamp is Unix epoch seconds)
2. Generate HMAC using `secretKey`
3. Base64 encode the result
4. Set in `Signature` header

**Implementation Location:** `backend/src/main/java/com/gigwave/infrastructure/payments/onepipe/OnePipeClientImpl.java` → `generateSignature()` method
