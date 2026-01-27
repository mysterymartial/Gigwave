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
    return <div className="text-center py-8">Loading...</div>;
  }

  if (!booking) {
    return <div className="text-center py-8">Booking not found</div>;
  }

  const isMusician = user?.role === UserRole.MUSICIAN && user.id === booking.musicianId;
  const isOrganizer = user?.role === UserRole.EVENT_OWNER;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white rounded-lg shadow-md p-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">Booking Details</h1>

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

        <div className="border-t pt-6 space-y-4">
          <div>
            <span className="font-medium">Accepted Amount:</span>
            <span className="ml-2">₦{booking.acceptedAmount?.toLocaleString()}</span>
          </div>
          {isOrganizer && platformFee && (
            <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
              <p className="text-sm text-blue-800">
                <strong>Platform Fee:</strong> A fixed fee of ₦{platformFee.platformFeeAmount.toLocaleString()} will be added to your payment.
                <br />
                <span className="text-xs">Total amount to be debited: ₦{(booking.acceptedAmount + platformFee.platformFeeAmount).toLocaleString()}</span>
              </p>
            </div>
          )}
          <div>
            <span className="font-medium">Booking Status:</span>
            <span className="ml-2">{booking.bookingStatus}</span>
          </div>
          <div>
            <span className="font-medium">Payment Status:</span>
            <span className="ml-2">{booking.paymentStatus}</span>
          </div>
        </div>

        <div className="border-t pt-6 mt-6 space-y-4">
          <div className="flex space-x-4">
            <Link
              to={`/bookings/${booking.id}/chat`}
              className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
            >
              Chat
            </Link>
            <Link
              to={`/bookings/${booking.id}/reviews`}
              className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
            >
              Reviews
            </Link>
            <Link
              to={`/bookings/${booking.id}/disputes`}
              className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
            >
              Disputes
            </Link>
          </div>

          {isMusician && booking.bookingStatus === BookingStatus.ACCEPTED && (
            <button
              onClick={handleMarkDone}
              disabled={markDone.isPending}
              className="w-full bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 disabled:opacity-50"
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
                className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {confirmAndPay.isPending ? 'Processing...' : 'Confirm & Pay'}
              </button>
            )}

          {booking.paymentStatus === PaymentStatus.DEBIT_PENDING && !showOtpInput && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4">
              <p className="text-yellow-800">Payment is being processed. Please wait...</p>
            </div>
          )}

          {showOtpInput && (
            <div className="bg-blue-50 border border-blue-200 rounded-md p-4">
              <p className="text-blue-800 mb-3">Please enter the OTP sent to your phone to complete the payment.</p>
              <div className="flex space-x-2">
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="Enter OTP"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md"
                  maxLength={6}
                />
                <button
                  onClick={handleValidateOtp}
                  disabled={validateOtp.isPending || !otp}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  {validateOtp.isPending ? 'Validating...' : 'Validate'}
                </button>
                <button
                  onClick={() => {
                    setShowOtpInput(false);
                    setOtp('');
                  }}
                  className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {booking.paymentStatus === PaymentStatus.DEBIT_SUCCESS && (
            <div className="bg-green-50 border border-green-200 rounded-md p-4">
              <p className="text-green-800">Payment successful! Payout to musician is in progress.</p>
            </div>
          )}

          {booking.paymentStatus === PaymentStatus.PAID_OUT && (
            <div className="bg-green-50 border border-green-200 rounded-md p-4">
              <p className="text-green-800">Payment completed successfully!</p>
            </div>
          )}

          {booking.paymentStatus === PaymentStatus.DEBIT_FAILED && (
            <div className="bg-red-50 border border-red-200 rounded-md p-4">
              <p className="text-red-800">Payment failed. Please try again or contact support.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

