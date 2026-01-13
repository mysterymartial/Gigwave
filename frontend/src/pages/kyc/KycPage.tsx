import { useState, type FormEvent } from 'react';
import { useKycDocuments, useKycStatus, useUploadKycDocument } from '../../hooks/useKyc';
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
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">KYC Verification</h1>

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <div className="mb-4">
          <h2 className="text-xl font-semibold mb-2">Verification Status</h2>
          {status && (
            <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColors[status]}`}>
              {status}
            </span>
          )}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Upload Document</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Document Type</label>
            <select
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
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
            <label className="block text-sm font-medium text-gray-700 mb-1">Document File</label>
            <input
              type="file"
              accept="image/*,.pdf"
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              required
            />
          </div>
          <button
            type="submit"
            disabled={uploadDocument.isPending}
            className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {uploadDocument.isPending ? 'Uploading...' : 'Upload Document'}
          </button>
        </form>
      </div>

      <div>
        <h2 className="text-xl font-semibold mb-4">Uploaded Documents</h2>
        {documents && documents.length > 0 ? (
          <div className="space-y-4">
            {documents.map((doc) => (
              <div key={doc.id} className="bg-white rounded-lg shadow-md p-6">
                <div className="flex justify-between items-start">
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



