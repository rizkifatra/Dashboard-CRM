import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1 class="brand-title">CRM Dashboard</h1>
          <p class="brand-subtitle">{{ message }}</p>
        </div>

        <div class="login-content">
          <div class="loading-spinner"></div>
          <p class="redirecting-text">Redirecting to Microsoft login...</p>

          <button class="microsoft-login-btn" (click)="loginWithMicrosoft()">
            <span>Click here if not redirected</span>
          </button>
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
        background: #f5f5f7;
        padding: 20px;
      }

      .login-card {
        background: transparent;
        max-width: 440px;
        width: 100%;
        padding: 48px 40px;
      }

      .login-header {
        text-align: center;
        margin-bottom: 40px;
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

      .microsoft-login-btn {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 14px 24px;
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

      .microsoft-login-btn:hover {
        background: #5a4ddb;
      }

      .microsoft-login-btn:active {
        transform: scale(0.98);
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
export class LoginComponent implements OnInit {
  message = 'Preparing to sign in...';

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // Auto-redirect to Microsoft login after a short delay
    console.log(
      '🔐 Login page loaded - Auto-redirecting to Microsoft login...',
    );
    setTimeout(() => {
      this.loginWithMicrosoft();
    }, 1000); // 1 second delay to show the loading message
  }

  loginWithMicrosoft(): void {
    this.authService.loginWithMicrosoft();
  }
}
