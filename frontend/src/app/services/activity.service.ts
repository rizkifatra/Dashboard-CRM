import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Activity, ApiResponse } from '../models';

export interface EmailStats {
  staffEmail: string;
  incomingEmails: number;
  outgoingEmails: number;
  totalEmails: number;
}

export type { Activity, ApiResponse };

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
    top: number = 50,
    skip: number = 0,
    filter?: string
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
    top: number = 50
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/staff/${email}`,
      { params }
    );
  }

  /**
   * Get activities by account
   * @param accountId Account ID
   * @param top Maximum number of activities to return
   */
  getActivitiesByAccount(
    accountId: string,
    top: number = 50
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/account/${accountId}`,
      { params }
    );
  }

  /**
   * Get email activities only
   * @param top Maximum number of activities to return
   * @param skip Number of activities to skip (for pagination)
   */
  getEmailActivities(
    top: number = 50,
    skip: number = 0
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
    top: number = 50
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/emails/sent/${email}`,
      { params }
    );
  }

  /**
   * Get received emails by staff
   * @param email Staff member's email
   * @param top Maximum number of activities to return
   */
  getReceivedEmailsByStaff(
    email: string,
    top: number = 50
  ): Observable<ApiResponse<Activity[]>> {
    let params = new HttpParams().set('top', top.toString());

    return this.http.get<ApiResponse<Activity[]>>(
      `${this.apiUrl}/emails/received/${email}`,
      { params }
    );
  }

  /**
   * Get email statistics by staff
   * @param email Staff member's email
   */
  getEmailStatsByStaff(email: string): Observable<ApiResponse<EmailStats>> {
    return this.http.get<ApiResponse<EmailStats>>(
      `${this.apiUrl}/emails/stats/${email}`
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
    toDate?: string
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
}
