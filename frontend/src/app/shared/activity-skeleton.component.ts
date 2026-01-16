import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Activity Page Skeleton
 * Only shows skeleton for the table content (filter tabs + rows)
 * Metric cards are already visible with 0 values while loading
 */
@Component({
  selector: 'app-activity-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="activity-skeleton">
      <!-- Filter Tabs -->
      <div class="filter-tabs">
        <div class="skeleton tab" *ngFor="let i of [1, 2, 3, 4, 5, 6]"></div>
      </div>

      <!-- Table Rows -->
      <div class="table-container">
        <div
          class="table-row"
          *ngFor="let i of [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
        >
          <div class="row-left">
            <div class="skeleton badge"></div>
            <div class="skeleton status-badge"></div>
            <div class="skeleton subject"></div>
          </div>
          <div class="row-right">
            <div class="skeleton staff-name"></div>
            <div class="skeleton date"></div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .activity-skeleton {
        margin-top: 16px;
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

      .filter-tabs {
        display: flex;
        gap: 8px;
        margin-bottom: 16px;
        padding: 6px;
        background: white;
        border-radius: 10px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .tab {
        width: 75px;
        height: 34px;
        border-radius: 6px;
      }

      .table-container {
        background: white;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .table-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 14px 20px;
        border-bottom: 1px solid #f3f4f6;
      }

      .table-row:last-child {
        border-bottom: none;
      }

      .row-left {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;
      }

      .badge {
        width: 42px;
        height: 22px;
        border-radius: 4px;
      }

      .status-badge {
        width: 85px;
        height: 20px;
        border-radius: 4px;
      }

      .subject {
        width: 40%;
        height: 14px;
        min-width: 180px;
      }

      .row-right {
        display: flex;
        align-items: center;
        gap: 20px;
      }

      .staff-name {
        width: 110px;
        height: 14px;
      }

      .date {
        width: 70px;
        height: 14px;
      }

      @media (max-width: 768px) {
        .table-row {
          flex-direction: column;
          align-items: flex-start;
          gap: 10px;
        }
        .row-right {
          width: 100%;
          justify-content: space-between;
        }
        .subject {
          width: 100%;
        }
        .filter-tabs {
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class ActivitySkeletonComponent {}
