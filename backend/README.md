# Microsoft Dynamics 365 CRM Dashboard - Backend

A Spring Boot application that integrates with Microsoft Dynamics 365 CRM using Azure AD authentication to display and manage CRM data.

## Features

- ✅ Azure AD OAuth 2.0 Authentication
- ✅ Dynamics 365 Web API Integration
- ✅ Account Entity CRUD Operations
- ✅ **Staff Performance Tracking & Rankings** 🆕
- ✅ Email Response Analytics
- ✅ RESTful API Endpoints
- ✅ Health Check & Connection Testing
- ✅ Error Handling & Logging

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Microsoft Azure AD Application Registration
- Dynamics 365 CRM Access
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Azure AD Setup

### 1. Register Application in Azure Portal

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Click **New registration**
4. Enter application name (e.g., "D365 Dashboard")
5. Select **Accounts in this organizational directory only**
6. Click **Register**

### 2. Configure API Permissions

1. In your app registration, go to **API permissions**
2. Click **Add a permission**
3. Select **Dynamics CRM**
4. Choose **Delegated permissions**
5. Select **user_impersonation**
6. Click **Add permissions**
7. Click **Grant admin consent**

### 3. Create Client Secret

1. Go to **Certificates & secrets**
2. Click **New client secret**
3. Add description and set expiration
4. Click **Add**
5. **Copy the secret value immediately** (it won't be shown again)

### 4. Note Your Credentials

From the **Overview** page, copy:

- **Application (client) ID**
- **Directory (tenant) ID**

## Configuration

### 1. Create Environment File

Create a `.env` file or set environment variables:

```bash
# Azure AD Configuration
export AZURE_TENANT_ID="your-tenant-id"
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"

# Dynamics 365 Configuration
export D365_BASE_URL="https://yourorg.crm5.dynamics.com/api/data/v9.2"
export D365_SCOPE="https://yourorg.crm5.dynamics.com/.default"
```

### 2. Update application.properties

Alternatively, update `src/main/resources/application.properties`:

```properties
azure.ad.tenant-id=your-tenant-id
azure.ad.client-id=your-client-id
azure.ad.client-secret=your-client-secret

d365.api.base-url=https://yourorg.crm5.dynamics.com/api/data/v9.2
d365.api.scope=https://yourorg.crm5.dynamics.com/.default
```

## Installation & Running

### Build the Project

```bash
mvn clean install
```

### Run the Application

```bash
mvn spring-boot:run
```

Or run with environment variables:

```bash
AZURE_TENANT_ID=xxx AZURE_CLIENT_ID=xxx AZURE_CLIENT_SECRET=xxx mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check Endpoints

#### Check Application Health

```bash
GET /api/health
```

**Response:**

```json
{
  "success": true,
  "message": "Application is running",
  "data": {
    "status": "UP",
    "application": "D365 Dashboard API",
    "timestamp": 1700000000000,
    "configured": true
  }
}
```

#### Test D365 Connection

```bash
GET /api/health/d365-connection
```

**Response:**

```json
{
  "success": true,
  "message": "Successfully connected to Dynamics 365",
  "data": {
    "baseUrl": "https://yourorg.crm5.dynamics.com/api/data/v9.2",
    "timestamp": 1700000000000,
    "authenticationStatus": "SUCCESS",
    "connectionStatus": "SUCCESS",
    "status": "CONNECTED"
  }
}
```

#### Get Configuration Info

```bash
GET /api/health/config
```

### Account Endpoints

#### Get All Accounts

```bash
GET /api/accounts
GET /api/accounts?top=10
GET /api/accounts?select=name,emailaddress1,telephone1
```

**Parameters:**

- `top` (optional): Maximum number of records to return (default: 50)
- `select` (optional): Comma-separated list of fields to return

**Response:**

```json
{
  "success": true,
  "message": "Successfully retrieved 10 accounts",
  "data": [
    {
      "accountId": "guid-here",
      "name": "Contoso Ltd",
      "accountNumber": "ACC-001",
      "emailAddress": "contact@contoso.com",
      "telephone": "+1-555-0100",
      "city": "Seattle",
      "country": "USA",
      "revenue": 1000000.0
    }
  ]
}
```

#### Get Account by ID

```bash
GET /api/accounts/{accountId}
```

**Response:**

```json
{
  "success": true,
  "message": "Account found",
  "data": {
    "accountId": "guid-here",
    "name": "Contoso Ltd",
    "accountNumber": "ACC-001",
    "emailAddress": "contact@contoso.com"
  }
}
```

#### Get Account Count

```bash
GET /api/accounts/count
```

**Response:**

```json
{
  "success": true,
  "message": "Account count retrieved",
  "data": {
    "count": 150
  }
}
```

#### Search Accounts

```bash
GET /api/accounts/search?city=Seattle&top=20
GET /api/accounts/search?country=USA
```

**Parameters:**

- `city` (optional): Filter by city
- `country` (optional): Filter by country
- `top` (optional): Maximum records (default: 50)

### Staff Performance Endpoints 🆕

#### Get Staff Rankings

```bash
GET /api/staff-performance/rankings
GET /api/staff-performance/rankings?top=10&daysBack=30
```

**Parameters:**

- `top` (optional): Maximum number of staff to return
- `daysBack` (optional): Number of days to analyze (default: 30)

**Response:**

```json
{
  "success": true,
  "message": "Successfully retrieved 10 staff rankings for the last 30 days",
  "data": [
    {
      "staffId": "abc123-...",
      "staffName": "John Doe",
      "email": "john.doe@company.com",
      "totalEmailsSent": 150,
      "totalEmailsReceived": 120,
      "responseRate": 125.0,
      "rank": 1,
      "periodStart": "2025-10-15T00:00:00",
      "periodEnd": "2025-11-14T00:00:00"
    }
  ]
}
```

#### Get Top Performers

```bash
GET /api/staff-performance/top-performers?limit=5
```

**Parameters:**

- `limit` (optional): Number of top performers (default: 10)

#### Get Staff Performance by ID

```bash
GET /api/staff-performance/{staffId}?daysBack=30
```

**Parameters:**

- `staffId` (required): Staff member's system user ID
- `daysBack` (optional): Number of days to analyze (default: 30)

📖 **For complete staff performance API documentation, see [STAFF_PERFORMANCE_API.md](STAFF_PERFORMANCE_API.md)**

## Testing the API

### Using cURL

```bash
# Health check
curl http://localhost:8080/api/health

# Test D365 connection
curl http://localhost:8080/api/health/d365-connection

# Get accounts
curl http://localhost:8080/api/accounts

# Get specific account
curl http://localhost:8080/api/accounts/{account-id}

# Get account count
curl http://localhost:8080/api/accounts/count

# Search accounts
curl "http://localhost:8080/api/accounts/search?city=Seattle"

# Get staff rankings
curl "http://localhost:8080/api/staff-performance/rankings?top=10"

# Get top performers
curl http://localhost:8080/api/staff-performance/top-performers

# Get staff performance by ID
curl "http://localhost:8080/api/staff-performance/{staff-id}?daysBack=7"
```

### Using Test Scripts

```bash
# Test account endpoints
./test-api.sh

# Test staff performance endpoints
./test-staff-performance.sh
```

### Using Postman

1. Import the endpoints into Postman
2. Create a new collection
3. Add requests for each endpoint
4. Test the API

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/backend/
│   │   │   ├── config/
│   │   │   │   ├── AzureAdConfig.java
│   │   │   │   ├── D365Config.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AccountController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   └── StaffPerformanceController.java 🆕
│   │   │   ├── model/
│   │   │   │   ├── Account.java
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── D365Response.java
│   │   │   │   └── StaffPerformance.java 🆕
│   │   │   ├── service/
│   │   │   │   ├── D365AccountService.java
│   │   │   │   ├── D365AuthService.java
│   │   │   │   └── D365StaffPerformanceService.java 🆕
│   │   │   └── BackendApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties 🆕
│   │       └── application-prod.properties 🆕
│   └── test/
├── json/
│   └── CRM.json (D365 metadata reference)
├── pom.xml
├── CONFIGURATION.md 🆕
├── PROJECT_BRIEFING.md
├── README.md
├── STAFF_PERFORMANCE_API.md 🆕
├── test-api.sh
└── test-staff-performance.sh 🆕
```

## Troubleshooting

### Issue: Authentication Failed

**Solution:**

- Verify Azure AD credentials are correct
- Check that client secret hasn't expired
- Ensure API permissions are granted
- Verify tenant ID is correct

### Issue: Connection to D365 Failed

**Solution:**

- Check D365 base URL is correct
- Verify network connectivity
- Ensure user has access to D365
- Check if D365 instance is online

### Issue: Empty Response

**Solution:**

- Verify user has permissions to read accounts in D365
- Check if accounts exist in the CRM
- Review application logs for errors

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Enable Debug Logging

In `application.properties`:

```properties
logging.level.com.example.backend=DEBUG
```

## Security Considerations

- ⚠️ Never commit credentials to version control
- ✅ Use environment variables for sensitive data
- ✅ Keep client secrets secure
- ✅ Rotate secrets regularly
- ✅ Use HTTPS in production
- ✅ Implement rate limiting

## Future Enhancements

- [ ] Add more CRM entities (Contacts, Leads, Opportunities)
- [ ] Implement full CRUD operations
- [ ] Add pagination support
- [ ] Implement OData query filtering
- [ ] Add caching layer
- [ ] Create frontend dashboard
- [ ] Add unit and integration tests
- [ ] Implement retry logic
- [ ] Add monitoring and metrics

## License

This project is for internal use.

## Support

For issues or questions, please contact the development team.

---

**Last Updated:** November 14, 2025
