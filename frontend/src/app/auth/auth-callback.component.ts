import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';

type CallbackState = 'processing' | 'success' | 'error';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-container">
      <div class="callback-card">
        <!-- Processing State -->
        <div *ngIf="state === 'processing'" class="state-content">
          <div class="loading-spinner"></div>
          <p class="loading-text">{{ message }}</p>
        </div>

        <!-- Success State -->
        <div *ngIf="state === 'success'" class="state-content">
          <div class="success-icon">
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
          <h3 class="success-title">Welcome back!</h3>
          <p class="success-text">{{ message }}</p>
        </div>

        <!-- Error State -->
        <div *ngIf="state === 'error'" class="state-content">
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
          <p class="error-text">{{ message }}</p>
          <button class="retry-btn" (click)="goToLogin()">
            Return to Login
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .callback-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #f5f5f7 0%, #e8e8ed 100%);
        padding: 20px;
      }

      .callback-card {
        background: transparent;
        padding: 48px 40px;
        text-align: center;
        max-width: 440px;
        width: 100%;
      }

      .state-content {
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      .loading-spinner {
        width: 48px;
        height: 48px;
        border: 3px solid #e5e7eb;
        border-top-color: #6d5dff;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin-bottom: 20px;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .loading-text {
        margin: 0;
        color: #1f2937;
        font-size: 14px;
        font-weight: 400;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .success-icon,
      .error-icon {
        margin-bottom: 16px;
      }

      .success-icon svg,
      .error-icon svg {
        width: 56px;
        height: 56px;
      }

      .success-title,
      .error-title {
        font-size: 20px;
        font-weight: 600;
        color: #1f2937;
        margin: 0 0 8px 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .success-text,
      .error-text {
        font-size: 14px;
        color: #6b7280;
        margin: 0;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }

      .retry-btn {
        margin-top: 24px;
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
    `,
  ],
})
export class AuthCallbackComponent implements OnInit, OnDestroy {
  state: CallbackState = 'processing';
  message = 'Authenticating...';

  private redirectTimeout: any;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.handleCallback();
  }

  ngOnDestroy(): void {
    if (this.redirectTimeout) {
      clearTimeout(this.redirectTimeout);
    }
  }

  private handleCallback(): void {
    // Extract token and user info from URL query parameters
    this.route.queryParams.subscribe((params) => {
      const token = params['token'];
      const email = params['email'];
      const name = params['name'];
      const error = params['error'];

      // Check for error first
      if (error) {
        this.state = 'error';
        this.message = this.getErrorMessage(error);
        console.error('🔐 Auth callback error:', error);
        return;
      }

      if (token && email) {
        // Store token and user info
        const authResponse = {
          token: token,
          user: {
            email: email,
            name: name || email,
          },
        };

        try {
          this.authService.setSession(authResponse);
          this.state = 'success';
          this.message = 'Redirecting to dashboard...';

          this.redirectTimeout = setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 1500);
        } catch (err) {
          console.error('🔐 Failed to set session:', err);
          this.state = 'error';
          this.message = 'Failed to process authentication. Please try again.';
        }
      } else {
        console.error('🔐 Authentication failed: No token received');
        this.state = 'error';
        this.message = 'No authentication data received. Please try again.';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login'], {
      queryParams: { error: 'true', reason: 'invalid_token' },
    });
  }

  private getErrorMessage(error: string): string {
    switch (error) {
      case 'access_denied':
        return 'Access was denied. Please contact your administrator.';
      case 'server_error':
        return 'Server error occurred. Please try again later.';
      case 'invalid_request':
        return 'Invalid authentication request. Please try again.';
      default:
        return 'Authentication failed. Please try again.';
    }
  }
}
