import { useState, type FormEvent } from 'react';
import { useKycDocuments, useKycStatus } from '../../hooks/useKyc';
import { useUploadKycDocument } from '../../hooks/useFiles';
import { KycStatus } from '../../types';

export default function KycPage() {
  const { data: documents } = useKycDocuments();
  const { data: status } = useKycStatus();
  const uploadDocument = useUploadKycDocument();

  const [documentType, setDocumentType] = useState('NATIONAL_ID');
  const [file, setFile] = useState<File | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!file) return;
    await uploadDocument.mutateAsync({ file, documentType });
    setFile(null);
  };

  const statusColors: Record<KycStatus, string> = {
    [KycStatus.PENDING]: 'bg-yellow-100 text-yellow-800',
    [KycStatus.VERIFIED]: 'bg-green-100 text-green-800',
    [KycStatus.REJECTED]: 'bg-red-100 text-red-800',
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6">KYC Verification</h1>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6 mb-4 sm:mb-6">
        <div className="mb-3 sm:mb-4">
          <h2 className="text-lg sm:text-xl font-semibold mb-2 text-gray-900 dark:text-white">Verification Status</h2>
          {status && (
            <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColors[status]}`}>
              {status}
            </span>
          )}
        </div>
      </div>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6 mb-4 sm:mb-6">
        <h2 className="text-lg sm:text-xl font-semibold mb-3 sm:mb-4 text-gray-900 dark:text-white">Upload Document</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Document Type</label>
            <select
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              required
            >
              <option value="NATIONAL_ID">National ID</option>
              <option value="PASSPORT">Passport</option>
              <option value="DRIVERS_LICENSE">Driver's License</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Document File</label>
            <input
              type="file"
              accept="image/*,.pdf"
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              required
            />
          </div>
          <button
            type="submit"
            disabled={uploadDocument.isPending}
            className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm sm:text-base"
          >
            {uploadDocument.isPending ? 'Uploading...' : 'Upload Document'}
          </button>
        </form>
      </div>

      <div>
        <h2 className="text-lg sm:text-xl font-semibold mb-3 sm:mb-4 text-gray-900 dark:text-white">Uploaded Documents</h2>
        {documents && documents.length > 0 ? (
          <div className="space-y-3 sm:space-y-4">
            {documents.map((doc) => (
              <div key={doc.id} className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-start gap-2">
                  <div>
                    <h3 className="font-semibold">{doc.documentType}</h3>
                    <p className="text-sm text-gray-500">
                      Uploaded: {new Date(doc.uploadedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <a
                    href={doc.documentUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline"
                  >
                    View
                  </a>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">No documents uploaded yet</div>
        )}
      </div>
    </div>
  );
}



