#!/bin/bash

# Test Dynamics 365 API Access with Azure AD App
# This tests if the application can get a token and access D365 API

# Load environment variables
if [ -f backend/.env ]; then
    export $(cat backend/.env | grep -v '^#' | xargs)
else
    echo "❌ Error: backend/.env not found"
    exit 1
fi

echo "========================================="
echo "Testing Dynamics 365 API Access"
echo "========================================="
echo ""

echo "Configuration:"
echo "  Tenant ID: $D365_TENANT_ID"
echo "  Client ID: $D365_CLIENT_ID"
echo "  D365 URL:  $D365_BASE_URL"
echo "  Scope:     $D365_SCOPE"
echo ""

# Step 1: Get Access Token
echo "Step 1: Requesting access token from Azure AD..."
echo "--------------------------------------------"

TOKEN_RESPONSE=$(curl -s -X POST \
  "https://login.microsoftonline.com/$D365_TENANT_ID/oauth2/v2.0/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$D365_CLIENT_ID" \
  -d "client_secret=$D365_CLIENT_SECRET" \
  -d "scope=$D365_SCOPE" \
  -d "grant_type=client_credentials")

# Check if we got an error
if echo "$TOKEN_RESPONSE" | grep -q "error"; then
    echo "❌ Failed to get access token!"
    echo ""
    echo "Error Response:"
    echo "$TOKEN_RESPONSE" | jq '.'
    echo ""
    echo "Common causes:"
    echo "  1. Invalid client ID or client secret"
    echo "  2. App not registered in Azure AD tenant"
    echo "  3. API permissions not configured"
    echo "  4. Admin consent not granted"
    exit 1
fi

# Extract token
ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    echo "❌ No access token in response"
    echo "$TOKEN_RESPONSE" | jq '.'
    exit 1
fi

echo "✅ Access token obtained successfully!"
echo "   Token length: ${#ACCESS_TOKEN} characters"
echo ""

# Step 2: Test D365 API Access
echo "Step 2: Testing D365 API access..."
echo "--------------------------------------------"

D365_RESPONSE=$(curl -s -X GET \
  "$D365_BASE_URL/emails?\$select=activityid,subject,createdon&\$top=5" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/json" \
  -H "OData-MaxVersion: 4.0" \
  -H "OData-Version: 4.0")

# Check for errors
if echo "$D365_RESPONSE" | grep -q "error"; then
    echo "❌ D365 API returned an error!"
    echo ""
    echo "Error Response:"
    echo "$D365_RESPONSE" | jq '.'
    echo ""
    echo "Common causes:"
    echo "  1. Application User not created in Dynamics 365"
    echo "  2. Application User doesn't have proper security role"
    echo "  3. API permissions not granted"
    exit 1
fi

# Check if we got data
EMAIL_COUNT=$(echo "$D365_RESPONSE" | jq '.value | length')

if [ -z "$EMAIL_COUNT" ] || [ "$EMAIL_COUNT" = "null" ]; then
    echo "❌ Invalid response from D365 API"
    echo "$D365_RESPONSE" | head -20
    exit 1
fi

echo "✅ D365 API access successful!"
echo "   Retrieved $EMAIL_COUNT emails"
echo ""

# Show sample of retrieved emails
if [ "$EMAIL_COUNT" -gt 0 ]; then
    echo "Sample emails retrieved:"
    echo "$D365_RESPONSE" | jq '.value[] | {subject: .subject, created: .createdon}'
fi

echo ""
echo "========================================="
echo "✅ All tests PASSED!"
echo "========================================="
echo ""
echo "Your D365 API access is working correctly."
echo "The issue with empty email detection must be in the business logic."
