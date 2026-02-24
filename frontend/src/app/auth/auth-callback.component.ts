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
      <div class="loading-spinner"></div>
      <p class="loading-text">{{ message }}</p>
    </div>
  `,
  styles: [
    `
      .callback-container {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      .loading-spinner {
        width: 50px;
        height: 50px;
        border: 4px solid rgba(255, 255, 255, 0.3);
        border-top-color: white;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .loading-text {
        margin-top: 20px;
        color: white;
        font-size: 18px;
        font-weight: 500;
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
