import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonLoaderComponent } from './skeleton-loader.component';

@Component({
  selector: 'app-dashboard-skeleton',
  standalone: true,
  imports: [CommonModule, SkeletonLoaderComponent],
  template: `
    <div class="skeleton-container">
      <!-- Header Skeleton -->
      <div class="skeleton-header">
        <div class="header-left">
          <app-skeleton-loader type="title" width="200px"></app-skeleton-loader>
          <app-skeleton-loader
            type="text"
            width="150px"
            height="14px"
          ></app-skeleton-loader>
        </div>
        <div class="header-right">
          <app-skeleton-loader type="button"></app-skeleton-loader>
        </div>
      </div>

      <!-- Metrics Section -->
      <div class="skeleton-section">
        <div class="section-header-skeleton">
          <app-skeleton-loader type="text" width="180px"></app-skeleton-loader>
          <app-skeleton-loader
            type="button"
            width="150px"
          ></app-skeleton-loader>
        </div>
        <div class="metrics-grid">
          <app-skeleton-loader
            type="metric-card"
            *ngFor="let i of [1, 2]"
          ></app-skeleton-loader>
        </div>
        <div class="staff-skeleton">
          <app-skeleton-loader
            type="text"
            width="140px"
            height="18px"
          ></app-skeleton-loader>
          <div class="staff-items">
            <app-skeleton-loader
              type="table-row"
              *ngFor="let i of [1, 2, 3]"
            ></app-skeleton-loader>
          </div>
        </div>
      </div>

      <!-- Second Section -->
      <div class="skeleton-section">
        <div class="section-header-skeleton">
          <app-skeleton-loader type="text" width="160px"></app-skeleton-loader>
          <app-skeleton-loader
            type="button"
            width="150px"
          ></app-skeleton-loader>
        </div>
        <div class="metrics-grid">
          <app-skeleton-loader
            type="metric-card"
            *ngFor="let i of [1, 2]"
          ></app-skeleton-loader>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .skeleton-container {
        padding: 28px 36px 36px 36px;
        background: #f5f5f7;
        min-height: 100vh;
      }

      .skeleton-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 0 16px 0;
        border-bottom: 1px solid #e5e7eb;
        margin-bottom: 24px;
      }

      .header-left {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .header-right {
        display: flex;
        gap: 15px;
      }

      .skeleton-section {
        background: white;
        border-radius: 16px;
        padding: 24px;
        margin-bottom: 24px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .section-header-skeleton {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
      }

      .metrics-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 16px;
        margin-bottom: 24px;
      }

      .staff-skeleton {
        margin-top: 24px;
      }

      .staff-items {
        margin-top: 16px;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
    `,
  ],
})
export class DashboardSkeletonComponent {}
