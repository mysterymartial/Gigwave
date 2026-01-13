# MongoDB Atlas Setup Guide (UI-Based)

## Step-by-Step Setup Using MongoDB Atlas Web Interface

### Step 1: Create MongoDB Atlas Account

1. **Go to MongoDB Atlas Website**
   - Visit: https://www.mongodb.com/cloud/atlas/register
   - Click "Try Free" or "Sign Up"

2. **Sign Up**
   - Choose "Sign up with Email"
   - Enter your email address
   - Create a password
   - Accept terms and conditions
   - Click "Create your Atlas account"

3. **Verify Email**
   - Check your email inbox
   - Click the verification link

---

### Step 2: Create Organization and Project

1. **Create Organization** (if prompted)
   - Organization Name: `GigWave` (or your choice)
   - Click "Next"

2. **Create Project**
   - Project Name: `GigWave Project` (or your choice)
   - Click "Next"

3. **Skip Optional Steps**
   - Click "Skip" on any optional setup screens

---

### Step 3: Create a Free Cluster

1. **Click "Build a Database"**
   - You'll see this button on the main dashboard

2. **Choose Deployment Type**
   - Select **"M0 FREE"** (Free tier)
   - Click "Create"

3. **Choose Cloud Provider**
   - Select a cloud provider (AWS, Google Cloud, or Azure)
   - **Important**: Choose the region closest to you for better performance
   - Example: If you're in Nigeria, choose a region like `eu-west-1` (Ireland) or `us-east-1` (N. Virginia)

4. **Name Your Cluster**
   - Cluster Name: `GigWave-Cluster` (or your choice)
   - Leave other settings as default

5. **Click "Create Cluster"**
   - Wait 3-5 minutes for cluster to be created
   - You'll see a progress indicator

---

### Step 4: Create Database User

1. **Go to Database Access**
   - Click "Database Access" in the left sidebar menu

2. **Add New Database User**
   - Click the green "Add New Database User" button

3. **Authentication Method**
   - Select **"Password"** (default)

4. **User Details**
   - Username: `gigwave-user` (or your preferred username)
   - Password: Click "Autogenerate Secure Password" OR create your own
   - **IMPORTANT**: Copy and save the password! You won't see it again.

5. **User Privileges**
   - Select **"Atlas admin"** (gives full access)
   - OR select "Read and write to any database" (more secure)

6. **Click "Add User"**
   - Wait for confirmation message

---

### Step 5: Configure Network Access (Whitelist IPs)

1. **Go to Network Access**
   - Click "Network Access" in the left sidebar menu

2. **Add IP Address**
   - Click the green "Add IP Address" button

3. **For Development/Testing:**
   - Click **"Allow Access from Anywhere"**
   - This adds `0.0.0.0/0` (allows all IPs)
   - **Note**: This is fine for development but NOT recommended for production
   - Click "Confirm"

4. **For Production (Later):**
   - Click "Add Current IP Address" to add your current IP
   - Or manually add specific IP addresses of your servers

5. **Wait for Confirmation**
   - Status should show "Active" (green checkmark)

---

### Step 6: Get Connection String

1. **Go to Database**
   - Click "Database" in the left sidebar menu

2. **Connect to Your Cluster**
   - Find your cluster (e.g., "GigWave-Cluster")
   - Click the **"Connect"** button

3. **Choose Connection Method**
   - Select **"Connect your application"**

4. **Copy Connection String**
   - You'll see a connection string like:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```
   - Click the **copy icon** to copy it

5. **Replace Placeholders**
   - Replace `<username>` with your database username (e.g., `gigwave-user`)
   - Replace `<password>` with your database password
   - **Example result**:
   ```
   mongodb+srv://gigwave-user:MyPassword123@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```

6. **Add Database Name**
   - Add your database name at the end:
   ```
   mongodb+srv://gigwave-user:MyPassword123@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority
   ```

---

### Step 7: Update Your Application Configuration

1. **For Production Environment**
   - Update `backend/src/main/resources/application-prod.yml`
   - Or set environment variable: `MONGODB_URI`

2. **Connection String Format**
   ```yaml
   spring:
     data:
       mongodb:
         uri: ${MONGODB_URI:mongodb+srv://gigwave-user:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority}
         database: gigwave_prod
   ```

3. **Set Environment Variable** (Recommended)
   ```bash
   # Windows PowerShell
   $env:MONGODB_URI="mongodb+srv://gigwave-user:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority"
   
   # Linux/Mac
   export MONGODB_URI="mongodb+srv://gigwave-user:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_prod?retryWrites=true&w=majority"
   ```

---

### Step 8: Test Connection

1. **Restart Your Application**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Check Logs**
   - Look for: "Connection pool ready"
   - Look for: "Monitor thread successfully connected"
   - No errors = Success!

---

## Important Notes

### Security Best Practices

1. **Never commit passwords to Git**
   - Use environment variables
   - Use `.env` files (add to `.gitignore`)

2. **IP Whitelist for Production**
   - Remove `0.0.0.0/0` in production
   - Add only your server IPs

3. **Use Strong Passwords**
   - At least 12 characters
   - Mix of letters, numbers, symbols

### Free Tier Limitations

- **Storage**: 512 MB (enough for development/testing)
- **RAM**: Shared (may be slower during peak times)
- **No Backup**: Free tier doesn't include automated backups
- **No Multi-Region**: Single region only

### Connection String Security

- **URL Encode Special Characters**: If your password has special characters like `@`, `#`, `%`, encode them:
  - `@` becomes `%40`
  - `#` becomes `%23`
  - `%` becomes `%25`

---

## Troubleshooting

### Connection Failed
- Check IP whitelist includes your IP
- Verify username and password are correct
- Check if password has special characters (URL encode them)
- Verify cluster is running (status should be green)

### Authentication Failed
- Double-check username and password
- Make sure you replaced `<username>` and `<password>` in connection string
- Try regenerating password in Database Access

### Timeout Errors
- Check your internet connection
- Verify cluster region is accessible
- Check firewall settings

---

## Next Steps

After setting up MongoDB Atlas:

1. ✅ Update `application-prod.yml` with connection string
2. ✅ Set environment variables
3. ✅ Test connection
4. ✅ Deploy your application

Your database will be automatically created when your application first writes data to it!


