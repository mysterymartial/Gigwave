import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { bookingApi } from '../lib/api';
import type { Booking } from '../types';

export const useGetBooking = (id: string) => {
  return useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingApi.get(id),
    enabled: !!id,
  });
};

export const useListMyBookings = () => {
  return useQuery({
    queryKey: ['myBookings'],
    queryFn: () => bookingApi.listMyBookings(),
  });
};

export const useAcceptGig = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ gigId, acceptedAmount }: { gigId: string; acceptedAmount: number }) =>
      bookingApi.accept(gigId, acceptedAmount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    },
  });
};

export const useMarkBookingDone = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (bookingId: string) => bookingApi.markDone(bookingId),
    onSuccess: (_, bookingId) => {
      queryClient.invalidateQueries({ queryKey: ['booking', bookingId] });
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    },
  });
};

export const useConfirmAndPay = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (bookingId: string) => bookingApi.confirmAndPay(bookingId),
    onSuccess: (_, bookingId) => {
      queryClient.invalidateQueries({ queryKey: ['booking', bookingId] });
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    },
  });
};







