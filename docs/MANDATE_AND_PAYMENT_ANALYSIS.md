# Mandate and Payment Analysis

## Current Implementation

### ✅ Mandate Setup
- **OnePipe Request Type**: `setup_mandate`
- **Purpose**: Creates a direct debit mandate
- **Max Amount**: Set when creating mandate (e.g., ₦100,000)
- **Status**: PENDING → ACTIVE (via webhook)

### ✅ Debit/Collect Implementation
- **OnePipe Request Type**: `collect` (NOT "simple_payment")
- **Purpose**: Debits money from organizer's account using mandate reference
- **How it works**: Uses the mandate reference to authorize each debit

---

## Multiple Debits with Different Amounts

### ✅ YES - You CAN debit multiple times with different amounts!

**Current Implementation:**
1. **Mandate Setup**: User sets a `maxAmount` (e.g., ₦100,000)
2. **Each Debit**: Can be any amount up to `maxAmount`
3. **Multiple Debits**: Can use the same mandate multiple times
4. **Different Amounts**: Each debit can have a different amount

**Example:**
```
Mandate created with maxAmount = ₦100,000

Debit 1: ₦20,000 (for booking 1) ✅
Debit 2: ₦15,000 (for booking 2) ✅
Debit 3: ₦30,000 (for booking 3) ✅
Total debited: ₦65,000 (still within maxAmount)
```

### ⚠️ Current Limitation

**The code does NOT track cumulative debits!**

- Each debit is validated against `maxAmount` individually
- No check for total amount debited across all transactions
- If you debit ₦100,000, you can still debit another ₦100,000 (exceeds max)

**This could be a problem!** The mandate's `maxAmount` should be the **total** that can be debited, not per transaction.

---

## Payment Method Used

### ❌ NOT using "simple_payment"

**Current Implementation:**
- **Request Type**: `collect` (direct debit via mandate)
- **This is**: Mandate-based recurring/repeated debits
- **NOT**: Simple one-time payment

**OnePipe Request Types:**
- `setup_mandate` - Create mandate ✅ (We use this)
- `collect` - Debit using mandate ✅ (We use this)
- `disburse` - Payout/transfer ✅ (We use this)
- `simple_payment` - One-time payment ❌ (We DON'T use this)

---

## How It Works

### Flow:
1. **Setup Mandate**: 
   - User authorizes mandate with maxAmount (e.g., ₦100,000)
   - Mandate status becomes ACTIVE

2. **Debit Transaction**:
   - Each booking creates a debit request
   - Uses the same `mandateRef`
   - Amount can vary (e.g., ₦20,000, ₦15,000, etc.)
   - Each debit is independent

3. **Validation**:
   - Currently: Only checks if mandate is ACTIVE
   - Currently: Only checks if individual debit ≤ maxAmount
   - **Missing**: Check if (cumulative debits + new debit) ≤ maxAmount

---

## Recommendations

### Option 1: Track Cumulative Debits (Recommended)

Add validation to ensure total debits don't exceed maxAmount:

```java
// In PaymentService.initiateDebitForBooking()
BigDecimal totalDebited = debitRepository
    .findByMandateId(mandate.getId())
    .stream()
    .filter(d -> d.getStatus() == DebitStatus.SUCCESS)
    .map(DebitTransaction::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal newTotal = totalDebited.add(totalAmount);
if (newTotal.compareTo(mandate.getMaxAmount()) > 0) {
    throw new IllegalStateException(
        "Debit would exceed mandate max amount. " +
        "Already debited: ₦" + totalDebited + 
        ", Attempting: ₦" + totalAmount + 
        ", Max allowed: ₦" + mandate.getMaxAmount()
    );
}
```

### Option 2: Per-Transaction Limit

If `maxAmount` should be per transaction (not cumulative):
- Current implementation is correct
- But rename `maxAmount` to `maxPerTransaction` for clarity

### Option 3: Use Simple Payment Instead

If you want one-time payments (no mandate):
- Switch to `simple_payment` request type
- Requires account details for each payment
- No mandate setup needed
- But: User must authorize each payment separately

---

## Current Status Summary

✅ **Multiple Debits**: Supported (same mandate, different amounts)
✅ **Different Amounts**: Supported (each debit can be different)
⚠️ **Cumulative Limit**: NOT enforced (could exceed maxAmount)
❌ **Simple Payment**: NOT used (using `collect` with mandate)

---

## Questions to Answer

1. **Is `maxAmount` per transaction or cumulative?**
   - If cumulative: Need to add tracking ✅
   - If per transaction: Current code is fine

2. **Do you want to use simple_payment instead?**
   - If yes: Need to refactor to use `simple_payment` request type
   - If no: Current `collect` with mandate is fine

3. **Should we enforce cumulative limit?**
   - Recommended: YES (prevents exceeding mandate authorization)



## Current Implementation

### ✅ Mandate Setup
- **OnePipe Request Type**: `setup_mandate`
- **Purpose**: Creates a direct debit mandate
- **Max Amount**: Set when creating mandate (e.g., ₦100,000)
- **Status**: PENDING → ACTIVE (via webhook)

### ✅ Debit/Collect Implementation
- **OnePipe Request Type**: `collect` (NOT "simple_payment")
- **Purpose**: Debits money from organizer's account using mandate reference
- **How it works**: Uses the mandate reference to authorize each debit

---

## Multiple Debits with Different Amounts

### ✅ YES - You CAN debit multiple times with different amounts!

**Current Implementation:**
1. **Mandate Setup**: User sets a `maxAmount` (e.g., ₦100,000)
2. **Each Debit**: Can be any amount up to `maxAmount`
3. **Multiple Debits**: Can use the same mandate multiple times
4. **Different Amounts**: Each debit can have a different amount

**Example:**
```
Mandate created with maxAmount = ₦100,000

Debit 1: ₦20,000 (for booking 1) ✅
Debit 2: ₦15,000 (for booking 2) ✅
Debit 3: ₦30,000 (for booking 3) ✅
Total debited: ₦65,000 (still within maxAmount)
```

### ⚠️ Current Limitation

**The code does NOT track cumulative debits!**

- Each debit is validated against `maxAmount` individually
- No check for total amount debited across all transactions
- If you debit ₦100,000, you can still debit another ₦100,000 (exceeds max)

**This could be a problem!** The mandate's `maxAmount` should be the **total** that can be debited, not per transaction.

---

## Payment Method Used

### ❌ NOT using "simple_payment"

**Current Implementation:**
- **Request Type**: `collect` (direct debit via mandate)
- **This is**: Mandate-based recurring/repeated debits
- **NOT**: Simple one-time payment

**OnePipe Request Types:**
- `setup_mandate` - Create mandate ✅ (We use this)
- `collect` - Debit using mandate ✅ (We use this)
- `disburse` - Payout/transfer ✅ (We use this)
- `simple_payment` - One-time payment ❌ (We DON'T use this)

---

## How It Works

### Flow:
1. **Setup Mandate**: 
   - User authorizes mandate with maxAmount (e.g., ₦100,000)
   - Mandate status becomes ACTIVE

2. **Debit Transaction**:
   - Each booking creates a debit request
   - Uses the same `mandateRef`
   - Amount can vary (e.g., ₦20,000, ₦15,000, etc.)
   - Each debit is independent

3. **Validation**:
   - Currently: Only checks if mandate is ACTIVE
   - Currently: Only checks if individual debit ≤ maxAmount
   - **Missing**: Check if (cumulative debits + new debit) ≤ maxAmount

---

## Recommendations

### Option 1: Track Cumulative Debits (Recommended)

Add validation to ensure total debits don't exceed maxAmount:

```java
// In PaymentService.initiateDebitForBooking()
BigDecimal totalDebited = debitRepository
    .findByMandateId(mandate.getId())
    .stream()
    .filter(d -> d.getStatus() == DebitStatus.SUCCESS)
    .map(DebitTransaction::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal newTotal = totalDebited.add(totalAmount);
if (newTotal.compareTo(mandate.getMaxAmount()) > 0) {
    throw new IllegalStateException(
        "Debit would exceed mandate max amount. " +
        "Already debited: ₦" + totalDebited + 
        ", Attempting: ₦" + totalAmount + 
        ", Max allowed: ₦" + mandate.getMaxAmount()
    );
}
```

### Option 2: Per-Transaction Limit

If `maxAmount` should be per transaction (not cumulative):
- Current implementation is correct
- But rename `maxAmount` to `maxPerTransaction` for clarity

### Option 3: Use Simple Payment Instead

If you want one-time payments (no mandate):
- Switch to `simple_payment` request type
- Requires account details for each payment
- No mandate setup needed
- But: User must authorize each payment separately

---

## Current Status Summary

✅ **Multiple Debits**: Supported (same mandate, different amounts)
✅ **Different Amounts**: Supported (each debit can be different)
⚠️ **Cumulative Limit**: NOT enforced (could exceed maxAmount)
❌ **Simple Payment**: NOT used (using `collect` with mandate)

---

## Questions to Answer

1. **Is `maxAmount` per transaction or cumulative?**
   - If cumulative: Need to add tracking ✅
   - If per transaction: Current code is fine

2. **Do you want to use simple_payment instead?**
   - If yes: Need to refactor to use `simple_payment` request type
   - If no: Current `collect` with mandate is fine

3. **Should we enforce cumulative limit?**
   - Recommended: YES (prevents exceeding mandate authorization)



