# Project Briefing: Microsoft Dynamics 365 CRM Dashboard

## Project Overview

A Spring Boot dashboard application that integrates with Microsoft Dynamics 365 CRM API to display and manage CRM data, specifically focusing on the Account entity.

## Technical Stack

- **Backend Framework**: Spring Boot 3.5.7
- **Java Version**: 17
- **Authentication**: Microsoft Azure AD (OAuth 2.0)
- **API**: Microsoft Dynamics 365 Web API v9.2
- **CRM Instance**: bintarasolutions.crm5.dynamics.com

## Key Features

### Phase 1 - MVP (Current Implementation)

1. **Azure AD Authentication**

   - OAuth 2.0 integration with Microsoft Identity Platform
   - Token management and refresh
   - Secure credential storage

2. **Account Data Display**

   - Fetch accounts from Dynamics 365 CRM
   - Display account list with key information
   - RESTful API endpoints for frontend integration

3. **API Testing Endpoints**
   - Health check endpoint
   - Test connection to D365
   - Retrieve account data

## Architecture

### Components

1. **Configuration Layer**

   - `AzureAdConfig`: Azure AD OAuth configuration
   - `application.properties`: External configuration

2. **Service Layer**

   - `D365AuthService`: Handles authentication with Azure AD
   - `D365AccountService`: Manages Account entity operations

3. **Controller Layer**

   - `AccountController`: REST endpoints for Account operations
   - `HealthController`: System health and connection testing

4. **Model Layer**
   - `Account`: Account entity model
   - `D365Response`: Generic response wrapper

## API Endpoints

### Authentication Test

- `GET /api/health` - Check application health
- `GET /api/health/d365-connection` - Test D365 connection

### Account Operations

- `GET /api/accounts` - Get all accounts
- `GET /api/accounts/{id}` - Get specific account by ID
- `GET /api/accounts/count` - Get total account count

## Configuration Required

### Azure AD App Registration

1. Register application in Azure Portal
2. Configure API permissions for Dynamics 365
3. Required permissions:
   - `Dynamics CRM.user_impersonation`
   - `Dynamics CRM.api.read`
4. Generate client secret

### Application Properties

```properties
# Azure AD Configuration
azure.ad.tenant-id=<your-tenant-id>
azure.ad.client-id=<your-client-id>
azure.ad.client-secret=<your-client-secret>
azure.ad.scope=https://<your-org>.crm5.dynamics.com/.default

# Dynamics 365 Configuration
d365.api.base-url=https://bintarasolutions.crm5.dynamics.com/api/data/v9.2
d365.api.timeout=30000
```

## Data Structure

### CRM JSON Reference

The `json/CRM.json` file contains metadata about all available entities in the D365 instance:

- 9,329 lines of entity definitions
- Key entity: `accounts` (EntitySet)
- API endpoint: `https://bintarasolutions.crm5.dynamics.com/api/data/v9.2/accounts`

### Account Entity (Common Fields)

- `accountid` - Primary key (GUID)
- `name` - Account name
- `accountnumber` - Account number
- `emailaddress1` - Primary email
- `telephone1` - Primary phone
- `address1_city` - City
- `address1_country` - Country
- `revenue` - Annual revenue
- `industrycode` - Industry code
- `createdon` - Created date
- `modifiedon` - Modified date

## Security Considerations

1. Never commit credentials to version control
2. Use environment variables for sensitive data
3. Implement proper error handling without exposing system details
4. Add rate limiting for API calls
5. Validate and sanitize all inputs

## Testing Strategy

1. **Unit Tests**: Service layer business logic
2. **Integration Tests**: D365 API connectivity
3. **Manual Testing**: Postman/cURL for endpoint validation

## Future Enhancements

- Add more entities (Contacts, Leads, Opportunities)
- Implement CRUD operations
- Add pagination and filtering
- Create dashboard UI with React/Angular
- Real-time data sync
- Advanced reporting and analytics
- Caching layer for performance

## Development Setup

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- Azure AD access
- Dynamics 365 CRM access

### Build and Run

```bash
# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run

# Run tests
mvn test
```

### Testing the API

```bash
# Health check
curl http://localhost:8080/api/health

# Test D365 connection
curl http://localhost:8080/api/health/d365-connection

# Get accounts
curl http://localhost:8080/api/accounts
```

## Dependencies Added

- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-security` - Security framework
- `spring-boot-starter-oauth2-client` - OAuth 2.0 client
- `azure-identity` - Azure authentication
- `jackson-databind` - JSON processing
- `lombok` - Reduce boilerplate code

## Contact & Support

- Project Lead: [Your Name]
- Development Team: [Team Members]
- Documentation: This file and inline code comments

---

**Document Version**: 1.0  
**Last Updated**: November 14, 2025  
**Status**: Initial Implementation
