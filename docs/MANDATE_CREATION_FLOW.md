# Payment Mandate Creation Flow

## Current Implementation Status

### ❌ Mandates are NOT created at registration

**Registration Flow:**
1. User registers via `POST /api/auth/register`
2. `AuthService.register()` is called
3. `UserService.registerUser()` creates only the User entity
4. **No mandate is created at this point**

### ✅ Mandates are created separately

**Mandate Setup Flow:**

#### For Event Owners (Organizers):
1. User adds bank account: `POST /api/bank-accounts`
2. User sets up payment mandate: `POST /api/payments/mandate/setup/organizer`
   - Requires: `bankAccountId` and `maxAmount`
   - Endpoint: `PaymentController.setupOrganizerMandate()`
   - Service: `PaymentService.setupMandateForOrganizer()`
   - Creates: `PaymentMandate` entity with status `PENDING`

#### For Musicians:
1. User adds bank account: `POST /api/bank-accounts`
2. User sets up payout mandate: `POST /api/payments/mandate/setup/musician`
   - Requires: `bankAccountId` and `maxAmount`
   - Endpoint: `PaymentController.setupMusicianMandate()`
   - Service: `PaymentService.setupMandateForMusician()`
   - Creates: `PaymentMandate` entity with status `PENDING`

### Mandate Status Flow

1. **PENDING** - Created when user sets up mandate
2. **ACTIVE** - Updated via webhook when OnePipe confirms mandate
3. **SUSPENDED** - If mandate is suspended
4. **REVOKED** - If mandate is revoked

### Webhook Handler

- Endpoint: `POST /api/payments/webhooks/mandate`
- Updates mandate status based on OnePipe callback
- Handler: `PaymentService.handleMandateWebhook()`

---

## Recommendation

**Current design is correct** - Mandates should NOT be created at registration because:
1. Users need to add bank accounts first
2. Mandates require bank account details
3. Users may not have bank accounts ready at registration time
4. Mandates are payment-specific, not user-specific

**Flow is:**
1. Register → Create User
2. Add Bank Account → Create BankAccount
3. Setup Mandate → Create PaymentMandate (links User + BankAccount)

This is the correct separation of concerns.




## Current Implementation Status

### ❌ Mandates are NOT created at registration

**Registration Flow:**
1. User registers via `POST /api/auth/register`
2. `AuthService.register()` is called
3. `UserService.registerUser()` creates only the User entity
4. **No mandate is created at this point**

### ✅ Mandates are created separately

**Mandate Setup Flow:**

#### For Event Owners (Organizers):
1. User adds bank account: `POST /api/bank-accounts`
2. User sets up payment mandate: `POST /api/payments/mandate/setup/organizer`
   - Requires: `bankAccountId` and `maxAmount`
   - Endpoint: `PaymentController.setupOrganizerMandate()`
   - Service: `PaymentService.setupMandateForOrganizer()`
   - Creates: `PaymentMandate` entity with status `PENDING`

#### For Musicians:
1. User adds bank account: `POST /api/bank-accounts`
2. User sets up payout mandate: `POST /api/payments/mandate/setup/musician`
   - Requires: `bankAccountId` and `maxAmount`
   - Endpoint: `PaymentController.setupMusicianMandate()`
   - Service: `PaymentService.setupMandateForMusician()`
   - Creates: `PaymentMandate` entity with status `PENDING`

### Mandate Status Flow

1. **PENDING** - Created when user sets up mandate
2. **ACTIVE** - Updated via webhook when OnePipe confirms mandate
3. **SUSPENDED** - If mandate is suspended
4. **REVOKED** - If mandate is revoked

### Webhook Handler

- Endpoint: `POST /api/payments/webhooks/mandate`
- Updates mandate status based on OnePipe callback
- Handler: `PaymentService.handleMandateWebhook()`

---

## Recommendation

**Current design is correct** - Mandates should NOT be created at registration because:
1. Users need to add bank accounts first
2. Mandates require bank account details
3. Users may not have bank accounts ready at registration time
4. Mandates are payment-specific, not user-specific

**Flow is:**
1. Register → Create User
2. Add Bank Account → Create BankAccount
3. Setup Mandate → Create PaymentMandate (links User + BankAccount)

This is the correct separation of concerns.




