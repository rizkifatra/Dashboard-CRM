# Multi-stage build for Angular application

# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json* ./
RUN npm ci

# Copy source code
COPY . .

# Build for production
RUN npm run build -- --configuration production

# Production stage - Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built Angular app from build stage
COPY --from=build /app/dist/frontend/browser .

# Copy custom nginx configuration as template
COPY nginx.conf /tmp/nginx.conf.template

# Set default backend URL (can be overridden at runtime)
ENV BACKEND_URL=backend:8080
# Backend host for Host header (without port)
ENV BACKEND_HOST=backend
# DNS resolver: 127.0.0.11 for Docker, 168.63.129.16 for Azure
ENV DNS_RESOLVER=127.0.0.11
# Forwarded protocol: http for local, https for Azure (SSL termination)
ENV FORWARDED_PROTO=http

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# Use sed to replace placeholders, then start nginx
CMD sed -e "s|\${BACKEND_URL}|${BACKEND_URL}|g" -e "s|\${BACKEND_HOST}|${BACKEND_HOST}|g" -e "s|\${DNS_RESOLVER}|${DNS_RESOLVER}|g" -e "s|\${FORWARDED_PROTO}|${FORWARDED_PROTO}|g" /tmp/nginx.conf.template > /etc/nginx/conf.d/default.conf && nginx -g "daemon off;"
