import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi } from '../lib/api';
import type { User } from '../types';

function readUserFromStorage(): User | null {
  try {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    return JSON.parse(userStr) as User;
  } catch {
    return null;
  }
}

export const useAuth = () => {
  const [user, setUser] = useState<User | null>(readUserFromStorage);

  useEffect(() => {
    const sync = () => setUser(readUserFromStorage());
    window.addEventListener('storage', sync);
    return () => window.removeEventListener('storage', sync);
  }, []);

  // Re-read user when auth changes (e.g. after login/register in same tab)
  useEffect(() => {
    const onAuthChange = () => setUser(readUserFromStorage());
    window.addEventListener('gigwave-auth-change', onAuthChange);
    return () => window.removeEventListener('gigwave-auth-change', onAuthChange);
  }, []);

  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const isAuthenticated = !!user && !!token;

  return {
    user,
    isAuthenticated,
  };
};

export const useLogin = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { phone: string; password: string }) => authApi.login(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
  });
};

export const useRegister = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { phone: string; email?: string; password: string; role: string }) =>
      authApi.register(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
  });
};

export const useLogout = () => {
  const queryClient = useQueryClient();

  return () => {
    authApi.logout();
    queryClient.clear();
  };
};







