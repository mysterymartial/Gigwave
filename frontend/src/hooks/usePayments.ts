import { useQuery, useMutation } from '@tanstack/react-query';
import { paymentApi } from '../lib/api';
import type { Bank } from '../types';

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

export const useSetupOrganizerMandate = () => {
  return useMutation({
    mutationFn: ({ bankAccountId, maxAmount }: { bankAccountId: string; maxAmount: number }) =>
      paymentApi.setupOrganizerMandate(bankAccountId, maxAmount),
  });
};

export const useSetupMusicianMandate = () => {
  return useMutation({
    mutationFn: ({ bankAccountId, maxAmount }: { bankAccountId: string; maxAmount: number }) =>
      paymentApi.setupMusicianMandate(bankAccountId, maxAmount),
  });
};


