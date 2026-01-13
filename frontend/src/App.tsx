import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { ErrorBoundary } from './components/ErrorBoundary';
import { useAuth } from './hooks/useAuth';
import Layout from './components/Layout';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import HomePage from './pages/HomePage';
import GigListPage from './pages/gigs/GigListPage';
import GigDetailsPage from './pages/gigs/GigDetailsPage';
import CreateGigPage from './pages/gigs/CreateGigPage';
import BookingDetailsPage from './pages/bookings/BookingDetailsPage';
import MyGigsPage from './pages/gigs/MyGigsPage';
import ProfilePage from './pages/profile/ProfilePage';
import BankAccountsPage from './pages/payments/BankAccountsPage';
import ChatPage from './pages/chat/ChatPage';
import ReviewsPage from './pages/reviews/ReviewsPage';
import DisputesPage from './pages/disputes/DisputesPage';
import KycPage from './pages/kyc/KycPage';
import AccountReportPage from './pages/reports/AccountReportPage';
import AdminReportsPage from './pages/reports/AdminReportsPage';

function PrivateRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
}

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <Layout />
              </PrivateRoute>
            }
          >
          <Route index element={<HomePage />} />
          <Route path="gigs" element={<GigListPage />} />
          <Route path="gigs/:id" element={<GigDetailsPage />} />
          <Route path="gigs/create" element={<CreateGigPage />} />
          <Route path="my-gigs" element={<MyGigsPage />} />
          <Route path="bookings/:id" element={<BookingDetailsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="bank-accounts" element={<BankAccountsPage />} />
          <Route path="bookings/:bookingId/chat" element={<ChatPage />} />
          <Route path="bookings/:bookingId/reviews" element={<ReviewsPage />} />
          <Route path="bookings/:bookingId/disputes" element={<DisputesPage />} />
          <Route path="kyc" element={<KycPage />} />
          <Route path="reports/new" element={<AccountReportPage />} />
          <Route path="admin/reports" element={<AdminReportsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;

