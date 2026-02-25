import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-container">
      <div class="callback-card">
        <div class="loading-spinner"></div>
        <p class="loading-text">{{ message }}</p>
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
        background: #f5f5f7;
        padding: 20px;
      }

      .callback-card {
        background: transparent;
        padding: 48px 40px;
        text-align: center;
        max-width: 440px;
        width: 100%;
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

      .loading-text {
        margin: 0;
        color: #1f2937;
        font-size: 14px;
        font-weight: 400;
        font-family:
          -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
      }
    `,
  ],
})
export class AuthCallbackComponent implements OnInit {
  message = 'Authenticating...';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.handleCallback();
  }

  private handleCallback(): void {
    // Extract token and user info from URL query parameters
    this.route.queryParams.subscribe((params) => {
      const token = params['token'];
      const email = params['email'];
      const name = params['name'];

      if (token && email) {
        // Store token and user info
        const authResponse = {
          token: token,
          user: {
            email: email,
            name: name || email,
          },
        };

        this.authService.setSession(authResponse);
        this.message = 'Login successful! Redirecting...';

        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 1000);
      } else {
        console.error('Authentication failed: No token received');
        this.message = 'Authentication failed. Redirecting to login...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      }
    });
  }
}
