import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { disputeApi } from '../lib/api';
import type { Dispute, DisputeStatus } from '../types';

export const useDisputesForBooking = (bookingId: string) => {
  return useQuery({
    queryKey: ['disputes', 'booking', bookingId],
    queryFn: () => disputeApi.listForBooking(bookingId),
    enabled: !!bookingId,
  });
};

export const useMyDisputes = () => {
  return useQuery({
    queryKey: ['myDisputes'],
    queryFn: () => disputeApi.listMyDisputes(),
  });
};

export const useGetDispute = (disputeId: string) => {
  return useQuery({
    queryKey: ['dispute', disputeId],
    queryFn: () => disputeApi.get(disputeId),
    enabled: !!disputeId,
  });
};

export const useCreateDispute = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Omit<Dispute, 'id' | 'createdAt' | 'resolvedAt' | 'status'>) => disputeApi.create(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['disputes', 'booking', variables.bookingId] });
      queryClient.invalidateQueries({ queryKey: ['myDisputes'] });
    },
  });
};

export const useResolveDispute = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ disputeId, resolution }: { disputeId: string; resolution: DisputeStatus }) =>
      disputeApi.resolve(disputeId, resolution),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['dispute', data.id] });
      queryClient.invalidateQueries({ queryKey: ['disputes', 'booking', data.bookingId] });
      queryClient.invalidateQueries({ queryKey: ['myDisputes'] });
    },
  });
};



