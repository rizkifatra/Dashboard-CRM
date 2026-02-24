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
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

# Wait a moment for backend to initialize
sleep 5

# Check if frontend directory exists
if [ ! -d "frontend" ]; then
    echo "✗ Error: frontend directory not found"
    kill $BACKEND_PID
    exit 1
fi

# Start the frontend server
echo "Starting frontend server..."
cd frontend
ng serve &
FRONTEND_PID=$!

# Wait for both processes
echo "✓ Both servers started successfully"
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "Press Ctrl+C to stop both servers"

# Trap to cleanup on exit
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" SIGINT SIGTERM

wait
