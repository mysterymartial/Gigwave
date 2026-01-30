import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useReviewsForBooking, useCreateReview } from '../../hooks/useReviews';
import { useGetBooking } from '../../hooks/useBookings';
import { useAuth } from '../../hooks/useAuth';

export default function ReviewsPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  if (!bookingId) return <div>Booking ID required</div>;
  const { user } = useAuth();
  const { data: booking } = useGetBooking(bookingId);
  const { data: reviews } = useReviewsForBooking(bookingId);
  const createReview = useCreateReview();

  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!booking || !user) return;

    const reviewedUserId =
      user.id === booking.musicianId
        ? (booking.organizerId ?? '')
        : booking.musicianId;
    if (!reviewedUserId) return;

    await createReview.mutateAsync({
      bookingId: booking.id,
      reviewerId: user.id,
      reviewedUserId,
      rating,
      comment,
    });
    setComment('');
    setRating(5);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white mb-4 sm:mb-6">Reviews</h1>

      {booking && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6 mb-4 sm:mb-6">
          <h2 className="text-lg sm:text-xl font-semibold mb-3 sm:mb-4 text-gray-900 dark:text-white">Write a Review</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Rating</label>
              <div className="flex space-x-1 sm:space-x-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className={`text-xl sm:text-2xl ${star <= rating ? 'text-yellow-400' : 'text-gray-300'}`}
                  >
                    ⭐
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Comment</label>
              <textarea
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                rows={4}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
              />
            </div>
            <button
              type="submit"
              disabled={createReview.isPending}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm sm:text-base"
            >
              {createReview.isPending ? 'Submitting...' : 'Submit Review'}
            </button>
          </form>
        </div>
      )}

      <div>
        <h2 className="text-lg sm:text-xl font-semibold mb-3 sm:mb-4 text-gray-900 dark:text-white">Reviews</h2>
        {reviews && reviews.length > 0 ? (
          <div className="space-y-3 sm:space-y-4">
            {reviews.map((review) => (
              <div key={review.id} className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 sm:p-6">
                <div className="flex flex-col sm:flex-row items-start justify-between gap-2">
                  <div>
                    <div className="flex items-center mb-2">
                      {[...Array(5)].map((_, i) => (
                        <span key={i} className={i < review.rating ? 'text-yellow-400' : 'text-gray-300'}>
                          ⭐
                        </span>
                      ))}
                    </div>
                    {review.comment && <p className="text-gray-700">{review.comment}</p>}
                  </div>
                  <span className="text-sm text-gray-500">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">No reviews yet</div>
        )}
      </div>
    </div>
  );
}

