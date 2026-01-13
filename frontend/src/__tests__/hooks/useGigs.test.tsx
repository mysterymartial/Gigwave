import React from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import { useListGigs, useCreateGig } from '../../hooks/useGigs';
import { gigApi } from '../../lib/api';
import { GigStatus } from '../../types';

vi.mock('../../lib/api');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useListGigs', () => {
  test('should fetch gigs successfully', async () => {
    const mockGigs = [
      {
        id: '1',
        title: 'Test Gig',
        status: GigStatus.OPEN,
        budgetMin: 50000,
        budgetMax: 100000,
      },
    ];

    (gigApi.list as any).mockResolvedValue(mockGigs);

    const { result } = renderHook(() => useListGigs(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual(mockGigs);
  });

  test('should handle empty gig list', async () => {
    // Boundary: empty result
    (gigApi.list as any).mockResolvedValue([]);

    const { result } = renderHook(() => useListGigs(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual([]);
  });

  test('should handle filters', async () => {
    const filters = { city: 'Lagos', minBudget: 50000, maxBudget: 100000 };
    (gigApi.list as any).mockResolvedValue([]);

    const { result } = renderHook(() => useListGigs(filters), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(gigApi.list).toHaveBeenCalledWith(filters);
  });

  test('should handle null filters', async () => {
    // Boundary: null filters
    (gigApi.list as any).mockResolvedValue([]);

    const { result } = renderHook(() => useListGigs(undefined), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });

  test('should handle API error', async () => {
    // Boundary: API failure
    (gigApi.list as any).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useListGigs(), { wrapper: createWrapper() });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});

describe('useCreateGig', () => {
  test('should create gig successfully', async () => {
    const newGig = {
      organizerId: '123',
      title: 'New Gig',
      description: 'Description',
      eventDate: new Date().toISOString(),
      location: 'Lagos',
      budgetMin: 50000,
      budgetMax: 100000,
    };

    const createdGig = { ...newGig, id: '456', status: GigStatus.OPEN, createdAt: new Date().toISOString() };
    (gigApi.create as any).mockResolvedValue(createdGig);

    const { result } = renderHook(() => useCreateGig(), { wrapper: createWrapper() });

    result.current.mutate(newGig);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });

  test('should handle zero budget', async () => {
    // Boundary: zero budget
    const newGig = {
      organizerId: '123',
      title: 'Free Gig',
      budgetMin: 0,
      budgetMax: 0,
    } as any;

    (gigApi.create as any).mockResolvedValue({ ...newGig, id: '456' });

    const { result } = renderHook(() => useCreateGig(), { wrapper: createWrapper() });

    result.current.mutate(newGig);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });

  test('should handle very large budget', async () => {
    // Boundary: very large budget
    const newGig = {
      organizerId: '123',
      title: 'Large Budget Gig',
      budgetMin: 1000000000,
      budgetMax: 2000000000,
    } as any;

    (gigApi.create as any).mockResolvedValue({ ...newGig, id: '456' });

    const { result } = renderHook(() => useCreateGig(), { wrapper: createWrapper() });

    result.current.mutate(newGig);

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });
});


