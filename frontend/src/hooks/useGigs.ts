import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gigApi } from '../lib/api';
import type { Gig } from '../types';

export const useListGigs = (
  filters?: { city?: string; minBudget?: number; maxBudget?: number },
  options?: { enabled?: boolean }
) => {
  return useQuery({
    queryKey: ['gigs', filters],
    queryFn: () => gigApi.list(filters),
    enabled: options?.enabled !== false,
  });
};

export const useGetGig = (id: string) => {
  return useQuery({
    queryKey: ['gig', id],
    queryFn: () => gigApi.get(id),
    enabled: !!id,
  });
};

export const useCreateGig = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Omit<Gig, 'id' | 'createdAt' | 'status'>) => gigApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gigs'] });
      queryClient.invalidateQueries({ queryKey: ['myGigs'] });
    },
  });
};

export const useListMyGigs = () => {
  return useQuery({
    queryKey: ['myGigs'],
    queryFn: () => gigApi.listMyGigs(),
  });
};

export const useUpdateGig = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Gig> }) => gigApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['gig', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['gigs'] });
      queryClient.invalidateQueries({ queryKey: ['myGigs'] });
    },
  });
};

export const useCancelGig = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => gigApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['gigs'] });
      queryClient.invalidateQueries({ queryKey: ['myGigs'] });
    },
  });
};







