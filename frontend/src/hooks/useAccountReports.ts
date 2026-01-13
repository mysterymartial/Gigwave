import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { accountReportApi } from '../lib/api';
import type { AccountReport, ReportStatus } from '../types';

export const useMyReports = () => {
  return useQuery({
    queryKey: ['accountReports', 'my-reports'],
    queryFn: () => accountReportApi.getMyReports(),
  });
};

export const useReportsAgainstMe = () => {
  return useQuery({
    queryKey: ['accountReports', 'against-me'],
    queryFn: () => accountReportApi.getReportsAgainstMe(),
  });
};

export const useCreateReport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { reportedUserId: string; reason: string; evidenceUrl?: string }) =>
      accountReportApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountReports'] });
    },
  });
};

export const useGetReport = (reportId: string) => {
  return useQuery({
    queryKey: ['accountReports', reportId],
    queryFn: () => accountReportApi.get(reportId),
    enabled: !!reportId,
  });
};

// Admin hooks
export const useAllReports = () => {
  return useQuery({
    queryKey: ['accountReports', 'admin', 'all'],
    queryFn: () => accountReportApi.getAllReports(),
  });
};

export const usePendingReports = () => {
  return useQuery({
    queryKey: ['accountReports', 'admin', 'pending'],
    queryFn: () => accountReportApi.getPendingReports(),
  });
};

export const useReviewReport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reportId, data }: { reportId: string; data: { status: ReportStatus; adminReview?: string } }) =>
      accountReportApi.reviewReport(reportId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountReports'] });
    },
  });
};

export const useDisableAccount = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason?: string }) =>
      accountReportApi.disableAccount(userId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountReports'] });
    },
  });
};

export const useEnableAccount = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason?: string }) =>
      accountReportApi.enableAccount(userId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountReports'] });
    },
  });
};



