import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { User } from '../models/auth.model';

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
        <div class="user-info" *ngIf="currentUser">
          <div class="user-avatar">{{ getUserInitials() }}</div>
          <div class="user-details">
            <div class="user-name">{{ currentUser.name }}</div>
            <div class="user-email">{{ currentUser.email }}</div>
          </div>
        </div>

        <button class="logout-btn" (click)="logout()">
          <span class="logout-icon">🚪</span>
          <span>Logout</span>
        </button>
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
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .user-info {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        background: #f9fafb;
        border-radius: 10px;
      }

      .user-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 14px;
        flex-shrink: 0;
      }

      .user-details {
        flex: 1;
        min-width: 0;
      }

      .user-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .user-email {
        font-size: 12px;
        color: #6b7280;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .logout-btn {
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        padding: 10px;
        background: white;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 500;
        color: #6b7280;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .logout-btn:hover {
        background: #fef2f2;
        border-color: #ef4444;
        color: #dc2626;
      }

      .logout-icon {
        font-size: 16px;
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

        .user-name {
          font-size: 13px;
        }

        .user-email {
          font-size: 11px;
        }
      }
    `,
  ],
})
export class NavigationComponent implements OnInit {
  currentUser: User | null = null;

  navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/ranking', label: 'Ranking', icon: '🏆' },
    { path: '/activities', label: 'Activity Management', icon: '👥' },
    { path: '/accounts', label: 'Accounts', icon: '👥' },
    { path: '/opportunities', label: 'Opportunities', icon: '💼' },
  ];

  constructor(
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
    });
  }

  getUserInitials(): string {
    if (!this.currentUser?.name) return 'U';
    const names = this.currentUser.name.split(' ');
    if (names.length >= 2) {
      return (names[0][0] + names[names.length - 1][0]).toUpperCase();
    }
    return this.currentUser.name.substring(0, 2).toUpperCase();
  }

  logout(): void {
    this.authService.logout();
  }
}
