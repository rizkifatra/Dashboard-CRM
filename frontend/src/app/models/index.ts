/**
 * Common API Response interface used across all services
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: string;
}

/**
 * Account entity from Dynamics 365 CRM
 */
export interface Account {
  accountId: string;
  name: string;
  accountNumber: string;
  emailAddress: string;
  telephone: string;
  websiteUrl: string;
  city: string;
  state: string;
  country: string;
  revenue: number;
  numberOfEmployees: number;
  industryCode: number;
  description: string;
  createdOn: string;
  modifiedOn: string;
  ownerId: string;
}

/**
 * Activity entity from Dynamics 365 CRM
 */
export interface Activity {
  activityId: string;
  subject: string;
  description: string;
  activityType: string;
  activityTypeCode: string; // e.g., 'email', 'phonecall', 'appointment', 'task'
  direction: string;
  createdOn: string;
  modifiedOn: string;
  staffName: string;
  staffEmail: string;
  staffTitle: string;
  owningUserId?: string;
  // Email-specific fields (populated via expanded queries)
  sender?: string;
  fromEmail?: string;
  toEmail?: string;
  ccEmail?: string;
  accountName?: string; // Account/Client name this activity is related to
  regardingObjectName?: string; // Related account or contact name
  regardingObjectId?: string;
  actualStart?: string;
  actualEnd?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  priorityCode?: number;
  stateCode: number; // 0=Open, 1=Completed, 2=Cancelled
  statusCode?: number;
}

/**
 * Email Reminder for tracking emails that need follow-up
 */
export interface EmailReminder {
  activityId: string;
  subject: string;
  toEmail: string;
  accountId?: string;
  accountName?: string;
  staffName: string;
  staffEmail?: string;
  sentDate: string;
  daysOverdue: number;
  urgencyLevel: string; // 'low', 'medium', 'critical'
  urgencyColor: string; // 'yellow', 'orange', 'red'
  urgencyBadge: string; // '3 Days', '7 Days', '14+ Days'
}

/**
 * Email Reminder Counts
 */
export interface EmailReminderCounts {
  total: number;
  critical: number;
  medium: number;
  low: number;
}

/**
 * Staff (System User) entity from Dynamics 365 CRM
 */
export interface Staff {
  systemUserId: string;
  fullName: string;
  email: string;
  title: string;
  totalEmailCount: number;
  incomingEmailCount: number;
  outgoingEmailCount: number;
  respondedEmailCount: number;
  averageResponseTimeMinutes: number;
}

/**
 * Dashboard metrics summary
 */
export interface DashboardMetrics {
  totalActivities: number;
  activeOpportunities: number;
  totalEmailsSent: number;
  averageResponseRate: number;
  totalStaff: number;
  dateRange: {
    from: string;
    to: string;
  };
}

/**
 * Top performer statistics
 */
export interface TopPerformer {
  staffId: string;
  fullname: string;
  title: string;
  email: string;
  totalEmails: number;
  incomingEmails: number;
  outgoingEmails: number;
  responseRate: number;
  averageResponseTimeMinutes: number | null;
  performanceScore: number;
}

/**
 * Email performance statistics
 */
export interface EmailPerformance {
  staffName: string;
  incomingEmails: number;
  outgoingEmails: number;
  totalEmails: number;
}

/**
 * Date range filter
 */
export interface DateRange {
  from: string;
  to: string;
}

/**
 * Email statistics for a staff member
 */
export interface EmailStats {
  staffEmail: string;
  incomingEmails: number;
  outgoingEmails: number;
  totalEmails: number;
}

/**
 * Health status response
 */
export interface HealthStatus {
  status: string;
  application: string;
  timestamp: number;
  configured: boolean;
  warning?: string;
}

/**
 * D365 connection status
 */
export interface D365ConnectionStatus {
  baseUrl: string;
  timestamp: number;
  status: string;
  authenticationStatus?: string;
  connectionStatus?: string;
  message?: string;
  error?: string;
}

/**
 * System configuration information
 */
export interface SystemConfig {
  d365BaseUrl: string;
  d365Scope: string;
  azureTenantId: string;
  azureClientIdConfigured: boolean;
  azureClientSecretConfigured: boolean;
  fullyConfigured: boolean;
}

/**
 * Revenue metrics for dashboard
 */
export interface RevenueMetrics {
  estimatedRevenue: number;
  wonRevenue: number;
  inProgressRevenue: number;
  wonCount: number;
  lostCount: number;
  openCount: number;
  averageDealSize: number;
  winRate: number;
  dateRange: {
    from: string;
    to: string;
  };
}

/**
 * Monthly revenue data
 */
export interface MonthlyRevenue {
  month: string;
  year: number;
  monthLabel: string;
  estimatedRevenue: number;
  wonRevenue: number;
  wonCount: number;
  openCount: number;
  lostCount: number;
}

/**
 * Unreplied email with reminder metadata
 */
export interface UnrepliedEmail {
  activityId: string;
  subject: string;
  fromEmail: string;
  sender: string;
  description: string;
  assignedTo: string; // Staff email
  assignedToName: string; // Staff name
  createdOn: string;
  modifiedOn: string;

  // Reminder metadata
  hoursUnreplied: number; // How many hours since received
  urgencyLevel: 'low' | 'medium' | 'high' | 'critical';
  ageCategory: '< 24h' | '24-48h' | '48-72h' | '> 72h';
  isOverdue: boolean; // Over 48 hours

  // Related account info
  regardingObjectId: string;
  regardingObjectName: string;
  regardingObjectTypeCode: string;
}
