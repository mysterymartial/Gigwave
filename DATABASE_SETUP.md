# Database Setup Guide

## Current Setup

### Development (Current)
- **Type**: Local MongoDB
- **Connection**: `mongodb://localhost:27017/gigwave_dev`
- **Status**: Real database (from your logs, it's connected and working)

### Tests (Current)
- **Type**: Mockito Mocks
- **Status**: No real database - all repositories are mocked
- **Location**: All test files use `@Mock` annotations

---

## Option 1: Use Embedded MongoDB for Tests (Recommended)

This allows tests to run with a real MongoDB instance without needing a running database.

### Setup Embedded MongoDB for Tests

1. **Create Test Configuration**

Create `backend/src/test/resources/application-test.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: ${spring.data.mongodb.uri}
      database: gigwave_test

logging:
  level:
    com.gigwave: DEBUG
    org.springframework.data.mongodb: DEBUG
```

2. **Create Base Test Class**

Create `backend/src/test/java/com/gigwave/TestBase.java`:

```java
package com.gigwave;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/gigwave_test"
})
public abstract class TestBase {
    // Base class for integration tests
}
```

3. **Update Test to Use Real MongoDB**

Example: Update `AuthServiceTest.java` to use embedded MongoDB:

```java
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Clean database before each test
    }
    
    @Test
    void testRegister() {
        // Test with real database
        RegisterRequest request = new RegisterRequest(...);
        authService.register(request);
        // Assertions...
    }
}
```

### Run Tests with Embedded MongoDB

```bash
cd backend
mvn test
```

The embedded MongoDB will automatically start and stop with tests.

---

## Option 2: Set Up MongoDB Atlas (Cloud Database)

### Step 1: Create MongoDB Atlas Account

1. Go to https://www.mongodb.com/cloud/atlas/register
2. Sign up for a free account
3. Create a new organization and project

### Step 2: Create a Cluster

1. Click "Build a Database"
2. Choose **FREE (M0)** tier
3. Select a cloud provider and region (choose closest to you)
4. Name your cluster (e.g., "GigWave-Cluster")
5. Click "Create"

### Step 3: Create Database User

1. Go to "Database Access" in left menu
2. Click "Add New Database User"
3. Choose "Password" authentication
4. Username: `gigwave-user` (or your choice)
5. Password: Generate a strong password (save it!)
6. Database User Privileges: "Atlas admin" (or "Read and write to any database")
7. Click "Add User"

### Step 4: Configure Network Access

1. Go to "Network Access" in left menu
2. Click "Add IP Address"
3. For development: Click "Allow Access from Anywhere" (0.0.0.0/0)
   - **Note**: For production, add only your server IPs
4. Click "Confirm"

### Step 5: Get Connection String

1. Go to "Database" in left menu
2. Click "Connect" on your cluster
3. Choose "Connect your application"
4. Copy the connection string (looks like):
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```

### Step 6: Update Application Configuration

Update `backend/src/main/resources/application-dev.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb+srv://gigwave-user:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_dev?retryWrites=true&w=majority}
      database: gigwave_dev
```

Or set environment variable:
```bash
export MONGODB_URI="mongodb+srv://gigwave-user:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/gigwave_dev?retryWrites=true&w=majority"
```

### Step 7: Test Connection

Restart your application and check logs - you should see successful connection.

---

## Option 3: Use Local MongoDB (Current Setup - Keep Using)

Your current setup is already working! You're using:
- **Local MongoDB** on `localhost:27017`
- **Database**: `gigwave_dev`

### To Create/Reset Database

MongoDB automatically creates databases when you first write to them. To reset:

1. **Using MongoDB Shell**:
```bash
mongosh
use gigwave_dev
db.dropDatabase()
```

2. **Or delete data manually**:
```bash
# Stop your application
# Delete MongoDB data directory (if you want fresh start)
# Restart MongoDB
```

### To Add Test Data

Create a data seeding script or use MongoDB Compass GUI.

---

## Recommended Setup for Development

### For Local Development:
- ✅ Keep using **local MongoDB** (what you have now)
- ✅ Database auto-creates when you first use it
- ✅ No setup needed - it's already working!

### For Testing:
- ✅ Use **Embedded MongoDB** (in-memory, no setup needed)
- ✅ Tests run independently
- ✅ No need for running MongoDB during tests

### For Production:
- ✅ Use **MongoDB Atlas** (cloud, managed, scalable)
- ✅ Set connection string in environment variables
- ✅ Configure proper IP whitelist

---

## Quick Start: Add Test Data

### Option A: Using MongoDB Compass (GUI)

1. Download MongoDB Compass: https://www.mongodb.com/try/download/compass
2. Connect to: `mongodb://localhost:27017`
3. Select database: `gigwave_dev`
4. Create collections and insert documents manually

### Option B: Using MongoDB Shell

```bash
mongosh
use gigwave_dev

# Insert a test user
db.users.insertOne({
  _id: ObjectId(),
  phone: "08012345678",
  email: "test@example.com",
  passwordHash: "$2a$10$...", // BCrypt hash
  role: "MUSICIAN",
  isDisabled: false,
  createdAt: new Date()
})
```

### Option C: Create a Data Seeder Class

Create `backend/src/main/java/com/gigwave/infrastructure/persistence/DataSeeder.java`:

```java
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Seed initial data
            User admin = User.builder()
                .phone("08000000000")
                .email("admin@gigwave.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build();
            userRepository.save(admin);
        }
    }
}
```

---

## Summary

**Current Status:**
- ✅ You're using **real local MongoDB** (not mocks for running app)
- ✅ Tests use **Mockito mocks** (not real database)
- ✅ Database `gigwave_dev` is already created and working

**For Testing:**
- Tests currently use mocks (no database needed)
- You can optionally switch to embedded MongoDB for integration tests

**For Production:**
- Set up MongoDB Atlas when ready
- Update connection string in environment variables

**No Action Needed Right Now** - Your setup is working! The database is automatically created when you first use it.


