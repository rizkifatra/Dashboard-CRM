import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { DashboardSkeletonComponent } from '../shared/dashboard-skeleton.component';

interface FiscalYearMetrics {
  fiscalYear: string;
  targetRevenue: number;
  wonRevenue: number;
  estimatedRevenue: number;
  lostRevenue: number;
}

interface QuarterMetrics {
  quarter: string;
  targetRevenue: number;
  wonRevenue: number;
  estimatedRevenue: number;
  lostRevenue: number;
}

interface OpportunityStats {
  totalOpportunities: number;
  wonOpportunities: number;
  lostOpportunities: number;
  openOpportunities: number;
}

interface StaffPerformance {
  ownerId: string;
  ownerName: string;
  wonCount: number;
  wonRevenue: number;
}

@Component({
  selector: 'app-fiscal-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DashboardSkeletonComponent],
  templateUrl: './fiscal-dashboard.component.html',
  styleUrls: ['./fiscal-dashboard.component.css'],
})
export class FiscalDashboardComponent implements OnInit {
  private apiUrl = 'http://localhost:8080/api/dashboard';

  loading = true;
  error: string | null = null;
  lastUpdated: Date | null = null;

  // Fiscal Year
  selectedFiscalYear = 'current';
  fiscalYears: number[] = [];
  fiscalYearMetrics: FiscalYearMetrics | null = null;
  fiscalYearTopStaff: StaffPerformance[] = [];

  // Quarter - now linked to fiscal year
  selectedQuarter = 'current';
  quarterMetrics: QuarterMetrics | null = null;
  quarterTopStaff: StaffPerformance[] = [];

  // Monthly Opportunities - now linked to quarter
  selectedMonth = 'current';
  months = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ];
  monthlyOpps: OpportunityStats | null = null;

  // Quarter Opportunities - uses selectedQuarter
  quarterOpps: OpportunityStats | null = null;

  // Fiscal Year Opportunities - uses selectedFiscalYear
  fiscalYearOpps: OpportunityStats | null = null;

  // Unreplied Emails
  unrepliedCount = 0;

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {
    // Generate fiscal years (current and past 3 years)
    const currentYear = new Date().getFullYear();
    const currentMonth = new Date().getMonth() + 1;

    // If before June, current fiscal year started last year
    const startYear = currentMonth < 6 ? currentYear - 1 : currentYear;

    for (let i = 0; i < 4; i++) {
      this.fiscalYears.push(startYear - i);
    }
  }

  // Helper method to calculate percentage
  calculatePercentage(
    achieved: number | undefined,
    target: number | undefined,
  ): number {
    if (!target || target === 0) return 0;
    if (!achieved) return 0;
    return Math.round((achieved / target) * 100);
  }

  // Helper method to calculate remaining amount to reach target
  getRemainingToTarget(
    target: number | undefined,
    achieved: number | undefined,
  ): number {
    if (!target) return 0;
    if (!achieved) return target;
    const remaining = target - achieved;
    return remaining > 0 ? remaining : 0;
  }

  // Helper method to get current fiscal year display text
  getFiscalYearText(): string {
    if (this.selectedFiscalYear === 'current') {
      return 'This Year';
    }
    const year = Number(this.selectedFiscalYear);
    return `FY${year}-${year + 1}`;
  }

  // Helper method to get current quarter display text
  getQuarterText(): string {
    if (this.selectedQuarter === 'current') {
      return 'This Quarter';
    }
    return `Q${this.selectedQuarter}`;
  }

  // Helper method to get current month display text
  getMonthText(): string {
    if (this.selectedMonth === 'current') {
      return 'This Month';
    }
    const monthIndex = Number(this.selectedMonth) - 1;
    return this.months[monthIndex];
  }

  // Helper method to check if any filters are active
  hasActiveFilters(): boolean {
    return (
      this.selectedFiscalYear !== 'current' ||
      this.selectedQuarter !== 'current'
    );
  }

  // Helper method to get filter description text
  getFilterDescription(): string {
    const filters: string[] = [];

    if (this.selectedFiscalYear !== 'current') {
      const year = Number(this.selectedFiscalYear);
      filters.push(`FY${year}-${year + 1}`);
    }

    if (this.selectedQuarter !== 'current') {
      filters.push(`Q${this.selectedQuarter}`);
    }

    if (this.selectedMonth !== 'current') {
      filters.push(this.getMonthText());
    }

    return filters.length > 0 ? `📊 Filtered by: ${filters.join(' • ')}` : '';
  }

  ngOnInit() {
    this.loadAllData();
    this.loadUnrepliedEmails();
  }

  /**
   * Load all dashboard data in parallel for optimal performance
   */
  loadAllData() {
    this.loading = true;
    this.error = null;

    // Load all data in parallel to minimize loading time
    Promise.all([
      this.loadFiscalYearData(),
      this.loadQuarterData(),
      this.loadMonthlyOpportunities(),
      this.loadQuarterOpportunities(),
      this.loadFiscalYearOpportunities(),
    ])
      .then(() => {
        this.loading = false;
        this.lastUpdated = new Date();
      })
      .catch((error) => {
        this.loading = false;
        this.error = 'Failed to load dashboard data. Please try again.';
        console.error('Error loading dashboard:', error);
      });
  }

  refreshDashboard() {
    this.loadAllData();
  }

  /**
   * Load fiscal year metrics and top staff performance
   * Also updates quarter and month data to match the selected fiscal year
   */
  loadFiscalYearData(): Promise<void> {
    const fiscalYear =
      this.selectedFiscalYear === 'current' ? null : this.selectedFiscalYear;

    // Reset quarter and month to current when fiscal year changes
    this.selectedQuarter = 'current';
    this.selectedMonth = 'current';

    // Load metrics
    const metricsUrl = fiscalYear
      ? `${this.apiUrl}/fiscal-year-metrics?fiscalYear=${fiscalYear}`
      : `${this.apiUrl}/fiscal-year-metrics`;

    const metricsPromise = this.http
      .get<any>(metricsUrl)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.fiscalYearMetrics = response.data;
          console.log('Fiscal year metrics loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading fiscal year metrics:', error);
        throw error;
      });

    // Load top staff for fiscal year
    const staffUrl = fiscalYear
      ? `${this.apiUrl}/top-staff-performance?period=fiscal-year&fiscalYear=${fiscalYear}`
      : `${this.apiUrl}/top-staff-performance?period=fiscal-year`;

    const staffPromise = this.http
      .get<any>(staffUrl)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.fiscalYearTopStaff = response.data || [];
          console.log('Fiscal year top staff loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading fiscal year staff:', error);
        throw error;
      });

    return Promise.all([metricsPromise, staffPromise]).then(() => {
      // After fiscal year loads, also reload dependent data
      return Promise.all([
        this.loadQuarterData(),
        this.loadMonthlyOpportunities(),
        this.loadQuarterOpportunities(),
        this.loadFiscalYearOpportunities(),
      ]).then(() => {});
    });
  }

  /**
   * Load quarterly metrics and top staff performance
   * Uses the selected fiscal year context
   */
  loadQuarterData(): Promise<void> {
    const quarter =
      this.selectedQuarter === 'current' ? null : this.selectedQuarter;
    const fiscalYear =
      this.selectedFiscalYear === 'current' ? null : this.selectedFiscalYear;

    // Reset month to current when quarter changes
    this.selectedMonth = 'current';

    // Build URL with fiscal year context
    let metricsUrl = `${this.apiUrl}/quarterly-metrics`;
    const params: string[] = [];
    if (quarter) params.push(`quarter=${quarter}`);
    if (fiscalYear) params.push(`fiscalYear=${fiscalYear}`);
    if (params.length > 0) metricsUrl += '?' + params.join('&');

    const metricsPromise = this.http
      .get<any>(metricsUrl)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.quarterMetrics = response.data;
          console.log('Quarter metrics loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading quarter metrics:', error);
        throw error;
      });

    // Build staff URL with fiscal year context
    let staffUrl = `${this.apiUrl}/top-staff-performance?period=quarter`;
    const staffParams: string[] = [];
    if (quarter) staffParams.push(`quarter=${quarter}`);
    if (fiscalYear) staffParams.push(`fiscalYear=${fiscalYear}`);
    if (staffParams.length > 0) staffUrl += '&' + staffParams.join('&');

    const staffPromise = this.http
      .get<any>(staffUrl)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.quarterTopStaff = response.data || [];
          console.log('Quarter top staff loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading quarter staff:', error);
        throw error;
      });

    return Promise.all([metricsPromise, staffPromise]).then(() => {
      // After quarter loads, also reload month opportunities
      return this.loadMonthlyOpportunities().then(() => {});
    });
  }

  /**
   * Load monthly opportunity statistics
   * Uses the selected fiscal year and quarter context
   */
  loadMonthlyOpportunities(): Promise<void> {
    const month = this.selectedMonth === 'current' ? null : this.selectedMonth;
    const quarter =
      this.selectedQuarter === 'current' ? null : this.selectedQuarter;
    const fiscalYear =
      this.selectedFiscalYear === 'current' ? null : this.selectedFiscalYear;

    // Build URL with context
    let url = `${this.apiUrl}/monthly-opportunities`;
    const params: string[] = [];
    if (month) params.push(`month=${month}`);
    if (quarter) params.push(`quarter=${quarter}`);
    if (fiscalYear) params.push(`fiscalYear=${fiscalYear}`);
    if (params.length > 0) url += '?' + params.join('&');

    return this.http
      .get<any>(url)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.monthlyOpps = response.data;
          console.log('Monthly opportunities loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading monthly opportunities:', error);
        throw error;
      });
  }

  /**
   * Load quarterly opportunity statistics
   * Uses the selected fiscal year context
   */
  loadQuarterOpportunities(): Promise<void> {
    const quarter =
      this.selectedQuarter === 'current' ? null : this.selectedQuarter;
    const fiscalYear =
      this.selectedFiscalYear === 'current' ? null : this.selectedFiscalYear;

    // Build URL with context
    let url = `${this.apiUrl}/quarterly-opportunities`;
    const params: string[] = [];
    if (quarter) params.push(`quarter=${quarter}`);
    if (fiscalYear) params.push(`fiscalYear=${fiscalYear}`);
    if (params.length > 0) url += '?' + params.join('&');

    return this.http
      .get<any>(url)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.quarterOpps = response.data;
          console.log('Quarter opportunities loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading quarter opportunities:', error);
        throw error;
      });
  }

  /**
   * Load fiscal year opportunity statistics
   * Uses the selected fiscal year context
   */
  loadFiscalYearOpportunities(): Promise<void> {
    const fiscalYear =
      this.selectedFiscalYear === 'current' ? null : this.selectedFiscalYear;

    const url = fiscalYear
      ? `${this.apiUrl}/fiscal-year-opportunities?fiscalYear=${fiscalYear}`
      : `${this.apiUrl}/fiscal-year-opportunities`;

    return this.http
      .get<any>(url)
      .toPromise()
      .then((response) => {
        if (response?.success) {
          this.fiscalYearOpps = response.data;
          console.log('Fiscal year opportunities loaded:', response.data);
        }
      })
      .catch((error) => {
        console.error('Error loading fiscal year opportunities:', error);
        throw error;
      });
  }

  /**
   * Load unreplied emails count
   */
  loadUnrepliedEmails() {
    this.http
      .get<any>(
        'http://localhost:8080/api/activities/unreplied?maxHoursOld=168',
      )
      .subscribe({
        next: (response) => {
          this.unrepliedCount = response.data?.length || 0;
        },
        error: (err) => {
          console.error('Error loading unreplied emails:', err);
          this.unrepliedCount = 0;
        },
      });
  }

  /**
   * Navigate to activities page with unreplied filter
   */
  navigateToUnrepliedEmails() {
    this.router.navigate(['/activities'], {
      queryParams: { filter: 'unreplied' },
    });
  }
}
