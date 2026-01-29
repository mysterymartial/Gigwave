import { useParams, useNavigate } from 'react-router-dom';
import { useGetGig } from '../../hooks/useGigs';
import { useAcceptGig } from '../../hooks/useBookings';
import { useAuth } from '../../hooks/useAuth';
import { UserRole } from '../../types';
import { format } from 'date-fns';
import { useState } from 'react';
import LocationMap from '../../components/LocationMap';
import { getAuthErrorMessage } from '../../lib/authErrors';

export default function GigDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: gig, isLoading } = useGetGig(id!);
  const acceptGig = useAcceptGig();
  const [acceptedAmount, setAcceptedAmount] = useState('');
  const [acceptError, setAcceptError] = useState<string | null>(null);

  const handleAccept = async () => {
    if (!acceptedAmount || !gig) return;
    setAcceptError(null);
    try {
      const booking = await acceptGig.mutateAsync({
        gigId: gig.id,
        acceptedAmount: parseFloat(acceptedAmount),
      });
      navigate(`/bookings/${booking.id}`);
    } catch (error) {
      setAcceptError(getAuthErrorMessage(error, 'Failed to accept gig. Please try again.'));
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
          <p className="text-gray-400 mt-4">Loading...</p>
        </div>
      </div>
    );
  }

  if (!gig) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center text-gray-400">Gig not found</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 transition-colors">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-2xl border border-emerald-700/40 p-8 shadow-xl">
          <h1 className="text-3xl font-bold text-white mb-4">{gig.title}</h1>
          <div className="space-y-4 mb-6">
            <p className="text-emerald-200/90">{gig.description}</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div className="text-emerald-200/80">
                <span className="font-medium text-white">Event Date:</span>
                <span className="ml-2">{format(new Date(gig.eventDate), 'MMM dd, yyyy h:mm a')}</span>
              </div>
              <div className="text-emerald-200/80">
                <span className="font-medium text-white">Budget:</span>
                <span className="ml-2 text-teal-400">
                  ₦{gig.budgetMin?.toLocaleString()} - ₦{gig.budgetMax?.toLocaleString()}
                </span>
              </div>
              <div className="text-emerald-200/80">
                <span className="font-medium text-white">Status:</span>
                <span className="ml-2">{gig.status}</span>
              </div>
            </div>
          </div>

          {/* Location Map */}
          <div className="mt-6 border-t border-emerald-700/40 pt-6">
            <h2 className="text-xl font-semibold text-white mb-4">Location</h2>
            <LocationMap
              location={gig.location}
              latitude={gig.latitude}
              longitude={gig.longitude}
              height="400px"
            />
          </div>

          {user?.role === UserRole.MUSICIAN && gig.status === 'OPEN' && (
            <div className="border-t border-emerald-700/40 pt-6">
              <h2 className="text-xl font-semibold text-white mb-4">Accept This Gig</h2>
              {acceptError && (
                <div className="mb-4 p-3 rounded-lg bg-red-500/20 border border-red-500/50 text-red-200 text-sm">
                  {acceptError}
                </div>
              )}
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-emerald-100 mb-1">
                    Your Accepted Amount (₦)
                  </label>
                  <input
                    type="number"
                    className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Enter amount"
                    value={acceptedAmount}
                    onChange={(e) => setAcceptedAmount(e.target.value)}
                    min={gig.budgetMin}
                    max={gig.budgetMax}
                  />
                  <p className="mt-1 text-sm text-emerald-200/70">
                    Budget range: ₦{gig.budgetMin?.toLocaleString()} - ₦{gig.budgetMax?.toLocaleString()}
                  </p>
                </div>
                <button
                  onClick={handleAccept}
                  disabled={acceptGig.isPending || !acceptedAmount}
                  className="w-full bg-teal-500 hover:bg-teal-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
                >
                  {acceptGig.isPending ? 'Accepting...' : 'Accept Gig'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}


