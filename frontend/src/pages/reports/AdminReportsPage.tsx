import { useState } from 'react';
import { usePendingReports, useReviewReport, useDisableAccount, useEnableAccount } from '../../hooks/useAccountReports';
import { ReportStatus } from '../../types';
import { format } from 'date-fns';

export default function AdminReportsPage() {
  const { data: reports, isLoading } = usePendingReports();
  const reviewReport = useReviewReport();
  const disableAccount = useDisableAccount();
  const enableAccount = useEnableAccount();

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
    return <div className="text-center py-8">Loading...</div>;
  }

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-6">Admin - Account Reports</h1>

      <div className="space-y-4">
        {reports && reports.length > 0 ? (
          reports.map((report) => (
            <div key={report.id} className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">Report ID: {report.id}</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">Reporter: {report.reporterId}</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">Reported User: {report.reportedUserId}</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    Created: {format(new Date(report.createdAt), 'MMM dd, yyyy h:mm a')}
                  </p>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                  report.status === ReportStatus.PENDING ? 'bg-yellow-100 text-yellow-800' :
                  report.status === ReportStatus.APPROVED ? 'bg-green-100 text-green-800' :
                  'bg-red-100 text-red-800'
                }`}>
                  {report.status}
                </span>
              </div>
              <div className="mb-4">
                <p className="font-semibold text-gray-900 dark:text-white mb-2">Reason:</p>
                <p className="text-gray-700 dark:text-gray-300">{report.reason}</p>
              </div>
              {report.evidenceUrl && (
                <div className="mb-4">
                  <a
                    href={report.evidenceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline text-sm"
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
                    <div className="flex space-x-2">
                      <button
                        onClick={() => handleReview(report.id)}
                        disabled={reviewReport.isPending}
                        className={`px-4 py-2 rounded-md text-white ${
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
                        className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => setSelectedReportId(report.id)}
                  className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  Review Report
                </button>
              )}
            </div>
          ))
        ) : (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400">No pending reports</div>
        )}
      </div>
    </div>
  );
}



