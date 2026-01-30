import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateGig } from '../../hooks/useGigs';
import { useAuth } from '../../hooks/useAuth';
import { useUploadVenueImage } from '../../hooks/useFiles';

export default function CreateGigPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const createGig = useCreateGig();
  const uploadVenueImage = useUploadVenueImage();
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    eventDate: '',
    location: '',
    budgetMin: '',
    budgetMax: '',
    venuePictureUrl: '',
  });
  const [imageUploading, setImageUploading] = useState(false);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      alert('Please upload an image file');
      return;
    }

    setImageUploading(true);
    try {
      const result = await uploadVenueImage.mutateAsync(file);
      setFormData({ ...formData, venuePictureUrl: result.url });
      e.target.value = '';
    } catch (error) {
      console.error('Failed to upload image', error);
      alert('Failed to upload image');
    } finally {
      setImageUploading(false);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const gig = await createGig.mutateAsync({
        organizerId: user!.id,
        title: formData.title,
        description: formData.description,
        eventDate: formData.eventDate,
        location: formData.location,
        budgetMin: parseFloat(formData.budgetMin),
        budgetMax: parseFloat(formData.budgetMax),
        venuePictureUrl: formData.venuePictureUrl || undefined,
      });
      navigate(`/gigs/${gig.id}`);
    } catch (error) {
      console.error('Failed to create gig', error);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-6 sm:py-8 transition-colors">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-white mb-4 sm:mb-6">Post a New Gig</h1>
        <form onSubmit={handleSubmit} className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-2xl border border-emerald-700/40 p-4 sm:p-6 lg:p-8 space-y-4 sm:space-y-6 shadow-xl">
        <div>
          <label className="block text-sm font-medium text-emerald-100 mb-1">Title</label>
          <input
            type="text"
            required
            className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-emerald-100 mb-1">Description</label>
          <textarea
            required
            rows={4}
            className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-emerald-100 mb-1">Event Date</label>
            <input
              type="datetime-local"
              required
              className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              value={formData.eventDate}
              onChange={(e) => setFormData({ ...formData, eventDate: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-emerald-100 mb-1">Location</label>
            <input
              type="text"
              required
              className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              value={formData.location}
              onChange={(e) => setFormData({ ...formData, location: e.target.value })}
            />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-emerald-100 mb-1">Min Budget (₦)</label>
            <input
              type="number"
              required
              className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              value={formData.budgetMin}
              onChange={(e) => setFormData({ ...formData, budgetMin: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-emerald-100 mb-1">Max Budget (₦)</label>
            <input
              type="number"
              required
              className="w-full px-4 py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              value={formData.budgetMax}
              onChange={(e) => setFormData({ ...formData, budgetMax: e.target.value })}
            />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-emerald-100 mb-1">Venue Image (Optional)</label>
          {formData.venuePictureUrl ? (
            <div className="mb-2">
              <img src={formData.venuePictureUrl} alt="Venue" className="w-full h-48 object-cover rounded-lg border border-emerald-700/50" />
              <button
                type="button"
                onClick={() => setFormData({ ...formData, venuePictureUrl: '' })}
                className="mt-2 text-sm text-red-400 hover:text-red-300"
              >
                Remove Image
              </button>
            </div>
          ) : (
            <input
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
              disabled={imageUploading}
              className="block w-full text-sm text-emerald-200 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-teal-500 file:text-white hover:file:bg-teal-400 file:cursor-pointer disabled:opacity-50"
            />
          )}
          {imageUploading && <p className="text-sm text-emerald-200/60 mt-1">Uploading image...</p>}
        </div>
        <div className="flex flex-col sm:flex-row justify-end gap-3 sm:gap-4 sm:space-x-0">
          <button
            type="button"
            onClick={() => navigate('/my-gigs')}
            className="w-full sm:w-auto px-6 py-2 border-2 border-emerald-700/50 rounded-lg text-emerald-200 hover:bg-emerald-900/60 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createGig.isPending}
            className="w-full sm:w-auto px-6 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-400 disabled:opacity-50 transition-colors font-semibold"
          >
            {createGig.isPending ? 'Creating...' : 'Create Gig'}
          </button>
        </div>
      </form>
      </div>
    </div>
  );
}


