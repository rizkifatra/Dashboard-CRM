# Configuration Guide

## How to Use Different Environments

### Development Mode (Default)

1. Edit `application-dev.properties`
2. Replace the placeholder values with your actual Azure AD credentials:
   ```properties
   azure.ad.tenant-id=your-actual-tenant-id
   azure.ad.client-id=your-actual-client-id
   azure.ad.client-secret=your-actual-client-secret
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Production Mode

1. Set environment variables on your production server:
   ```bash
   export AZURE_TENANT_ID="your-tenant-id"
   export AZURE_CLIENT_ID="your-client-id"
   export AZURE_CLIENT_SECRET="your-client-secret"
   export D365_BASE_URL="https://bintarasolutions.crm5.dynamics.com/api/data/v9.2"
   export D365_SCOPE="https://bintarasolutions.crm5.dynamics.com/.default"
   export CORS_ALLOWED_ORIGINS="https://your-domain.com"
   ```
2. Run with production profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
   ```

## Configuration Files Overview

| File                          | Purpose                              | Contains Secrets?         |
| ----------------------------- | ------------------------------------ | ------------------------- |
| `application.properties`      | Common settings for all environments | ❌ No                     |
| `application-dev.properties`  | Development settings (local testing) | ⚠️ Yes (but only for dev) |
| `application-prod.properties` | Production settings (uses env vars)  | ❌ No (reads from env)    |

## Switching Environments

Edit `application.properties` and change the active profile:

```properties
spring.profiles.active=dev   # for development
spring.profiles.active=prod  # for production
```

Or use command line:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Where to Get Azure AD Credentials

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**
3. Select your application
4. Find:
   - **Tenant ID**: Overview page
   - **Client ID**: Overview page
   - **Client Secret**: Certificates & secrets tab
