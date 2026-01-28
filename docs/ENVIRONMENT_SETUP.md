# Environment Variables Setup

## ✅ Configuration Status

**All secrets are configured to use environment variables!**

The application is configured to read all sensitive data from environment variables, not hardcoded in files.

---

## Backend Environment Variables

### Required for Production

Create a `.env` file in the `backend/` directory (or set as system environment variables):

```bash
# MongoDB Atlas Configuration (set in .env; never commit real credentials)
MONGODB_URI=mongodb+srv://USERNAME:PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=gigwave_prod

# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-change-in-production-minimum-32-characters

# OnePipe API Configuration (REQUIRED - Get from OnePipe dashboard)
ONEPIPE_API_KEY=your-onepipe-api-key
ONEPIPE_SECRET_KEY=your-onepipe-secret-key
ONEPIPE_BASE_URL=https://api.onepipe.io/v2/transact
ONEPIPE_ENV=production
ONEPIPE_BILLER_CODE=

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# CORS Configuration
CORS_ORIGINS=https://gigwave.com
```

### How to Set Environment Variables

#### Option 1: Create `.env` file (for local development)
1. Copy `backend/env.example` to `backend/.env`
2. Fill in your actual values
3. The `.env` file is already in `.gitignore` (won't be committed)

#### Option 2: System Environment Variables (for production)
```bash
# Linux/Mac
export MONGODB_URI="mongodb+srv://..."
export JWT_SECRET="your-secret-key"
export ONEPIPE_API_KEY="your-api-key"
export ONEPIPE_SECRET_KEY="your-secret-key"

# Windows PowerShell
$env:MONGODB_URI="mongodb+srv://..."
$env:JWT_SECRET="your-secret-key"
$env:ONEPIPE_API_KEY="your-api-key"
$env:ONEPIPE_SECRET_KEY="your-secret-key"
```

#### Option 3: Docker Environment Variables
Update `docker-compose.yml`:
```yaml
backend:
  environment:
    MONGODB_URI: ${MONGODB_URI}
    JWT_SECRET: ${JWT_SECRET}
    ONEPIPE_API_KEY: ${ONEPIPE_API_KEY}
    ONEPIPE_SECRET_KEY: ${ONEPIPE_SECRET_KEY}
    ONEPIPE_BILLER_CODE: ${ONEPIPE_BILLER_CODE:-}
```

---

## Current Configuration Files

### ✅ `application-prod.yml`
- **Status**: Uses environment variables only
- **No hardcoded secrets**: All values read from `${ENV_VAR}`

### ✅ `application.yml`
- **Status**: Has default values for development
- **Production**: Overridden by `application-prod.yml` when `SPRING_PROFILES_ACTIVE=prod`

---

## Security Checklist

- ✅ MongoDB connection string: Uses `MONGODB_URI` environment variable
- ✅ JWT secret: Uses `JWT_SECRET` environment variable
- ✅ OnePipe API key: Uses `ONEPIPE_API_KEY` environment variable
- ✅ OnePipe secret key: Uses `ONEPIPE_SECRET_KEY` environment variable
- ✅ OnePipe biller code: Uses `ONEPIPE_BILLER_CODE` (optional; for create mandate & collect)
- ✅ `.env` files: Already in `.gitignore` (won't be committed)
- ✅ `env.example`: Template file for reference (safe to commit)

---

## Production Deployment

### Before Deploying:

1. **Set MongoDB Atlas Connection String** (use your Atlas URI from `.env`):
   ```bash
   export MONGODB_URI="mongodb+srv://USERNAME:PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority&appName=Cluster0"
   ```

2. **Generate Strong JWT Secret**:
   ```bash
   # Linux/Mac
   openssl rand -base64 32
   
   # Or use online generator
   # Minimum 32 characters
   ```

3. **Get OnePipe Credentials**:
   - Log into OnePipe dashboard
   - Get API key and secret key
   - Set environment variables

4. **Set All Environment Variables**:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export MONGODB_URI="..."
   export JWT_SECRET="..."
   export ONEPIPE_API_KEY="..."
   export ONEPIPE_SECRET_KEY="..."
   export ONEPIPE_BILLER_CODE=""
   export CORS_ORIGINS="https://gigwave.com"
   ```

---

## Verification

To verify environment variables are being read:

1. **Check logs** when application starts
2. **Look for**: "Found key 'MONGODB_URI' in PropertySource 'environmentProperties'"
3. **If missing**: Application will fail to start with clear error message

---

## Important Notes

⚠️ **NEVER commit `.env` files to Git**
- Already in `.gitignore` ✅
- Use `env.example` as template

⚠️ **Production Secrets**:
- Use strong, unique secrets
- Rotate secrets regularly
- Use secret management services (AWS Secrets Manager, HashiCorp Vault, etc.) for production

⚠️ **OnePipe API Keys**:
- Required for payment processing
- Get from OnePipe dashboard
- Keep secure and never expose in logs

