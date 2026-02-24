#!/bin/bash

# Test Email Reminders endpoint with authentication

if [ -z "$1" ]; then
    echo "Usage: ./test-email-reminders.sh <JWT_TOKEN>"
    echo ""
    echo "Example:"
    echo "  ./test-email-reminders.sh eyJhbGciOiJIUzUxMiJ9..."
    exit 1
fi

JWT_TOKEN="$1"

echo "========================================="
echo "Testing Email Reminders API"
echo "========================================="
echo ""

echo "Step 1: Testing /api/email-reminders endpoint..."
echo "--------------------------------------------"

RESPONSE=$(curl -s -w "\n%{http_code}" \
  "http://localhost:8080/api/email-reminders" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Accept: application/json")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "HTTP Status: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Request failed with status $HTTP_CODE"
    echo ""
    echo "Response:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    exit 1
fi

echo "✅ Request successful (HTTP 200)"
echo ""

# Parse response
SUCCESS=$(echo "$BODY" | jq -r '.success')
MESSAGE=$(echo "$BODY" | jq -r '.message')
DATA_COUNT=$(echo "$BODY" | jq '.data | length')
ERROR=$(echo "$BODY" | jq -r '.error')

echo "Response Details:"
echo "  Success: $SUCCESS"
echo "  Message: $MESSAGE"
echo "  Data Count: $DATA_COUNT items"
echo "  Error: $ERROR"
echo ""

if [ "$DATA_COUNT" -gt 0 ]; then
    echo "✅ Found $DATA_COUNT email reminders!"
    echo ""
    echo "Sample reminders:"
    echo "$BODY" | jq '.data[0:3] | .[] | {subject: .subject, to: .toEmail, daysOverdue: .daysOverdue, urgency: .urgencyLevel}'
else
    echo "⚠️  No email reminders found (empty data array)"
    echo ""
    echo "This could mean:"
    echo "  1. All outgoing emails have been replied to"
    echo "  2. No outgoing emails older than 3 days"
    echo "  3. Date filter might need adjustment"
    echo "  4. Check backend logs for filtering details"
fi

echo ""
echo "Step 2: Testing /api/email-reminders/counts endpoint..."
echo "--------------------------------------------"

COUNTS_RESPONSE=$(curl -s \
  "http://localhost:8080/api/email-reminders/counts" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Accept: application/json")

echo "Counts Response:"
echo "$COUNTS_RESPONSE" | jq '.'

echo ""
echo "========================================="
echo "Backend Logs (Last 20 lines):"
echo "========================================="
tail -20 /tmp/backend-latest.log | grep -E "(email reminder|Fetching emails|Found.*emails|outgoing|filtering)"

echo ""
echo "========================================="
echo "Test Complete"
echo "========================================="
