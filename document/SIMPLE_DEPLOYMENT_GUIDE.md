# CRM Dashboard - Simple Deployment Guide

A quick guide to deploy the CRM Dashboard using Docker on Azure.

---

## Quick Start (Local)

### 1. Setup Environment

```bash
# Copy example env file
cp .env.example .env

# Edit with your credentials
nano .env
```

### 2. Run with Docker

```bash
# Start everything
docker-compose up --build

# Access the app
# Frontend: http://localhost
# Backend:  http://localhost:8080/api/health
```

### 3. Stop

```bash
docker-compose down
```

---

## Deploy to Azure

### Step 1: Login to Azure

```bash
az login
```

### Step 2: Create Resources

```bash
# Create resource group
az group create --name rg-crm --location southeastasia

# Create container registry
az acr create --name mycrm --resource-group rg-crm --sku Basic --admin-enabled true
```

### Step 3: Push Docker Images

```bash
# Login to registry
az acr login --name mycrm

# Build and push backend
docker build -t mycrm.azurecr.io/backend:latest ./backend
docker push mycrm.azurecr.io/backend:latest

# Build and push frontend
docker build -t mycrm.azurecr.io/frontend:latest ./frontend
docker push mycrm.azurecr.io/frontend:latest
```

### Step 4: Deploy to Container Apps

```bash
# Create environment
az containerapp env create --name crm-env --resource-group rg-crm --location southeastasia

# Deploy backend
az containerapp create \
  --name backend \
  --resource-group rg-crm \
  --environment crm-env \
  --image mycrm.azurecr.io/backend:latest \
  --target-port 8080 \
  --ingress internal

# Deploy frontend
az containerapp create \
  --name frontend \
  --resource-group rg-crm \
  --environment crm-env \
  --image mycrm.azurecr.io/frontend:latest \
  --target-port 80 \
  --ingress external
```

### Step 5: Get Your App URL

```bash
az containerapp show --name frontend --resource-group rg-crm --query properties.configuration.ingress.fqdn
```

---

## Environment Variables

| Variable              | Description                | Example                                       |
| --------------------- | -------------------------- | --------------------------------------------- |
| `AZURE_TENANT_ID`     | Azure AD Tenant ID         | `abc-123-...`                                 |
| `AZURE_CLIENT_ID`     | App Registration Client ID | `def-456-...`                                 |
| `AZURE_CLIENT_SECRET` | App Registration Secret    | `***`                                         |
| `D365_BASE_URL`       | Dynamics 365 API URL       | `https://org.crm5.dynamics.com/api/data/v9.2` |
| `D365_SCOPE`          | D365 API Scope             | `https://org.crm5.dynamics.com/.default`      |
| `JWT_SECRET`          | JWT signing key            | Random 32+ chars                              |

---

## Common Issues

| Problem               | Solution                                    |
| --------------------- | ------------------------------------------- |
| Login redirect error  | Update redirect URI in Azure AD             |
| CORS error            | Check `CORS_ALLOWED_ORIGINS` in backend     |
| 403 Forbidden         | Grant admin consent for API permissions     |
| Container won't start | Check logs with `az containerapp logs show` |

---

## Useful Commands

```bash
# View logs
docker-compose logs -f

# Restart containers
docker-compose restart

# Check Azure container status
az containerapp show --name backend --resource-group rg-crm

# Delete everything
az group delete --name rg-crm --yes
```

---

## Architecture

```
User → Frontend (Nginx:80) → Backend (Spring:8080) → Dynamics 365 API
              ↓
         Azure AD (OAuth2)
```

---

For detailed documentation, see [DEPLOY_DOCKER_AZURE.md](DEPLOY_DOCKER_AZURE.md).
