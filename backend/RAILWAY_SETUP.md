# Quick Railway Setup Checklist

## 🚀 Quick Deploy Steps

1. **Connect Repository**
   - Go to railway.app → New Project → Deploy from GitHub
   - Select your repository
   - Set root directory to `backend`

2. **Add Environment Variables** (Copy-paste these into Railway Variables tab)

```bash
SPRING_PROFILES_ACTIVE=prod
MONGODB_URI=your-mongodb-atlas-connection-string
MONGODB_DATABASE=gigwave_prod
JWT_SECRET=generate-a-strong-32-char-secret
ONEPIPE_API_KEY=your-key
ONEPIPE_SECRET_KEY=your-secret
ONEPIPE_BASE_URL=https://api.onepipe.io/v2/transact
ONEPIPE_ENV=production
IMAGEKIT_PUBLIC_KEY=your-key
IMAGEKIT_PRIVATE_KEY=your-key
IMAGEKIT_URL_ENDPOINT=your-endpoint
FILE_STORAGE_TYPE=imagekit
CORS_ORIGINS=https://your-frontend-domain.com
```

3. **Deploy**
   - Railway will auto-deploy
   - Wait for build to complete
   - Get your URL from Settings → Networking

4. **Test**
   - Health: `https://your-app.up.railway.app/actuator/health`
   - API Docs: `https://your-app.up.railway.app/swagger-ui.html`

## 📋 Full Guide

See `docs/RAILWAY_DEPLOYMENT.md` for complete instructions.
