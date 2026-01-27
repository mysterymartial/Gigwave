import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { useAllCustomers, useCustomer, useDebitCustomer } from '../../hooks/useAdmin';
import { adminApi } from '../../lib/api';
import type { Customer, AdminDebitRequest } from '../../types';

vi.mock('../../lib/api');

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('useAdmin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  describe('useAllCustomers', () => {
    it('should fetch all customers', async () => {
      const mockCustomers: Customer[] = [
        {
          id: '1',
          phone: '08012345678',
          email: 'test@example.com',
          role: 'MUSICIAN' as any,
          isDisabled: false,
          createdAt: '2024-01-01T00:00:00Z',
          hasActiveMandate: true,
        },
      ];

      vi.mocked(adminApi.getAllCustomers).mockResolvedValue(mockCustomers);

      const { result } = renderHook(() => useAllCustomers(), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockCustomers);
      expect(adminApi.getAllCustomers).toHaveBeenCalledOnce();
    });
  });

  describe('useCustomer', () => {
    it('should fetch customer by ID', async () => {
      const mockCustomer: Customer = {
        id: '1',
        phone: '08012345678',
        email: 'test@example.com',
        role: 'MUSICIAN' as any,
        isDisabled: false,
        createdAt: '2024-01-01T00:00:00Z',
        hasActiveMandate: true,
      };

      vi.mocked(adminApi.getCustomer).mockResolvedValue(mockCustomer);

      const { result } = renderHook(() => useCustomer('1'), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toEqual(mockCustomer);
      expect(adminApi.getCustomer).toHaveBeenCalledWith('1');
    });
  });

  describe('useDebitCustomer', () => {
    it('should debit customer successfully', async () => {
      const mockResponse = {
        debitTransactionId: 'tx-123',
        customerId: '1',
        amount: 1000,
        status: 'pending',
        transactionRef: 'ref-123',
        message: 'Debit initiated',
        attemptedAt: '2024-01-01T00:00:00Z',
      };

      vi.mocked(adminApi.debitCustomer).mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useDebitCustomer(), { wrapper });

      const debitRequest: AdminDebitRequest = {
        amount: 1000,
        reason: 'Fraudulent activity',
      };

      result.current.mutate({ customerId: '1', data: debitRequest });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(adminApi.debitCustomer).toHaveBeenCalledWith('1', debitRequest);
      expect(result.current.data).toEqual(mockResponse);
    });
  });
});
