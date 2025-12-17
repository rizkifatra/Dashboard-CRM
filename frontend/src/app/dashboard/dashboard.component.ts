import {
  Component,
  OnInit,
  AfterViewInit,
  ElementRef,
  ViewChild,
} from '@angular/core';
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
import {
  OpportunityService,
  OpportunityStats,
} from '../services/opportunity.service';
import { DateUtilsService } from '../services/date-utils.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  providers: [
    DashboardService,
    ActivityService,
    OpportunityService,
    DateUtilsService,
  ],
})
export class DashboardComponent implements OnInit, AfterViewInit {
  metrics: DashboardMetrics | null = null;
  topPerformers: TopPerformer[] = [];
  emailPerformance: EmailPerformance[] = [];
  recentActivities: Activity[] = [];
  opportunityStats: OpportunityStats | null = null;
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

  // Chart properties
  @ViewChild('lineChart') lineChartRef!: ElementRef<HTMLCanvasElement>;
  private chart: Chart | null = null;
  monthlyTrends: any[] = [];

  constructor(
    private dashboardService: DashboardService,
    private activityService: ActivityService,
    private opportunityService: OpportunityService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    this.applyDateRange();
    this.loadDashboardData();
  }

  ngAfterViewInit() {
    // Load monthly trends after view is initialized
    // This ensures the canvas element exists
    this.loadMonthlyTrends();
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
      this.loadOpportunityStats(),
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

  private loadOpportunityStats(): Promise<void> {
    return new Promise((resolve) => {
      this.opportunityService
        .getOpportunityStatistics(this.fromDate, this.toDate)
        .subscribe({
          next: (response) => {
            if (response.success) {
              this.opportunityStats = response.data;
            } else {
              console.warn('Failed to load opportunity stats:', response.error);
              this.opportunityStats = null;
            }
            resolve();
          },
          error: (err) => {
            console.error('Error loading opportunity stats:', err);
            this.opportunityStats = null;
            resolve(); // Resolve anyway to not block other data
          },
        });
    });
  }

  getRelativeTime(timestamp: string): string {
    return this.activityService.getRelativeTime(timestamp);
  }

  formatCurrency(value: number): string {
    return this.opportunityService.formatCurrency(value);
  }

  formatPercentage(value: number): string {
    return this.opportunityService.formatPercentage(value);
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

  loadMonthlyTrends() {
    console.log('Starting to load monthly trends...');
    this.opportunityService.getMonthlyTrends(6).subscribe({
      next: (response) => {
        console.log('Monthly trends API response:', response);
        if (response.success && response.data) {
          this.monthlyTrends = response.data;
          console.log('Monthly trends data:', this.monthlyTrends);
          console.log('Number of months:', this.monthlyTrends.length);
          // Wait for dashboard loading to complete before initializing chart
          this.waitForCanvasAndInitialize();
        } else {
          console.error(
            'Failed to load trends - success flag or no data:',
            response
          );
        }
      },
      error: (err) => {
        console.error('API Error loading monthly trends:', err);
        console.error('Error status:', err.status);
        console.error('Error message:', err.message);
      },
    });
  }

  private waitForCanvasAndInitialize() {
    // Check if canvas is ready, retry if not
    const checkCanvas = () => {
      if (this.lineChartRef && this.lineChartRef.nativeElement) {
        console.log('Canvas found, initializing chart...');
        this.initializeChart();
      } else if (!this.loading) {
        // If not loading but still no canvas, wait a bit and try again
        console.log('Canvas not ready yet, retrying...');
        setTimeout(checkCanvas, 50);
      } else {
        // Still loading, wait for loading to finish
        console.log('Dashboard still loading, waiting...');
        setTimeout(checkCanvas, 100);
      }
    };
    checkCanvas();
  }

  initializeChart() {
    if (
      !this.lineChartRef ||
      !this.lineChartRef.nativeElement ||
      this.monthlyTrends.length === 0
    ) {
      console.log('Chart initialization skipped:', {
        hasRef: !!this.lineChartRef,
        hasElement: !!this.lineChartRef?.nativeElement,
        trendsLength: this.monthlyTrends.length,
      });
      return;
    }

    const ctx = this.lineChartRef.nativeElement.getContext('2d');
    if (!ctx) {
      console.error('Failed to get canvas context');
      return;
    }

    if (this.chart) {
      console.log('Destroying existing chart');
      this.chart.destroy();
    }

    const labels = this.monthlyTrends.map((t) => t.month);
    console.log('Chart labels:', labels);
    console.log('Chart data sample:', {
      total: this.monthlyTrends.map((t) => t.total),
      won: this.monthlyTrends.map((t) => t.won),
      open: this.monthlyTrends.map((t) => t.open),
      winRate: this.monthlyTrends.map((t) => t.winRate),
    });

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Total Opportunities',
            data: this.monthlyTrends.map((t) => t.total),
            borderColor: '#6D5DFF',
            backgroundColor: 'rgba(109, 93, 255, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            pointRadius: 4,
            pointHoverRadius: 6,
          },
          {
            label: 'Won Opportunities',
            data: this.monthlyTrends.map((t) => t.won),
            borderColor: '#28C76F',
            backgroundColor: 'rgba(40, 199, 111, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            pointRadius: 4,
            pointHoverRadius: 6,
          },
          {
            label: 'Open Opportunities',
            data: this.monthlyTrends.map((t) => t.open),
            borderColor: '#FF9F43',
            backgroundColor: 'rgba(255, 159, 67, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            pointRadius: 4,
            pointHoverRadius: 6,
          },
          {
            label: 'Win Rate (%)',
            data: this.monthlyTrends.map((t) => t.winRate),
            borderColor: '#00A8E8',
            backgroundColor: 'rgba(0, 168, 232, 0.1)',
            borderWidth: 2,
            tension: 0.4,
            fill: false,
            pointRadius: 4,
            pointHoverRadius: 6,
            yAxisID: 'y1',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            display: true,
            position: 'top',
            labels: {
              font: {
                family:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
                size: 12,
              },
              color: '#1F2937',
              usePointStyle: true,
              padding: 15,
            },
          },
          tooltip: {
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            titleColor: '#1F2937',
            bodyColor: '#6B7280',
            borderColor: '#E5E7EB',
            borderWidth: 1,
            padding: 12,
            boxPadding: 6,
            usePointStyle: true,
            callbacks: {
              label: function (context) {
                let label = context.dataset.label || '';
                if (label) {
                  label += ': ';
                }
                if (context.parsed.y !== null) {
                  if (context.dataset.yAxisID === 'y1') {
                    label += context.parsed.y.toFixed(1) + '%';
                  } else {
                    label += context.parsed.y;
                  }
                }
                return label;
              },
            },
          },
        },
        scales: {
          x: {
            grid: {
              display: false,
            },
            ticks: {
              font: {
                family:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
                size: 11,
              },
              color: '#6B7280',
            },
          },
          y: {
            position: 'left',
            grid: {
              color: '#F3F4F6',
            },
            ticks: {
              font: {
                family:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
                size: 11,
              },
              color: '#6B7280',
            },
            title: {
              display: true,
              text: 'Number of Opportunities',
              font: {
                family:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
                size: 12,
                weight: 500,
              },
              color: '#374151',
            },
          },
          y1: {
            position: 'right',
            grid: {
              display: false,
            },
            ticks: {
              font: {
                family:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
                size: 11,
              },
              color: '#6B7280',
              callback: function (value) {
                return value + '%';
              },
            },
            title: {
              display: true,
              text: 'Win Rate (%)',
              font: {
                family:
                  '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
                size: 12,
                weight: 500,
              },
              color: '#374151',
            },
          },
        },
      },
    });
    console.log('Chart created successfully:', this.chart);
  }
}
