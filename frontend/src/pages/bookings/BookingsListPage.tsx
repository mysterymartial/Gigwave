import { Link } from 'react-router-dom';
import { useListMyBookings } from '../../hooks/useBookings';
import { useAuth } from '../../hooks/useAuth';
import { UserRole } from '../../types';
import { format } from 'date-fns';
import { BookingStatus, PaymentStatus } from '../../types';

export default function BookingsListPage() {
  const { user } = useAuth();
  const { data: bookings, isLoading } = useListMyBookings();
  const isAdmin = user?.role === UserRole.ADMIN;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500" />
          <p className="text-gray-600 dark:text-gray-400 mt-4">Loading bookings...</p>
        </div>
      </div>
    );
  }

  const list = bookings ?? [];
  const hasBookings = list.length > 0;

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Bookings & Messages</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">
          {isAdmin
            ? 'View and manage booking-related messages and details.'
            : 'Your bookings and conversations with musicians or event owners.'}
        </p>
      </div>

      {isAdmin && !hasBookings && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-8 text-center shadow-sm">
          <p className="text-gray-700 dark:text-gray-300 mb-2">
            You have no personal bookings as an admin account.
          </p>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Use <Link to="/admin/reports" className="text-teal-600 dark:text-teal-400 hover:underline font-medium">Reports</Link> or{' '}
            <Link to="/admin/customers" className="text-teal-600 dark:text-teal-400 hover:underline font-medium">Customers</Link> to view and manage bookings and user activity.
          </p>
        </div>
      )}

      {!isAdmin && !hasBookings && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-8 text-center shadow-sm">
          <p className="text-gray-700 dark:text-gray-300">You have no bookings yet.</p>
          <Link
            to="/gigs"
            className="inline-block mt-4 text-teal-600 dark:text-teal-400 hover:underline font-medium"
          >
            Browse gigs →
          </Link>
        </div>
      )}

      {hasBookings && (
        <div className="space-y-4">
          {list.map((booking) => (
            <Link
              key={booking.id}
              to={`/bookings/${booking.id}`}
              className="block bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:border-teal-500 dark:hover:border-teal-500 shadow-sm transition-colors"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-gray-900 dark:text-white font-medium">Booking #{booking.id.slice(0, 8)}</span>
                <span className="text-sm text-gray-600 dark:text-gray-400">
                  {format(new Date(booking.createdAt), 'MMM d, yyyy')}
                </span>
              </div>
              <div className="mt-2 flex flex-wrap gap-2">
                <span
                  className={`inline-flex text-xs font-medium px-2 py-1 rounded ${
                    booking.bookingStatus === BookingStatus.COMPLETED
                      ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
                      : booking.bookingStatus === BookingStatus.IN_PROGRESS
                      ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300'
                      : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
                  }`}
                >
                  {booking.bookingStatus}
                </span>
                <span
                  className={`inline-flex text-xs font-medium px-2 py-1 rounded ${
                    booking.paymentStatus === PaymentStatus.PAID_OUT
                      ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
                      : booking.paymentStatus === PaymentStatus.DEBIT_PENDING || booking.paymentStatus === PaymentStatus.DEBIT_SUCCESS
                      ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300'
                      : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
                  }`}
                >
                  {booking.paymentStatus}
                </span>
              </div>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                Amount: ₦{booking.acceptedAmount?.toLocaleString()}
              </p>
              <p className="mt-1 text-sm text-teal-600 dark:text-teal-400">View details →</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
