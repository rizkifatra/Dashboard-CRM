import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Activity,
  ApiResponse,
  UnrepliedEmail,
  EmailReminder,
  EmailReminderCounts,
} from '../models';

export interface EmailStats {
  staffEmail: string;
  incomingEmails: number;
  outgoingEmails: number;
  totalEmails: number;
}

export type { Activity, ApiResponse, UnrepliedEmail };

@Injectable({
  providedIn: 'root',
})
export class ActivityService {
  private apiUrl = 'http://localhost:8080/api/activities';

  constructor(private http: HttpClient) {}

  /**
   * Get all activities with optional filtering
   * @param top Maximum number of activities to return
   * @param skip Number of activities to skip (for pagination)
   * @param filter Optional OData filter
   */
  getAllActivities(
    top: number = 100,
    skip: number = 0,
    filter?: string,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams()
      .set('top', top.toString())
      .set('skip', skip.toString());
    if (filter) params = params.set('filter', filter);

    return this.http.get<ApiResponse<Activity[]>>(this.apiUrl, { params });
  }

  /**
   * Get activity by ID
   * @param id The activity ID
   */
  getActivityById(id: string): Observable<ApiResponse<Activity>> {
    return this.http.get<ApiResponse<Activity>>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get activities by staff email
   * @param email Staff member's email
   * @param top Maximum number of activities to return
   */
  getActivitiesByStaff(
    email: string,
    top: number = 50,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/staff/${email}`,
      { params },
    );
  }

  /**
   * Get activities by account
   * @param accountId Account ID
   * @param top Maximum number of activities to return
   */
  getActivitiesByAccount(
    accountId: string,
    top: number = 50,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/account/${accountId}`,
      { params },
    );
  }

  /**
   * Get email activities only
   * @param top Maximum number of activities to return
   * @param skip Number of activities to skip (for pagination)
   */
  getEmailActivities(
    top: number = 50,
    skip: number = 0,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams()
      .set('top', top.toString())
      .set('skip', skip.toString());

    return this.http.get<ApiResponse<Activity[]>>(`${this.apiUrl}/emails`, {
      params,
    });
  }

  /**
   * Get sent emails by staff
   * @param email Staff member's email
   * @param top Maximum number of activities to return
   */
  getSentEmailsByStaff(
    email: string,
    top: number = 50,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/emails/sent/${email}`,
      { params },
    );
  }

  /**
   * Get received emails by staff
   * @param email Staff member's email
   * @param top Maximum number of activities to return
   */
  getReceivedEmailsByStaff(
    email: string,
    top: number = 50,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/emails/received/${email}`,
      { params },
    );
  }

  /**
   * Get email statistics by staff
   * @param email Staff member's email
   */
  getEmailStatsByStaff(email: string): Observable<ApiResponse<EmailStats>> {
    return this.http.get<ApiResponse<EmailStats>>(
      `${this.apiUrl}/emails/stats/${email}`,
    );
  }

  /**
   * Get activity count
   * @param filter Optional OData filter
   */
  getActivityCount(filter?: string): Observable<ApiResponse<number>> {
    let params = new HttpParams();
    if (filter) params = params.set('filter', filter);

    return this.http.get<ApiResponse<number>>(`${this.apiUrl}/count`, {
      params,
    });
  }

  /**
   * Get recent activities across all staff
   * @param top Maximum number of activities to return
   * @param fromDate Optional start date filter
   * @param toDate Optional end date filter
   */
  getRecentActivities(
    top: number = 50,
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Activity[]>>(`${this.apiUrl}/recent`, {
      params,
    });
  }

  /**
   * Get initials from staff name
   * @param name Staff full name
   */
  getInitials(name: string): string {
    if (!name) return '??';
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  /**
   * Format relative time (e.g., "2 hours ago")
   * @param timestamp ISO date string
   */
  getRelativeTime(timestamp: string): string {
    if (!timestamp) return 'N/A';

    const now = new Date();
    const past = new Date(timestamp);
    const diffMs = now.getTime() - past.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays < 7) return `${diffDays} days ago`;

    return past.toLocaleDateString();
  }

  /**
   * Get unreplied incoming emails for reminder system
   * @param maxHoursOld Maximum age of emails to check (null = all time, undefined = use default)
   */
  getUnrepliedEmails(
    maxHoursOld?: number | null,
  ): Observable<ApiResponse<UnrepliedEmail[]>> {
    let params = new HttpParams();

    // Only add maxHoursOld parameter if it's a number (not null or undefined)
    if (maxHoursOld !== null && maxHoursOld !== undefined) {
      params = params.set('maxHoursOld', maxHoursOld.toString());
    }

    return this.http.get<ApiResponse<UnrepliedEmail[]>>(
      `${this.apiUrl}/unreplied`,
      { params },
    );
  }

  /**
   * Get email follow-up reminders
   */
  getEmailReminders(): Observable<ApiResponse<EmailReminder[]>> {
    return this.http.get<ApiResponse<EmailReminder[]>>(
      'http://localhost:8080/api/email-reminders',
    );
  }

  /**
   * Get email reminder counts by urgency
   */
  getEmailReminderCounts(): Observable<ApiResponse<EmailReminderCounts>> {
    return this.http.get<ApiResponse<EmailReminderCounts>>(
      'http://localhost:8080/api/email-reminders/counts',
    );
  }

  /**
   * Get urgency color based on urgency level
   */
  getUrgencyColor(level: string): string {
    switch (level) {
      case 'low':
        return '#10b981'; // green
      case 'medium':
        return '#f59e0b'; // amber
      case 'high':
        return '#ef4444'; // red
      case 'critical':
        return '#dc2626'; // dark red
      default:
        return '#6b7280'; // gray
    }
  }
}
