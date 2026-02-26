import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Account {
  accountid: string;
  name: string;
  accountnumber: string;
  emailaddress1: string;
  emailaddress2: string;
  emailaddress3: string;
  telephone1: string;
  websiteurl: string;
  address1_city: string;
  address1_stateorprovince: string;
  address1_country: string;
  revenue: number;
  numberofemployees: number;
  industrycode: number;
  description: string;
  createdon: string;
  modifiedon: string;
  _ownerid_value: string;
  ownerid?: {
    fullname: string;
  };
  _primarycontactid_value: string;
  primarycontactid_formatted: string;
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
export class AccountService {
  private apiUrl = `${environment.apiUrl}/accounts`;

  constructor(private http: HttpClient) {}

  /**
   * Get all accounts with optional filtering
   * @param top Maximum number of accounts to return
   * @param skip Number of records to skip (for pagination)
   * @param searchTerm Optional search term for filtering
   * @param ownerId Optional owner ID filter
   * @param status Optional status filter (active/inactive)
   * @param fromDate Optional start date filter
   * @param toDate Optional end date filter
   */
  getAccounts(
    top: number = 50,
    skip: number = 0,
    searchTerm?: string,
    ownerId?: string,
    status?: string,
    fromDate?: string,
    toDate?: string,
  ): Observable<ApiResponse<Account[]>> {
    let params = new HttpParams()
      .set('top', top.toString())
      .set('skip', skip.toString());
    if (searchTerm && searchTerm.trim())
      params = params.set('search', searchTerm.trim());
    if (ownerId && ownerId !== 'all') params = params.set('ownerId', ownerId);
    if (status && status !== 'all') params = params.set('status', status);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);

    return this.http.get<ApiResponse<Account[]>>(this.apiUrl, { params });
  }

  /**
   * Get total count of accounts
   */
  getAccountCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${this.apiUrl}/count`);
  }

  /**
   * Get a single account by ID
   * @param accountId The account identifier
   */
  getAccountById(accountId: string): Observable<ApiResponse<Account>> {
    return this.http.get<ApiResponse<Account>>(`${this.apiUrl}/${accountId}`);
  }

  /**
   * Update an existing account
   * @param account The account object with updated values
   */
  updateAccount(account: Account): Observable<ApiResponse<Account>> {
    return this.http.put<ApiResponse<Account>>(
      `${this.apiUrl}/${account.accountid}`,
      account,
    );
  }

  /**
   * Format revenue as currency
   * @param revenue The revenue amount
   */
  formatRevenue(revenue: number): string {
    if (!revenue) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(revenue);
  }

  /**
   * Format date string to readable format
   * @param dateString ISO date string
   */
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  /**
   * Search accounts by name or account number
   * @param searchTerm The search term
   * @param top Maximum number of results
   */
  searchAccounts(
    searchTerm: string,
    top: number = 50,
  ): Observable<ApiResponse<Account[]>> {
    let params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('top', top.toString());

    return this.http.get<ApiResponse<Account[]>>(`${this.apiUrl}/search`, {
      params,
    });
  }

  /**
   * Get initials from account name
   * @param name Account name
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
}
