import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { AuthResponse, User } from '../models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';

  private currentUserSubject = new BehaviorSubject<User | null>(
    this.getUserFromStorage(),
  );
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(
    this.hasToken(),
  );
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Initiate Microsoft login (redirect to backend OAuth2 endpoint)
   */
  loginWithMicrosoft(): void {
    window.location.href = environment.oauth2AuthorizationUrl;
  }

  /**
   * Get user info from backend after OAuth2 redirect
   */
  getUserInfo(): Observable<AuthResponse> {
    return this.http
      .get<{ success: boolean; data: AuthResponse }>(`${this.API_URL}/user`, {
        withCredentials: true,
      })
      .pipe(
        map((response) => response.data),
        tap((authResponse) => {
          this.setSession(authResponse);
        }),
      );
  }

  /**
   * Validate current token
   */
  validateToken(): Observable<boolean> {
    const token = this.getToken();
    if (!token) {
      return new Observable((observer) => {
        observer.next(false);
        observer.complete();
      });
    }

    return this.http
      .post<{ success: boolean; data: any }>(
        `${this.API_URL}/validate`,
        {},
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      )
      .pipe(
        map((response) => response.success),
        tap((isValid) => {
          if (!isValid) {
            this.logout();
          }
        }),
      );
  }

  /**
   * Logout user - securely clear all session data
   */
  logout(): void {
    const token = this.getToken();

    // Call backend logout endpoint to invalidate token server-side
    if (token) {
      this.http
        .post(
          `${this.API_URL}/logout`,
          {},
          {
            headers: { Authorization: `Bearer ${token}` },
          },
        )
        .subscribe({
          next: () => {
            console.log('Backend logout successful');
          },
          error: (err) => {
            console.error('Backend logout failed:', err);
            // Continue with client-side cleanup even if backend fails
          },
          complete: () => {
            // Ensure session is cleared after backend call completes
            this.performSecureLogout();
          },
        });
    } else {
      // No token, just clear client-side data
      this.performSecureLogout();
    }
  }

  /**
   * Perform complete client-side logout
   */
  private performSecureLogout(): void {
    // Clear all authentication data
    this.clearSession();

    // Clear any other sensitive data in localStorage
    localStorage.clear();

    // Clear sessionStorage
    sessionStorage.clear();

    // Reset observables
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);

    // Redirect to login page with logout flag to show success message
    window.location.href = '/login?logout=true';
  }

  /**
   * Store authentication data
   */
  setSession(
    authResponse:
      | AuthResponse
      | { token: string; user: { email: string; name: string } },
  ): void {
    // Handle both response formats
    let token: string;
    let user: User;

    if ('token' in authResponse && 'user' in authResponse) {
      // New format from URL params
      token = authResponse.token;
      user = {
        email: authResponse.user.email,
        name: authResponse.user.name,
        givenName: authResponse.user.name.split(' ')[0],
        surname: authResponse.user.name.split(' ').slice(1).join(' '),
      };
    } else {
      // Original format from API
      token = (authResponse as AuthResponse).token;
      user = {
        email: (authResponse as AuthResponse).email,
        name: (authResponse as AuthResponse).name,
        givenName: (authResponse as AuthResponse).givenName,
        surname: (authResponse as AuthResponse).surname,
      };
    }

    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
  }

  /**
   * Clear authentication data
   */
  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }

  /**
   * Get stored token
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Get current user from storage
   */
  private getUserFromStorage(): User | null {
    const userJson = localStorage.getItem(this.USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  }

  /**
   * Check if token exists
   */
  private hasToken(): boolean {
    return !!this.getToken();
  }

  /**
   * Get current user
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }
}
