import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateReport } from '../../hooks/useAccountReports';
import { useUploadReportEvidence } from '../../hooks/useFiles';
import { useAuth } from '../../hooks/useAuth';

export default function AccountReportPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const createReport = useCreateReport();
  const uploadEvidence = useUploadReportEvidence();

  const [reportedUserId, setReportedUserId] = useState('');
  const [reason, setReason] = useState('');
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null);
  const [evidenceUrl, setEvidenceUrl] = useState<string>('');

  const handleEvidenceUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const result = await uploadEvidence.mutateAsync(file);
      setEvidenceUrl(result.url);
      setEvidenceFile(null);
    } catch (error) {
      console.error('Failed to upload evidence', error);
      alert('Failed to upload evidence');
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!reportedUserId || !reason) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      await createReport.mutateAsync({
        reportedUserId,
        reason,
        evidenceUrl: evidenceUrl || undefined,
      });
      alert('Report submitted successfully');
      navigate('/reports/my-reports');
    } catch (error: any) {
      if (error?.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to submit report');
      }
    }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-6">Report Account</h1>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Reported User ID
            </label>
            <input
              type="text"
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              value={reportedUserId}
              onChange={(e) => setReportedUserId(e.target.value)}
              placeholder="Enter user ID to report"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Reason for Report
            </label>
            <textarea
              required
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Describe the issue or misbehavior..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Evidence (Optional)
            </label>
            <input
              type="file"
              accept="image/*,video/*,.pdf"
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              onChange={handleEvidenceUpload}
            />
            {evidenceUrl && (
              <p className="text-green-600 text-sm mt-1">Evidence uploaded: {evidenceUrl}</p>
            )}
          </div>
          <button
            type="submit"
            disabled={createReport.isPending}
            className="w-full bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 disabled:opacity-50"
          >
            {createReport.isPending ? 'Submitting...' : 'Submit Report'}
          </button>
        </form>
      </div>
    </div>
  );
}



