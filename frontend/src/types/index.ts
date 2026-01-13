export enum UserRole {
  EVENT_OWNER = 'EVENT_OWNER',
  MUSICIAN = 'MUSICIAN',
  ADMIN = 'ADMIN',
}

export enum GigStatus {
  OPEN = 'OPEN',
  MATCHED = 'MATCHED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
}

export enum BookingStatus {
  REQUESTED = 'REQUESTED',
  ACCEPTED = 'ACCEPTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export enum PaymentStatus {
  NOT_INITIATED = 'NOT_INITIATED',
  DEBIT_PENDING = 'DEBIT_PENDING',
  DEBIT_SUCCESS = 'DEBIT_SUCCESS',
  DEBIT_FAILED = 'DEBIT_FAILED',
  PAID_OUT = 'PAID_OUT',
}

export interface User {
  id: string;
  phone: string;
  email?: string;
  role: UserRole;
  isDisabled?: boolean;
}

export interface Gig {
  id: string;
  organizerId: string;
  title: string;
  description: string;
  eventDate: string;
  location: string;
  latitude?: number;
  longitude?: number;
  venuePictureUrl?: string;
  budgetMin: number;
  budgetMax: number;
  status: GigStatus;
  createdAt: string;
}

export interface Booking {
  id: string;
  gigId: string;
  musicianId: string;
  organizerMandateId?: string;
  bookingStatus: BookingStatus;
  paymentStatus: PaymentStatus;
  acceptedAmount: number;
  acceptedAt?: string;
  musicianDoneAt?: string;
  ownerConfirmedAt?: string;
  completedAt?: string;
  postGigMediaUrls?: string[];
  createdAt: string;
}

export interface Bank {
  code: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  phone: string;
  email?: string;
  role: UserRole;
}

export interface MusicianProfile {
  userId: string;
  stageName: string;
  genres: string[];
  city?: string;
  minFee?: number;
  rating: number;
  instagramHandle?: string;
  tiktokHandle?: string;
  xHandle?: string;
  performanceVideoUrls?: string[];
}

export interface OrganizerProfile {
  userId: string;
  organizationName: string;
  eventTypes: string[];
  instagramHandle?: string;
  tiktokHandle?: string;
  xHandle?: string;
}

export interface BankAccount {
  id: string;
  userId: string;
  bankName: string;
  bankCode: string;
  accountNumber: string;
  accountName: string;
  isPayoutDefault: boolean;
}

export interface ChatMessage {
  id: string;
  threadId: string;
  senderId: string;
  messageType: 'TEXT' | 'IMAGE' | 'VIDEO' | 'VOICE_NOTE' | 'LOCATION';
  text?: string;
  mediaUrl?: string;
  locationLat?: number;
  locationLng?: number;
  createdAt: string;
}

export interface ChatThread {
  id: string;
  bookingId?: string;
  userId1?: string;
  userId2?: string;
  createdAt: string;
}

export interface Review {
  id: string;
  bookingId: string;
  reviewerId: string;
  reviewedUserId: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

export interface Dispute {
  id: string;
  bookingId: string;
  raisedBy: string;
  reason: string;
  evidenceUrl?: string;
  status: 'OPEN' | 'RESOLVED_MUSICIAN' | 'RESOLVED_ORGANIZER' | 'CANCELLED';
  createdAt: string;
  resolvedAt?: string;
}

export interface KycDocument {
  id: string;
  userId: string;
  documentType: string;
  documentUrl: string;
  uploadedAt: string;
}

export enum KycStatus {
  PENDING = 'PENDING',
  VERIFIED = 'VERIFIED',
  REJECTED = 'REJECTED',
}

export enum ReportStatus {
  PENDING = 'PENDING',
  REVIEWING = 'REVIEWING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  RESOLVED = 'RESOLVED',
}

export interface AccountReport {
  id: string;
  reporterId: string;
  reportedUserId: string;
  reason: string;
  evidenceUrl?: string;
  status: ReportStatus;
  adminReview?: string;
  reviewedBy?: string;
  createdAt: string;
  reviewedAt?: string;
}

export interface PlatformFeeInfo {
  platformFeeAmount: number;
  currency: string;
  description: string;
}

