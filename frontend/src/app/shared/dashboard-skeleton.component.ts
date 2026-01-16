import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Dashboard Page Skeleton
 * Matches the exact layout of CRM Dashboard (Fiscal Dashboard):
 * - Fiscal Year section with 2 metric cards
 * - Current Quarter section with 2 metric cards
 * - Opportunities Monthly section with grid cards
 */
@Component({
  selector: 'app-dashboard-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-skeleton">
      <!-- Fiscal Year Section -->
      <div class="fiscal-section">
        <div class="section-header">
          <div class="skeleton section-title"></div>
          <div class="skeleton period-select"></div>
        </div>
        <div class="metrics-row two-col">
          <div class="metric-card large">
            <div class="skeleton metric-label"></div>
            <div class="skeleton metric-value-large"></div>
            <div class="skeleton metric-subtitle"></div>
            <div class="skeleton metric-remaining"></div>
          </div>
          <div class="metric-card large">
            <div class="skeleton metric-label"></div>
            <div class="skeleton metric-value-large"></div>
            <div class="skeleton metric-subtitle"></div>
          </div>
        </div>
      </div>

      <!-- Current Quarter Section -->
      <div class="fiscal-section">
        <div class="section-header">
          <div class="skeleton section-title"></div>
          <div class="skeleton period-select"></div>
        </div>
        <div class="metrics-row two-col">
          <div class="metric-card large">
            <div class="skeleton metric-label"></div>
            <div class="skeleton metric-value-large"></div>
            <div class="skeleton metric-subtitle"></div>
            <div class="skeleton metric-remaining"></div>
          </div>
          <div class="metric-card large">
            <div class="skeleton metric-label"></div>
            <div class="skeleton metric-value-large"></div>
            <div class="skeleton metric-subtitle"></div>
          </div>
        </div>
      </div>

      <!-- Opportunities Monthly Section -->
      <div class="fiscal-section">
        <div class="section-header">
          <div class="skeleton section-title"></div>
          <div class="skeleton period-select"></div>
        </div>
        <div class="opportunities-grid">
          <div class="opp-card" *ngFor="let i of [1, 2, 3, 4, 5, 6]">
            <div class="skeleton opp-label"></div>
            <div class="skeleton opp-value"></div>
            <div class="skeleton opp-subtitle"></div>
          </div>
        </div>
      </div>

      <!-- Staff Rankings Section -->
      <div class="fiscal-section">
        <div class="section-header">
          <div class="skeleton section-title"></div>
        </div>
        <div class="ranking-list">
          <div class="ranking-item" *ngFor="let i of [1, 2, 3, 4, 5]">
            <div class="skeleton rank-number"></div>
            <div class="skeleton rank-name"></div>
            <div class="skeleton rank-value"></div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .dashboard-skeleton {
        padding: 0;
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

      /* Fiscal Section */
      .fiscal-section {
        background: white;
        border-radius: 16px;
        padding: 24px;
        margin-bottom: 24px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
      }

      .section-title {
        width: 180px;
        height: 24px;
      }

      .period-select {
        width: 140px;
        height: 36px;
        border-radius: 8px;
      }

      /* Metrics Row */
      .metrics-row {
        display: grid;
        gap: 16px;
      }

      .metrics-row.two-col {
        grid-template-columns: repeat(2, 1fr);
      }

      .metric-card {
        background: #f8f9fa;
        border-radius: 12px;
        padding: 20px;
      }

      .metric-card.large {
        padding: 24px;
      }

      .metric-label {
        width: 120px;
        height: 14px;
        margin-bottom: 12px;
      }

      .metric-value-large {
        width: 180px;
        height: 36px;
        margin-bottom: 10px;
      }

      .metric-subtitle {
        width: 140px;
        height: 14px;
        margin-bottom: 8px;
      }

      .metric-remaining {
        width: 200px;
        height: 14px;
      }

      /* Opportunities Grid */
      .opportunities-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 16px;
      }

      .opp-card {
        background: #f8f9fa;
        border-radius: 12px;
        padding: 20px;
      }

      .opp-label {
        width: 140px;
        height: 14px;
        margin-bottom: 12px;
      }

      .opp-value {
        width: 80px;
        height: 32px;
        margin-bottom: 8px;
      }

      .opp-subtitle {
        width: 120px;
        height: 12px;
      }

      /* Ranking List */
      .ranking-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .ranking-item {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 14px 16px;
        background: #f8f9fa;
        border-radius: 10px;
      }

      .rank-number {
        width: 28px;
        height: 28px;
        border-radius: 50%;
      }

      .rank-name {
        flex: 1;
        height: 16px;
      }

      .rank-value {
        width: 100px;
        height: 16px;
      }

      /* Responsive */
      @media (max-width: 1024px) {
        .opportunities-grid {
          grid-template-columns: repeat(2, 1fr);
        }
      }

      @media (max-width: 768px) {
        .metrics-row.two-col {
          grid-template-columns: 1fr;
        }
        .opportunities-grid {
          grid-template-columns: 1fr;
        }
        .section-header {
          flex-direction: column;
          align-items: flex-start;
          gap: 12px;
        }
      }
    `,
  ],
})
export class DashboardSkeletonComponent {}
