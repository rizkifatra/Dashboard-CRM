#!/bin/bash

# Exit on error
set -e

# Load environment variables from .env file
if [ -f backend/.env ]; then
    set -a
    source backend/.env
    set +a
    echo "✓ Environment variables loaded"
else
    echo "✗ Error: backend/.env file not found"
    exit 1
fi

# Check if backend directory exists
if [ ! -d "backend" ]; then
    echo "✗ Error: backend directory not found"
    exit 1
fi

# Start the backend server in background
echo "Starting backend server..."
cd backend
mvn spring-boot:run 



