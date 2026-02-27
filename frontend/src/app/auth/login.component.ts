import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

type LoginState = 'checking' | 'ready' | 'redirecting' | 'error' | 'logged-out';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo-container">
            <img
              src="https://cdn.brandfetch.io/idB_nYYar2/w/1148/h/414/theme/dark/logo.png?c=1dxbfHSJFAPEGdCLU4o5B"
              alt="Logo"
              class="logo-icon"
            />
          </div>
          <p class="brand-subtitle">{{ subtitle }}</p>
        </div>

        <div class="login-content">
          <!-- Error State -->
          <div *ngIf="state === 'error'" class="error-container">
            <div class="error-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="#ef4444"
                  stroke-width="2"
                />
                <path
                  d="M12 8v4M12 16h.01"
                  stroke="#ef4444"
                  stroke-width="2"
                  stroke-linecap="round"
                />
              </svg>
            </div>
            <h3 class="error-title">Authentication Failed</h3>
            <p class="error-message">{{ errorMessage }}</p>
            <button class="retry-btn" (click)="retryLogin()">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M1 4v6h6M23 20v-6h-6"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <path
                  d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              Try Again
            </button>
          </div>

          <!-- Logged Out State -->
          <div *ngIf="state === 'logged-out'" class="logout-container">
            <div class="logout-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="#10b981"
                  stroke-width="2"
                />
                <path
                  d="M8 12l3 3 5-5"
                  stroke="#10b981"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <h3 class="logout-title">Signed Out Successfully</h3>
            <p class="logout-message">You have been safely signed out.</p>
            <button class="microsoft-login-btn" (click)="loginWithMicrosoft()">
              <svg
                class="ms-icon"
                viewBox="0 0 21 21"
                xmlns="http://www.w3.org/2000/svg"
              >
                <rect x="1" y="1" width="9" height="9" fill="#f25022" />
                <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
                <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
                <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
              </svg>
              Sign in with Microsoft
            </button>
          </div>

          <!-- Checking/Ready State -->
          <div
            *ngIf="state === 'checking' || state === 'ready'"
            class="ready-container"
          >
            <button class="microsoft-login-btn" (click)="loginWithMicrosoft()">
              <svg
                class="ms-icon"
                viewBox="0 0 21 21"
                xmlns="http://www.w3.org/2000/svg"
              >
                <rect x="1" y="1" width="9" height="9" fill="#f25022" />
                <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
                <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
                <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
              </svg>
              Sign in with Microsoft
            </button>
            <p class="help-text">Use your organization account to sign in</p>
          </div>

          <!-- Redirecting State -->
          <div *ngIf="state === 'redirecting'" class="redirecting-container">
            <div class="loading-spinner"></div>
            <p class="redirecting-text">Redirecting to Microsoft login...</p>
            <button class="fallback-btn" (click)="loginWithMicrosoft()">
              Click here if not redirected
            </button>
          </div>
        </div>

        <div class="login-footer">
          <p class="footer-text">
            © 2026 Bintara Solutions. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .login-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #f5f5f7 0%, #e8e8ed 100%);
        padding: 20px;
      }

      .login-card {
        background: transparent;
        max-width: 440px;
        width: 100%;
        padding: 48px 40px;
        border-radius: 16px;
      }

      .login-header {
        text-align: center;
        margin-bottom: 40px;
      }

      .logo-container {
        margin-bottom: 32px;
      }

      .logo-icon {
        width: auto;
        height: 80px;
        max-width: 280px;
        object-fit: contain;
      }

      .brand-title {
        font-size: 28px;
        font-weight: 600;
        color: #1f2937;
        margin: 0 0 8px 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .brand-subtitle {
        font-size: 14px;
        color: #6b7280;
        margin: 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .login-content {
        margin-bottom: 32px;
      }

      /* Ready/Checking State */
      .ready-container {
        text-align: center;
      }

      .help-text {
        margin-top: 16px;
        font-size: 13px;
        color: #9ca3af;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      /* Redirecting State */
      .redirecting-container {
        text-align: center;
      }

      .loading-spinner {
        width: 40px;
        height: 40px;
        border: 3px solid #e5e7eb;
        border-top-color: #6d5dff;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin: 0 auto 20px;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .redirecting-text {
        text-align: center;
        font-size: 14px;
        color: #6b7280;
        font-weight: 400;
        margin-bottom: 24px;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .fallback-btn {
        background: transparent;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 10px 20px;
        font-size: 13px;
        color: #6b7280;
        cursor: pointer;
        transition: all 0.2s ease;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .fallback-btn:hover {
        border-color: #6d5dff;
        color: #6d5dff;
      }

      /* Error State */
      .error-container {
        text-align: center;
      }

      .error-icon {
        margin-bottom: 16px;
      }

      .error-icon svg {
        width: 48px;
        height: 48px;
      }

      .error-title {
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
        margin: 0 0 8px 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .error-message {
        font-size: 14px;
        color: #6b7280;
        margin: 0 0 24px 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .retry-btn {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 12px 24px;
        background: #6d5dff;
        border: none;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 500;
        color: white;
        cursor: pointer;
        transition: all 0.2s ease;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .retry-btn:hover {
        background: #5a4ddb;
      }

      .retry-btn svg {
        width: 16px;
        height: 16px;
      }

      /* Logout State */
      .logout-container {
        text-align: center;
      }

      .logout-icon {
        margin-bottom: 16px;
      }

      .logout-icon svg {
        width: 48px;
        height: 48px;
      }

      .logout-title {
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
        margin: 0 0 8px 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .logout-message {
        font-size: 14px;
        color: #6b7280;
        margin: 0 0 24px 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      /* Microsoft Login Button */
      .microsoft-login-btn {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        padding: 14px 24px;
        background: #2f2f2f;
        border: none;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 500;
        color: white;
        cursor: pointer;
        transition: all 0.2s ease;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .microsoft-login-btn:hover {
        background: #1a1a1a;
      }

      .microsoft-login-btn:active {
        transform: scale(0.98);
      }

      .ms-icon {
        width: 20px;
        height: 20px;
      }

      .login-footer {
        text-align: center;
        padding-top: 24px;
        border-top: 1px solid #f3f4f6;
      }

      .footer-text {
        font-size: 12px;
        color: #9ca3af;
        font-weight: 400;
        margin: 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      @media (max-width: 480px) {
        .login-card {
          padding: 32px 24px;
        }

        .brand-title {
          font-size: 24px;
        }
      }
    `,
  ],
})
export class LoginComponent implements OnInit, OnDestroy {
  state: LoginState = 'checking';
  subtitle = 'Sign in to continue';
  errorMessage = 'Something went wrong. Please try again.';

  private redirectTimeout: any;

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // Check URL parameters for error or logout state
    this.route.queryParams.subscribe((params) => {
      const hasError = params['error'] === 'true';
      const justLoggedOut = params['logout'] === 'true';
      const errorReason = params['reason'];

      if (hasError) {
        this.state = 'error';
        this.subtitle = 'Authentication error';
        this.errorMessage = this.getErrorMessage(errorReason);
        console.error('🔐 Login error detected:', errorReason);
      } else if (justLoggedOut) {
        this.state = 'logged-out';
        this.subtitle = 'Come back soon!';
        console.log('🔐 User logged out successfully');
      } else if (this.authService.isAuthenticated()) {
        // Already authenticated, redirect to dashboard
        console.log('🔐 Already authenticated, redirecting to dashboard...');
        this.router.navigate(['/dashboard']);
      } else {
        // Ready for login
        this.state = 'ready';
        this.subtitle = 'Sign in to continue';
        console.log('🔐 Login page ready');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }
  }

  loginWithMicrosoft(): void {
    this.state = 'redirecting';
    this.subtitle = 'Please wait...';

    // Small delay to show the redirecting state
    this.redirectTimeout = setTimeout(() => {
      this.authService.loginWithMicrosoft();
    }, 500);
  }

  retryLogin(): void {
    // Clear error state and try again
    this.state = 'ready';
    this.subtitle = 'Sign in to continue';

    // Clean up URL
    this.router.navigate(['/login'], { queryParams: {} });
  }

  private getErrorMessage(reason: string | undefined): string {
    switch (reason) {
      case 'token_expired':
        return 'Your session has expired. Please sign in again.';
      case 'invalid_token':
        return 'Authentication token is invalid. Please sign in again.';
      case 'access_denied':
        return 'Access was denied. Please contact your administrator.';
      case 'server_error':
        return 'Server error occurred. Please try again later.';
      case 'network_error':
        return 'Network connection failed. Please check your internet.';
      default:
        return 'Something went wrong during authentication. Please try again.';
    }
  }
}
