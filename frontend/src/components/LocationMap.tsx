import React, { useMemo } from 'react';
import { LoadScript, GoogleMap, Marker } from '@react-google-maps/api';

interface LocationMapProps {
  location: string;
  latitude?: number;
  longitude?: number;
  height?: string;
}

const LocationMap: React.FC<LocationMapProps> = ({ 
  location, 
  latitude, 
  longitude,
  height = '400px' 
}) => {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

  const center = useMemo(() => {
    if (latitude && longitude) {
      return { lat: latitude, lng: longitude };
    }
    // Default to Lagos, Nigeria if no coordinates
    return { lat: 6.5244, lng: 3.3792 };
  }, [latitude, longitude]);

  const mapContainerStyle = {
    width: '100%',
    height: height,
  };

  const options = {
    disableDefaultUI: false,
    zoomControl: true,
    streetViewControl: true,
    mapTypeControl: true,
  };

  const getDirectionsUrl = () => {
    const address = encodeURIComponent(location);
    return `https://www.google.com/maps/dir/?api=1&destination=${address}`;
  };

  if (!apiKey) {
    return (
      <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-8 text-center">
        <p className="text-gray-600 dark:text-gray-400 mb-4">
          Google Maps API key is not configured. Please add VITE_GOOGLE_MAPS_API_KEY to your .env file.
        </p>
        <div className="text-sm text-gray-500 dark:text-gray-500">
          <p className="font-medium mb-2">Location:</p>
          <p>{location}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="relative rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
        <LoadScript googleMapsApiKey={apiKey}>
          <GoogleMap
            mapContainerStyle={mapContainerStyle}
            center={center}
            zoom={latitude && longitude ? 15 : 12}
            options={options}
          >
            {latitude && longitude && (
              <Marker
                position={{ lat: latitude, lng: longitude }}
                title={location}
              />
            )}
          </GoogleMap>
        </LoadScript>
      </div>
      
      <div className="flex items-center justify-between">
        <div className="text-sm text-gray-600 dark:text-gray-400">
          <span className="font-medium text-gray-900 dark:text-white">Location:</span>
          <span className="ml-2">{location}</span>
        </div>
        <a
          href={getDirectionsUrl()}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center px-4 py-2 bg-teal-500 hover:bg-teal-600 text-white text-sm font-medium rounded-lg transition-colors"
        >
          <svg
            className="w-4 h-4 mr-2"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"
            />
          </svg>
          Get Directions
        </a>
      </div>
    </div>
  );
};

export default LocationMap;
