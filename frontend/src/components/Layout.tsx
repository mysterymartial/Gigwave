import { Outlet, useLocation, Navigate } from 'react-router-dom';
import AppHeader from './AppHeader';
import Footer from './Footer';
import { useAuth } from '../hooks/useAuth';
import { useBankAccounts } from '../hooks/useBankAccounts';
import { UserRole } from '../types';

export default function Layout() {
  const { user } = useAuth();
  const location = useLocation();
  const { data: bankAccounts, isLoading: bankAccountsLoading } = useBankAccounts({ enabled: !!user });

  const pathname = location.pathname;
  const isBankAccountsPage = pathname === '/bank-accounts' || pathname.endsWith('/bank-accounts');

  // Enforce add bank account: logged-in user with no bank accounts must go to /bank-accounts first (skip for admin)
  if (user && user.role !== UserRole.ADMIN && !bankAccountsLoading) {
    const hasNoBankAccounts = !bankAccounts || bankAccounts.length === 0;
    if (hasNoBankAccounts && !isBankAccountsPage) {
      return <Navigate to="/bank-accounts" replace />;
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col transition-colors">
      <AppHeader />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

