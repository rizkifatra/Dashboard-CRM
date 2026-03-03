# Deploy CRM Dashboard to Azure via Docker

This guide provides step-by-step instructions for deploying the CRM Dashboard application to Azure using Docker containers.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Azure AD Configuration](#azure-ad-configuration)
4. [Local Docker Testing](#local-docker-testing)
5. [Push Images to Azure Container Registry](#push-images-to-azure-container-registry)
6. [Deploy to Azure Container Apps](#deploy-to-azure-container-apps)
7. [Configure Environment Variables](#configure-environment-variables)
8. [Post-Deployment Verification](#post-deployment-verification)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Azure Cloud                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────────┐     ┌──────────────────────────────────┐ │
│   │  Azure AD        │     │  Azure Container Registry (ACR)  │ │
│   │  (OAuth2 Auth)   │     │  - crm-frontend:latest           │ │
│   └──────────────────┘     │  - crm-backend:latest            │ │
│            │               └──────────────────────────────────┘ │
│            │                            │                        │
│            ▼                            ▼                        │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │              Azure Container Apps Environment            │  │
│   │  ┌─────────────────┐        ┌─────────────────────────┐  │  │
│   │  │    Frontend     │───────▶│        Backend          │  │  │
│   │  │   (Nginx)       │  /api  │    (Spring Boot)        │  │  │
│   │  │   Port 80       │        │      Port 8080          │  │  │
│   │  └─────────────────┘        └─────────────────────────┘  │  │
│   └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              ▼                                   │
│                    ┌──────────────────┐                         │
│                    │  Dynamics 365    │                         │
│                    │   CRM API        │                         │
│                    └──────────────────┘                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Components

| Component      | Technology            | Port | Description           |
| -------------- | --------------------- | ---- | --------------------- |
| Frontend       | Angular + Nginx       | 80   | SPA with API proxy    |
| Backend        | Spring Boot (Java 17) | 8080 | REST API with OAuth2  |
| Authentication | Azure AD              | -    | OAuth2/OpenID Connect |
| CRM Data       | Dynamics 365          | -    | Data source via API   |

---

## Prerequisites

### Required Tools

```bash
# Azure CLI
brew install azure-cli  # macOS
# or
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash  # Linux

# Docker
# Install Docker Desktop from https://www.docker.com/products/docker-desktop/

# Verify installations
az --version
docker --version
docker-compose --version
```

### Azure Resources Required

- Azure Subscription with Contributor access
- Azure Container Registry (ACR)
- Azure Container Apps Environment
- Azure AD App Registration
- Dynamics 365 CRM instance

---

## Azure AD Configuration

### Step 1: Create App Registration

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**
3. Click **+ New registration**

| Field                   | Value                                            |
| ----------------------- | ------------------------------------------------ |
| Name                    | `CRM-Dashboard-Production`                       |
| Supported account types | Single tenant                                    |
| Redirect URI (Web)      | `https://<your-app-url>/login/oauth2/code/azure` |

### Step 2: Configure API Permissions

1. Go to **API permissions** → **+ Add a permission**
2. Add the following permissions:

| API             | Permission         | Type      |
| --------------- | ------------------ | --------- |
| Microsoft Graph | User.Read          | Delegated |
| Dynamics CRM    | user_impersonation | Delegated |

3. Click **Grant admin consent**

### Step 3: Create Client Secret

1. Go to **Certificates & secrets**
2. Click **+ New client secret**
3. Set expiration (recommended: 12 months)
4. **Copy the secret value immediately** (shown only once)

### Step 4: Note Credentials

Record these values:

| Credential    | Location                           |
| ------------- | ---------------------------------- |
| Tenant ID     | Overview → Directory (tenant) ID   |
| Client ID     | Overview → Application (client) ID |
| Client Secret | Certificates & secrets → Value     |

---

## Local Docker Testing

### Step 1: Create Environment File

Create a `.env` file in the project root:

```bash
# Azure AD Configuration
AZURE_TENANT_ID=your-tenant-id
AZURE_CLIENT_ID=your-client-id
AZURE_CLIENT_SECRET=your-client-secret

# Dynamics 365 Configuration
D365_TENANT_ID=your-tenant-id
D365_CLIENT_ID=your-client-id
D365_CLIENT_SECRET=your-client-secret
D365_BASE_URL=https://yourorg.crm5.dynamics.com/api/data/v9.2
D365_SCOPE=https://yourorg.crm5.dynamics.com/.default

# JWT Secret (generate a secure random string)
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
```

Or run the configuration script:

```bash
./configure-azure-credentials.sh
```

### Step 2: Build and Run Locally

```bash
# Build and start containers
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop containers
docker-compose down
```

### Step 3: Verify Local Deployment

- Frontend: http://localhost
- Backend Health: http://localhost:8080/api/health
- API (via frontend proxy): http://localhost/api/health

---

## Push Images to Azure Container Registry

### Step 1: Create Azure Resources

```bash
# Login to Azure
az login

# Set subscription (if needed)
az account set --subscription "Your Subscription Name"

# Create resource group
az group create \
  --name rg-crm-dashboard \
  --location southeastasia

# Create Azure Container Registry
az acr create \
  --resource-group rg-crm-dashboard \
  --name crmacr$(date +%s) \
  --sku Basic \
  --admin-enabled true

# Save the ACR name
ACR_NAME=$(az acr list --resource-group rg-crm-dashboard --query "[0].name" -o tsv)
echo "ACR Name: $ACR_NAME"
```

### Step 2: Build and Push Images

```bash
# Login to ACR
az acr login --name $ACR_NAME

# Get ACR login server
ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --query loginServer -o tsv)
echo "ACR Login Server: $ACR_LOGIN_SERVER"

# Build backend image
docker build -t $ACR_LOGIN_SERVER/crm-backend:latest ./backend

# Build frontend image
docker build -t $ACR_LOGIN_SERVER/crm-frontend:latest ./frontend

# Push images
docker push $ACR_LOGIN_SERVER/crm-backend:latest
docker push $ACR_LOGIN_SERVER/crm-frontend:latest

# Verify images in ACR
az acr repository list --name $ACR_NAME -o table
```

---

## Deploy to Azure Container Apps

### Step 1: Create Container Apps Environment

```bash
# Install Container Apps extension
az extension add --name containerapp --upgrade

# Register required providers
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights

# Create Log Analytics workspace
az monitor log-analytics workspace create \
  --resource-group rg-crm-dashboard \
  --workspace-name crm-logs

# Get workspace credentials
LOG_ANALYTICS_WORKSPACE_ID=$(az monitor log-analytics workspace show \
  --resource-group rg-crm-dashboard \
  --workspace-name crm-logs \
  --query customerId -o tsv)

LOG_ANALYTICS_KEY=$(az monitor log-analytics workspace get-shared-keys \
  --resource-group rg-crm-dashboard \
  --workspace-name crm-logs \
  --query primarySharedKey -o tsv)

# Create Container Apps Environment
az containerapp env create \
  --name crm-env \
  --resource-group rg-crm-dashboard \
  --location southeastasia \
  --logs-workspace-id $LOG_ANALYTICS_WORKSPACE_ID \
  --logs-workspace-key $LOG_ANALYTICS_KEY
```

### Step 2: Deploy Backend Container App

```bash
# Get ACR credentials
ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv)

# Deploy backend
az containerapp create \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --environment crm-env \
  --image $ACR_LOGIN_SERVER/crm-backend:latest \
  --target-port 8080 \
  --ingress internal \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 1.0 \
  --memory 2.0Gi \
  --registry-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --env-vars \
    AZURE_TENANT_ID=your-tenant-id \
    AZURE_CLIENT_ID=your-client-id \
    AZURE_CLIENT_SECRET=secretref:azure-client-secret \
    D365_TENANT_ID=your-tenant-id \
    D365_CLIENT_ID=your-client-id \
    D365_CLIENT_SECRET=secretref:d365-client-secret \
    D365_BASE_URL=https://yourorg.crm5.dynamics.com/api/data/v9.2 \
    D365_SCOPE=https://yourorg.crm5.dynamics.com/.default \
    JWT_SECRET=secretref:jwt-secret \
    SPRING_PROFILES_ACTIVE=prod \
    FRONTEND_URL=https://your-app-url \
    CORS_ALLOWED_ORIGINS=https://your-app-url

# Get backend FQDN (internal)
BACKEND_FQDN=$(az containerapp show \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --query properties.configuration.ingress.fqdn -o tsv)
echo "Backend FQDN: $BACKEND_FQDN"
```

### Step 3: Deploy Frontend Container App

```bash
# Deploy frontend
az containerapp create \
  --name crm-frontend \
  --resource-group rg-crm-dashboard \
  --environment crm-env \
  --image $ACR_LOGIN_SERVER/crm-frontend:latest \
  --target-port 80 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --registry-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --env-vars \
    BACKEND_URL=crm-backend:8080 \
    BACKEND_HOST=crm-backend \
    DNS_RESOLVER=168.63.129.16 \
    FORWARDED_PROTO=https

# Get frontend URL
FRONTEND_URL=$(az containerapp show \
  --name crm-frontend \
  --resource-group rg-crm-dashboard \
  --query properties.configuration.ingress.fqdn -o tsv)
echo "Frontend URL: https://$FRONTEND_URL"
```

### Step 4: Update Backend with Correct Frontend URL

```bash
# Update backend environment variables
az containerapp update \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --set-env-vars \
    FRONTEND_URL=https://$FRONTEND_URL \
    CORS_ALLOWED_ORIGINS=https://$FRONTEND_URL
```

---

## Configure Environment Variables

### Using Azure Portal

1. Navigate to **Container Apps** → **crm-backend**
2. Go to **Settings** → **Secrets**
3. Add secrets:
   - `azure-client-secret`
   - `d365-client-secret`
   - `jwt-secret`

4. Go to **Containers** → **Environment variables**
5. Reference secrets using `secretref:secret-name`

### Using Azure CLI with Secrets

```bash
# Add secrets
az containerapp secret set \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --secrets \
    azure-client-secret="your-actual-client-secret" \
    d365-client-secret="your-d365-client-secret" \
    jwt-secret="your-jwt-secret"

# Update environment variables to use secrets
az containerapp update \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --set-env-vars \
    AZURE_CLIENT_SECRET=secretref:azure-client-secret \
    D365_CLIENT_SECRET=secretref:d365-client-secret \
    JWT_SECRET=secretref:jwt-secret
```

---

## Post-Deployment Verification

### Step 1: Update Azure AD Redirect URI

1. Go to Azure Portal → App registrations → Your App
2. Add redirect URI: `https://<frontend-url>/login/oauth2/code/azure`

### Step 2: Health Checks

```bash
# Check backend health
curl https://<frontend-url>/api/health

# Expected response:
# {"status":"UP","timestamp":"..."}
```

### Step 3: Test Authentication Flow

1. Open `https://<frontend-url>` in browser
2. Click **Login with Microsoft**
3. Complete Azure AD authentication
4. Verify redirect back to dashboard

### Step 4: View Logs

```bash
# View backend logs
az containerapp logs show \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --follow

# View frontend logs
az containerapp logs show \
  --name crm-frontend \
  --resource-group rg-crm-dashboard \
  --follow
```

---

## Troubleshooting

### Common Issues

#### 1. OAuth2 Redirect Mismatch

**Error:** `AADSTS50011: The redirect URI does not match`

**Solution:**

- Verify redirect URI in Azure AD matches exactly: `https://<your-url>/login/oauth2/code/azure`
- Check `FRONTEND_URL` environment variable in backend

#### 2. CORS Errors

**Error:** `Access-Control-Allow-Origin` errors in browser console

**Solution:**

```bash
az containerapp update \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --set-env-vars CORS_ALLOWED_ORIGINS=https://your-frontend-url
```

#### 3. Backend Connection Refused

**Error:** Frontend cannot reach backend

**Solution:**

- Verify backend ingress is set to `internal`
- Check `BACKEND_URL` is `crm-backend:8080` (service name, not FQDN)
- Verify both apps are in the same Container Apps environment

#### 4. Dynamics 365 API 403 Forbidden

**Error:** `AADSTS65001` or 403 errors when fetching data

**Solution:**

1. Verify API permissions in Azure AD app
2. Grant admin consent for permissions
3. Create Application User in Power Platform Admin Center
4. Assign appropriate security role to application user

#### 5. Container Fails to Start

**Diagnose:**

```bash
# Check container status
az containerapp show \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --query "properties.runningStatus"

# View startup logs
az containerapp logs show \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --type system
```

### DNS Resolution (Azure vs Docker)

| Environment          | DNS Resolver    | Notes               |
| -------------------- | --------------- | ------------------- |
| Local Docker         | `127.0.0.11`    | Docker embedded DNS |
| Azure Container Apps | `168.63.129.16` | Azure DNS           |

The frontend Dockerfile automatically handles this via `DNS_RESOLVER` environment variable.

---

## Useful Commands Reference

### Container Management

```bash
# Restart container
az containerapp revision restart \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --revision <revision-name>

# Scale containers
az containerapp update \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --min-replicas 2 \
  --max-replicas 5

# Delete container app
az containerapp delete \
  --name crm-backend \
  --resource-group rg-crm-dashboard
```

### Image Updates

```bash
# Build and push new image
docker build -t $ACR_LOGIN_SERVER/crm-backend:v2 ./backend
docker push $ACR_LOGIN_SERVER/crm-backend:v2

# Update container app with new image
az containerapp update \
  --name crm-backend \
  --resource-group rg-crm-dashboard \
  --image $ACR_LOGIN_SERVER/crm-backend:v2
```

### Resource Cleanup

```bash
# Delete all resources
az group delete --name rg-crm-dashboard --yes --no-wait
```

---

## Security Best Practices

1. **Never commit secrets** - Use `.env` files locally, Azure secrets in production
2. **Use managed identity** - Consider Azure Managed Identity for ACR access
3. **Enable HTTPS only** - Azure Container Apps provides automatic HTTPS
4. **Rotate secrets regularly** - Set calendar reminders for client secret expiration
5. **Restrict network access** - Use internal ingress for backend
6. **Monitor logs** - Set up alerts for authentication failures

---

## Additional Resources

- [Azure Container Apps Documentation](https://docs.microsoft.com/azure/container-apps/)
- [Azure Container Registry Documentation](https://docs.microsoft.com/azure/container-registry/)
- [Azure AD App Registration](https://docs.microsoft.com/azure/active-directory/develop/quickstart-register-app)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Dynamics 365 Web API](https://docs.microsoft.com/dynamics365/customerengagement/on-premises/developer/webapi/overview)
