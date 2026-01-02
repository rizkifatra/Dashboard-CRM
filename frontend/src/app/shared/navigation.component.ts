import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="main-navigation">
      <div class="nav-brand">
        <div class="brand-logo">📊</div>
        <h1 class="brand-title">CRM Dashboard</h1>
      </div>

      <ul class="nav-menu">
        <li *ngFor="let item of navItems" class="nav-item">
          <a
            [routerLink]="item.path"
            routerLinkActive="active"
            [routerLinkActiveOptions]="{ exact: item.path === '/dashboard' }"
            class="nav-link"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
          </a>
        </li>
      </ul>

      <div class="nav-footer">
        <div class="status-indicator">
          <div class="status-dot active"></div>
          <span class="status-text">Online</span>
        </div>
      </div>
    </nav>
  `,
  styles: [
    `
      .main-navigation {
        position: fixed;
        top: 0;
        left: 0;
        height: 100vh;
        width: 260px;
        background: #ffffff;
        border-right: 1px solid #e5e7eb;
        display: flex;
        flex-direction: column;
        padding: 24px 16px;
        box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
        z-index: 1000;
      }

      .nav-brand {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 0 8px 24px 8px;
        border-bottom: 1px solid #f3f4f6;
        margin-bottom: 24px;
      }

      .brand-logo {
        font-size: 32px;
        line-height: 1;
      }

      .brand-title {
        font-size: 18px;
        font-weight: 700;
        color: #1f2937;
        margin: 0;
        white-space: nowrap;
      }

      .nav-menu {
        flex: 1;
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .nav-item {
        margin: 0;
      }

      .nav-link {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        border-radius: 10px;
        color: #6b7280;
        text-decoration: none;
        font-size: 15px;
        font-weight: 500;
        transition: all 0.2s ease;
        position: relative;
      }

      .nav-link:hover {
        background: #f9fafb;
        color: #374151;
        transform: translateX(2px);
      }

      .nav-link.active {
        background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
        color: white;
        box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
      }

      .nav-link.active:hover {
        transform: translateX(0);
      }

      .nav-icon {
        font-size: 20px;
        line-height: 1;
        min-width: 20px;
        text-align: center;
      }

      .nav-label {
        flex: 1;
      }

      .nav-footer {
        padding-top: 16px;
        border-top: 1px solid #f3f4f6;
      }

      .status-indicator {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 16px;
        background: #f0fdf4;
        border-radius: 8px;
      }

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #10b981;
        animation: pulse 2s infinite;
      }

      @keyframes pulse {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.5;
        }
      }

      .status-text {
        font-size: 13px;
        font-weight: 500;
        color: #059669;
      }

      /* Responsive */
      @media (max-width: 768px) {
        .main-navigation {
          width: 220px;
        }

        .brand-title {
          font-size: 16px;
        }

        .nav-link {
          font-size: 14px;
          padding: 10px 12px;
        }
      }
    `,
  ],
})
export class NavigationComponent {
  navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/accounts', label: 'Accounts', icon: '🏢' },
    { path: '/opportunities', label: 'Opportunities', icon: '💼' },
    { path: '/activities', label: 'Activities', icon: '📧' },
    { path: '/staff', label: 'Staff', icon: '👥' },
  ];

  constructor(private router: Router) {}
}
