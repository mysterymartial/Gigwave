import { useState } from 'react';
import { useAllReports, usePendingReports, useReviewReport } from '../../hooks/useAccountReports';
import { ReportStatus } from '../../types';
import { format } from 'date-fns';

type ReportTab = 'pending' | 'all';

export default function AdminReportsPage() {
  const [tab, setTab] = useState<ReportTab>('pending');
  const { data: pendingReports, isLoading: pendingLoading } = usePendingReports();
  const { data: allReports, isLoading: allLoading } = useAllReports();
  const reviewReport = useReviewReport();

  const reports = tab === 'pending' ? pendingReports : allReports;
  const isLoading = tab === 'pending' ? pendingLoading : allLoading;

  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [reviewStatus, setReviewStatus] = useState<ReportStatus>(ReportStatus.APPROVED);
  const [adminReview, setAdminReview] = useState('');

  const handleReview = async (reportId: string) => {
    try {
      await reviewReport.mutateAsync({
        reportId,
        data: {
          status: reviewStatus,
          adminReview: adminReview || undefined,
        },
      });
      setSelectedReportId(null);
      setAdminReview('');
      alert('Report reviewed successfully');
    } catch (error: any) {
      if (error?.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to review report');
      }
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
          <p className="text-gray-600 dark:text-gray-400 mt-4">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-6 sm:py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6">Admin - Account Reports</h1>

        <div className="flex flex-col sm:flex-row gap-2 mb-4 sm:mb-6">
          <button
            onClick={() => setTab('pending')}
            className={`w-full sm:w-auto px-4 py-2 rounded-lg font-medium transition-colors text-sm sm:text-base ${
              tab === 'pending'
                ? 'bg-teal-500 text-white'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
            }`}
          >
            Pending
          </button>
          <button
            onClick={() => setTab('all')}
            className={`w-full sm:w-auto px-4 py-2 rounded-lg font-medium transition-colors text-sm sm:text-base ${
              tab === 'all'
                ? 'bg-teal-500 text-white'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
            }`}
          >
            All Reports
          </button>
        </div>

        <div className="space-y-3 sm:space-y-4">
          {reports && reports.length > 0 ? (
            reports.map((report) => (
              <div key={report.id} className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-xl border border-emerald-700/40 p-4 sm:p-6 shadow-lg">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-start gap-3 mb-3 sm:mb-4">
                <div className="flex-1">
                  <p className="text-xs sm:text-sm text-emerald-200/80 break-all">Report ID: {report.id}</p>
                  <p className="text-xs sm:text-sm text-emerald-200/80">Reporter: {report.reporterId}</p>
                  <p className="text-xs sm:text-sm text-emerald-200/80">Reported User: {report.reportedUserId}</p>
                  <p className="text-xs sm:text-sm text-emerald-200/80">
                    Created: {format(new Date(report.createdAt), 'MMM dd, yyyy h:mm a')}
                  </p>
                </div>
                <span className={`px-2 sm:px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap ${
                  report.status === ReportStatus.PENDING ? 'bg-yellow-500/30 text-yellow-200 border border-yellow-500/50' :
                  report.status === ReportStatus.APPROVED ? 'bg-green-500/30 text-green-200 border border-green-500/50' :
                  'bg-red-500/30 text-red-200 border border-red-500/50'
                }`}>
                  {report.status}
                </span>
              </div>
              <div className="mb-4">
                <p className="font-semibold text-white mb-2">Reason:</p>
                <p className="text-emerald-200/90">{report.reason}</p>
              </div>
              {report.evidenceUrl && (
                <div className="mb-4">
                  <a
                    href={report.evidenceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-teal-400 hover:text-teal-300 text-sm"
                  >
                    View Evidence
                  </a>
                </div>
              )}
              {selectedReportId === report.id ? (
                <div className="border-t pt-4 mt-4">
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Review Status
                      </label>
                      <select
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        value={reviewStatus}
                        onChange={(e) => setReviewStatus(e.target.value as ReportStatus)}
                      >
                        <option value={ReportStatus.APPROVED}>Approve</option>
                        <option value={ReportStatus.REJECTED}>Reject</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Admin Review/Comments
                      </label>
                      <textarea
                        rows={3}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        value={adminReview}
                        onChange={(e) => setAdminReview(e.target.value)}
                      />
                    </div>
                    <div className="flex flex-col sm:flex-row gap-2 sm:space-x-2">
                      <button
                        onClick={() => handleReview(report.id)}
                        disabled={reviewReport.isPending}
                        className={`w-full sm:w-auto px-4 py-2 rounded-md text-white text-sm sm:text-base ${
                          reviewStatus === ReportStatus.APPROVED ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'
                        } disabled:opacity-50`}
                      >
                        {reviewReport.isPending ? 'Processing...' : reviewStatus === ReportStatus.APPROVED ? 'Approve & Disable Account' : 'Reject Report'}
                      </button>
                      <button
                        onClick={() => {
                          setSelectedReportId(null);
                          setAdminReview('');
                        }}
                        className="w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm sm:text-base"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                </div>
              ) : report.status === ReportStatus.PENDING ? (
                <button
                  onClick={() => setSelectedReportId(report.id)}
                  className="mt-3 sm:mt-4 w-full sm:w-auto px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-400 font-semibold transition-colors text-sm sm:text-base"
                >
                  Review Report
                </button>
              ) : null}
            </div>
          ))
        ) : (
          <div className="text-center py-12 text-gray-500 dark:text-gray-400">
            {tab === 'pending' ? 'No pending reports' : 'No reports'}
          </div>
        )}
      </div>
      </div>
    </div>
  );
}





