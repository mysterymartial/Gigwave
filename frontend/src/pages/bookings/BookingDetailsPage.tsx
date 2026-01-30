import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useGetBooking, useMarkBookingDone, useConfirmAndPay } from '../../hooks/useBookings';
import { useAuth } from '../../hooks/useAuth';
import { usePlatformFee, useValidateOtp } from '../../hooks/usePayments';
import { UserRole, BookingStatus, PaymentStatus } from '../../types';
import BookingTimeline from '../../components/BookingTimeline';

export default function BookingDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const { data: booking, isLoading } = useGetBooking(id!);
  const { data: platformFee } = usePlatformFee();
  const markDone = useMarkBookingDone();
  const confirmAndPay = useConfirmAndPay();
  const validateOtp = useValidateOtp();
  const [otp, setOtp] = useState('');
  const [showOtpInput, setShowOtpInput] = useState(false);

  const handleMarkDone = async () => {
    if (!booking) return;
    try {
      await markDone.mutateAsync(booking.id);
    } catch (error) {
      console.error('Failed to mark done', error);
    }
  };

  const handleConfirmAndPay = async () => {
    if (!booking) return;
    try {
      const updatedBooking = await confirmAndPay.mutateAsync(booking.id);
      // If payment status is DEBIT_PENDING, OTP might be required
      // Show OTP input to allow user to enter OTP if needed
      if (updatedBooking.paymentStatus === PaymentStatus.DEBIT_PENDING) {
        setShowOtpInput(true);
      }
    } catch (error: any) {
      console.error('Failed to confirm and pay', error);
      alert(error?.response?.data?.message || 'Failed to initiate payment');
    }
  };

  const handleValidateOtp = async () => {
    if (!booking || !otp) return;
    try {
      await validateOtp.mutateAsync({ bookingId: booking.id, otp });
      setShowOtpInput(false);
      setOtp('');
      alert('OTP validated successfully');
    } catch (error: any) {
      if (error?.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to validate OTP');
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

  if (!booking) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center text-gray-700 dark:text-gray-400">Booking not found</div>
      </div>
    );
  }

  const isMusician = user?.role === UserRole.MUSICIAN && user.id === booking.musicianId;
  const isOrganizer = user?.role === UserRole.EVENT_OWNER;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-6 sm:py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="bg-white dark:bg-emerald-900/50 rounded-2xl border border-gray-200 dark:border-emerald-700/40 p-4 sm:p-6 lg:p-8 shadow-xl">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6">Booking Details</h1>

        <div className="mb-6">
          <BookingTimeline
            bookingStatus={booking.bookingStatus}
            paymentStatus={booking.paymentStatus}
            acceptedAt={booking.acceptedAt}
            musicianDoneAt={booking.musicianDoneAt}
            ownerConfirmedAt={booking.ownerConfirmedAt}
            completedAt={booking.completedAt}
          />
        </div>

        <div className="border-t border-gray-200 dark:border-emerald-700/40 pt-6 space-y-4">
          <div className="text-gray-700 dark:text-emerald-200/90">
            <span className="font-medium text-gray-900 dark:text-white">Accepted Amount:</span>
            <span className="ml-2 text-teal-600 dark:text-teal-400">₦{booking.acceptedAmount?.toLocaleString()}</span>
          </div>
          {isOrganizer && platformFee && (
            <div className="bg-teal-50 dark:bg-teal-500/20 border border-teal-200 dark:border-teal-500/40 rounded-lg p-4">
              <p className="text-sm text-teal-800 dark:text-teal-200">
                <strong>Platform Fee:</strong> A fixed fee of ₦{platformFee.platformFeeAmount.toLocaleString()} will be added to your payment.
                <br />
                <span className="text-xs">Total amount to be debited: ₦{(booking.acceptedAmount + platformFee.platformFeeAmount).toLocaleString()}</span>
              </p>
            </div>
          )}
          <div className="text-gray-700 dark:text-emerald-200/90">
            <span className="font-medium text-gray-900 dark:text-white">Booking Status:</span>
            <span className="ml-2">{booking.bookingStatus}</span>
          </div>
          <div className="text-gray-700 dark:text-emerald-200/90">
            <span className="font-medium text-gray-900 dark:text-white">Payment Status:</span>
            <span className="ml-2">{booking.paymentStatus}</span>
          </div>
        </div>

        <div className="border-t border-gray-200 dark:border-emerald-700/40 pt-6 mt-6 space-y-4">
          <div className="flex flex-wrap gap-3">
            <Link
              to={`/bookings/${booking.id}/chat`}
              className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-white rounded-lg font-medium transition-colors"
            >
              Chat
            </Link>
            <Link
              to={`/bookings/${booking.id}/reviews`}
              className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-white rounded-lg font-medium transition-colors"
            >
              Reviews
            </Link>
            <Link
              to={`/bookings/${booking.id}/disputes`}
              className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-white rounded-lg font-medium transition-colors"
            >
              Disputes
            </Link>
          </div>

          {isMusician && booking.bookingStatus === BookingStatus.ACCEPTED && (
            <button
              onClick={handleMarkDone}
              disabled={markDone.isPending}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
            >
              {markDone.isPending ? 'Marking...' : 'Mark Performance Done'}
            </button>
          )}

          {isOrganizer &&
            booking.bookingStatus === BookingStatus.IN_PROGRESS &&
            booking.paymentStatus === PaymentStatus.NOT_INITIATED && (
              <button
                onClick={handleConfirmAndPay}
                disabled={confirmAndPay.isPending}
                className="w-full bg-teal-500 hover:bg-teal-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
              >
                {confirmAndPay.isPending ? 'Processing...' : 'Confirm & Pay'}
              </button>
            )}

          {booking.paymentStatus === PaymentStatus.DEBIT_PENDING && !showOtpInput && (
            <div className="bg-yellow-50 dark:bg-yellow-500/20 border border-yellow-200 dark:border-yellow-500/40 rounded-lg p-4">
              <p className="text-yellow-800 dark:text-yellow-200">Payment is being processed. Please wait...</p>
            </div>
          )}

          {showOtpInput && (
            <div className="bg-teal-50 dark:bg-teal-500/20 border border-teal-200 dark:border-teal-500/40 rounded-lg p-3 sm:p-4">
              <p className="text-sm sm:text-base text-teal-800 dark:text-teal-200 mb-3">Please enter the OTP sent to your phone to complete the payment.</p>
              <div className="flex flex-col sm:flex-row gap-2 sm:space-x-2">
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="Enter OTP"
                  className="flex-1 px-3 sm:px-4 py-2 bg-white dark:bg-gray-800/80 border border-gray-300 dark:border-emerald-700/50 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 text-sm sm:text-base"
                  maxLength={6}
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleValidateOtp}
                    disabled={validateOtp.isPending || !otp}
                    className="flex-1 sm:flex-none px-4 py-2 bg-teal-500 hover:bg-teal-400 text-white rounded-lg disabled:opacity-50 transition-colors text-sm sm:text-base"
                  >
                    {validateOtp.isPending ? 'Validating...' : 'Validate'}
                  </button>
                  <button
                    onClick={() => {
                      setShowOtpInput(false);
                      setOtp('');
                    }}
                    className="flex-1 sm:flex-none px-4 py-2 border-2 border-gray-300 dark:border-emerald-700/50 rounded-lg text-gray-700 dark:text-emerald-200 hover:bg-gray-100 dark:hover:bg-emerald-900/60 transition-colors text-sm sm:text-base"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}

          {booking.paymentStatus === PaymentStatus.DEBIT_SUCCESS && (
            <div className="bg-emerald-50 dark:bg-emerald-500/20 border border-emerald-200 dark:border-emerald-500/40 rounded-lg p-4">
              <p className="text-emerald-800 dark:text-emerald-200">Payment successful! Payout to musician is in progress.</p>
            </div>
          )}

          {booking.paymentStatus === PaymentStatus.PAID_OUT && (
            <div className="bg-emerald-50 dark:bg-emerald-500/20 border border-emerald-200 dark:border-emerald-500/40 rounded-lg p-4">
              <p className="text-emerald-800 dark:text-emerald-200">Payment completed successfully!</p>
            </div>
          )}

          {booking.paymentStatus === PaymentStatus.DEBIT_FAILED && (
            <div className="bg-red-50 dark:bg-red-500/20 border border-red-200 dark:border-red-500/40 rounded-lg p-4">
              <p className="text-red-800 dark:text-red-200">Payment failed. Please try again or contact support.</p>
            </div>
          )}
        </div>
        </div>
      </div>
    </div>
  );
}

