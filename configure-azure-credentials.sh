#!/bin/bash

# ====================================
# Azure Credentials Configuration Script
# ====================================
# This script helps you configure Azure AD credentials for your backend

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Azure AD Credentials Configuration for CRM Dashboard    ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if user has created a new Azure AD app
echo -e "${YELLOW}IMPORTANT:${NC} Have you created a new Azure AD app following CREATE_NEW_AZURE_APP.md?"
echo ""
echo "  1) Yes - I have created a new app and have the credentials"
echo "  2) No - I need to create it first"
echo "  3) I want to use the existing credentials"
echo ""
read -p "Enter your choice (1/2/3): " choice

case $choice in
  1)
    echo -e "\n${GREEN}Great!${NC} Let's configure your new credentials.\n"
    ;;
  2)
    echo -e "\n${YELLOW}Please follow these steps first:${NC}"
    echo "  1. Open: CREATE_NEW_AZURE_APP.md"
    echo "  2. Complete Steps 1-5 to create a new Azure AD app"
    echo "  3. Copy the credentials (Tenant ID, Client ID, Client Secret)"
    echo "  4. Come back and run this script again"
    echo ""
    echo "  Quick link: https://portal.azure.com → Azure AD → App registrations"
    echo ""
    exit 0
    ;;
  3)
    echo -e "\n${BLUE}Using existing credentials...${NC}\n"
    ;;
  *)
    echo -e "\n${RED}Invalid choice. Exiting.${NC}\n"
    exit 1
    ;;
esac

# Current credentials
CURRENT_TENANT="b54af29e-7225-44f6-8f90-c592bb23d426"
CURRENT_CLIENT="e7c3e530-a97d-49e9-9bc4-314fe1a32122"

echo "════════════════════════════════════════════════════════════"
echo "Current Configuration:"
echo "════════════════════════════════════════════════════════════"
echo "Tenant ID:     $CURRENT_TENANT"
echo "Client ID:     $CURRENT_CLIENT"
echo "Client Secret: ************************************"
echo ""

# Prompt for new credentials
echo "════════════════════════════════════════════════════════════"
echo "Enter New Credentials:"
echo "════════════════════════════════════════════════════════════"
echo ""

echo -e "${BLUE}📋 Tenant ID:${NC}"
echo "   (From Azure Portal → App Registration → Overview → Directory (tenant) ID)"
read -p "   Enter Tenant ID [press Enter to keep current]: " TENANT_ID
TENANT_ID=${TENANT_ID:-$CURRENT_TENANT}

echo ""
echo -e "${BLUE}📋 Client ID:${NC}"
echo "   (From Azure Portal → App Registration → Overview → Application (client) ID)"
read -p "   Enter Client ID [press Enter to keep current]: " CLIENT_ID
CLIENT_ID=${CLIENT_ID:-$CURRENT_CLIENT}

echo ""
echo -e "${BLUE}🔐 Client Secret:${NC}"
echo "   (From Azure Portal → App Registration → Certificates & secrets → Client secrets)"
echo "   ${YELLOW}IMPORTANT:${NC} Copy the SECRET VALUE (not the Secret ID!)"
read -sp "   Enter Client Secret: " CLIENT_SECRET
echo ""

if [ -z "$CLIENT_SECRET" ]; then
    echo ""
    echo -e "${RED}Error: Client Secret cannot be empty!${NC}"
    exit 1
fi

# Confirm before saving
echo ""
echo "════════════════════════════════════════════════════════════"
echo "Review New Configuration:"
echo "════════════════════════════════════════════════════════════"
echo "Tenant ID:     $TENANT_ID"
echo "Client ID:     $CLIENT_ID"
echo "Client Secret: ${CLIENT_SECRET:0:8}********************************"
echo ""

read -p "Save these credentials? (yes/no): " confirm

if [[ ! "$confirm" =~ ^[Yy][Ee][Ss]$ ]]; then
    echo -e "\n${YELLOW}Configuration cancelled.${NC}\n"
    exit 0
fi

# Create/update .env file
ENV_FILE="backend/.env"

echo ""
echo "Creating $ENV_FILE..."

cat > "$ENV_FILE" << EOF
# Azure AD Configuration for User Authentication
AZURE_TENANT_ID=$TENANT_ID
AZURE_CLIENT_ID=$CLIENT_ID
AZURE_CLIENT_SECRET=$CLIENT_SECRET

# Azure AD Configuration for Dynamics 365 API Access
# Using same credentials as above (can be different app if needed)
D365_TENANT_ID=$TENANT_ID
D365_CLIENT_ID=$CLIENT_ID
D365_CLIENT_SECRET=$CLIENT_SECRET

# JWT Secret for token signing (change in production)
JWT_SECRET=your-very-long-secret-key-min-256-bits-for-hs256-algorithm-please-change-this-in-production-environment

# Dynamics 365 Configuration (already correct)
D365_BASE_URL=https://bintarasolutions.crm5.dynamics.com/api/data/v9.2
D365_SCOPE=https://bintarasolutions.crm5.dynamics.com/.default

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
EOF

echo -e "${GREEN}✓${NC} Configuration saved to $ENV_FILE"

# Update frontend environment if needed
FRONTEND_ENV="frontend/src/environments/environment.ts"
if [ -f "$FRONTEND_ENV" ]; then
    echo -e "${BLUE}ℹ${NC}  Frontend environment file found."
    echo "   Note: Frontend uses backend API for authentication."
fi

echo ""
echo "════════════════════════════════════════════════════════════"
echo -e "${GREEN}✓ Configuration Complete!${NC}"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Next Steps:"
echo ""
echo "  1. ${YELLOW}Verify Azure AD Permissions:${NC}"
echo "     • Open: https://portal.azure.com"
echo "     • Go to: App registrations → Your App → API permissions"
echo "     • Ensure: Dynamics CRM permission is granted"
echo ""
echo "  2. ${YELLOW}Create Application User in Power Platform:${NC}"
echo "     • Open: https://admin.powerplatform.microsoft.com"
echo "     • Go to: Environments → BINTARA CRM → Settings"
echo "     • Navigate: Users + permissions → Application users"
echo "     • Click: + New app user"
echo "     • Add your app with Client ID: $CLIENT_ID"
echo "     • Assign: System Administrator role"
echo ""
echo "  3. ${YELLOW}Restart Backend:${NC}"
echo "     • Run: ./start-backend.sh"
echo ""
echo "  4. ${YELLOW}Test:${NC}"
echo "     • Open: http://localhost:4200"
echo "     • Login and check if data loads"
echo ""
echo "For detailed steps, see:"
echo "  • CREATE_NEW_AZURE_APP.md (Steps 1-9)"
echo "  • ACTION_PLAN_FIX_403.md (Complete checklist)"
echo ""
EOF

chmod +x configure-azure-credentials.sh

echo "✅ Created script: configure-azure-credentials.sh"
