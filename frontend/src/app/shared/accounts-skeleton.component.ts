import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Accounts Page Skeleton
 * Only shows skeleton for table content (data info bar + table rows)
 * Metric cards are already visible with 0 values while loading
 */
@Component({
  selector: 'app-accounts-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="accounts-skeleton">
      <!-- Data Info Bar -->
      <div class="info-bar">
        <div class="skeleton info-loaded"></div>
        <div class="skeleton info-updated"></div>
      </div>

      <!-- Table -->
      <div class="table-container">
        <!-- Table Header -->
        <div class="table-header">
          <div class="skeleton th" style="width: 25%;"></div>
          <div class="skeleton th" style="width: 20%;"></div>
          <div class="skeleton th" style="width: 15%;"></div>
          <div class="skeleton th" style="width: 15%;"></div>
          <div class="skeleton th" style="width: 15%;"></div>
          <div class="skeleton th" style="width: 10%;"></div>
        </div>

        <!-- Table Rows -->
        <div
          class="table-row"
          *ngFor="let i of [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
        >
          <div class="skeleton td-name"></div>
          <div class="skeleton td-email"></div>
          <div class="skeleton td-phone"></div>
          <div class="skeleton td-city"></div>
          <div class="skeleton td-owner"></div>
          <div class="skeleton td-date"></div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .accounts-skeleton {
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

      .info-bar {
        display: flex;
        justify-content: space-between;
        background: white;
        border-radius: 10px;
        padding: 14px 20px;
        margin-bottom: 16px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .info-loaded {
        width: 180px;
        height: 16px;
      }

      .info-updated {
        width: 140px;
        height: 16px;
      }

      .table-container {
        background: white;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .table-header {
        display: flex;
        gap: 16px;
        padding: 16px 20px;
        background: #fafafa;
        border-bottom: 1px solid #e5e7eb;
      }

      .th {
        height: 14px;
      }

      .table-row {
        display: flex;
        gap: 16px;
        padding: 14px 20px;
        border-bottom: 1px solid #f3f4f6;
        align-items: center;
      }

      .table-row:last-child {
        border-bottom: none;
      }

      .td-name {
        width: 25%;
        height: 14px;
      }
      .td-email {
        width: 20%;
        height: 14px;
      }
      .td-phone {
        width: 15%;
        height: 14px;
      }
      .td-city {
        width: 15%;
        height: 14px;
      }
      .td-owner {
        width: 15%;
        height: 14px;
      }
      .td-date {
        width: 10%;
        height: 14px;
      }

      @media (max-width: 768px) {
        .table-header {
          display: none;
        }
        .table-row {
          flex-wrap: wrap;
          gap: 8px;
        }
        .td-name,
        .td-email,
        .td-phone,
        .td-city,
        .td-owner,
        .td-date {
          width: calc(50% - 4px);
        }
      }
    `,
  ],
})
export class AccountsSkeletonComponent {}
