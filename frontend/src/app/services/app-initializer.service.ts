import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

/**
 * Service to initialize app and validate authentication state
 */
@Injectable({
  providedIn: 'root',
})
export class AppInitializerService {
  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  /**
   * Initialize the application
   * Validates token and redirects if invalid
   */
  async initialize(): Promise<void> {
    console.log('🚀 Initializing application...');

    const token = this.authService.getToken();
    const currentPath = window.location.pathname;

    // Skip validation for public routes
    if (currentPath === '/login' || currentPath === '/auth-callback') {
      console.log('✅ On public route, skipping token validation');
      return;
    }

    if (!token) {
      console.warn('⚠️ No token found, redirecting to login');
      this.router.navigate(['/login']);
      return;
    }

    // Validate token with backend
    try {
      console.log('🔐 Validating authentication token...');
      const isValid = await this.authService.validateToken().toPromise();

      if (isValid) {
        console.log('✅ Token is valid');
      } else {
        console.warn('⚠️ Token validation failed, redirecting to login');
        this.authService.logout();
      }
    } catch (error) {
      console.error('❌ Token validation error:', error);
      // If validation fails, clear session and redirect
      localStorage.clear();
      sessionStorage.clear();
      this.router.navigate(['/login'], {
        queryParams: { reason: 'validation_failed' },
      });
    }
  }
}
