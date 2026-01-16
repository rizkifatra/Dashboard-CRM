import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Staff Page Skeleton
 * Only shows skeleton for staff cards grid
 * Header and search section are already visible while loading
 */
@Component({
  selector: 'app-staff-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="staff-skeleton">
      <!-- Staff Grid Only - no search section (already visible) -->
      <div class="staff-grid">
        <div class="staff-card" *ngFor="let i of [1, 2, 3, 4, 5, 6]">
          <!-- Avatar -->
          <div class="skeleton avatar"></div>

          <!-- Staff Info -->
          <div class="staff-info">
            <div class="skeleton staff-name"></div>
            <div class="skeleton staff-title"></div>
            <div class="skeleton staff-email"></div>
          </div>

          <!-- Stats Grid -->
          <div class="stats-grid">
            <div class="stat-item" *ngFor="let j of [1, 2, 3, 4, 5, 6]">
              <div class="skeleton stat-label"></div>
              <div class="skeleton stat-value"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .staff-skeleton {
        margin-top: 0;
      }

      .skeleton {
        background: linear-gradient(
          90deg,
          #e8e8e8 25%,
          #f5f5f5 50%,
          #e8e8e8 75%
        );
        background-size: 200% 100%;
        animation: shimmer 1.5s infinite ease-in-out;
        border-radius: 6px;
      }

      @keyframes shimmer {
        0% {
          background-position: 200% 0;
        }
        100% {
          background-position: -200% 0;
        }
      }

      /* Staff Grid */
      .staff-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 20px;
      }

      .staff-card {
        background: white;
        border-radius: 12px;
        padding: 24px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .avatar {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        margin-bottom: 16px;
      }

      .staff-info {
        margin-bottom: 20px;
      }

      .staff-name {
        width: 140px;
        height: 20px;
        margin-bottom: 8px;
      }

      .staff-title {
        width: 100px;
        height: 14px;
        margin-bottom: 6px;
      }

      .staff-email {
        width: 180px;
        height: 14px;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
        padding-top: 16px;
        border-top: 1px solid #f0f0f0;
      }

      .stat-item {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .stat-label {
        width: 80px;
        height: 12px;
      }

      .stat-value {
        width: 50px;
        height: 18px;
      }

      /* Responsive */
      @media (max-width: 768px) {
        .staff-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class StaffSkeletonComponent {}
