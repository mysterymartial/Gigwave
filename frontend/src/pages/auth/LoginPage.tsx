import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useLogin } from '../../hooks/useAuth';
import { getAuthErrorMessage } from '../../lib/authErrors';

export default function LoginPage() {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const login = useLogin();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await login.mutateAsync({ phone, password });
      navigate('/', { replace: true });
    } catch (err: unknown) {
      setError(getAuthErrorMessage(err, 'Sign in failed. Check your credentials.'));
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        {/* Logo */}
        <Link
          to="/"
          className="flex items-center justify-center space-x-2 mb-8"
        >
          <div className="w-10 h-10 bg-teal-500 rounded-lg flex items-center justify-center">
            <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z" />
            </svg>
          </div>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">GigWave</span>
        </Link>

        {/* Card */}
        <div className="bg-white dark:bg-emerald-900/50 rounded-2xl border border-gray-200 dark:border-emerald-700/40 p-8 shadow-xl">
          <h2 className="text-center text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Sign in to GigWave
          </h2>
          <p className="text-center text-gray-600 dark:text-emerald-200/80 text-sm mb-6">
            Enter your phone or email and password to continue
          </p>

          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-500/20 border border-red-500/50 text-red-200 text-sm">
              {error}
            </div>
          )}

          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-emerald-100 mb-1">
                Phone or email
              </label>
              <input
                id="phone"
                name="phone"
                type="text"
                required
                autoComplete="username"
                className="block w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="08012345678"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-emerald-100 mb-1">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                autoComplete="current-password"
                className="block w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={login.isPending}
              className="w-full flex justify-center py-3 px-4 rounded-lg text-white font-semibold bg-teal-500 hover:bg-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:ring-offset-2 focus:ring-offset-gray-900 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {login.isPending ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-emerald-200/80">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="font-medium text-teal-400 hover:text-teal-300">
              Sign up
            </Link>
          </p>
        </div>

        <p className="mt-6 text-center">
          <Link to="/" className="text-sm text-gray-600 dark:text-gray-400 hover:text-teal-600 dark:hover:text-teal-400">
            ← Back to home
          </Link>
        </p>
      </div>
    </div>
  );
}
