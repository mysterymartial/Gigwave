import { useParams, useNavigate } from 'react-router-dom';
import { useGetGig } from '../../hooks/useGigs';
import { useAcceptGig } from '../../hooks/useBookings';
import { useAuth } from '../../hooks/useAuth';
import { UserRole } from '../../types';
import { format } from 'date-fns';
import { useState } from 'react';
import LocationMap from '../../components/LocationMap';

export default function GigDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: gig, isLoading } = useGetGig(id!);
  const acceptGig = useAcceptGig();
  const [acceptedAmount, setAcceptedAmount] = useState('');

  const handleAccept = async () => {
    if (!acceptedAmount || !gig) return;
    try {
      const booking = await acceptGig.mutateAsync({
        gigId: gig.id,
        acceptedAmount: parseFloat(acceptedAmount),
      });
      navigate(`/bookings/${booking.id}`);
    } catch (error) {
      console.error('Failed to accept gig', error);
    }
  };

  if (isLoading) {
    return <div className="text-center py-8">Loading...</div>;
  }

  if (!gig) {
    return <div className="text-center py-8">Gig not found</div>;
  }

  return (
    <div className="min-h-screen bg-white dark:bg-gray-900 py-8 transition-colors">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">{gig.title}</h1>
          <div className="space-y-4 mb-6">
            <p className="text-gray-700 dark:text-gray-300">{gig.description}</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div className="text-gray-700 dark:text-gray-300">
                <span className="font-medium text-gray-900 dark:text-white">Event Date:</span>
                <span className="ml-2">{format(new Date(gig.eventDate), 'MMM dd, yyyy h:mm a')}</span>
              </div>
              <div className="text-gray-700 dark:text-gray-300">
                <span className="font-medium text-gray-900 dark:text-white">Budget:</span>
                <span className="ml-2 text-teal-600 dark:text-teal-400">
                  ₦{gig.budgetMin?.toLocaleString()} - ₦{gig.budgetMax?.toLocaleString()}
                </span>
              </div>
              <div className="text-gray-700 dark:text-gray-300">
                <span className="font-medium text-gray-900 dark:text-white">Status:</span>
                <span className="ml-2">{gig.status}</span>
              </div>
            </div>
          </div>

          {/* Location Map */}
          <div className="mt-6 border-t border-gray-200 dark:border-gray-700 pt-6">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Location</h2>
            <LocationMap
              location={gig.location}
              latitude={gig.latitude}
              longitude={gig.longitude}
              height="400px"
            />
          </div>

          {user?.role === UserRole.MUSICIAN && gig.status === 'OPEN' && (
            <div className="border-t border-gray-200 dark:border-gray-700 pt-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Accept This Gig</h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Your Accepted Amount (₦)
                  </label>
                  <input
                    type="number"
                    className="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-teal-500 focus:border-teal-500"
                    placeholder="Enter amount"
                    value={acceptedAmount}
                    onChange={(e) => setAcceptedAmount(e.target.value)}
                    min={gig.budgetMin}
                    max={gig.budgetMax}
                  />
                  <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                    Budget range: ₦{gig.budgetMin?.toLocaleString()} - ₦{gig.budgetMax?.toLocaleString()}
                  </p>
                </div>
                <button
                  onClick={handleAccept}
                  disabled={acceptGig.isPending || !acceptedAmount}
                  className="w-full bg-teal-500 hover:bg-teal-600 text-white px-4 py-2 rounded-lg font-semibold disabled:opacity-50 transition-colors"
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


