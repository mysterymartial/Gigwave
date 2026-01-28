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

Railway will auto-detect Spring Boot, but verify:
- **Root Directory**: `backend`
- **Build Command**: `mvn clean package -DskipTests` (or Railway will auto-detect)
- **Start Command**: `java -jar target/*.jar`

### Step 3: Set Environment Variables

Go to your Railway project → **Variables** tab and add:

#### Required Variables

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# MongoDB (use MongoDB Atlas connection string)
MONGODB_URI=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority
MONGODB_DATABASE=gigwave_prod

# JWT Authentication
JWT_SECRET=your-strong-secret-key-minimum-32-characters-long
JWT_EXPIRATION=86400000

# OnePipe API
ONEPIPE_API_KEY=your-onepipe-api-key
ONEPIPE_SECRET_KEY=your-onepipe-secret-key
ONEPIPE_BASE_URL=https://api.onepipe.io/v2/transact
ONEPIPE_ENV=production
ONEPIPE_BILLER_CODE=

# ImageKit (File Storage)
IMAGEKIT_PUBLIC_KEY=your-imagekit-public-key
IMAGEKIT_PRIVATE_KEY=your-imagekit-private-key
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your-imagekit-id
FILE_STORAGE_TYPE=imagekit

# CORS (Important: Set your frontend domain)
CORS_ORIGINS=https://your-frontend-domain.com,https://www.your-frontend-domain.com

# Flutterwave (Optional - for transfers)
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

#### Platform Configuration (required for production)

Set these via Railway env; **do not use defaults** for real account data:

```bash
PLATFORM_SETTLEMENT_ACCOUNT=<your-settlement-account>
PLATFORM_SETTLEMENT_BANK_CODE=<bank-code>
PLATFORM_SETTLEMENT_ACCOUNT_NAME=<account-name>
PLATFORM_ACCOUNT=<your-platform-account>
PLATFORM_BANK_CODE=<bank-code>
PLATFORM_ACCOUNT_NAME=<account-name>
```

### Step 4: Deploy

1. Railway will automatically start building when you push to your connected branch
2. Or click **"Deploy"** in the Railway dashboard
3. Wait for the build to complete (usually 3-5 minutes)
4. Check the **Logs** tab for any errors

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

### CORS Configuration

**Critical**: Update `CORS_ORIGINS` with your actual frontend domain(s):
- For development: `http://localhost:3000,http://localhost:5173`
- For production: `https://yourdomain.com,https://www.yourdomain.com`

### Webhook URLs

If you're using OnePipe webhooks, update your OnePipe dashboard with:
```
https://your-app-name.up.railway.app/api/payments/webhooks/mandate
https://your-app-name.up.railway.app/api/payments/webhooks/debit
https://your-app-name.up.railway.app/api/payments/webhooks/payout
```

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
