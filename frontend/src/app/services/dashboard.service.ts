import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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

export interface EmailPerformance {
  staffName: string;
  incomingEmails: number;
  outgoingEmails: number;
  totalEmails: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: string;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  /**
   * Get dashboard metrics (4 metric cards)
   */
  getMetrics(
    fromDate?: string,
    toDate?: string
  ): Observable<ApiResponse<DashboardMetrics>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<DashboardMetrics>>(
      `${this.apiUrl}/dashboard/metrics`,
      { params }
    );
  }

  /**
   * Get top performing staff members
   */
  getTopPerformers(
    top: number = 10,
    fromDate?: string,
    toDate?: string
  ): Observable<ApiResponse<TopPerformer[]>> {
    let params = new HttpParams().set('top', top.toString());
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<TopPerformer[]>>(
      `${this.apiUrl}/dashboard/top-performers`,
      { params }
    );
  }

  /**
   * Get email performance by staff (for chart)
   */
  getEmailPerformance(
    fromDate?: string,
    toDate?: string
  ): Observable<ApiResponse<EmailPerformance[]>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<EmailPerformance[]>>(
      `${this.apiUrl}/dashboard/email-performance`,
      { params }
    );
  }
}
