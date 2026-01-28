# Environment Variables Setup

## ⚠️ IMPORTANT: Secrets Management

All sensitive data (database credentials, API keys, JWT secrets) **MUST** be stored in environment variables, NOT in configuration files.

## Backend Environment Variables

### Required Variables for Production

Use a single `.env` file at the **project root** (where `docker-compose.yml` lives). All secrets go there; **never commit** `.env`.

1. Copy `.env.example` to `.env` in the project root.
2. Set your MongoDB Atlas URI, JWT secret, OnePipe keys, etc. in `.env`.
3. **Docker Compose** reads `.env` automatically for `docker-compose up` / `docker-compose build`.
4. For **local** `mvn spring-boot:run`: export variables from `.env` (e.g. `set -a && source .env && set +a` on Linux/Mac, or use your IDE’s env config) then run the backend.

Example `.env` contents (see `.env.example` for full list):

```bash
# MongoDB Atlas – set your real URI here
MONGODB_URI=mongodb+srv://USERNAME:PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=gigwave_prod

# JWT – generate: openssl rand -base64 32
JWT_SECRET=your-generated-256-bit-secret-key-minimum-32-characters

# OnePipe (from OnePipe dashboard)
ONEPIPE_API_KEY=your-onepipe-api-key
ONEPIPE_SECRET_KEY=your-onepipe-secret-key
ONEPIPE_BASE_URL=https://api.onepipe.io/v2/transact
ONEPIPE_ENV=sandbox
ONEPIPE_BILLER_CODE=

# File storage: use "local" unless you set ImageKit keys
FILE_STORAGE_TYPE=local

# CORS, etc.
CORS_ORIGINS=https://gigwave.com,https://www.gigwave.com
SPRING_PROFILES_ACTIVE=prod
```

### How to Set Environment Variables

#### Option 1: .env at project root (recommended)
1. Copy `.env.example` to `.env` at the project root.
2. Fill in your actual values (MongoDB Atlas, JWT, OnePipe, etc.).
3. **Docker Compose** uses `.env` for variable substitution when you run `docker-compose up`.
4. For **local backend** run: export vars from `.env` (or use IDE env), then `mvn spring-boot:run`.

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

- ✅ `.env` is in `.gitignore`; never commit `.env`
- ✅ `.env.example` exists at project root (placeholders only, safe to commit)
- ✅ No hardcoded secrets in config or code
- ✅ Production config uses `${VAR}` from environment
- ✅ Code and scripts **do not log or expose** env variable values (e.g. keys, passwords)

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

Use your own Atlas credentials (store in `.env` only; never commit):
- Username: your Atlas database user
- Password: your Atlas database password
- Cluster: `cluster0.xxxxx.mongodb.net` (from Atlas)
- Database: `gigwave_prod`


