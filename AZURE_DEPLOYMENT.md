# Azure Container Apps Deployment Guide

This guide walks you through deploying the CRM Dashboard to Azure Container Apps.

## Prerequisites

1. **Azure CLI** installed and logged in (`az login`)
2. **Docker** installed locally
3. **Azure subscription** with permissions to create resources
4. **Azure AD App Registration** configured for OAuth2

## Deployment Steps

> **Important for ARM Mac Users (M1/M2/M3):** When building Docker images on ARM-based Macs, you must include `--platform linux/amd64` flag to ensure compatibility with Azure's x86_64 architecture.

### Step 1: Set Environment Variables

```bash
# Azure resource configuration
export RESOURCE_GROUP="crm-dashboard-rg"
export LOCATION="southeastasia"
export ACR_NAME="crmdashboardacr"              # Must be unique globally
export ENVIRONMENT_NAME="crm-dashboard-env"
export BACKEND_APP_NAME="crm-backend"
export FRONTEND_APP_NAME="crm-frontend"

# From your existing .env file
export AZURE_TENANT_ID="your-azure-tenant-id"
export AZURE_CLIENT_ID="your-azure-client-id"
export AZURE_CLIENT_SECRET="your-azure-client-secret"
export D365_TENANT_ID="your-d365-tenant-id"
export D365_CLIENT_ID="your-d365-client-id"
export D365_CLIENT_SECRET="your-d365-client-secret"
export D365_BASE_URL="https://yourorg.crm5.dynamics.com/api/data/v9.2"
export D365_SCOPE="https://yourorg.crm5.dynamics.com/.default"
export JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters"
```

### Step 2: Create Azure Resources

```bash
# Create Resource Group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Azure Container Registry
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $ACR_NAME \
  --sku Basic \
  --admin-enabled true

# Get ACR credentials
export ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --query loginServer -o tsv)
export ACR_USERNAME=$(az acr credential show --name $ACR_NAME --query username -o tsv)
export ACR_PASSWORD=$(az acr credential show --name $ACR_NAME --query "passwords[0].value" -o tsv)

# Create Container Apps Environment
az containerapp env create \
  --name $ENVIRONMENT_NAME \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION
```

### Step 3: Build and Push Backend Image

```bash
# Login to ACR
az acr login --name $ACR_NAME

# Build and push backend image
# Note: Add --platform linux/amd64 if building on ARM Mac (M1/M2/M3)
cd backend
docker build --platform linux/amd64 -t $ACR_LOGIN_SERVER/crm-backend:latest .
docker push $ACR_LOGIN_SERVER/crm-backend:latest
cd ..
```

### Step 4: Deploy Backend Container App

```bash
az containerapp create \
  --name $BACKEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $ACR_LOGIN_SERVER/crm-backend:latest \
  --registry-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --target-port 8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --env-vars \
    AZURE_TENANT_ID=$AZURE_TENANT_ID \
    AZURE_CLIENT_ID=$AZURE_CLIENT_ID \
    AZURE_CLIENT_SECRET=secretref:azure-client-secret \
    D365_TENANT_ID=$D365_TENANT_ID \
    D365_CLIENT_ID=$D365_CLIENT_ID \
    D365_CLIENT_SECRET=secretref:d365-client-secret \
    D365_BASE_URL=$D365_BASE_URL \
    D365_SCOPE=$D365_SCOPE \
    JWT_SECRET=secretref:jwt-secret \
    SPRING_PROFILES_ACTIVE=prod

# Get the backend URL
export BACKEND_URL=$(az containerapp show \
  --name $BACKEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query properties.configuration.ingress.fqdn -o tsv)

echo "Backend URL: https://$BACKEND_URL"
```

### Step 5: Update Frontend Environment & Rebuild

Before building the frontend, update the production environment with your backend URL:

**Edit `frontend/src/environments/environment.prod.ts`:**

```typescript
export const environment = {
  production: true,
  apiUrl: "https://YOUR_BACKEND_URL/api", // Replace with actual backend URL
  apiBaseUrl: "https://YOUR_BACKEND_URL",
  oauth2AuthorizationUrl: "https://YOUR_BACKEND_URL/oauth2/authorization/azure",
};
```

### Step 6: Build and Push Frontend Image

```bash
# Build and push frontend image
# Note: Add --platform linux/amd64 if building on ARM Mac (M1/M2/M3)
cd frontend
docker build --platform linux/amd64 -t $ACR_LOGIN_SERVER/crm-frontend:latest .
docker push $ACR_LOGIN_SERVER/crm-frontend:latest
cd ..
```

### Step 7: Deploy Frontend Container App

```bash
az containerapp create \
  --name $FRONTEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $ACR_LOGIN_SERVER/crm-frontend:latest \
  --registry-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --target-port 80 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.25 \
  --memory 0.5Gi

# Get the frontend URL
export FRONTEND_URL=$(az containerapp show \
  --name $FRONTEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query properties.configuration.ingress.fqdn -o tsv)

echo "Frontend URL: https://$FRONTEND_URL"
```

### Step 8: Update Azure AD Redirect URIs

**IMPORTANT:** Update your Azure AD App Registration with the new URLs:

1. Go to Azure Portal → Azure Active Directory → App Registrations
2. Select your application
3. Go to **Authentication**
4. Add the following **Redirect URIs**:
   - `https://<BACKEND_URL>/login/oauth2/code/azure`
   - `https://<FRONTEND_URL>/auth/callback`
5. Click **Save**

### Step 9: Update CORS Configuration

Update the backend to allow the frontend origin:

```bash
az containerapp update \
  --name $BACKEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars CORS_ALLOWED_ORIGINS=https://$FRONTEND_URL
```

## Verification

1. Visit `https://<FRONTEND_URL>` in your browser
2. Click "Login with Microsoft"
3. Authenticate with your Microsoft account
4. You should be redirected to the dashboard

## Troubleshooting

### Check Container Logs

```bash
# Backend logs
az containerapp logs show \
  --name $BACKEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --follow

# Frontend logs
az containerapp logs show \
  --name $FRONTEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --follow
```

### Common Issues

1. **OAuth2 redirect error**: Ensure redirect URIs are correctly configured in Azure AD
2. **CORS errors**: Verify CORS_ALLOWED_ORIGINS includes the frontend URL
3. **502 Bad Gateway**: Backend might still be starting, wait a few minutes
4. **Connection refused**: Check if backend health endpoint responds

## Cleanup

To remove all deployed resources:

```bash
az group delete --name $RESOURCE_GROUP --yes --no-wait
```

## Cost Estimate

Approximate monthly costs (subject to change):

- Container Apps (2 apps, minimal usage): ~$10-20/month
- Container Registry (Basic): ~$5/month
- **Total**: ~$15-25/month for light usage

## Alternative: Deploy with Secrets

For better security, use Azure Container Apps secrets:

```bash
# Create secrets
az containerapp secret set \
  --name $BACKEND_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --secrets \
    azure-client-secret=$AZURE_CLIENT_SECRET \
    d365-client-secret=$D365_CLIENT_SECRET \
    jwt-secret=$JWT_SECRET

# Reference secrets in env vars (already done in Step 4)
```
