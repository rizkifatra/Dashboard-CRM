# CRM Dashboard - System Documentation

## Overview

CRM Dashboard is a web application for managing customer relationships with Microsoft Dynamics 365 integration.

---

## System Components

| Component    | Technology            | Purpose             |
| ------------ | --------------------- | ------------------- |
| **Frontend** | Angular 18            | User interface      |
| **Backend**  | Spring Boot (Java 17) | REST API server     |
| **Database** | Dynamics 365          | CRM data storage    |
| **Auth**     | Azure AD (OAuth2)     | User authentication |

---

## Project Structure

```
Dashboard-CRM/
├── frontend/           # Angular application
│   ├── src/app/        # Components, services, models
│   ├── Dockerfile      # Container config
│   └── nginx.conf      # Web server config
│
├── backend/            # Spring Boot application
│   ├── src/main/java/  # Java source code
│   ├── src/main/resources/  # Config files
│   ├── Dockerfile      # Container config
│   └── pom.xml         # Maven dependencies
│
├── docker-compose.yml  # Multi-container setup
├── .env.example        # Environment template
└── document/           # Documentation
```

---

## Features

### Dashboard

- Fiscal year overview
- Revenue charts
- Opportunity pipeline

### Accounts

- Customer list
- Account details
- Industry breakdown

### Opportunities

- Sales pipeline
- Deal tracking
- Win/loss analytics

### Activities

- Task management
- Activity timeline
- Follow-up tracking

### Staff

- Team member list
- Performance metrics

---

## Authentication Flow

```
1. User clicks "Login"
2. Redirect to Azure AD
3. User enters Microsoft credentials
4. Azure AD returns token
5. Backend validates token
6. User accesses dashboard
```

---

## API Endpoints

| Method | Endpoint                        | Description        |
| ------ | ------------------------------- | ------------------ |
| GET    | `/api/health`                   | Health check       |
| GET    | `/api/dashboard/fiscal-summary` | Dashboard data     |
| GET    | `/api/accounts`                 | List accounts      |
| GET    | `/api/opportunities`            | List opportunities |
| GET    | `/api/activities`               | List activities    |
| GET    | `/api/staff`                    | List staff         |

---

## Configuration Files

### Backend

| File                          | Purpose            |
| ----------------------------- | ------------------ |
| `application.properties`      | Default settings   |
| `application-dev.properties`  | Development config |
| `application-prod.properties` | Production config  |

### Frontend

| File                  | Purpose              |
| --------------------- | -------------------- |
| `environment.ts`      | Development settings |
| `environment.prod.ts` | Production settings  |

---

## Running the Application

### Development Mode

**Backend:**

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install
npm start
```

### Docker Mode

```bash
docker-compose up --build
```

---

## Ports

| Service           | Port |
| ----------------- | ---- |
| Frontend (dev)    | 4200 |
| Frontend (docker) | 80   |
| Backend           | 8080 |

---

## Dependencies

### Backend (Maven)

- Spring Boot 3.x
- Spring Security OAuth2
- Microsoft MSAL
- Lombok

### Frontend (npm)

- Angular 18
- Angular Material
- Chart.js
- RxJS

---

## Security

- OAuth2 authentication via Azure AD
- JWT tokens for API authorization
- HTTPS in production
- CORS restrictions
- Non-root container users

---

## Environment Variables

See [.env.example](../.env.example) for required variables.

---

## Support

For issues:

1. Check logs: `docker-compose logs -f`
2. Verify environment variables
3. Review [Troubleshooting Guide](SIMPLE_DEPLOYMENT_GUIDE.md#common-issues)
