import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  totalConversations: number;
  averageEmailsPerConversation: number;
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
export class StaffService {
  private apiUrl = `${environment.apiUrl}/staff`;

  constructor(private http: HttpClient) {}

  /**
   * Get all staff (UNFILTERED - shows all staff regardless of job title)
   * Use this for Staff Management page
   * @param includeEmailStats Whether to include email statistics
   * @param fromDate Optional start date filter
   * @param toDate Optional end date filter
   */
  getAllStaff(
    includeEmailStats: boolean = true,
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Staff[]>> {
    let params = new HttpParams().set(
      'includeEmailStats',
      includeEmailStats.toString(),
    );
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Staff[]>>(`${this.apiUrl}/all`, {
      params,
    });
  }

  /**
   * Get tracked staff only (FILTERED by configured job titles)
   * Use this for Dashboard/Ranking page
   * @param includeEmailStats Whether to include email statistics
   * @param fromDate Optional start date filter
   * @param toDate Optional end date filter
   */
  getStaff(
    includeEmailStats: boolean = true,
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Staff[]>> {
    let params = new HttpParams().set(
      'includeEmailStats',
      includeEmailStats.toString(),
    );
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Staff[]>>(this.apiUrl, { params });
  }

  /**
   * Get a single staff member by ID
   * @param staffId The staff member identifier
   * @param includeEmailStats Whether to include email statistics
   * @param fromDate Optional start date filter
   * @param toDate Optional end date filter
   */
  getStaffById(
    staffId: string,
    includeEmailStats: boolean = true,
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Staff>> {
    let params = new HttpParams().set(
      'includeEmailStats',
      includeEmailStats.toString(),
    );
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Staff>>(`${this.apiUrl}/${staffId}`, {
      params,
    });
  }

  /**
   * Search all staff by name or email (UNFILTERED)
   * Use this for Staff Management page
   * @param searchTerm The search term
   * @param top Maximum number of results
   */
  searchStaff(
    searchTerm: string,
    top: number = 50,
  ): Observable<ApiResponse<Staff[]>> {
    let params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('top', top.toString());

    return this.http.get<ApiResponse<Staff[]>>(`${this.apiUrl}/search`, {
      params,
    });
  }

  /**
   * Get total staff count
   */
  getStaffCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${this.apiUrl}/count`);
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
   * Format response time in minutes to readable format
   * @param minutes Response time in minutes
   */
  formatResponseTime(minutes: number): string {
    if (!minutes || minutes === 0) return 'N/A';

    if (minutes < 60) {
      return `${Math.round(minutes)} min`;
    } else if (minutes < 1440) {
      const hours = Math.round(minutes / 60);
      return `${hours} hour${hours > 1 ? 's' : ''}`;
    } else {
      const days = Math.round(minutes / 1440);
      return `${days} day${days > 1 ? 's' : ''}`;
    }
  }

  /**
   * Calculate response rate percentage
   * @param responded Number of responded emails
   * @param total Total number of emails
   */
  calculateResponseRate(responded: number, total: number): number {
    if (!total || total === 0) return 0;
    return Math.round((responded / total) * 100);
  }
}
