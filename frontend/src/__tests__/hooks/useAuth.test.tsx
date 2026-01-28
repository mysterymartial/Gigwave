import React from 'react';
import { describe, test, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth, useLogin, useRegister } from '../../hooks/useAuth';
import { authApi } from '../../lib/api';
import { UserRole } from '../../types';

vi.mock('../../lib/api');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  test('should return null user when not authenticated', () => {
    const { result } = renderHook(() => useAuth(), { wrapper: createWrapper() });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  test('should return user when authenticated', () => {
    const mockUser = {
      id: '123',
      phone: '08012345678',
      role: UserRole.MUSICIAN,
    };
    localStorage.setItem('user', JSON.stringify(mockUser));
    localStorage.setItem('token', 'test-token');

    const { result } = renderHook(() => useAuth(), { wrapper: createWrapper() });

    expect(result.current.user).toEqual(mockUser);
    expect(result.current.isAuthenticated).toBe(true);
  });
});

describe('useLogin', () => {
  test('should login successfully', async () => {
    const mockResponse = {
      token: 'test-token',
      userId: '123',
      phone: '08012345678',
      role: UserRole.MUSICIAN,
    };

    (authApi.login as any).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => useLogin(), { wrapper: createWrapper() });

    await waitFor(() => {
      result.current.mutate({ phone: '08012345678', password: 'password123' });
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(localStorage.getItem('token')).toBe('test-token');
  });

  test('should handle login failure', async () => {
    (authApi.login as any).mockRejectedValue(new Error('Invalid credentials'));

    const { result } = renderHook(() => useLogin(), { wrapper: createWrapper() });

    result.current.mutate({ phone: '08012345678', password: 'wrongpassword' });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });

  test('should handle empty phone', async () => {
    (authApi.login as any).mockRejectedValue(new Error('Phone is required'));

    const { result } = renderHook(() => useLogin(), { wrapper: createWrapper() });

    result.current.mutate({ phone: '', password: 'password123' });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });

  test('should handle empty password', async () => {
    (authApi.login as any).mockRejectedValue(new Error('Password is required'));

    const { result } = renderHook(() => useLogin(), { wrapper: createWrapper() });

    result.current.mutate({ phone: '08012345678', password: '' });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});

describe('useRegister', () => {
  test('should register successfully', async () => {
    const mockResponse = {
      token: 'test-token',
      userId: '123',
      phone: '08012345678',
      role: UserRole.MUSICIAN,
    };

    (authApi.register as any).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => useRegister(), { wrapper: createWrapper() });

    result.current.mutate({
      phone: '08012345678',
      email: 'test@example.com',
      password: 'password123',
      role: UserRole.MUSICIAN,
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(localStorage.getItem('token')).toBe('test-token');
  });

  test('should handle registration with null email', async () => {
    const mockResponse = {
      token: 'test-token',
      userId: '123',
      phone: '08012345678',
      role: UserRole.MUSICIAN,
    };

    (authApi.register as any).mockResolvedValue(mockResponse);

    const { result } = renderHook(() => useRegister(), { wrapper: createWrapper() });

    result.current.mutate({
      phone: '08012345678',
      email: undefined,
      password: 'password123',
      role: UserRole.MUSICIAN,
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
  });
});
