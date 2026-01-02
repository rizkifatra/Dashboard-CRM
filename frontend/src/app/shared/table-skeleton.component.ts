import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonLoaderComponent } from './skeleton-loader.component';

@Component({
  selector: 'app-table-skeleton',
  standalone: true,
  imports: [CommonModule, SkeletonLoaderComponent],
  template: `
    <div class="table-skeleton-container">
      <!-- Header Skeleton -->
      <div class="table-header-skeleton">
        <div class="header-left">
          <app-skeleton-loader type="title" width="180px"></app-skeleton-loader>
        </div>
        <div class="header-right">
          <app-skeleton-loader
            type="button"
            width="100px"
            height="36px"
          ></app-skeleton-loader>
          <app-skeleton-loader
            type="button"
            width="100px"
            height="36px"
          ></app-skeleton-loader>
        </div>
      </div>

      <!-- Stats Cards Skeleton -->
      <div class="stats-skeleton">
        <div class="stat-card-skeleton" *ngFor="let i of [1, 2, 3, 4]">
          <app-skeleton-loader
            type="text"
            width="80px"
            height="12px"
          ></app-skeleton-loader>
          <app-skeleton-loader
            type="text"
            width="60px"
            height="32px"
          ></app-skeleton-loader>
        </div>
      </div>

      <!-- Table Skeleton -->
      <div class="table-content-skeleton">
        <div class="table-header-row">
          <app-skeleton-loader
            type="text"
            width="120px"
            height="14px"
            *ngFor="let i of [1, 2, 3, 4, 5]"
          ></app-skeleton-loader>
        </div>
        <div class="table-rows">
          <div
            class="table-row-skeleton"
            *ngFor="let i of [1, 2, 3, 4, 5, 6, 7, 8]"
          >
            <app-skeleton-loader
              type="text"
              width="80%"
              height="14px"
            ></app-skeleton-loader>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .table-skeleton-container {
        padding: 28px 36px;
        background: #f5f5f7;
        min-height: 100vh;
      }

      .table-header-skeleton {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
      }

      .header-right {
        display: flex;
        gap: 12px;
      }

      .stats-skeleton {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 16px;
        margin-bottom: 24px;
      }

      .stat-card-skeleton {
        background: white;
        padding: 20px;
        border-radius: 12px;
        display: flex;
        flex-direction: column;
        gap: 12px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .table-content-skeleton {
        background: white;
        border-radius: 12px;
        padding: 24px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      }

      .table-header-row {
        display: grid;
        grid-template-columns: repeat(5, 1fr);
        gap: 16px;
        padding-bottom: 16px;
        border-bottom: 1px solid #e5e7eb;
        margin-bottom: 16px;
      }

      .table-rows {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }

      .table-row-skeleton {
        padding: 12px 0;
        border-bottom: 1px solid #f3f4f6;
      }
    `,
  ],
})
export class TableSkeletonComponent {}
