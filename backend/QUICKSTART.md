# Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Configure Azure AD

1. **Get your credentials** from Azure Portal:

   - Tenant ID
   - Client ID (Application ID)
   - Client Secret

2. **Set environment variables**:

**macOS/Linux:**

```bash
export AZURE_TENANT_ID="your-tenant-id"
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"
export D365_BASE_URL="https://bintarasolutions.crm5.dynamics.com/api/data/v9.2"
export D365_SCOPE="https://bintarasolutions.crm5.dynamics.com/.default"
```

**Windows (PowerShell):**

```powershell
$env:AZURE_TENANT_ID="your-tenant-id"
$env:AZURE_CLIENT_ID="your-client-id"
$env:AZURE_CLIENT_SECRET="your-client-secret"
$env:D365_BASE_URL="https://bintarasolutions.crm5.dynamics.com/api/data/v9.2"
$env:D365_SCOPE="https://bintarasolutions.crm5.dynamics.com/.default"
```

### Step 2: Build & Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

Wait for the message: `Started BackendApplication in X seconds`

### Step 3: Test the API

Open a new terminal and run:

```bash
# 1. Check if application is running
curl http://localhost:8080/api/health

# 2. Test connection to Dynamics 365
curl http://localhost:8080/api/health/d365-connection

# 3. Fetch accounts from CRM
curl http://localhost:8080/api/accounts

# 4. Get account count
curl http://localhost:8080/api/accounts/count
```

### Expected Results

#### Health Check Response:

```json
{
  "success": true,
  "message": "Application is running",
  "data": {
    "status": "UP",
    "configured": true
  }
}
```

#### D365 Connection Test:

```json
{
  "success": true,
  "message": "Successfully connected to Dynamics 365",
  "data": {
    "authenticationStatus": "SUCCESS",
    "connectionStatus": "SUCCESS",
    "status": "CONNECTED"
  }
}
```

#### Accounts Response:

```json
{
  "success": true,
  "message": "Successfully retrieved X accounts",
  "data": [
    {
      "accountId": "...",
      "name": "Account Name",
      "emailAddress": "email@example.com"
    }
  ]
}
```

## 🎯 Key Endpoints to Test

| Endpoint                                | Description              |
| --------------------------------------- | ------------------------ |
| `GET /api/health`                       | Application health check |
| `GET /api/health/d365-connection`       | Test D365 connectivity   |
| `GET /api/accounts`                     | Get all accounts         |
| `GET /api/accounts?top=10`              | Get 10 accounts          |
| `GET /api/accounts/count`               | Get total count          |
| `GET /api/accounts/{id}`                | Get specific account     |
| `GET /api/accounts/search?city=Seattle` | Search accounts          |

## 🔧 Troubleshooting

### "Configuration required" error

➡️ Check that environment variables are set correctly

### "Authentication failed" error

➡️ Verify Azure AD credentials and API permissions

### "Connection failed" error

➡️ Check D365 base URL and network connectivity

## 📝 Next Steps

1. ✅ Test all endpoints with your CRM data
2. 🎨 Build a frontend dashboard (React/Angular/Vue)
3. 🔒 Add more security features
4. 📊 Implement additional CRM entities
5. 🚀 Deploy to production

## 📚 Documentation

- Full documentation: `README.md`
- Project briefing: `PROJECT_BRIEFING.md`
- API reference: See README.md API Endpoints section

## 💡 Tips

- Use Postman for easier API testing
- Check logs in console for detailed error messages
- Enable DEBUG logging for troubleshooting
- Keep client secrets secure and never commit them

---

**Happy Coding! 🎉**
