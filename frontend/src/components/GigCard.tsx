import { Link, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { useState, useEffect } from 'react';
import type { Gig, OrganizerProfile } from '../types';
import { GigStatus } from '../types';
import { profileApi } from '../lib/api';
import { useAuth } from '../hooks/useAuth';
import { UserRole } from '../types';

interface GigCardProps {
  gig: Gig;
}

// Determine gig category/tag based on title or description
const getGigTag = (gig: Gig): string => {
  const title = gig.title.toLowerCase();
  if (title.includes('wedding') || title.includes('band')) return 'Live Band';
  if (title.includes('dj') || title.includes('disc jockey')) return 'DJ';
  if (title.includes('session') || title.includes('studio')) return 'Session';
  if (title.includes('solo') || title.includes('pianist') || title.includes('guitarist')) return 'Soloist';
  if (title.includes('orchestra') || title.includes('violin')) return 'Orchestra';
  if (title.includes('drum') || title.includes('drummer')) return 'Drummer';
  return 'Gig';
};

export default function GigCard({ gig }: GigCardProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [organizerProfile, setOrganizerProfile] = useState<OrganizerProfile | null>(null);
  const [isApplying, setIsApplying] = useState(false);

  useEffect(() => {
    // Fetch organizer profile for display
    profileApi.getOrganizerProfileByUserId(gig.organizerId)
      .then(setOrganizerProfile)
      .catch(() => {
        // If profile not found, that's okay - we'll show a default
      });
  }, [gig.organizerId]);

  const handleApplyNow = async (e: React.MouseEvent) => {
    e.preventDefault();
    if (!user || user.role !== UserRole.MUSICIAN) {
      navigate('/login');
      return;
    }

    setIsApplying(true);
    try {
      // Navigate to gig details page where musician can set accepted amount
      navigate(`/gigs/${gig.id}`);
    } catch (error) {
      console.error('Error applying to gig', error);
    } finally {
      setIsApplying(false);
    }
  };

  const gigTag = getGigTag(gig);
  const organizerName = organizerProfile?.organizationName || 'Organizer';
  const displayPrice = gig.budgetMax ? `₦${gig.budgetMax.toLocaleString()}` : `₦${gig.budgetMin?.toLocaleString() || '0'}`;

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700 hover:border-teal-500 dark:hover:border-teal-500 transition-all hover:shadow-lg">
      {/* Gig Image */}
      <div className="relative h-48 bg-gradient-to-br from-gray-200 to-gray-300 dark:from-gray-700 dark:to-gray-900">
        {gig.venuePictureUrl ? (
          <img
            src={gig.venuePictureUrl}
            alt={gig.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <svg className="w-16 h-16 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
            </svg>
          </div>
        )}
        {/* Tag */}
        <div className="absolute top-3 right-3 bg-gray-900/80 dark:bg-gray-900/80 backdrop-blur-sm text-white px-3 py-1 rounded-md text-xs font-medium">
          {gigTag}
        </div>
      </div>

      {/* Card Content */}
      <div className="p-5">
        <Link to={`/gigs/${gig.id}`}>
          <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-3 hover:text-teal-600 dark:hover:text-teal-400 transition-colors">
            {gig.title}
          </h3>
        </Link>

        {/* Price */}
        <div className="text-teal-600 dark:text-teal-400 text-2xl font-bold mb-4">
          {displayPrice}
        </div>

        {/* Date and Location */}
        <div className="space-y-2 text-sm text-gray-600 dark:text-gray-400 mb-4">
          <div className="flex items-center">
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span>{format(new Date(gig.eventDate), 'EEE, dd MMM • h:mm a')}</span>
          </div>
          <div className="flex items-center">
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span>{gig.location}</span>
          </div>
        </div>

        {/* Organizer Info */}
        <div className="flex items-center mb-4 pb-4 border-b border-gray-200 dark:border-gray-700">
          <div className="w-8 h-8 rounded-full bg-teal-500 flex items-center justify-center text-white font-semibold text-sm mr-3">
            {organizerName.charAt(0).toUpperCase()}
          </div>
          <div className="text-sm text-gray-600 dark:text-gray-400">
            By <span className="text-gray-900 dark:text-white">{organizerName}</span>
          </div>
        </div>

        {/* Apply Now Button */}
        {user?.role === UserRole.MUSICIAN && gig.status === GigStatus.OPEN ? (
          <button
            onClick={handleApplyNow}
            disabled={isApplying}
            className="w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-lg font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isApplying ? 'Applying...' : 'Apply Now'}
          </button>
        ) : (
          <Link
            to={`/gigs/${gig.id}`}
            className="block w-full bg-teal-500 hover:bg-teal-600 text-white py-3 rounded-lg font-semibold transition-colors text-center"
          >
            View Details
          </Link>
        )}
      </div>
    </div>
  );
}
