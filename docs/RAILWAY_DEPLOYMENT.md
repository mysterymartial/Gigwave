# Railway Deployment Guide for GigWave Backend

This guide will help you deploy the GigWave backend to Railway.

## Prerequisites

1. **Railway Account**: Sign up at [railway.app](https://railway.app)
2. **GitHub Repository**: Your code should be pushed to GitHub
3. **MongoDB Atlas**: Set up a MongoDB Atlas cluster (or use Railway's MongoDB service)
4. **API Keys**: Have all your API keys ready:
   - OnePipe API credentials
   - ImageKit credentials
   - JWT Secret (generate a strong secret)
   - Flutterwave credentials (if using)

## Step-by-Step Deployment

### Step 1: Create a New Project on Railway

1. Go to [railway.app](https://railway.app) and sign in
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Choose your `Gigwave` repository
5. Select the `backend` folder as the root directory

### Step 2: Configure Build Settings

- **Root Directory**: `backend`
- **Builder**: `backend/railway.json` sets `DOCKERFILE`. Railway builds with `backend/Dockerfile` (multi-stage Maven → JRE). Do **not** override the Start Command in the dashboard; the Dockerfile’s `ENTRYPOINT` runs `java -jar app.jar`.

### Step 3: Set Environment Variables

Go to your Railway project → **Variables** tab and add:

#### Minimum required to start (otherwise app crashes)

```bash
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=mongodb+srv://USER:PASS@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority
MONGODB_DATABASE=gigwave_prod
JWT_SECRET=your-strong-secret-key-minimum-32-characters-long
```

Without `MONGODB_URI` or `JWT_SECRET`, the app fails at startup. File storage defaults to `local`; platform vars have placeholders so the app can boot.

#### Recommended / optional

```bash
JWT_EXPIRATION=86400000

# OnePipe API
ONEPIPE_API_KEY=your-onepipe-api-key
ONEPIPE_SECRET_KEY=your-onepipe-secret-key
ONEPIPE_BASE_URL=https://api.onepipe.io/v2/transact
ONEPIPE_ENV=production
ONEPIPE_BILLER_CODE=

# Server base URL (for OnePipe/Flutterwave webhook callbacks)
SERVER_URL=https://gigwave-production.up.railway.app

# CORS (set your frontend domain)
CORS_ORIGINS=https://your-frontend-domain.com,https://www.your-frontend-domain.com

# ImageKit (optional; default is local storage)
# FILE_STORAGE_TYPE=imagekit
# IMAGEKIT_PUBLIC_KEY=...
# IMAGEKIT_PRIVATE_KEY=...
# IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your-id

# Flutterwave (optional - for transfers)
FLUTTERWAVE_SECRET_KEY=your-flutterwave-secret-key
FLUTTERWAVE_BASE_URL=https://api.flutterwave.com/v3
FLUTTERWAVE_ENABLED=true
FLUTTERWAVE_TRANSFER_CHARGE=10

# Termii Notifications (Optional)
TERMII_API_KEY=your-termii-api-key
TERMII_SENDER_ID=GigWave
NOTIFICATION_SMS_ENABLED=false
NOTIFICATION_EMAIL_ENABLED=false
```

#### Platform Configuration (before real payouts)

Prod uses placeholder defaults so the app can start. **Set real values** in Railway Variables before processing real payouts:

```bash
PLATFORM_FEE_AMOUNT=200
PLATFORM_SETTLEMENT_NUMBER=<your-settlement-nuban>
PLATFORM_SETTLEMENT_BANK_CODE=070
PLATFORM_SETTLEMENT_NAME=Agbaosi Bolarinwa Minasu
PLATFORM_ACCOUNT_NUMBER=<your-platform-nuban>
PLATFORM_ACCOUNT_BANK_CODE=070
PLATFORM_ACCOUNT_NAME=Agbaosi Bolarinwa Minasu
```

### Step 4: Deploy

1. Railway will automatically start building when you push to your connected branch
2. Or click **"Deploy"** in the Railway dashboard
3. Wait for the build to complete (usually 3-5 minutes)
4. Check the **Deploy Logs** (runtime, not just build). Scroll past the `@ConditionalOnClass` / "Did not match" lines—those are normal. Look for the real error: `Exception`, `Caused by`, `Could not resolve placeholder 'MONGODB_URI'`, `Failed to configure a DataSource`, etc.
5. If you see **"Could not resolve placeholder 'MONGODB_URI'"** or **"Could not resolve placeholder 'JWT_SECRET'"**: add those variables in Railway → Variables and redeploy.

### Step 5: Get Your Backend URL

1. Once deployed, Railway will provide a URL like: `https://your-app-name.up.railway.app`
2. Go to **Settings** → **Networking** → **Generate Domain** to get a custom domain
3. Copy this URL - you'll need it for your frontend

### Step 6: Verify Deployment

1. Check health endpoint: `https://your-app-name.up.railway.app/actuator/health`
2. Check API docs: `https://your-app-name.up.railway.app/swagger-ui.html`
3. Test an endpoint: `https://your-app-name.up.railway.app/api/payments/banks`

## Important Notes

### Port Configuration
- Railway automatically provides a `PORT` environment variable
- The application is configured to use `${PORT:8080}` which will use Railway's PORT
- No manual port configuration needed

### MongoDB Setup Options

**Option 1: MongoDB Atlas (Recommended)**
1. Go to [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Create a free cluster
3. Get your connection string
4. Add it to Railway environment variables

**Option 2: Railway MongoDB Service**
1. In Railway, click **"New"** → **"Database"** → **"Add MongoDB"**
2. Railway will automatically create a MongoDB instance
3. The connection string will be available as `MONGO_URL` (you may need to map it to `MONGODB_URI`)

### Frontend calling the deployed backend

For production builds, the frontend must call the deployed API. Set **`VITE_API_BASE_URL`** (no trailing slash) at **build time**:

```bash
VITE_API_BASE_URL=https://gigwave-production.up.railway.app
```

- **Local dev**: Omit it; Vite proxies `/api` to `http://localhost:8080`.
- **Production**: Set it in your frontend host (e.g. Vercel/Netlify/Railway env vars), then rebuild. The app will use `https://gigwave-production.up.railway.app/api` for all API requests.

See `frontend/.env.example`.

### CORS Configuration

**Critical**: Update `CORS_ORIGINS` in the **backend** Railway Variables with your actual frontend domain(s):
- For development: `http://localhost:3000,http://localhost:5173`
- For production: `https://your-frontend-domain.com,https://www.your-frontend-domain.com`

### Webhook URLs (for OnePipe & Flutterwave)

**Backend base:** `https://gigwave-production.up.railway.app`

Set this **single** webhook URL in your OnePipe dashboard (mandate + debit both use it):

| Purpose | URL |
|--------|-----|
| **OnePipe (mandate + debit)** | `https://gigwave-production.up.railway.app/api/payments/webhooks/onepipe` |

Set **Flutterwave** webhook URL in the Flutterwave dashboard (for payout/transfer notifications):

| Purpose | URL |
|--------|-----|
| **Payout / transfer** | `https://gigwave-production.up.railway.app/api/payments/webhooks/payout` |

Ensure Railway **Variables** includes `SERVER_URL=https://gigwave-production.up.railway.app` so the backend uses this base when registering callbacks with OnePipe/Flutterwave.

### Custom Domain (Optional)

1. Go to **Settings** → **Networking**
2. Click **"Generate Domain"** or add your custom domain
3. Follow Railway's DNS instructions

## Troubleshooting

### Build Fails

1. Check the **Logs** tab in Railway
2. Common issues:
   - Maven dependencies not downloading → Check internet connectivity
   - Java version mismatch → Ensure Java 17 is specified
   - Memory issues → Railway free tier has limits

### Application Crashes

1. Check **Logs** for error messages
2. Common issues:
   - Missing environment variables → Check all required vars are set
   - MongoDB connection failed → Verify `MONGODB_URI` is correct
   - Port binding issues → Railway handles this automatically

### Database Connection Issues

1. Verify MongoDB Atlas:
   - IP whitelist includes `0.0.0.0/0` (allows all IPs) or Railway's IPs
   - Database user has correct permissions
   - Connection string is correct

### CORS Errors

1. Verify `CORS_ORIGINS` includes your frontend domain
2. Check that the frontend is using the correct backend URL
3. Ensure no trailing slashes in URLs

## Monitoring

Railway provides:
- **Logs**: Real-time application logs
- **Metrics**: CPU, Memory, Network usage
- **Deployments**: History of all deployments

## Updating Your Deployment

1. Push changes to your GitHub repository
2. Railway will automatically detect and redeploy
3. Or manually trigger a redeploy from Railway dashboard

## Cost Considerations

- **Free Tier**: $5 credit/month
- **Hobby Plan**: $5/month (recommended for production)
- **Pro Plan**: $20/month (for higher traffic)

## Security Checklist

- [ ] All secrets are in environment variables (not in code)
- [ ] JWT_SECRET is strong (32+ characters, random)
- [ ] MongoDB connection string includes authentication
- [ ] CORS_ORIGINS is restricted to your domains
- [ ] Production profile is active (`SPRING_PROFILES_ACTIVE=prod`)
- [ ] Stack traces are hidden in production

## Next Steps

After backend deployment:
1. Update frontend API URL to point to Railway backend
2. Deploy frontend (Vercel, Netlify, or Railway)
3. Test end-to-end functionality
4. Set up monitoring and alerts

## Support

- Railway Docs: https://docs.railway.app
- Railway Discord: https://discord.gg/railway
- Check application logs in Railway dashboard for specific errors
