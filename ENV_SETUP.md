# Environment Variables Setup

## ⚠️ IMPORTANT: Secrets Management

All sensitive data (database credentials, API keys, JWT secrets) **MUST** be stored in environment variables, NOT in configuration files.

## Backend Environment Variables

### Required Variables for Production

Create a `.env` file in the `backend/` directory with the following variables:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# MongoDB Atlas Connection
MONGODB_URI=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=gigwave_prod

# JWT Configuration
# Generate a strong secret: openssl rand -base64 32
JWT_SECRET=your-generated-256-bit-secret-key-minimum-32-characters

# OnePipe Payment Gateway
ONEPIPE_BASE_URL=https://api.onepipe.io/v2/transact
ONEPIPE_API_KEY=your-onepipe-api-key
ONEPIPE_SECRET_KEY=your-onepipe-secret-key
ONEPIPE_ENV=sandbox

# CORS Configuration
CORS_ORIGINS=https://gigwave.com,https://www.gigwave.com
```

### How to Set Environment Variables

#### Option 1: Using .env file (Recommended for Development)
1. Copy `.env.example` to `.env` in the `backend/` directory
2. Fill in your actual values
3. The application will automatically load these (if using Spring Boot 2.4+)

#### Option 2: System Environment Variables (Recommended for Production)
```bash
# Linux/Mac
export MONGODB_URI="mongodb+srv://..."
export JWT_SECRET="your-secret"
export ONEPIPE_API_KEY="your-key"
export ONEPIPE_SECRET_KEY="your-secret"

# Windows PowerShell
$env:MONGODB_URI="mongodb+srv://..."
$env:JWT_SECRET="your-secret"
$env:ONEPIPE_API_KEY="your-key"
$env:ONEPIPE_SECRET_KEY="your-secret"
```

#### Option 3: Docker Environment Variables
```yaml
# docker-compose.yml
environment:
  MONGODB_URI: ${MONGODB_URI}
  JWT_SECRET: ${JWT_SECRET}
  ONEPIPE_API_KEY: ${ONEPIPE_API_KEY}
  ONEPIPE_SECRET_KEY: ${ONEPIPE_SECRET_KEY}
```

## Frontend Environment Variables

Create a `.env` file in the `frontend/` directory:

```bash
VITE_API_BASE_URL=https://api.gigwave.com
VITE_APP_NAME=GigWave
```

## Security Checklist

- ✅ `.env` files are in `.gitignore`
- ✅ `.env.example` files exist (without real secrets)
- ✅ No hardcoded secrets in configuration files
- ✅ Production config uses `${VARIABLE_NAME}` format
- ✅ Default values in config are placeholders only

## Current Status

### ✅ Fixed:
- MongoDB Atlas connection string moved to environment variable
- JWT secret uses environment variable
- OnePipe keys use environment variables
- `.gitignore` updated to exclude `.env` files

### ⚠️ TODO:
- Create actual `.env` file with your MongoDB Atlas credentials
- Generate a strong JWT secret for production
- Add OnePipe API credentials when ready

## Generating Secrets

### JWT Secret (32+ characters):
```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### MongoDB Connection String:
Format: `mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/database?retryWrites=true&w=majority&appName=Cluster0`

Your current credentials:
- Username: `bolasax16_db_user`
- Password: `wpNqIPQIuzpwrJIN`
- Cluster: `cluster0.wxojlvd.mongodb.net`
- Database: `gigwave_prod`


