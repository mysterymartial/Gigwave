import axios from 'axios';
import type {
  AuthResponse,
  User,
  Gig,
  Booking,
  Bank,
  MusicianProfile,
  OrganizerProfile,
  BankAccount,
  ChatMessage,
  ChatThread,
  Review,
  Dispute,
  KycDocument,
  KycStatus,
  AccountReport,
  ReportStatus,
  Customer,
  AdminDebitRequest,
  AdminDebitResponse,
  DisputeStatus,
} from '../types';

function authResponseToUser(data: AuthResponse): User {
  return {
    id: String(data.userId),
    phone: data.phone,
    email: data.email,
    role: data.role,
  };
}

const baseURL = import.meta.env.VITE_API_BASE_URL
  ? `${import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')}/api`
  : '/api';

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth API
export const authApi = {
  register: async (data: { phone: string; email?: string; password: string; role: string }) => {
    const response = await api.post<AuthResponse>('/auth/register', data);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(authResponseToUser(response.data)));
      window.dispatchEvent(new Event('gigwave-auth-change'));
    }
    return response.data;
  },
  login: async (data: { phone: string; password: string }) => {
    const response = await api.post<AuthResponse>('/auth/login', data);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(authResponseToUser(response.data)));
      window.dispatchEvent(new Event('gigwave-auth-change'));
    }
    return response.data;
  },
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
};

// Gig API
export const gigApi = {
  list: async (params?: { city?: string; minBudget?: number; maxBudget?: number }) => {
    const response = await api.get<Gig[]>('/gigs', { params });
    return response.data;
  },
  get: async (id: string) => {
    const response = await api.get<Gig>(`/gigs/${id}`);
    return response.data;
  },
  create: async (data: Omit<Gig, 'id' | 'createdAt' | 'status'>) => {
    const response = await api.post<Gig>('/gigs', data);
    return response.data;
  },
  update: async (id: string, data: Partial<Gig>) => {
    const response = await api.put<Gig>(`/gigs/${id}`, data);
    return response.data;
  },
  cancel: async (id: string) => {
    await api.delete(`/gigs/${id}`);
  },
  listMyGigs: async () => {
    const response = await api.get<Gig[]>('/gigs/organizer/my-gigs');
    return response.data;
  },
};

// Booking API
export const bookingApi = {
  accept: async (gigId: string, acceptedAmount: number) => {
    const response = await api.post<Booking>(`/bookings/gigs/${gigId}/accept`, null, {
      params: { acceptedAmount },
    });
    return response.data;
  },
  select: async (bookingId: string) => {
    const response = await api.post<Booking>(`/bookings/${bookingId}/select`);
    return response.data;
  },
  markDone: async (bookingId: string) => {
    const response = await api.post<Booking>(`/bookings/${bookingId}/mark-done`);
    return response.data;
  },
  confirmAndPay: async (bookingId: string) => {
    const response = await api.post<Booking>(`/bookings/${bookingId}/confirm-pay`);
    return response.data;
  },
  get: async (id: string) => {
    const response = await api.get<Booking>(`/bookings/${id}`);
    return response.data;
  },
  listMyBookings: async () => {
    const response = await api.get<Booking[]>('/bookings/my-bookings');
    return response.data;
  },
  addPostGigMedia: async (bookingId: string, mediaUrls: string[]) => {
    const response = await api.post<Booking>(`/bookings/${bookingId}/post-gig-media`, mediaUrls);
    return response.data;
  },
};

// Payment API
export const paymentApi = {
  getBanks: async () => {
    const response = await api.get<{ banks: Bank[] }>('/payments/banks');
    return response.data.banks;
  },
  getPlatformFee: async () => {
    const response = await api.get<{ platformFeeAmount: number; currency: string; description: string }>(
      '/payments/platform-fee'
    );
    return response.data;
  },
  setupOrganizerMandate: async (
    bankAccountId: string,
    maxAmount: number,
    bvn?: string,
    idempotencyKey?: string
  ) => {
    const params: Record<string, string | number> = { bankAccountId, maxAmount };
    if (bvn != null && bvn !== '') params.bvn = bvn;
    const config: { params: Record<string, string | number>; headers?: Record<string, string> } = { params };
    if (idempotencyKey != null && idempotencyKey !== '') config.headers = { 'Idempotency-Key': idempotencyKey };
    const response = await api.post<{ status: string; mandateRef: string; authorizationUrl: string }>(
      '/payments/mandate/setup/organizer',
      null,
      config
    );
    return response.data;
  },
  setupMusicianMandate: async (
    bankAccountId: string,
    maxAmount: number,
    bvn?: string,
    idempotencyKey?: string
  ) => {
    const params: Record<string, string | number> = { bankAccountId, maxAmount };
    if (bvn != null && bvn !== '') params.bvn = bvn;
    const config: { params: Record<string, string | number>; headers?: Record<string, string> } = { params };
    if (idempotencyKey != null && idempotencyKey !== '') config.headers = { 'Idempotency-Key': idempotencyKey };
    const response = await api.post<{ status: string; mandateRef: string; authorizationUrl: string }>(
      '/payments/mandate/setup/musician',
      null,
      config
    );
    return response.data;
  },
  initiateDebit: async (bookingId: string) => {
    const response = await api.post<{ status: string; transactionRef: string; message: string }>(
      `/payments/bookings/${bookingId}/debit`
    );
    return response.data;
  },
  validateOtp: async (bookingId: string, otp: string) => {
    const response = await api.post<{ status: string; transactionRef: string; message: string }>(
      `/payments/bookings/${bookingId}/validate-otp`,
      null,
      { params: { otp } }
    );
    return response.data;
  },
};

// Profile API
export const profileApi = {
  getMusicianProfile: async () => {
    const response = await api.get<MusicianProfile>('/profiles/musician');
    return response.data;
  },
  updateMusicianProfile: async (data: Partial<MusicianProfile>) => {
    const response = await api.post<MusicianProfile>('/profiles/musician', data);
    return response.data;
  },
  getOrganizerProfile: async () => {
    const response = await api.get<OrganizerProfile>('/profiles/organizer');
    return response.data;
  },
  getOrganizerProfileByUserId: async (userId: string) => {
    const response = await api.get<OrganizerProfile>(`/profiles/organizer/${userId}`);
    return response.data;
  },
  updateOrganizerProfile: async (data: Partial<OrganizerProfile>) => {
    const response = await api.post<OrganizerProfile>('/profiles/organizer', data);
    return response.data;
  },
};

// Bank Account API
export const bankAccountApi = {
  list: async () => {
    const response = await api.get<BankAccount[]>('/bank-accounts');
    return response.data;
  },
  add: async (data: Omit<BankAccount, 'id' | 'userId'>) => {
    const response = await api.post<BankAccount>('/bank-accounts', data);
    return response.data;
  },
  setDefault: async (accountId: string) => {
    await api.put(`/bank-accounts/${accountId}/set-default`);
  },
  delete: async (accountId: string) => {
    await api.delete(`/bank-accounts/${accountId}`);
  },
};

// Chat API
export const chatApi = {
  getThread: async (bookingId: string) => {
    const response = await api.get<ChatThread>(`/chat/bookings/${bookingId}/thread`);
    return response.data;
  },
  openThread: async (bookingId: string) => {
    const response = await api.post<ChatThread>(`/chat/bookings/${bookingId}/thread`);
    return response.data;
  },
  openDirectThread: async (userId1: string, userId2: string) => {
    const response = await api.post<ChatThread>(`/chat/direct/${userId1}/${userId2}`);
    return response.data;
  },
  getDirectThread: async (userId1: string, userId2: string) => {
    const response = await api.get<ChatThread>(`/chat/direct/${userId1}/${userId2}`);
    return response.data;
  },
  getMyDirectThreads: async () => {
    const response = await api.get<ChatThread[]>('/chat/direct/my-threads');
    return response.data;
  },
  sendMessage: async (threadId: string, data: Partial<ChatMessage>) => {
    const response = await api.post<ChatMessage>(`/chat/threads/${threadId}/messages`, data);
    return response.data;
  },
  listMessages: async (threadId: string) => {
    const response = await api.get<ChatMessage[]>(`/chat/threads/${threadId}/messages`);
    return response.data;
  },
};

// Review API
export const reviewApi = {
  create: async (data: Omit<Review, 'id' | 'createdAt'>) => {
    const response = await api.post<Review>('/reviews', data);
    return response.data;
  },
  listForUser: async (userId: string) => {
    const response = await api.get<Review[]>(`/reviews/user/${userId}`);
    return response.data;
  },
  listForBooking: async (bookingId: string) => {
    const response = await api.get<Review[]>(`/reviews/bookings/${bookingId}`);
    return response.data;
  },
  getAverageRating: async (userId: string) => {
    const response = await api.get<number>(`/reviews/user/${userId}/average-rating`);
    return response.data;
  },
};

// Dispute API
export const disputeApi = {
  create: async (data: Omit<Dispute, 'id' | 'createdAt' | 'resolvedAt'>) => {
    const response = await api.post<Dispute>('/disputes', data);
    return response.data;
  },
  listForBooking: async (bookingId: string) => {
    const response = await api.get<Dispute[]>(`/disputes/bookings/${bookingId}`);
    return response.data;
  },
  listMyDisputes: async () => {
    const response = await api.get<Dispute[]>('/disputes/my-disputes');
    return response.data;
  },
  get: async (disputeId: string) => {
    const response = await api.get<Dispute>(`/disputes/${disputeId}`);
    return response.data;
  },
  resolve: async (disputeId: string, resolution: DisputeStatus) => {
    const response = await api.post<Dispute>(`/disputes/${disputeId}/resolve`, null, {
      params: { resolution },
    });
    return response.data;
  },
};

// KYC API
export const kycApi = {
  submitDocument: async (documentType: string, documentUrl: string) => {
    const response = await api.post<KycDocument>('/kyc/documents', null, {
      params: { documentType, documentUrl },
    });
    return response.data;
  },
  listDocuments: async () => {
    const response = await api.get<KycDocument[]>('/kyc/documents');
    return response.data;
  },
  getStatus: async () => {
    const response = await api.get<KycStatus>('/kyc/status');
    return response.data;
  },
  verifyKyc: async (userId: string) => {
    await api.put(`/kyc/${userId}/verify`);
  },
  rejectKyc: async (userId: string) => {
    await api.put(`/kyc/${userId}/reject`);
  },
  updateKycStatus: async (userId: string, status: KycStatus) => {
    await api.put(`/kyc/${userId}/status`, null, { params: { status } });
  },
};

// File Upload API
export const fileApi = {
  uploadChatMedia: async (bookingId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<{ url: string; message: string }>(
      `/files/chat-media/${bookingId}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
  uploadDisputeEvidence: async (disputeId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<{ url: string; message: string }>(
      `/files/dispute-evidence/${disputeId}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
  uploadKycDocument: async (file: File, documentType: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    const response = await api.post<KycDocument>('/files/kyc-document', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  uploadReportEvidence: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<{ url: string; message: string }>(
      '/files/report-evidence',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
  uploadPerformanceVideo: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<{ url: string; message: string }>(
      '/files/performance-video',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
  uploadVenueImage: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<{ url: string; message: string }>(
      '/files/venue-image',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },
};

// Account Report API
export const accountReportApi = {
  create: async (data: { reportedUserId: string; reason: string; evidenceUrl?: string }) => {
    const response = await api.post<AccountReport>('/reports', data);
    return response.data;
  },
  getMyReports: async () => {
    const response = await api.get<AccountReport[]>('/reports/my-reports');
    return response.data;
  },
  getReportsAgainstMe: async () => {
    const response = await api.get<AccountReport[]>('/reports/reported-against-me');
    return response.data;
  },
  get: async (reportId: string) => {
    const response = await api.get<AccountReport>(`/reports/${reportId}`);
    return response.data;
  },
  // Admin endpoints
  getAllReports: async () => {
    const response = await api.get<AccountReport[]>('/reports/admin/all');
    return response.data;
  },
  getPendingReports: async () => {
    const response = await api.get<AccountReport[]>('/reports/admin/pending');
    return response.data;
  },
  reviewReport: async (reportId: string, data: { status: ReportStatus; adminReview?: string }) => {
    const response = await api.post<AccountReport>(`/reports/admin/${reportId}/review`, data);
    return response.data;
  },
  disableAccount: async (userId: string, reason?: string) => {
    await api.post(`/reports/admin/users/${userId}/disable`, null, { params: { reason } });
  },
  enableAccount: async (userId: string, reason?: string) => {
    await api.post(`/reports/admin/users/${userId}/enable`, null, { params: { reason } });
  },
};

// Admin API
export const adminApi = {
  getAllCustomers: async () => {
    const response = await api.get<Customer[]>('/admin/customers');
    return response.data;
  },
  getCustomer: async (customerId: string) => {
    const response = await api.get<Customer>(`/admin/customers/${customerId}`);
    return response.data;
  },
  debitCustomer: async (customerId: string, data: AdminDebitRequest) => {
    const response = await api.post<AdminDebitResponse>(`/admin/customers/${customerId}/debit`, data);
    return response.data;
  },
};


