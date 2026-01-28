import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { UserRole } from '../types';

export default function UserRoleCards() {
  const { user } = useAuth();

  return (
    <section className="py-12 px-4 sm:px-6 lg:px-8 bg-white dark:bg-gray-900 transition-colors">
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Event Owner Card - dark green card per design */}
          <div className="bg-emerald-900/90 dark:bg-emerald-900/80 rounded-xl p-8 border border-emerald-700/50 dark:border-emerald-700/50 hover:border-teal-400 dark:hover:border-teal-400 transition-colors relative overflow-hidden">
            <div className="flex justify-between items-start mb-6">
              <div className="flex-1">
                <h3 className="text-2xl font-bold text-white mb-3">I'm an Event Owner</h3>
                <p className="text-emerald-100/90">
                  Organizing a concert, wedding, or private party? Find verified talent quickly.
                </p>
              </div>
              <div className="ml-4 opacity-60">
                <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                </svg>
              </div>
            </div>
            {user?.role === UserRole.EVENT_OWNER ? (
              <Link
                to="/gigs/create"
                className="inline-flex items-center space-x-2 bg-emerald-500 hover:bg-emerald-400 text-white px-6 py-3 rounded-lg font-semibold transition-colors shadow-lg"
              >
                <span>Post a gig</span>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </Link>
            ) : (
              <Link
                to="/register?role=EVENT_OWNER"
                className="inline-flex items-center space-x-2 bg-emerald-500 hover:bg-emerald-400 text-white px-6 py-3 rounded-lg font-semibold transition-colors shadow-lg"
              >
                <span>Post a gig</span>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </Link>
            )}
          </div>

          {/* Musician Card - dark green card per design */}
          <div className="bg-emerald-900/90 dark:bg-emerald-900/80 rounded-xl p-8 border border-emerald-700/50 dark:border-emerald-700/50 hover:border-teal-400 dark:hover:border-teal-400 transition-colors relative overflow-hidden">
            <div className="flex justify-between items-start mb-6">
              <div className="flex-1">
                <h3 className="text-2xl font-bold text-white mb-3">I'm a Musician</h3>
                <p className="text-emerald-100/90">
                  Looking for your next performance? Browse gigs and get paid securely.
                </p>
              </div>
              <div className="ml-4 opacity-60">
                <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
                </svg>
              </div>
            </div>
            {user?.role === UserRole.MUSICIAN ? (
              <Link
                to="/gigs"
                className="inline-flex items-center space-x-2 bg-emerald-500 hover:bg-emerald-400 text-white px-6 py-3 rounded-lg font-semibold transition-colors shadow-lg"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span>Find gigs</span>
              </Link>
            ) : (
              <Link
                to="/register?role=MUSICIAN"
                className="inline-flex items-center space-x-2 bg-emerald-500 hover:bg-emerald-400 text-white px-6 py-3 rounded-lg font-semibold transition-colors shadow-lg"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span>Find gigs</span>
              </Link>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

