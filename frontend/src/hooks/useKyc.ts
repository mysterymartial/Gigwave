import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { kycApi, fileApi } from '../lib/api';
import type { KycStatus } from '../types';

export const useKycDocuments = () => {
  return useQuery({
    queryKey: ['kycDocuments'],
    queryFn: () => kycApi.listDocuments(),
  });
};

export const useKycStatus = () => {
  return useQuery({
    queryKey: ['kycStatus'],
    queryFn: () => kycApi.getStatus(),
  });
};

export const useSubmitKycDocument = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ documentType, documentUrl }: { documentType: string; documentUrl: string }) =>
      kycApi.submitDocument(documentType, documentUrl),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kycDocuments'] });
    },
  });
};

export const useUploadKycDocument = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ file, documentType }: { file: File; documentType: string }) =>
      fileApi.uploadKycDocument(file, documentType),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kycDocuments'] });
      queryClient.invalidateQueries({ queryKey: ['kycStatus'] });
    },
  });
};

export const useVerifyKyc = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => kycApi.verifyKyc(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kycDocuments'] });
      queryClient.invalidateQueries({ queryKey: ['kycStatus'] });
    },
  });
};

export const useRejectKyc = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => kycApi.rejectKyc(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kycDocuments'] });
      queryClient.invalidateQueries({ queryKey: ['kycStatus'] });
    },
  });
};

export const useUpdateKycStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, status }: { userId: string; status: KycStatus }) =>
      kycApi.updateKycStatus(userId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['kycDocuments'] });
      queryClient.invalidateQueries({ queryKey: ['kycStatus'] });
    },
  });
};

export type { KycStatus };


