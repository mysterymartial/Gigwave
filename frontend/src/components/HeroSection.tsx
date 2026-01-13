import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { UserRole } from '../types';

export default function HeroSection() {
  const { user } = useAuth();

  return (
    <section className="relative bg-gradient-to-br from-gray-100 via-white to-gray-100 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 py-20 px-4 sm:px-6 lg:px-8 transition-colors">
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Left Side - Content */}
          <div className="text-gray-900 dark:text-white">
            {/* Location Tag */}
            <div className="inline-block bg-green-600 text-white px-4 py-1 rounded-md text-sm font-medium mb-6">
              LIVE IN LAGOS & ABUJA
            </div>

            {/* Headline */}
            <h1 className="text-5xl md:text-6xl font-bold mb-6 leading-tight">
              Book and get booked for{' '}
              <span className="text-teal-600 dark:text-teal-400">music gigs</span>
            </h1>

            {/* Sub-headline */}
            <p className="text-xl text-gray-700 dark:text-gray-300 mb-8">
              The secure marketplace for Nigerian artists and event organizers. Safe payments, verified talent, and zero hassle.
            </p>

            {/* Features */}
            <div className="flex flex-wrap gap-6 mb-8">
              <div className="flex items-center space-x-2">
                <svg className="w-6 h-6 text-green-600 dark:text-green-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
                <span className="text-gray-700 dark:text-gray-300">Verified Artists</span>
              </div>
              <div className="flex items-center space-x-2">
                <svg className="w-6 h-6 text-green-600 dark:text-green-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                </svg>
                <span className="text-gray-700 dark:text-gray-300">Escrow Payments</span>
              </div>
            </div>

            {/* CTA Buttons */}
            {!user && (
              <div className="flex space-x-4">
                <Link
                  to="/register"
                  className="bg-teal-500 hover:bg-teal-600 text-white px-8 py-3 rounded-lg font-semibold transition-colors"
                >
                  Get Started
                </Link>
                <Link
                  to="/login"
                  className="border-2 border-teal-500 text-teal-400 hover:bg-teal-500 hover:text-white px-8 py-3 rounded-lg font-semibold transition-colors"
                >
                  Sign In
                </Link>
              </div>
            )}
          </div>

          {/* Right Side - Visual Element */}
          <div className="hidden lg:block">
            <div className="relative">
              <div className="bg-gray-200/50 dark:bg-gray-800/50 backdrop-blur-sm rounded-2xl p-6 border border-gray-300 dark:border-gray-700">
                <div className="bg-gray-100 dark:bg-gray-700 rounded-xl p-6">
                  <div className="flex items-center space-x-4 mb-4">
                    <div className="w-16 h-16 rounded-full bg-teal-500 flex items-center justify-center text-white font-bold text-xl">
                      M
                    </div>
                    <div>
                      <div className="text-white font-semibold">Musician Profile</div>
                      <div className="text-gray-400 text-sm">Ready to perform</div>
                    </div>
                  </div>
                  <div className="space-y-2 mb-4">
                    <div className="h-2 bg-gray-600 rounded w-3/4"></div>
                    <div className="h-2 bg-gray-600 rounded w-1/2"></div>
                  </div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex space-x-1">
                      <div className="w-2 h-2 bg-teal-400 rounded-full"></div>
                      <div className="w-2 h-2 bg-teal-400 rounded-full"></div>
                      <div className="w-2 h-2 bg-teal-400 rounded-full"></div>
                    </div>
                    <div className="text-teal-400 text-sm">Rating: 4.8</div>
                  </div>
                  <button className="w-full bg-teal-500 hover:bg-teal-600 text-white py-2 rounded-lg font-semibold transition-colors">
                    Book Now
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

