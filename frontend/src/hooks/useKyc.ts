import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { kycApi } from '../lib/api';
import type { KycDocument, KycStatus } from '../types';

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





