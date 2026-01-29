import { useMutation } from '@tanstack/react-query';
import { fileApi } from '../lib/api';

export const useUploadChatMedia = () => {
  return useMutation({
    mutationFn: ({ bookingId, file }: { bookingId: string; file: File }) =>
      fileApi.uploadChatMedia(bookingId, file),
  });
};

export const useUploadDisputeEvidence = () => {
  return useMutation({
    mutationFn: ({ disputeId, file }: { disputeId: string; file: File }) =>
      fileApi.uploadDisputeEvidence(disputeId, file),
  });
};

export const useUploadKycDocument = () => {
  return useMutation({
    mutationFn: ({ file, documentType }: { file: File; documentType: string }) =>
      fileApi.uploadKycDocument(file, documentType),
  });
};

export const useUploadReportEvidence = () => {
  return useMutation({
    mutationFn: (file: File) => fileApi.uploadReportEvidence(file),
  });
};

export const useUploadPerformanceVideo = () => {
  return useMutation({
    mutationFn: (file: File) => fileApi.uploadPerformanceVideo(file),
  });
};

export const useUploadVenueImage = () => {
  return useMutation({
    mutationFn: (file: File) => fileApi.uploadVenueImage(file),
  });
};



