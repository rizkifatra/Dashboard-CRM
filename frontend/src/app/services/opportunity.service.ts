import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Opportunity {
  opportunityId: string;
  name: string;
  description?: string;
  estimatedValue: number;
  actualValue?: number;
  estimatedCloseDate?: string;
  actualCloseDate?: string;
  closeProbability?: number;
  priorityCode?: number; // 1=Low, 2=Normal, 3=High
  salesStage?: number;
  stepName?: string;
  createdOn: string;
  modifiedOn?: string;
  stateCode: number; // 0=Open, 1=Won, 2=Lost
  statusCode: number;
  ownerId: string;
  ownerName?: string;
  customerId?: string;
  accountId?: string;
}

export interface OpportunityStats {
  totalOpportunities: number;
  openOpportunities: number;
  wonOpportunities: number;
  lostOpportunities: number;
  totalEstimatedValue: number;
  totalActualValue: number;
  wonValue: number;
  winRate: number;
  averageDealSize: number;
}

export interface StaffOpportunityStats {
  ownerId: string;
  totalOpportunities: number;
  openOpportunities: number;
  wonOpportunities: number;
  lostOpportunities: number;
  totalEstimatedValue: number;
  wonValue: number;
  winRate: number;
  averageDealSize: number;
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
export class OpportunityService {
  private apiUrl = 'http://localhost:8080/api/opportunities';

  constructor(private http: HttpClient) {}

  /**
   * Get all opportunities with pagination support
   * @param skip Number of records to skip (for pagination)
   * @param top Maximum number of records to return (default: 50)
   * @param search Search term for filtering opportunities
   * @param fromDate Optional start date filter
   * @param toDate Optional end date filter
   */
  getAllOpportunities(
    skip: number = 0,
    top: number = 50,
    search: string = '',
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Opportunity[]>> {
    let params = new HttpParams()
      .set('top', top.toString())
      .set('skip', skip.toString());
    if (search) params = params.set('search', search);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Opportunity[]>>(`${this.apiUrl}/all`, {
      params,
    });
  }

  /**
   * Get opportunity statistics
   */
  getOpportunityStatistics(
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<OpportunityStats>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<OpportunityStats>>(
      `${this.apiUrl}/stats`,
      { params },
    );
  }

  /**
   * Get opportunities by staff
   */
  getOpportunitiesByStaff(
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<{ [key: string]: StaffOpportunityStats }>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<{ [key: string]: StaffOpportunityStats }>>(
      `${this.apiUrl}/by-staff`,
      { params },
    );
  }

  /**
   * Get top opportunities by value
   */
  getTopOpportunities(
    top: number = 10,
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Opportunity[]>> {
    let params = new HttpParams().set('top', top.toString());
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Opportunity[]>>(`${this.apiUrl}/top`, {
      params,
    });
  }

  /**
   * Get opportunity by ID
   */
  getOpportunityById(id: string): Observable<ApiResponse<Opportunity>> {
    return this.http.get<ApiResponse<Opportunity>>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get count of active opportunities
   */
  getActiveOpportunityCount(
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<number>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<number>>(`${this.apiUrl}/count`, {
      params,
    });
  }

  /**
   * Format currency value
   */
  formatCurrency(value: number): string {
    if (!value) return 'RM 0';
    return `RM ${value.toLocaleString('en-MY', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    })}`;
  }

  /**
   * Format percentage
   */
  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  /**
   * Get status label
   */
  getStatusLabel(stateCode: number): string {
    switch (stateCode) {
      case 0:
        return 'Open';
      case 1:
        return 'Won';
      case 2:
        return 'Lost';
      default:
        return 'Unknown';
    }
  }

  /**
   * Get status color class
   */
  getStatusColorClass(stateCode: number): string {
    switch (stateCode) {
      case 0:
        return 'status-open';
      case 1:
        return 'status-won';
      case 2:
        return 'status-lost';
      default:
        return '';
    }
  }

  /**
   * Get monthly trends
   */
  getMonthlyTrends(months: number = 6): Observable<ApiResponse<any[]>> {
    let params = new HttpParams().set('months', months.toString());
    return this.http.get<ApiResponse<any[]>>(`${this.apiUrl}/monthly-trends`, {
      params,
    });
  }
}
