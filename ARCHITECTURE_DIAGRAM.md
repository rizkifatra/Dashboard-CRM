# Service Architecture Diagram

## 📊 Complete System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                          │
│                    (Browser - Port 4200)                        │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ANGULAR COMPONENTS LAYER                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Dashboard   │  │    Staff     │  │  Activities  │         │
│  │  Component   │  │  Component   │  │  Component   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│  ┌──────────────┐                                              │
│  │   Accounts   │                                              │
│  │  Component   │                                              │
│  └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                     SERVICES LAYER (NEW!)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Dashboard   │  │    Staff     │  │  Activity    │         │
│  │   Service    │  │   Service    │  │   Service    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │   Account    │  │  DateUtils   │                           │
│  │   Service    │  │   Service    │                           │
│  └──────────────┘  └──────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    MODELS LAYER (NEW!)                          │
│  ┌────────────────────────────────────────────────────────┐   │
│  │  TypeScript Interfaces: Account, Activity, Staff,      │   │
│  │  DashboardMetrics, ApiResponse, etc.                   │   │
│  └────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                      HTTP CLIENT LAYER                          │
│                    (Angular HttpClient)                         │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT REST API                         │
│                      (Port 8080)                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Dashboard   │  │    Staff     │  │  Activity    │         │
│  │ Controller   │  │ Controller   │  │ Controller   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│  ┌──────────────┐                                              │
│  │   Account    │                                              │
│  │ Controller   │                                              │
│  └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    D365 SERVICE LAYER                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ D365Dashboard│  │  D365Staff   │  │ D365Activity │         │
│  │   Service    │  │   Service    │  │   Service    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │ D365Account  │  │D365Opportunity│                           │
│  │   Service    │  │   Service    │                           │
│  └──────────────┘  └──────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│              MICROSOFT DYNAMICS 365 CRM                         │
│                  (OData v4 API - Cloud)                         │
│  ┌────────────────────────────────────────────────────────┐   │
│  │  Entities: accounts, systemusers, activitypointer,     │   │
│  │  emails, opportunities, contacts, leads                │   │
│  └────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow Example: Loading Accounts

```
1. User navigates to /accounts
        ↓
2. AccountsComponent ngOnInit() is called
        ↓
3. Component calls: accountService.getAccounts(50)
        ↓
4. AccountService constructs HTTP request:
   GET http://localhost:8080/api/accounts?top=50
        ↓
5. Angular HttpClient sends request
        ↓
6. Spring Boot AccountController receives request
        ↓
7. AccountController calls D365AccountService
        ↓
8. D365AccountService queries Dynamics 365 API
   GET https://[org].api.crm.dynamics.com/api/data/v9.2/accounts
        ↓
9. Dynamics 365 returns account data
        ↓
10. D365AccountService processes and returns data
        ↓
11. AccountController wraps in ApiResponse and returns
        ↓
12. HttpClient receives response
        ↓
13. AccountService emits Observable
        ↓
14. Component subscribes and updates:
    - this.accounts = response.data
    - this.loading = false
        ↓
15. Template re-renders with account data
        ↓
16. User sees account cards on screen
```

---

## 📦 Service Dependencies

### **AccountsComponent Dependencies**

```
AccountsComponent
    ↓ injects
AccountService
    ↓ uses
HttpClient
    ↓ calls
Backend API
```

### **StaffComponent Dependencies**

```
StaffComponent
    ↓ injects
StaffService + DateUtilsService
    ↓ uses
HttpClient
    ↓ calls
Backend API
```

### **ActivitiesComponent Dependencies**

```
ActivitiesComponent
    ↓ injects
ActivityService + DateUtilsService
    ↓ uses
HttpClient
    ↓ calls
Backend API
```

### **DashboardComponent Dependencies**

```
DashboardComponent
    ↓ injects
DashboardService + ActivityService
    ↓ uses
HttpClient
    ↓ calls
Backend API
```

---

## 🎯 Service Responsibilities

### **Frontend Services**

```
┌─────────────────────────────────────────────────┐
│          DashboardService                       │
│  • getMetrics()                                 │
│  • getTopPerformers()                           │
│  • getEmailPerformanceData()                    │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│          StaffService                           │
│  • getStaff()                                   │
│  • getStaffById()                               │
│  • formatResponseTime()                         │
│  • calculateResponseRate()                      │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│          ActivityService                        │
│  • getRecentActivities()                        │
│  • getRelativeTime()                            │
│  • getInitials()                                │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│          AccountService                         │
│  • getAccounts()                                │
│  • getAccountCount()                            │
│  • formatRevenue()                              │
│  • formatDate()                                 │
│  • getInitials()                                │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│          DateUtilsService                       │
│  • getCurrentMonthStart()                       │
│  • getCurrentMonthEnd()                         │
│  • getDaysAgo()                                 │
│  • formatDateToISO()                            │
│  • formatDate()                                 │
└─────────────────────────────────────────────────┘
```

### **Backend Services**

```
┌─────────────────────────────────────────────────┐
│       D365DashboardService                      │
│  • getMetrics()                                 │
│  • getTopPerformers()                           │
│  • getEmailPerformance()                        │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│       D365StaffService                          │
│  • getAllStaff()                                │
│  • getStaffWithEmailStats()                     │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│       D365ActivityService                       │
│  • getRecentActivities()                        │
│  • getActivitiesByStaff()                       │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│       D365AccountService                        │
│  • getAllAccounts()                             │
│  • getAccountById()                             │
│  • getAccountCount()                            │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│       D365OpportunityService                    │
│  • getAllOpportunities()                        │
│  • getActiveOpportunityCount()                  │
└─────────────────────────────────────────────────┘
```

---

## 🔐 Authentication Flow

```
User Login
    ↓
Azure AD Authentication
    ↓
Access Token Generated
    ↓
Token stored in Spring Boot Config
    ↓
Every API call to D365 includes:
    Authorization: Bearer {token}
    ↓
Dynamics 365 validates token
    ↓
Returns data if authorized
```

---

## 📊 Date Filtering Flow

```
Component initializes
    ↓
Calls: dateUtils.getCurrentMonthStart()
    ↓
Returns: "2025-12-01"
    ↓
Calls: dateUtils.getCurrentMonthEnd()
    ↓
Returns: "2025-12-31"
    ↓
Service call with dates:
service.getData(fromDate, toDate)
    ↓
HTTP request:
GET /api/endpoint?fromDate=2025-12-01&toDate=2025-12-31
    ↓
Backend applies OData filter:
$filter=createdOn ge 2025-12-01 and createdOn le 2025-12-31
    ↓
Returns filtered data
```

---

## 🎨 Component-Service Mapping

| Component               | Primary Services                  | Purpose                         |
| ----------------------- | --------------------------------- | ------------------------------- |
| **DashboardComponent**  | DashboardService, ActivityService | Metrics, charts, top performers |
| **StaffComponent**      | StaffService, DateUtilsService    | Staff list with email stats     |
| **ActivitiesComponent** | ActivityService, DateUtilsService | Recent activities timeline      |
| **AccountsComponent**   | AccountService                    | Account management              |

---

## 📈 Performance Optimization

### **Before Services Implementation**

```
Component
    ↓ Direct HTTP call
HttpClient
    ↓
Backend API

Issues:
❌ Duplicate HTTP logic in every component
❌ No caching possible
❌ Hard to test
❌ Tight coupling
```

### **After Services Implementation**

```
Component
    ↓ Service call
Service (Business Logic)
    ↓ HTTP call
HttpClient
    ↓
Backend API

Benefits:
✅ Centralized HTTP logic
✅ Caching possible at service level
✅ Easy to mock for testing
✅ Loose coupling
✅ Reusable utilities
```

---

## 🧪 Testing Strategy

### **Unit Testing Components**

```typescript
// Mock the service
const mockAccountService = {
  getAccounts: jasmine.createSpy().and.returnValue(
    of({
      success: true,
      data: mockAccounts,
    })
  ),
};

// Inject mock
TestBed.configureTestingModule({
  providers: [{ provide: AccountService, useValue: mockAccountService }],
});
```

### **Unit Testing Services**

```typescript
// Mock HttpClient
const mockHttp = {
  get: jasmine.createSpy().and.returnValue(of(mockResponse)),
};

// Test service
TestBed.configureTestingModule({
  providers: [AccountService, { provide: HttpClient, useValue: mockHttp }],
});
```

---

## 🚀 Deployment Architecture

```
Development:
    Frontend: http://localhost:4200
    Backend: http://localhost:8080
    ↓
Production:
    Frontend: Azure Static Web Apps / Netlify
    Backend: Azure App Service / AWS
    Database: Dynamics 365 Cloud
```

---

**Last Updated:** December 11, 2025  
**Architecture Version:** 2.0 (Service-Based)
