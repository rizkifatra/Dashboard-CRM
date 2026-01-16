import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Opportunities Page Skeleton
 * Only shows skeleton for table content
 * Metric cards are already visible with 0 values while loading
 */
@Component({
  selector: 'app-opportunities-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="opportunities-skeleton">
      <!-- Search Section -->
      <div class="search-section">
        <div class="skeleton search-title"></div>
        <div class="skeleton search-box"></div>
      </div>

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
          <div class="skeleton th" style="width: 15%;"></div>
          <div class="skeleton th" style="width: 15%;"></div>
          <div class="skeleton th" style="width: 12%;"></div>
          <div class="skeleton th" style="width: 15%;"></div>
          <div class="skeleton th" style="width: 18%;"></div>
        </div>

        <!-- Table Rows -->
        <div
          class="table-row"
          *ngFor="let i of [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"
        >
          <div class="td-name">
            <div class="skeleton name"></div>
            <div class="skeleton description"></div>
          </div>
          <div class="skeleton td-value"></div>
          <div class="skeleton td-stage"></div>
          <div class="skeleton td-probability"></div>
          <div class="skeleton td-owner"></div>
          <div class="skeleton td-date"></div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .opportunities-skeleton {
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

      .search-section {
        background: white;
        border-radius: 12px;
        padding: 20px;
        margin-bottom: 16px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .search-title {
        width: 160px;
        height: 20px;
        margin-bottom: 16px;
      }

      .search-box {
        width: 100%;
        max-width: 600px;
        height: 44px;
        border-radius: 10px;
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
        width: 200px;
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
        padding: 16px 24px;
        background: #fafafa;
        border-bottom: 1px solid #e5e7eb;
      }

      .th {
        height: 14px;
      }

      .table-row {
        display: flex;
        gap: 16px;
        padding: 16px 24px;
        border-bottom: 1px solid #f3f4f6;
        align-items: center;
      }

      .table-row:last-child {
        border-bottom: none;
      }

      .td-name {
        width: 25%;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .td-name .name {
        width: 100%;
        height: 16px;
      }
      .td-name .description {
        width: 80%;
        height: 12px;
      }
      .td-value {
        width: 15%;
        height: 14px;
      }
      .td-stage {
        width: 15%;
        height: 24px;
        border-radius: 12px;
      }
      .td-probability {
        width: 12%;
        height: 14px;
      }
      .td-owner {
        width: 15%;
        height: 14px;
      }
      .td-date {
        width: 18%;
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
        .td-name {
          width: 100%;
        }
        .td-value,
        .td-stage,
        .td-probability,
        .td-owner,
        .td-date {
          width: calc(50% - 4px);
        }
      }
    `,
  ],
})
export class OpportunitiesSkeletonComponent {}
