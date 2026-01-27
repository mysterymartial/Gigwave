package com.gigwave.infrastructure.payments.transfer;

import com.gigwave.infrastructure.payments.transfer.dto.TransferRequest;
import com.gigwave.infrastructure.payments.transfer.dto.TransferResponse;

/**
 * Interface for bank transfer/payout providers
 * Supports multiple providers: Flutterwave, Paystack, Monnify, etc.
 */
public interface TransferClient {
    /**
     * Transfer money to a bank account
     * @param request Transfer request with account details and amount
     * @return Transfer response with transaction reference and status
     */
    TransferResponse initiateTransfer(TransferRequest request);
}
