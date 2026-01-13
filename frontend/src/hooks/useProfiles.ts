import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '../lib/api';
import type { MusicianProfile, OrganizerProfile } from '../types';

export const useMusicianProfile = () => {
  return useQuery({
    queryKey: ['musicianProfile'],
    queryFn: () => profileApi.getMusicianProfile(),
  });
};

export const useUpdateMusicianProfile = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<MusicianProfile>) => profileApi.updateMusicianProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['musicianProfile'] });
    },
  });
};

export const useOrganizerProfile = () => {
  return useQuery({
    queryKey: ['organizerProfile'],
    queryFn: () => profileApi.getOrganizerProfile(),
  });
};

export const useUpdateOrganizerProfile = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<OrganizerProfile>) => profileApi.updateOrganizerProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizerProfile'] });
    },
  });
};





