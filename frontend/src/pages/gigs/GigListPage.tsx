import { useState } from 'react';
import { useListGigs } from '../../hooks/useGigs';
import GigCard from '../../components/GigCard';

export default function GigListPage() {
  const [city, setCity] = useState('');
  const [minBudget, setMinBudget] = useState('');
  const [maxBudget, setMaxBudget] = useState('');

  const filters = {
    city: city || undefined,
    minBudget: minBudget ? parseFloat(minBudget) : undefined,
    maxBudget: maxBudget ? parseFloat(maxBudget) : undefined,
  };

  const { data: gigs, isLoading } = useListGigs(filters);

  return (
    <div className="min-h-screen bg-gray-900 py-8 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-white mb-6">Find Gigs</h1>

        <div className="bg-emerald-900/40 dark:bg-emerald-900/50 p-6 rounded-xl border border-emerald-700/40 mb-6 shadow-lg">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">City</label>
              <input
                type="text"
                className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="Lagos"
                value={city}
                onChange={(e) => setCity(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Min Budget (₦)</label>
              <input
                type="number"
                className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="0"
                value={minBudget}
                onChange={(e) => setMinBudget(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Max Budget (₦)</label>
              <input
                type="number"
                className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="1000000"
                value={maxBudget}
                onChange={(e) => setMaxBudget(e.target.value)}
              />
            </div>
          </div>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
            <p className="text-gray-600 dark:text-gray-400 mt-4">Loading gigs...</p>
          </div>
        ) : gigs && gigs.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {gigs.map((gig) => (
              <GigCard key={gig.id} gig={gig} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-gray-400">No gigs found matching your criteria</div>
        )}
      </div>
    </div>
  );
}
