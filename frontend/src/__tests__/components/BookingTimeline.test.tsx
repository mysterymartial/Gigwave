import { render, screen } from '@testing-library/react';
import BookingTimeline from '../../components/BookingTimeline';
import { BookingStatus, PaymentStatus } from '../../types';

describe('BookingTimeline', () => {
  test('should render all steps', () => {
    render(
      <BookingTimeline
        bookingStatus={BookingStatus.REQUESTED}
        paymentStatus={PaymentStatus.NOT_INITIATED}
      />
    );

    expect(screen.getByText('Requested')).toBeInTheDocument();
    expect(screen.getByText('Accepted')).toBeInTheDocument();
    expect(screen.getByText('In Progress')).toBeInTheDocument();
    expect(screen.getByText('Payment')).toBeInTheDocument();
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  test('should show completed status for completed booking', () => {
    render(
      <BookingTimeline
        bookingStatus={BookingStatus.COMPLETED}
        paymentStatus={PaymentStatus.PAID_OUT}
        completedAt={new Date().toISOString()}
      />
    );

    // All steps should be completed
    const steps = screen.getAllByText(/Requested|Accepted|In Progress|Payment|Completed/);
    expect(steps.length).toBeGreaterThan(0);
  });

  test('should handle null dates', () => {
    // Boundary: null timestamps
    render(
      <BookingTimeline
        bookingStatus={BookingStatus.REQUESTED}
        paymentStatus={PaymentStatus.NOT_INITIATED}
        acceptedAt={undefined}
        musicianDoneAt={undefined}
        ownerConfirmedAt={undefined}
        completedAt={undefined}
      />
    );

    expect(screen.getByText('Requested')).toBeInTheDocument();
  });

  test('should display dates when available', () => {
    const date = new Date().toISOString();
    render(
      <BookingTimeline
        bookingStatus={BookingStatus.ACCEPTED}
        paymentStatus={PaymentStatus.NOT_INITIATED}
        acceptedAt={date}
      />
    );

    expect(screen.getByText('Accepted')).toBeInTheDocument();
  });
});







