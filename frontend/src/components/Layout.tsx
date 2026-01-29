import { Outlet, useLocation, Navigate } from 'react-router-dom';
import AppHeader from './AppHeader';
import Footer from './Footer';
import { useAuth } from '../hooks/useAuth';
import { useBankAccounts } from '../hooks/useBankAccounts';
import { useMandateStatus } from '../hooks/usePayments';
import { UserRole } from '../types';

export default function Layout() {
  const { user } = useAuth();
  const location = useLocation();
  const { data: bankAccounts, isLoading: bankAccountsLoading } = useBankAccounts({ enabled: !!user });
  const { data: mandateStatus, isLoading: mandateLoading } = useMandateStatus({
    enabled: !!user && user.role !== UserRole.ADMIN,
  });

  const pathname = location.pathname;
  const isBankAccountsPage = pathname === '/bank-accounts' || pathname.endsWith('/bank-accounts');

  // Enforce add bank account: logged-in user with no bank accounts must go to /bank-accounts first (skip for admin)
  if (user && user.role !== UserRole.ADMIN && !bankAccountsLoading) {
    const hasNoBankAccounts = !bankAccounts || bankAccounts.length === 0;
    if (hasNoBankAccounts && !isBankAccountsPage) {
      return <Navigate to="/bank-accounts" replace />;
    }
  }

  // Require active mandate to post gig (organizer) or find gigs (musician); allow multiple banks without mandate
  if (user && user.role !== UserRole.ADMIN && !bankAccountsLoading && !mandateLoading) {
    const hasBankAccounts = bankAccounts && bankAccounts.length > 0;
    const hasActiveMandate = mandateStatus?.hasActiveMandate === true;
    const organizerNeedsMandate = pathname === '/gigs/create' && user.role === UserRole.EVENT_OWNER;
    const musicianNeedsMandate = pathname === '/gigs' && user.role === UserRole.MUSICIAN;
    if (hasBankAccounts && !hasActiveMandate && (organizerNeedsMandate || musicianNeedsMandate)) {
      return <Navigate to="/bank-accounts?setup_mandate=1" replace />;
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

