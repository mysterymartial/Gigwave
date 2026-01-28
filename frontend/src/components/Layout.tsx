import { Outlet, useLocation, Navigate } from 'react-router-dom';
import AppHeader from './AppHeader';
import Footer from './Footer';
import { useAuth } from '../hooks/useAuth';
import { useBankAccounts } from '../hooks/useBankAccounts';
import { useMusicianProfile } from '../hooks/useProfiles';
import { UserRole } from '../types';

export default function Layout() {
  const { user } = useAuth();
  const location = useLocation();
  const { data: bankAccounts, isLoading: bankAccountsLoading } = useBankAccounts({ enabled: !!user });
  const { data: musicianProfile, isLoading: profileLoading } = useMusicianProfile({ enabled: !!user && user?.role === UserRole.MUSICIAN });

  const pathname = location.pathname;
  const isBankAccountsPage = pathname === '/bank-accounts' || pathname.endsWith('/bank-accounts');
  const isProfilePage = pathname === '/profile' || pathname.endsWith('/profile');

  // Enforce add bank account: logged-in user with no bank accounts must go to /bank-accounts first
  if (user && !bankAccountsLoading) {
    const hasNoBankAccounts = !bankAccounts || bankAccounts.length === 0;
    if (hasNoBankAccounts && !isBankAccountsPage) {
      return <Navigate to="/bank-accounts" replace />;
    }
  }

  // Enforce 3 videos for musicians after they have bank accounts (assumes mandate setup)
  // If musician has bank accounts but less than 3 videos, redirect to profile
  if (user?.role === UserRole.MUSICIAN && !bankAccountsLoading && !profileLoading) {
    const hasBankAccounts = bankAccounts && bankAccounts.length > 0;
    const videoCount = musicianProfile?.performanceVideoUrls?.length || 0;
    const hasLessThan3Videos = videoCount < 3;
    
    if (hasBankAccounts && hasLessThan3Videos && !isProfilePage && !isBankAccountsPage) {
      return <Navigate to="/profile" replace />;
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 flex flex-col transition-colors">
      <AppHeader />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

