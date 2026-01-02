import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [ngClass]="['skeleton', type, size]" [ngStyle]="customStyle">
      <div class="skeleton-shimmer"></div>
    </div>
  `,
  styles: [
    `
      .skeleton {
        position: relative;
        overflow: hidden;
        background: linear-gradient(
          90deg,
          #f0f0f0 25%,
          #e0e0e0 50%,
          #f0f0f0 75%
        );
        background-size: 200% 100%;
        animation: loading 1.5s ease-in-out infinite;
        border-radius: 8px;
      }

      @keyframes loading {
        0% {
          background-position: 200% 0;
        }
        100% {
          background-position: -200% 0;
        }
      }

      .skeleton-shimmer {
        position: absolute;
        top: 0;
        left: -100%;
        height: 100%;
        width: 100%;
        background: linear-gradient(
          90deg,
          rgba(255, 255, 255, 0) 0%,
          rgba(255, 255, 255, 0.3) 50%,
          rgba(255, 255, 255, 0) 100%
        );
        animation: shimmer 1.5s infinite;
      }

      @keyframes shimmer {
        0% {
          left: -100%;
        }
        100% {
          left: 100%;
        }
      }

      /* Types */
      .text {
        height: 16px;
        width: 100%;
        border-radius: 4px;
      }

      .title {
        height: 28px;
        width: 60%;
        border-radius: 6px;
      }

      .card {
        height: 120px;
        width: 100%;
        border-radius: 12px;
      }

      .avatar {
        border-radius: 50%;
        width: 48px;
        height: 48px;
      }

      .button {
        height: 40px;
        width: 120px;
        border-radius: 8px;
      }

      .table-row {
        height: 48px;
        width: 100%;
        border-radius: 6px;
        margin-bottom: 8px;
      }

      .metric-card {
        height: 140px;
        width: 100%;
        border-radius: 16px;
      }

      /* Sizes */
      .small {
        height: 12px;
      }

      .medium {
        height: 20px;
      }

      .large {
        height: 32px;
      }

      .full {
        width: 100%;
      }
    `,
  ],
})
export class SkeletonLoaderComponent {
  @Input() type:
    | 'text'
    | 'title'
    | 'card'
    | 'avatar'
    | 'button'
    | 'table-row'
    | 'metric-card' = 'text';
  @Input() size: 'small' | 'medium' | 'large' | 'full' = 'medium';
  @Input() width?: string;
  @Input() height?: string;
  @Input() borderRadius?: string;

  get customStyle() {
    return {
      width: this.width || undefined,
      height: this.height || undefined,
      borderRadius: this.borderRadius || undefined,
    };
  }
}
