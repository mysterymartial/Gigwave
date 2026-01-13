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
      user.id === booking.musicianId ? booking.organizerId : booking.musicianId;

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
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-6">Reviews</h1>

      {booking && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Write a Review</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Rating</label>
              <div className="flex space-x-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className={`text-2xl ${star <= rating ? 'text-yellow-400' : 'text-gray-300'}`}
                  >
                    ⭐
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Comment</label>
              <textarea
                className="w-full px-3 py-2 border border-gray-300 rounded-md"
                rows={4}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
              />
            </div>
            <button
              type="submit"
              disabled={createReview.isPending}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
              {createReview.isPending ? 'Submitting...' : 'Submit Review'}
            </button>
          </form>
        </div>
      )}

      <div>
        <h2 className="text-xl font-semibold mb-4">Reviews</h2>
        {reviews && reviews.length > 0 ? (
          <div className="space-y-4">
            {reviews.map((review) => (
              <div key={review.id} className="bg-white rounded-lg shadow-md p-6">
                <div className="flex items-start justify-between">
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

