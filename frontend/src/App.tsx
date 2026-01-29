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
import BookingsListPage from './pages/bookings/BookingsListPage';
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
import AdminCustomersPage from './pages/admin/AdminCustomersPage';

function PrivateRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
}

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="gigs" element={<PrivateRoute><GigListPage /></PrivateRoute>} />
            <Route path="gigs/:id" element={<PrivateRoute><GigDetailsPage /></PrivateRoute>} />
            <Route path="gigs/create" element={<PrivateRoute><CreateGigPage /></PrivateRoute>} />
            <Route path="my-gigs" element={<PrivateRoute><MyGigsPage /></PrivateRoute>} />
            <Route path="bookings" element={<PrivateRoute><BookingsListPage /></PrivateRoute>} />
            <Route path="bookings/:id" element={<PrivateRoute><BookingDetailsPage /></PrivateRoute>} />
            <Route path="profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
            <Route path="bank-accounts" element={<PrivateRoute><BankAccountsPage /></PrivateRoute>} />
            <Route path="bookings/:bookingId/chat" element={<PrivateRoute><ChatPage /></PrivateRoute>} />
            <Route path="bookings/:bookingId/reviews" element={<PrivateRoute><ReviewsPage /></PrivateRoute>} />
            <Route path="bookings/:bookingId/disputes" element={<PrivateRoute><DisputesPage /></PrivateRoute>} />
            <Route path="kyc" element={<PrivateRoute><KycPage /></PrivateRoute>} />
            <Route path="reports/new" element={<PrivateRoute><AccountReportPage /></PrivateRoute>} />
            <Route path="admin/reports" element={<PrivateRoute><AdminReportsPage /></PrivateRoute>} />
            <Route path="admin/customers" element={<PrivateRoute><AdminCustomersPage /></PrivateRoute>} />
          </Route>
      </Routes>
    </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;

