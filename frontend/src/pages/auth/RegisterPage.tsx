import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { useRegister } from '../../hooks/useAuth';
import { UserRole } from '../../types';
import { getAuthErrorMessage } from '../../lib/authErrors';

export default function RegisterPage() {
  const [searchParams] = useSearchParams();
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>(UserRole.MUSICIAN);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const register = useRegister();

  // Pre-fill role from URL (?role=EVENT_OWNER or ?role=MUSICIAN)
  useEffect(() => {
    const roleParam = searchParams.get('role');
    if (roleParam === UserRole.EVENT_OWNER || roleParam === UserRole.MUSICIAN) {
      setRole(roleParam);
    }
  }, [searchParams]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const trimmedPhone = phone.trim();
    const trimmedEmail = email ? email.trim() : '';
    if (!trimmedPhone) {
      setError('Phone number is required.');
      return;
    }
    try {
      const payload: { phone: string; email?: string; password: string; role: UserRole } = {
        phone: trimmedPhone,
        password,
        role,
      };
      if (trimmedEmail) payload.email = trimmedEmail;
      await register.mutateAsync(payload);
      // Enforce bank account: after registration send user to add bank account
      navigate('/bank-accounts', { replace: true });
    } catch (err: unknown) {
      setError(getAuthErrorMessage(err, 'Registration failed. Try again.'));
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
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
          <span className="text-2xl font-bold text-white">GigWave</span>
        </Link>

        {/* Card */}
        <div className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-2xl border border-emerald-700/40 p-8 shadow-xl">
          <h2 className="text-center text-2xl font-bold text-white mb-2">
            Create your GigWave account
          </h2>
          <p className="text-center text-emerald-200/80 text-sm mb-6">
            Join as a musician or event owner
          </p>

          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-500/20 border border-red-500/50 text-red-200 text-sm">
              {error}
            </div>
          )}

          <form className="space-y-4" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-emerald-100 mb-1">
                Phone Number
              </label>
              <input
                id="phone"
                name="phone"
                type="tel"
                required
                autoComplete="tel"
                className="block w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="08012345678"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-emerald-100 mb-1">
                Email (optional)
              </label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                className="block w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="email@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
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
                autoComplete="new-password"
                className="block w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="role" className="block text-sm font-medium text-emerald-100 mb-1">
                I am a
              </label>
              <select
                id="role"
                name="role"
                className="block w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
              >
                <option value={UserRole.MUSICIAN}>Musician</option>
                <option value={UserRole.EVENT_OWNER}>Event Owner</option>
              </select>
            </div>

            <button
              type="submit"
              disabled={register.isPending}
              className="w-full flex justify-center py-3 px-4 rounded-lg text-white font-semibold bg-teal-500 hover:bg-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:ring-offset-2 focus:ring-offset-gray-900 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {register.isPending ? 'Creating account...' : 'Sign up'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-emerald-200/80">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-teal-400 hover:text-teal-300">
              Sign in
            </Link>
          </p>
        </div>

        <p className="mt-6 text-center">
          <Link to="/" className="text-sm text-gray-400 hover:text-teal-400">
            ← Back to home
          </Link>
        </p>
      </div>
    </div>
  );
}
