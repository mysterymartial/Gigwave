import { useState, useEffect, type FormEvent } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useMusicianProfile, useUpdateMusicianProfile } from '../../hooks/useProfiles';
import { useOrganizerProfile, useUpdateOrganizerProfile } from '../../hooks/useProfiles';
import { useUploadPerformanceVideo } from '../../hooks/useFiles';
import { UserRole } from '../../types';

export default function ProfilePage() {
  const { user } = useAuth();
  const { data: musicianProfile } = useMusicianProfile();
  const { data: organizerProfile } = useOrganizerProfile();
  const updateMusician = useUpdateMusicianProfile();
  const updateOrganizer = useUpdateOrganizerProfile();
  const uploadVideo = useUploadPerformanceVideo();

  const [stageName, setStageName] = useState(musicianProfile?.stageName || '');
  const [genres, setGenres] = useState(musicianProfile?.genres?.join(', ') || '');
  const [city, setCity] = useState(musicianProfile?.city || '');
  const [minFee, setMinFee] = useState(musicianProfile?.minFee?.toString() || '');
  const [performanceVideos, setPerformanceVideos] = useState<string[]>(musicianProfile?.performanceVideoUrls || []);
  const [organizationName, setOrganizationName] = useState(organizerProfile?.organizationName || '');
  const [eventTypes, setEventTypes] = useState(organizerProfile?.eventTypes?.join(', ') || '');
  const [videoUploading, setVideoUploading] = useState(false);

  useEffect(() => {
    if (musicianProfile) {
      setStageName(musicianProfile.stageName || '');
      setGenres(musicianProfile.genres?.join(', ') || '');
      setCity(musicianProfile.city || '');
      setMinFee(musicianProfile.minFee?.toString() || '');
      setPerformanceVideos(musicianProfile.performanceVideoUrls || []);
    }
  }, [musicianProfile]);

  const handleVideoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('video/')) {
      alert('Please upload a video file');
      return;
    }

    setVideoUploading(true);
    try {
      const result = await uploadVideo.mutateAsync(file);
      setPerformanceVideos([...performanceVideos, result.url]);
      e.target.value = '';
    } catch (error) {
      console.error('Failed to upload video', error);
      alert('Failed to upload video');
    } finally {
      setVideoUploading(false);
    }
  };

  const handleRemoveVideo = (index: number) => {
    const newVideos = performanceVideos.filter((_, i) => i !== index);
    setPerformanceVideos(newVideos);
  };

  const handleMusicianSubmit = async (e: FormEvent) => {
    e.preventDefault();
    
    // Validate minimum 3 videos
    if (performanceVideos.length < 3) {
      alert('Musician must upload at least 3 performance videos');
      return;
    }

    try {
      await updateMusician.mutateAsync({
        stageName,
        genres: genres.split(',').map((g) => g.trim()).filter(Boolean),
        city,
        minFee: minFee ? parseFloat(minFee) : undefined,
        performanceVideoUrls: performanceVideos,
      });
    } catch (error: any) {
      if (error?.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to update profile');
      }
    }
  };

  const handleOrganizerSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await updateOrganizer.mutateAsync({
      organizationName,
      eventTypes: eventTypes.split(',').map((t) => t.trim()).filter(Boolean),
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold text-white mb-6">My Profile</h1>

        {user?.role === UserRole.MUSICIAN && (
          <div className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-2xl border border-emerald-700/40 p-8 shadow-xl">
            <h2 className="text-xl font-semibold mb-4 text-white">Musician Profile</h2>
            {musicianProfile && (
              <div className="mb-4">
                <p className="text-sm text-emerald-200/80">Rating: {musicianProfile.rating.toFixed(1)} ⭐</p>
              </div>
            )}
            <form onSubmit={handleMusicianSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Stage Name</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={stageName}
                onChange={(e) => setStageName(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Genres (comma-separated)</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={genres}
                onChange={(e) => setGenres(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">City</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={city}
                onChange={(e) => setCity(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Minimum Fee (₦)</label>
              <input
                type="number"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={minFee}
                onChange={(e) => setMinFee(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">
                Performance Videos (Minimum 3 required) - {performanceVideos.length}/3
              </label>
              <input
                type="file"
                accept="video/*"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-teal-500 file:text-white hover:file:bg-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                onChange={handleVideoUpload}
                disabled={videoUploading}
              />
              {performanceVideos.length < 3 && (
                <p className="text-red-400 text-sm mt-1">You must upload at least {3 - performanceVideos.length} more video(s)</p>
              )}
              <div className="mt-2 space-y-2">
                {performanceVideos.map((url, index) => (
                  <div key={index} className="flex items-center justify-between bg-gray-800/60 p-3 rounded-lg border border-emerald-700/30">
                    <span className="text-sm text-emerald-200/80 truncate flex-1">{url}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveVideo(index)}
                      className="text-red-400 hover:text-red-300 text-sm ml-2 font-medium"
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            </div>
            <button
              type="submit"
              disabled={updateMusician.isPending || performanceVideos.length < 3}
              className="w-full bg-teal-500 hover:bg-teal-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
            >
              {updateMusician.isPending ? 'Saving...' : 'Save Profile'}
            </button>
            </form>
          </div>
        )}

      {user?.role === UserRole.EVENT_OWNER && (
        <div className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-2xl border border-emerald-700/40 p-8 shadow-xl">
          <h2 className="text-xl font-semibold mb-4 text-white">Organizer Profile</h2>
          <form onSubmit={handleOrganizerSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Organization Name</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={organizationName}
                onChange={(e) => setOrganizationName(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-emerald-100 mb-1">Event Types (comma-separated)</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={eventTypes}
                onChange={(e) => setEventTypes(e.target.value)}
              />
            </div>
            <button
              type="submit"
              disabled={updateOrganizer.isPending}
              className="w-full bg-teal-500 hover:bg-teal-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
            >
              {updateOrganizer.isPending ? 'Saving...' : 'Save Profile'}
            </button>
          </form>
        </div>
      )}
      </div>
    </div>
  );
}



