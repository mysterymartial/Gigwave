# GigWave - Music Gig Marketplace

A two-sided marketplace connecting event owners with musicians for live performances.

## Architecture

### Backend (Java/Spring Boot)
- **Layered Architecture**: Clean separation between API, Application, Domain, and Infrastructure layers
- **Package by Feature**: Organized by domain (users, gigs, bookings, payments, chat, reviews, disputes)
- **Technology Stack**:
  - Java 17+
  - Spring Boot 3.2.0
  - MongoDB
  - Spring Data MongoDB
  - Spring Security with JWT
  - OnePipe API integration for payments (Collect & Transfer)

### Frontend (React/TypeScript)
- **Feature-based Organization**: Components organized by feature domain
- **Technology Stack**:
  - React 18
  - TypeScript
  - React Router
  - React Query (TanStack Query)
  - Tailwind CSS
  - Vite

## Project Structure

```
backend/
├── src/main/java/com/gigwave/
│   ├── api/                    # REST controllers and DTOs
│   ├── application/            # Application services (use cases)
│   ├── domain/                 # Domain entities and business logic
│   └── infrastructure/         # Persistence, external clients, security
├── src/main/resources/
│   └── application.yml         # Configuration
└── pom.xml

frontend/
├── src/
│   ├── components/             # Reusable UI components
│   ├── pages/                  # Page components
│   ├── hooks/                  # React Query hooks
│   ├── lib/                    # API client and utilities
│   └── types/                  # TypeScript type definitions
├── package.json
└── vite.config.ts
```

## Getting Started

### Backend Setup

1. **Prerequisites**:
   - Java 17+
   - Maven 3.8+
   - PostgreSQL 14+

2. **Database Setup**:
   - MongoDB 7.0+ (or use Docker Compose: `docker-compose up -d mongodb`)
   - Database will be created automatically on first connection

3. **Configuration**:
   Update `backend/src/main/resources/application.yml` with your MongoDB connection URI and OnePipe API keys.

4. **Run**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

### Frontend Setup

1. **Prerequisites**:
   - Node.js 18+
   - pnpm

2. **Install Dependencies**:
   ```bash
   cd frontend
   pnpm install
   ```

3. **Run**:
   ```bash
   pnpm run dev
   ```

## Key Features

### For Event Owners
- Post and manage gigs with venue location (Google Maps) and pictures
- Add social media handles (Instagram, TikTok, X)
- Browse and select musicians
- **Direct messaging with musicians to discuss gigs**
- Confirm performance and initiate payment
- Setup bank mandate for direct debit
- Pay platform fee (₦200) per successful gig payment

### For Musicians
- Browse available gigs with filters and venue locations
- **Upload minimum 3 performance videos during profile creation (required)**
- Add social media handles (Instagram, TikTok, X)
- **Direct messaging with event owners to discuss gigs**
- Accept gigs with custom pricing
- Mark performance as done
- Upload post-gig media (video/picture)
- Setup bank mandate for payouts
- Receive full payment (platform fee is paid by organizer)

### Payment Flow
1. Organizer sets up direct debit mandate via OnePipe
2. Musician sets up payout mandate via OnePipe
3. Musician marks performance done and uploads post-gig media
4. Organizer confirms and initiates payment
5. System collects (debits) from organizer: `acceptedAmount + ₦200 platform fee`
6. On successful debit:
   - System transfers `acceptedAmount` to musician
   - System transfers `₦200` platform fee to GigWave account

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Gigs
- `GET /api/gigs` - List open gigs (with filters)
- `GET /api/gigs/{id}` - Get gig details
- `POST /api/gigs` - Create new gig (EVENT_OWNER)
- `GET /api/gigs/organizer/my-gigs` - List organizer's gigs

### Bookings
- `POST /api/bookings/gigs/{gigId}/accept` - Musician accepts gig
- `POST /api/bookings/{id}/mark-done` - Musician marks done
- `POST /api/bookings/{id}/confirm-pay` - Organizer confirms and pays
- `GET /api/bookings/{id}` - Get booking details

### Payments
- `GET /api/payments/banks` - List supported banks
- `GET /api/payments/platform-fee` - Get platform fee information
- `POST /api/payments/mandate/setup/organizer` - Setup organizer payment mandate
- `POST /api/payments/mandate/setup/musician` - Setup musician payout mandate
- `POST /api/payments/webhooks/mandate` - OnePipe mandate webhook
- `POST /api/payments/webhooks/debit` - OnePipe debit (collect) webhook
- `POST /api/payments/webhooks/payout` - OnePipe payout (transfer) webhook

### Chat (Direct Messaging)
- `POST /api/chat/direct/{userId1}/{userId2}` - Create/open direct chat thread between users
- `GET /api/chat/direct/{userId1}/{userId2}` - Get direct chat thread
- `GET /api/chat/direct/my-threads` - Get all my direct chat threads
- `POST /api/chat/bookings/{bookingId}/thread` - Create booking-based chat thread
- `GET /api/chat/bookings/{bookingId}/thread` - Get booking chat thread
- `POST /api/chat/threads/{threadId}/messages` - Send message
- `GET /api/chat/threads/{threadId}/messages` - List messages in thread

### Account Reporting & Admin
- `POST /api/reports` - Submit account report
- `GET /api/reports/my-reports` - Get reports I've submitted
- `GET /api/reports/reported-against-me` - Get reports against my account
- `GET /api/reports/{reportId}` - Get report details
- `GET /api/reports/admin/all` - Get all reports (ADMIN only)
- `GET /api/reports/admin/pending` - Get pending reports (ADMIN only)
- `POST /api/reports/admin/{reportId}/review` - Review report (ADMIN only)
- `POST /api/reports/admin/users/{userId}/disable` - Disable user account (ADMIN only)
- `POST /api/reports/admin/users/{userId}/enable` - Enable user account (ADMIN only)

### File Uploads
- `POST /api/files/performance-video` - Upload performance video (musician)
- `POST /api/files/report-evidence` - Upload report evidence
- `POST /api/files/chat-media/{bookingId}` - Upload chat media
- `POST /api/files/dispute-evidence/{disputeId}` - Upload dispute evidence
- `POST /api/files/kyc-document` - Upload KYC document

## Domain Model

Key entities:
- **User**: System users (EVENT_OWNER, MUSICIAN, ADMIN) with `isDisabled` flag
- **MusicianProfile**: Musician profile with minimum 3 performance videos requirement
- **OrganizerProfile**: Event owner profile with social media and venue details
- **Gig**: Event postings by organizers with location (lat/lng) and venue picture
- **Booking**: Connection between gig and musician with post-gig media
- **PaymentMandate**: Direct debit authorization
- **DebitTransaction**: Debit from organizer account
- **Payout**: Transfer to musician account and platform fee
- **Dispute**: Conflict resolution (booking-based)
- **AccountReport**: User account reporting system (admin-reviewed)
- **ChatThread/ChatMessage**: Communication (booking-based and direct messaging)
- **Review**: Ratings and feedback

## Security

- JWT-based authentication
- Role-based access control (RBAC) - EVENT_OWNER, MUSICIAN, ADMIN
- Password encryption with BCrypt
- Secure API endpoints with Spring Security
- **Account disable/enable system** - Disabled accounts blocked from authentication and actions
- **Webhook signature verification** (HMAC-SHA256) for OnePipe callbacks
- Security headers configured (CORS, CSP, etc.)
- Rate limiting configured (Bucket4j)

## OnePipe Integration

The backend includes a OnePipe v2 client implementation:
- **Create Mandate**: OnePipe `create mandate` with TripleDES encryption, `activation_method: "transfer"` (NIBSS), `biller_code`, optional BVN
- **Collect API**: Debit from organizer's account using mandate; `auth_provider: "NIBSS"`, TripleDES for `auth.secure`
- **Transfer**: Flutterwave for musician payout and platform fee
- **Webhook Handling**: Mandate, debit, and payout status updates (HMAC-SHA256 verification)

### Platform Fee
- Fixed fee: **₦200 per successful gig payment** (configurable via `PLATFORM_FEE_AMOUNT`)
- Charged to: **Organizer** (added to accepted amount)
- Settlement and platform account details are **env-only** (`PLATFORM_SETTLEMENT_*`, `PLATFORM_ACCOUNT_*`). See docs.

### OnePipe API Actions Used
- `create mandate` - Direct debit mandate (PaywithAccount, meta: amount, bvn, biller_code, activation_method transfer)
- `collect` - Debit via mandate (NIBSS, TripleDES encrypted account; meta: biller_code)
- **Transfer** via Flutterwave (not OnePipe)

Set `ONEPIPE_API_KEY`, `ONEPIPE_SECRET_KEY`, and `ONEPIPE_BILLER_CODE` (optional) in production. See [API_CONFIGURATION](./docs/API_CONFIGURATION.md) and [API_INTEGRATION](./docs/API_INTEGRATION.md).

## Testing

### Backend Tests
- Unit tests with JUnit 5 and Mockito
- Boundary analysis and edge case testing
- Service layer tests for:
  - ProfileService (video validation - minimum 3 videos)
  - ChatService (direct messaging, booking-based chat, disabled account checks)
  - AccountReportService (reporting, admin review, disable/enable accounts)
  - PaymentService (platform fee, OnePipe integration)
- Controller tests for API endpoints
- Repository tests for data access

### Frontend Tests
- Component tests with Vitest and React Testing Library
- Hook tests for React Query integration
- Error boundary testing

Run tests:
```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
pnpm test
```

## New Features (Latest Update)

1. **Musician Video Requirement**:
   - **Minimum 3 performance videos required** during profile creation
   - Backend validation enforces this requirement
   - Frontend displays video count and validation feedback
   - Video upload endpoint: `/api/files/performance-video`

2. **Direct Messaging System**:
   - **Anyone can chat with anyone** - not limited to bookings
   - Event owners can directly message musicians
   - Chat threads normalized to prevent duplicates
   - Disabled accounts cannot create threads or send messages

3. **Account Reporting System**:
   - Users can report other accounts for misbehavior
   - Reports include reason and optional evidence
   - Admin review workflow (PENDING → APPROVED/REJECTED)
   - **Approved reports automatically disable reported account**
   - Admin can manually disable/enable accounts
   - Users can view their submitted reports and reports against them

## Development Notes

- The UI design is available in Stitch: https://stitch.withgoogle.com/projects/134253993186064354
- Components are structured to easily apply Stitch design assets
- Backend follows clean architecture principles for maintainability
- Frontend uses React Query for efficient data fetching and caching
- **Light/Dark mode** supported with system preference detection

## Production Readiness Checklist

### Backend ✅
- ✅ Environment-specific configurations (dev/staging/prod)
- ✅ MongoDB integration with Spring Data MongoDB
- ✅ JWT authentication with RBAC
- ✅ Global exception handler
- ✅ CORS configuration
- ✅ Rate limiting (Bucket4j)
- ✅ API documentation (Swagger/OpenAPI)
- ✅ Logging (Logback with AOP)
- ✅ Docker & Docker Compose
- ✅ CI/CD pipeline (GitHub Actions)
- ✅ Backup strategy documentation
- ✅ Security headers
- ✅ Webhook signature verification
- ✅ File upload endpoints with size limits
- ✅ Unit tests with boundary analysis
- ✅ KYC verification service
- ✅ Account disable/enable system

### Frontend ✅
- ✅ Error boundaries
- ✅ TypeScript strict mode
- ✅ React Query for data fetching
- ✅ Light/Dark mode support
- ✅ Responsive design
- ✅ Unit tests
- ✅ Production build configuration

### Integration ✅
- ✅ OnePipe API client structure (ready for credentials)
- ✅ Webhook endpoints for mandate, debit, and payout
- ✅ Platform fee calculation and transfer
- ✅ Direct chat and booking-based chat
- ✅ Account reporting and admin review

### Pending for Production
- ⚠️ **Database credentials** - Set MongoDB connection URI
- ⚠️ **OnePipe API credentials** - Configure actual API keys and secret
- ⚠️ **JWT secret** - Change default secret in production
- ✅ **File storage** - ImageKit configured (see `docs/IMAGEKIT_SETUP.md`)
- ⚠️ **Monitoring** - Set up application monitoring (e.g., Prometheus, Grafana)
- ⚠️ **Email/SMS notifications** - Configure actual notification provider

## Documentation

Comprehensive documentation is available in the [`docs/`](./docs/) folder:

- **[Production Readiness](./docs/PRODUCTION_READINESS.md)** - Complete production deployment guide
- **[Environment Setup](./docs/ENVIRONMENT_SETUP.md)** - Environment variable configuration
- **[ImageKit Setup](./docs/IMAGEKIT_SETUP.md)** - File storage configuration
- **[API Configuration](./docs/API_CONFIGURATION.md)** - API integration details
- **[Security Audit](./docs/SECURITY_AUDIT.md)** - Security best practices
- **[Backup Strategy](./docs/BACKUP_STRATEGY.md)** - Database backup procedures
- **[Flutterwave Integration](./docs/FLUTTERWAVE_INTEGRATION.md)** - Payment transfer setup
- **[Notification Setup](./docs/NOTIFICATION_SETUP_GUIDE.md)** - SMS/Email configuration

For a quick start, see [Getting Started](#getting-started) above.

## License

Proprietary - All rights reserved
