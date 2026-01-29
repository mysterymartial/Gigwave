import { useQuery, useMutation } from '@tanstack/react-query';
import { paymentApi } from '../lib/api';

export const usePaymentBanks = () => {
  return useQuery({
    queryKey: ['paymentBanks'],
    queryFn: () => paymentApi.getBanks(),
  });
};

export const usePlatformFee = () => {
  return useQuery({
    queryKey: ['platformFee'],
    queryFn: () => paymentApi.getPlatformFee(),
  });
};

export const useMandateStatus = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['mandateStatus'],
    queryFn: () => paymentApi.getMandateStatus(),
    enabled: options?.enabled !== false,
  });
};

export const useSetupOrganizerMandate = () => {
  return useMutation({
    mutationFn: ({
      bankAccountId,
      maxAmount,
      bvn,
      idempotencyKey,
    }: {
      bankAccountId: string;
      maxAmount: number;
      bvn?: string;
      idempotencyKey?: string;
    }) => paymentApi.setupOrganizerMandate(bankAccountId, maxAmount, bvn, idempotencyKey),
  });
};

export const useSetupMusicianMandate = () => {
  return useMutation({
    mutationFn: ({
      bankAccountId,
      maxAmount,
      bvn,
      idempotencyKey,
    }: {
      bankAccountId: string;
      maxAmount: number;
      bvn?: string;
      idempotencyKey?: string;
    }) => paymentApi.setupMusicianMandate(bankAccountId, maxAmount, bvn, idempotencyKey),
  });
};

export const useInitiateDebit = () => {
  return useMutation({
    mutationFn: (bookingId: string) => paymentApi.initiateDebit(bookingId),
  });
};

export const useValidateOtp = () => {
  return useMutation({
    mutationFn: ({ bookingId, otp }: { bookingId: string; otp: string }) =>
      paymentApi.validateOtp(bookingId, otp),
  });
};


