import { format } from 'date-fns';
import { BookingStatus, PaymentStatus } from '../types';

interface BookingTimelineProps {
  bookingStatus: BookingStatus;
  paymentStatus: PaymentStatus;
  acceptedAt?: string;
  musicianDoneAt?: string;
  ownerConfirmedAt?: string;
  completedAt?: string;
}

export default function BookingTimeline({
  bookingStatus,
  paymentStatus,
  acceptedAt,
  musicianDoneAt,
  ownerConfirmedAt,
  completedAt,
}: BookingTimelineProps) {
  const steps = [
    {
      label: 'Requested',
      status: bookingStatus !== BookingStatus.REQUESTED ? 'completed' : 'current',
      date: null,
    },
    {
      label: 'Accepted',
      status:
        bookingStatus === BookingStatus.ACCEPTED ||
        bookingStatus === BookingStatus.IN_PROGRESS ||
        bookingStatus === BookingStatus.COMPLETED
          ? 'completed'
          : bookingStatus === BookingStatus.REQUESTED
          ? 'current'
          : 'upcoming',
      date: acceptedAt,
    },
    {
      label: 'In Progress',
      status:
        bookingStatus === BookingStatus.IN_PROGRESS || bookingStatus === BookingStatus.COMPLETED
          ? 'completed'
          : bookingStatus === BookingStatus.ACCEPTED
          ? 'current'
          : 'upcoming',
      date: musicianDoneAt,
    },
    {
      label: 'Payment',
      status:
        paymentStatus === PaymentStatus.DEBIT_SUCCESS ||
        paymentStatus === PaymentStatus.PAID_OUT
          ? 'completed'
          : paymentStatus === PaymentStatus.DEBIT_PENDING
          ? 'current'
          : 'upcoming',
      date: ownerConfirmedAt,
    },
    {
      label: 'Completed',
      status: bookingStatus === BookingStatus.COMPLETED ? 'completed' : 'upcoming',
      date: completedAt,
    },
  ];

  return (
    <div className="space-y-4">
      {steps.map((step, index) => (
        <div key={index} className="flex items-start">
          <div className="flex flex-col items-center mr-4">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center ${
                step.status === 'completed'
                  ? 'bg-green-500 text-white'
                  : step.status === 'current'
                  ? 'bg-blue-500 text-white'
                  : 'bg-gray-300 text-gray-600'
              }`}
            >
              {step.status === 'completed' ? '✓' : index + 1}
            </div>
            {index < steps.length - 1 && (
              <div
                className={`w-0.5 h-12 ${
                  step.status === 'completed' ? 'bg-green-500' : 'bg-gray-300'
                }`}
              />
            )}
          </div>
          <div className="flex-1 pb-8">
            <h4 className="font-medium text-gray-900">{step.label}</h4>
            {step.date && (
              <p className="text-sm text-gray-500">
                {format(new Date(step.date), 'MMM dd, yyyy h:mm a')}
              </p>
            )}
            {step.status === 'current' && (
              <p className="text-sm text-blue-600 mt-1">In progress...</p>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}





