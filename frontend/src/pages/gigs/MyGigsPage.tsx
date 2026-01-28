import { useListMyGigs } from '../../hooks/useGigs';
import GigCard from '../../components/GigCard';
import { Link } from 'react-router-dom';

export default function MyGigsPage() {
  const { data: gigs, isLoading } = useListMyGigs();

  return (
    <div className="min-h-screen bg-gray-900 py-8 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-white">My Gigs</h1>
          <Link
            to="/gigs/create"
            className="bg-teal-500 hover:bg-teal-400 text-white px-6 py-2 rounded-lg font-semibold transition-colors"
          >
            Post New Gig
          </Link>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
            <p className="text-gray-400 mt-4">Loading...</p>
          </div>
        ) : gigs && gigs.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {gigs.map((gig) => (
              <GigCard key={gig.id} gig={gig} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-gray-400">
            <p className="mb-4">You haven't posted any gigs yet.</p>
            <Link
              to="/gigs/create"
              className="text-teal-400 hover:text-teal-300 underline font-medium"
            >
              Post your first gig
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}


