import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../lib/api';
import type { Customer, AdminDebitRequest, AdminDebitResponse } from '../types';

export const useAllCustomers = () => {
  return useQuery({
    queryKey: ['admin', 'customers'],
    queryFn: () => adminApi.getAllCustomers(),
  });
};

export const useCustomer = (customerId: string) => {
  return useQuery({
    queryKey: ['admin', 'customers', customerId],
    queryFn: () => adminApi.getCustomer(customerId),
    enabled: !!customerId,
  });
};

export const useDebitCustomer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ customerId, data }: { customerId: string; data: AdminDebitRequest }) =>
      adminApi.debitCustomer(customerId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'customers'] });
    },
  });
};
