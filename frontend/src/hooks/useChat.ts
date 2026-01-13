import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '../lib/api';
import type { ChatMessage, ChatThread } from '../types';

export const useChatThread = (bookingId: string) => {
  return useQuery({
    queryKey: ['chatThread', bookingId],
    queryFn: () => chatApi.getThread(bookingId),
    enabled: !!bookingId,
  });
};

export const useChatMessages = (threadId: string) => {
  return useQuery({
    queryKey: ['chatMessages', threadId],
    queryFn: () => chatApi.listMessages(threadId),
    enabled: !!threadId,
  });
};

export const useSendMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ threadId, data }: { threadId: string; data: Partial<ChatMessage> }) =>
      chatApi.sendMessage(threadId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['chatMessages', variables.threadId] });
    },
  });
};

export const useOpenThread = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (bookingId: string) => chatApi.openThread(bookingId),
    onSuccess: (_, bookingId) => {
      queryClient.invalidateQueries({ queryKey: ['chatThread', bookingId] });
    },
  });
};

export const useOpenDirectThread = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId1, userId2 }: { userId1: string; userId2: string }) =>
      chatApi.openDirectThread(userId1, userId2),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chatThread'] });
    },
  });
};

export const useDirectThread = (userId1: string, userId2: string) => {
  return useQuery({
    queryKey: ['directThread', userId1, userId2],
    queryFn: () => chatApi.getDirectThread(userId1, userId2),
    enabled: !!userId1 && !!userId2,
  });
};

export const useMyDirectThreads = () => {
  return useQuery({
    queryKey: ['myDirectThreads'],
    queryFn: () => chatApi.getMyDirectThreads(),
  });
};



