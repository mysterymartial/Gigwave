import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useDisputesForBooking, useCreateDispute } from '../../hooks/useDisputes';
import { useAuth } from '../../hooks/useAuth';
import { format } from 'date-fns';

export default function DisputesPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  if (!bookingId) return <div>Booking ID required</div>;
  const { user } = useAuth();
  const { data: disputes } = useDisputesForBooking(bookingId);
  const createDispute = useCreateDispute();

  const [reason, setReason] = useState('');
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!user) return;

    let evidenceUrl = '';
    if (evidenceFile) {
      // Note: In a real implementation, you'd create the dispute first, then upload evidence
      // For now, we'll skip evidence upload on dispute creation
      // const uploadResult = await uploadEvidence.mutateAsync({
      //   disputeId: 'temp',
      //   file: evidenceFile,
      // });
      // evidenceUrl = uploadResult.url;
    }

    await createDispute.mutateAsync({
      bookingId,
      raisedBy: user.id,
      reason,
      evidenceUrl: evidenceUrl || undefined,
      status: 'OPEN',
    });
    setReason('');
    setEvidenceFile(null);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6">Disputes</h1>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6 mb-4 sm:mb-6">
        <h2 className="text-lg sm:text-xl font-semibold mb-3 sm:mb-4 text-gray-900 dark:text-white">Raise a Dispute</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Reason</label>
              <textarea
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                rows={4}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Evidence (Optional)</label>
              <input
                type="file"
                accept="image/*,video/*,.pdf"
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                onChange={(e) => setEvidenceFile(e.target.files?.[0] || null)}
              />
            </div>
            <button
              type="submit"
              disabled={createDispute.isPending}
              className="w-full bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 disabled:opacity-50 text-sm sm:text-base"
            >
              {createDispute.isPending ? 'Submitting...' : 'Submit Dispute'}
            </button>
        </form>
      </div>

      <div>
        <h2 className="text-lg sm:text-xl font-semibold mb-3 sm:mb-4 text-gray-900 dark:text-white">Dispute History</h2>
        {disputes && disputes.length > 0 ? (
          <div className="space-y-3 sm:space-y-4">
            {disputes.map((dispute) => (
              <div key={dispute.id} className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-start gap-2 mb-2">
                  <span
                    className={`px-2 py-1 rounded text-xs ${
                      dispute.status === 'OPEN'
                        ? 'bg-yellow-100 text-yellow-800'
                        : dispute.status === 'CANCELLED'
                        ? 'bg-gray-100 text-gray-800'
                        : 'bg-green-100 text-green-800'
                    }`}
                  >
                    {dispute.status}
                  </span>
                  <span className="text-sm text-gray-500">
                    {format(new Date(dispute.createdAt), 'MMM dd, yyyy')}
                  </span>
                </div>
                <p className="text-gray-700 mb-2">{dispute.reason}</p>
                {dispute.evidenceUrl && (
                  <a
                    href={dispute.evidenceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline text-sm"
                  >
                    View Evidence
                  </a>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">No disputes raised</div>
        )}
      </div>
    </div>
  );
}

