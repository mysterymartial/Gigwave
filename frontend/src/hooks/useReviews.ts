import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reviewApi } from '../lib/api';
import type { Review } from '../types';

export const useReviewsForUser = (userId: string) => {
  return useQuery({
    queryKey: ['reviews', 'user', userId],
    queryFn: () => reviewApi.listForUser(userId),
    enabled: !!userId,
  });
};

export const useReviewsForBooking = (bookingId: string) => {
  return useQuery({
    queryKey: ['reviews', 'booking', bookingId],
    queryFn: () => reviewApi.listForBooking(bookingId),
    enabled: !!bookingId,
  });
};

export const useAverageRating = (userId: string) => {
  return useQuery({
    queryKey: ['averageRating', userId],
    queryFn: () => reviewApi.getAverageRating(userId),
    enabled: !!userId,
  });
};

export const useCreateReview = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Omit<Review, 'id' | 'createdAt'>) => reviewApi.create(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['reviews', 'user', variables.reviewedUserId] });
      queryClient.invalidateQueries({ queryKey: ['reviews', 'booking', variables.bookingId] });
      queryClient.invalidateQueries({ queryKey: ['averageRating', variables.reviewedUserId] });
    },
  });
};







