import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import {
  DashboardService,
  DashboardMetrics,
  TopPerformer,
  EmailPerformance,
} from '../services/dashboard.service';
import { ActivityService, Activity } from '../services/activity.service';
import { DateUtilsService } from '../services/date-utils.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  providers: [DashboardService, ActivityService, DateUtilsService],
})
export class DashboardComponent implements OnInit {
  metrics: DashboardMetrics | null = null;
  topPerformers: TopPerformer[] = [];
  emailPerformance: EmailPerformance[] = [];
  recentActivities: Activity[] = [];
  loading = true;
  error: string | null = null;
  lastUpdated: Date | null = null;

  // Date filter
  fromDate: string | undefined;
  toDate: string | undefined;
  dateRange: 'current-month' | 'last-7-days' | 'last-30-days' | 'last-90-days' =
    'current-month';

  // Display options
  topPerformersCount = 10;
  recentActivitiesCount = 10;

  // Additional stats
  emailActivityCount = 0;

  constructor(
    private dashboardService: DashboardService,
    private activityService: ActivityService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    this.applyDateRange();
    this.loadDashboardData();
  }

  applyDateRange() {
    let range;
    switch (this.dateRange) {
      case 'last-7-days':
        range = this.dateUtils.getLast7Days();
        break;
      case 'last-30-days':
        range = this.dateUtils.getLast30Days();
        break;
      case 'last-90-days':
        range = this.dateUtils.getLast90Days();
        break;
      case 'current-month':
      default:
        range = {
          from: this.dateUtils.getCurrentMonthStart(),
          to: this.dateUtils.getCurrentMonthEnd(),
        };
        break;
    }
    this.fromDate = range.from;
    this.toDate = range.to;
    console.log(`Dashboard date range: ${this.fromDate} to ${this.toDate}`);
  }

  onDateRangeChange() {
    this.applyDateRange();
    this.loadDashboardData();
  }

  refreshDashboard() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.loading = true;
    this.error = null;

    // Load all dashboard data
    Promise.all([
      this.loadMetrics(),
      this.loadTopPerformers(),
      this.loadEmailPerformance(),
      this.loadRecentActivities(),
      this.loadEmailActivityCount(),
    ])
      .then(() => {
        this.loading = false;
        this.lastUpdated = new Date();
      })
      .catch((err) => {
        this.error = 'Failed to load dashboard data';
        this.loading = false;
        console.error('Dashboard load error:', err);
      });
  }

  private loadMetrics(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.dashboardService.getMetrics(this.fromDate, this.toDate).subscribe({
        next: (response) => {
          if (response.success) {
            this.metrics = response.data;
            resolve();
          } else {
            reject(response.error);
          }
        },
        error: (err) => reject(err),
      });
    });
  }

  private loadTopPerformers(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.dashboardService
        .getTopPerformers(10, this.fromDate, this.toDate)
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.topPerformers = response.data;
              resolve();
            } else {
              reject(response.error);
            }
          },
          error: (err) => reject(err),
        });
    });
  }

  private loadEmailPerformance(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.dashboardService
        .getEmailPerformance(this.fromDate, this.toDate)
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.emailPerformance = response.data;
              resolve();
            } else {
              reject(response.error);
            }
          },
          error: (err) => reject(err),
        });
    });
  }

  private loadRecentActivities(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.activityService
        .getRecentActivities(
          this.recentActivitiesCount,
          this.fromDate,
          this.toDate
        )
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.recentActivities = response.data;
              resolve();
            } else {
              reject(response.error);
            }
          },
          error: (err) => reject(err),
        });
    });
  }

  getRelativeTime(timestamp: string): string {
    return this.activityService.getRelativeTime(timestamp);
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  getPerformanceColor(score: number): string {
    if (score >= 400) return '#8B5CF6'; // Purple
    if (score >= 300) return '#3B82F6'; // Blue
    if (score >= 200) return '#10B981'; // Green
    return '#6B7280'; // Gray
  }

  formatNumber(num: number): string {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
  }

  getMaxEmailCount(): number {
    if (!this.emailPerformance || this.emailPerformance.length === 0)
      return 500;
    return Math.max(
      ...this.emailPerformance.map((p) =>
        Math.max(p.incomingEmails, p.outgoingEmails)
      )
    );
  }

  getMetricPercentage(value: number, max: number): number {
    if (!max) return 0;
    return Math.min((value / max) * 100, 100);
  }

  getDateRangeLabel(): string {
    if (!this.fromDate || !this.toDate) return '';
    const from = new Date(this.fromDate);
    const to = new Date(this.toDate);
    return `${from.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    })} - ${to.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })}`;
  }

  private loadEmailActivityCount(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.activityService.getActivityCount().subscribe({
        next: (response) => {
          if (response.success) {
            // Estimate email activities as ~70% of total
            this.emailActivityCount = Math.floor(
              (this.metrics?.totalActivities || 0) * 0.7
            );
            resolve();
          } else {
            reject(response.error);
          }
        },
        error: (err) => {
          console.warn('Could not load email activity count:', err);
          resolve(); // Don't fail dashboard load for this
        },
      });
    });
  }

  getAverageEmailsPerStaff(): number {
    if (
      !this.metrics ||
      !this.metrics.totalStaff ||
      this.metrics.totalStaff === 0
    ) {
      return 0;
    }
    return Math.round(this.metrics.totalEmailsSent / this.metrics.totalStaff);
  }
}
