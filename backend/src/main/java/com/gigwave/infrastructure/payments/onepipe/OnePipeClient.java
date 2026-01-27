package com.gigwave.infrastructure.payments.onepipe;

import com.gigwave.infrastructure.payments.onepipe.dto.*;

public interface OnePipeClient {
    /**
     * Setup a direct debit mandate for a user's bank account
     */
    MandateResponse setupMandate(MandateRequest request);

    /**
     * Initiate a direct debit transaction
     */
    DebitResponse initiateDebit(DebitRequest request);

    /**
     * Validate OTP for a transaction that requires OTP
     */
    DebitResponse validateOtp(String transactionRef, String otp);

    /**
     * Get list of supported banks
     */
    BankListResponse getSupportedBanks();
    
    /**
     * Verify webhook signature
     */
    boolean verifyWebhookSignature(String payload, String signature);
}
