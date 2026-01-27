import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { bankAccountApi } from '../lib/api';
import type { BankAccount } from '../types';

export const useBankAccounts = () => {
  return useQuery({
    queryKey: ['bankAccounts'],
    queryFn: () => bankAccountApi.list(),
  });
};

export const useAddBankAccount = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Omit<BankAccount, 'id' | 'userId'>) => bankAccountApi.add(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bankAccounts'] });
    },
  });
};

export const useSetDefaultBankAccount = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (accountId: string) => bankAccountApi.setDefault(accountId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bankAccounts'] });
    },
  });
};

export const useDeleteBankAccount = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (accountId: string) => bankAccountApi.delete(accountId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bankAccounts'] });
    },
  });
};







