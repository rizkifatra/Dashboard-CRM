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
          <div class="brand-logo">📊</div>
          <h1 class="brand-title">CRM Dashboard</h1>
          <p class="brand-subtitle">{{ message }}</p>
        </div>

        <div class="login-content">
          <div class="loading-spinner"></div>
          <p class="redirecting-text">Redirecting to Microsoft login...</p>

          <button class="microsoft-login-btn" (click)="loginWithMicrosoft()">
            <svg
              class="microsoft-icon"
              viewBox="0 0 21 21"
              xmlns="http://www.w3.org/2000/svg"
            >
              <rect x="1" y="1" width="9" height="9" fill="#f25022" />
              <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
              <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
              <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
            </svg>
            <span>Click here if not redirected</span>
          </button>

          <div class="security-notice">
            <svg
              class="security-icon"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
              ></path>
            </svg>
            <p>Secure authentication via Microsoft 365</p>
          </div>
        </div>

        <div class="login-footer">
          <p>Protected by enterprise-grade security</p>
          <p class="footer-text">
            © 2026 Bintara Solutions. All rights reserved.
          </p>
        </div>
      </div>

      <div class="background-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
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
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        position: relative;
        overflow: hidden;
        padding: 20px;
      }

      .background-decoration {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
        overflow: hidden;
      }

      .decoration-circle {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.1);
        animation: float 20s infinite ease-in-out;
      }

      .circle-1 {
        width: 300px;
        height: 300px;
        top: -100px;
        left: -100px;
      }

      .circle-2 {
        width: 200px;
        height: 200px;
        bottom: -50px;
        right: -50px;
        animation-delay: -7s;
      }

      .circle-3 {
        width: 150px;
        height: 150px;
        top: 50%;
        right: 10%;
        animation-delay: -14s;
      }

      @keyframes float {
        0%,
        100% {
          transform: translateY(0) rotate(0deg);
        }
        50% {
          transform: translateY(-30px) rotate(180deg);
        }
      }

      .login-card {
        background: white;
        border-radius: 24px;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        max-width: 440px;
        width: 100%;
        padding: 48px 40px;
        position: relative;
        z-index: 1;
        animation: slideUp 0.6s ease-out;
      }

      @keyframes slideUp {
        from {
          opacity: 0;
          transform: translateY(30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .login-header {
        text-align: center;
        margin-bottom: 40px;
      }

      .brand-logo {
        font-size: 64px;
        margin-bottom: 16px;
        animation: bounce 2s infinite;
      }

      @keyframes bounce {
        0%,
        100% {
          transform: translateY(0);
        }
        50% {
          transform: translateY(-10px);
        }
      }

      .brand-title {
        font-size: 32px;
        font-weight: 700;
        color: #1f2937;
        margin: 0 0 8px 0;
      }

      .brand-subtitle {
        font-size: 16px;
        color: #6b7280;
        margin: 0;
      }

      .login-content {
        margin-bottom: 32px;
      }

      .loading-spinner {
        width: 50px;
        height: 50px;
        border: 4px solid #e5e7eb;
        border-top-color: #8b5cf6;
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
        font-size: 16px;
        color: #6b7280;
        font-weight: 500;
        margin-bottom: 24px;
      }

      .microsoft-login-btn {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        padding: 16px 24px;
        background: white;
        border: 2px solid #e5e7eb;
        border-radius: 12px;
        font-size: 16px;
        font-weight: 600;
        color: #374151;
        cursor: pointer;
        transition: all 0.3s ease;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      }

      .microsoft-login-btn:hover {
        background: #f9fafb;
        border-color: #8b5cf6;
        box-shadow: 0 4px 16px rgba(139, 92, 246, 0.2);
        transform: translateY(-2px);
      }

      .microsoft-login-btn:active {
        transform: translateY(0);
      }

      .microsoft-icon {
        width: 24px;
        height: 24px;
      }

      .security-notice {
        margin-top: 24px;
        padding: 16px;
        background: #f0fdf4;
        border-radius: 12px;
        display: flex;
        align-items: center;
        gap: 12px;
        border: 1px solid #86efac;
      }

      .security-icon {
        width: 24px;
        height: 24px;
        color: #16a34a;
        flex-shrink: 0;
      }

      .security-notice p {
        margin: 0;
        font-size: 14px;
        color: #166534;
        font-weight: 500;
      }

      .login-footer {
        text-align: center;
        padding-top: 24px;
        border-top: 1px solid #f3f4f6;
      }

      .login-footer p {
        margin: 8px 0;
        font-size: 14px;
        color: #6b7280;
        font-weight: 500;
      }

      .footer-text {
        font-size: 12px !important;
        color: #9ca3af !important;
        font-weight: 400 !important;
      }

      @media (max-width: 480px) {
        .login-card {
          padding: 32px 24px;
        }

        .brand-title {
          font-size: 28px;
        }

        .brand-logo {
          font-size: 48px;
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
