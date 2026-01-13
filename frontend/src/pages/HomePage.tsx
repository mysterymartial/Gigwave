import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useListGigs } from '../hooks/useGigs';
import { UserRole } from '../types';
import GigCard from '../components/GigCard';
import HeroSection from '../components/HeroSection';
import UserRoleCards from '../components/UserRoleCards';

export default function HomePage() {
  const { user } = useAuth();
  const { data: gigs, isLoading } = useListGigs();

  return (
    <div className="min-h-screen bg-white dark:bg-gray-900 transition-colors">
      {/* Hero Section */}
      <HeroSection />

      {/* User Role Cards */}
      {!user && <UserRoleCards />}

      {/* Recent Gigs Section */}
      <section className="py-12 px-4 sm:px-6 lg:px-8 bg-white dark:bg-gray-900">
        <div className="max-w-7xl mx-auto">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white">Recent gigs near you</h2>
            <Link
              to="/gigs"
              className="text-teal-600 dark:text-teal-400 hover:text-teal-700 dark:hover:text-teal-300 font-medium transition-colors"
            >
              View all →
            </Link>
          </div>

          {isLoading ? (
            <div className="text-center py-12">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
              <p className="text-gray-600 dark:text-gray-400 mt-4">Loading gigs...</p>
            </div>
          ) : gigs && gigs.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {gigs.slice(0, 6).map((gig) => (
                <GigCard key={gig.id} gig={gig} />
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <p className="text-gray-600 dark:text-gray-400 text-lg mb-4">No gigs available at the moment</p>
              {user?.role === UserRole.EVENT_OWNER && (
                <Link
                  to="/gigs/create"
                  className="inline-block bg-teal-500 hover:bg-teal-600 text-white px-6 py-3 rounded-lg font-semibold transition-colors"
                >
                  Post Your First Gig
                </Link>
              )}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
