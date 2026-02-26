import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface HealthStatus {
  status: string;
  application: string;
  timestamp: number;
  configured: boolean;
  warning?: string;
}

export interface D365ConnectionStatus {
  baseUrl: string;
  timestamp: number;
  status: string;
  authenticationStatus?: string;
  connectionStatus?: string;
  message?: string;
  error?: string;
}

export interface SystemConfig {
  d365BaseUrl: string;
  d365Scope: string;
  azureTenantId: string;
  azureClientIdConfigured: boolean;
  azureClientSecretConfigured: boolean;
  fullyConfigured: boolean;
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
export class HealthService {
  private apiUrl = `${environment.apiUrl}/health`;

  constructor(private http: HttpClient) {}

  /**
   * Check application health status
   */
  checkHealth(): Observable<ApiResponse<HealthStatus>> {
    return this.http.get<ApiResponse<HealthStatus>>(this.apiUrl);
  }

  /**
   * Test Dynamics 365 connection and authentication
   */
  testD365Connection(): Observable<ApiResponse<D365ConnectionStatus>> {
    return this.http.get<ApiResponse<D365ConnectionStatus>>(
      `${this.apiUrl}/d365-connection`,
    );
  }

  /**
   * Get system configuration information (without sensitive data)
   */
  getConfig(): Observable<ApiResponse<SystemConfig>> {
    return this.http.get<ApiResponse<SystemConfig>>(`${this.apiUrl}/config`);
  }

  /**
   * Check if system is fully configured
   */
  async isConfigured(): Promise<boolean> {
    try {
      const response = await this.getConfig().toPromise();
      return response?.data?.fullyConfigured || false;
    } catch (error) {
      console.error('Error checking configuration:', error);
      return false;
    }
  }

  /**
   * Get status indicator color based on health status
   * @param status The health status string
   */
  getStatusColor(status: string): string {
    switch (status.toLowerCase()) {
      case 'up':
      case 'connected':
      case 'success':
        return '#4CAF50'; // Green
      case 'not_configured':
      case 'configuration_required':
        return '#FF9800'; // Orange
      case 'connection_failed':
      case 'failed':
      case 'error':
        return '#F44336'; // Red
      default:
        return '#9E9E9E'; // Gray
    }
  }

  /**
   * Get human-readable status text
   * @param status The health status string
   */
  getStatusText(status: string): string {
    switch (status.toLowerCase()) {
      case 'up':
        return 'Healthy';
      case 'connected':
        return 'Connected';
      case 'success':
        return 'Success';
      case 'not_configured':
        return 'Not Configured';
      case 'connection_failed':
        return 'Connection Failed';
      case 'failed':
        return 'Failed';
      case 'error':
        return 'Error';
      default:
        return status;
    }
  }
}
