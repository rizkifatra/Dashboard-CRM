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

  // Quarter
  selectedQuarter = 'current';
  quarterMetrics: QuarterMetrics | null = null;
  quarterTopStaff: StaffPerformance[] = [];

  // Monthly Opportunities
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

  // Quarter Opportunities
  selectedOppQuarter = 'current';
  quarterOpps: OpportunityStats | null = null;

  // Fiscal Year Opportunities
  selectedFiscalYearOpps = 'current';
  fiscalYearOpps: OpportunityStats | null = null;

  // Unreplied Emails
  unrepliedCount = 0;

  constructor(private http: HttpClient, private router: Router) {
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
    target: number | undefined
  ): number {
    if (!target || target === 0) return 0;
    if (!achieved) return 0;
    return Math.round((achieved / target) * 100);
  }

  ngOnInit() {
    this.loadAllData();
    this.loadUnrepliedEmails();
  }

  /**
   * Load all dashboard data in parallel for optimal performance
   * Loads: Fiscal Year, Quarter, Monthly Opportunities, Quarter Opportunities, and Fiscal Year Opportunities
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
   * Includes: Target Revenue, Won Revenue, and Top 5 Staff
   */
  loadFiscalYearData(): Promise<void> {
    const fiscalYear =
      this.selectedFiscalYear === 'current' ? null : this.selectedFiscalYear;

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

    // Load top staff
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

    return Promise.all([metricsPromise, staffPromise]).then(() => {});
  }

  /**
   * Load quarterly metrics and top staff performance
   * Includes: Quarter Target Revenue, Won Revenue, and Top Staff
   */
  loadQuarterData(): Promise<void> {
    const quarter =
      this.selectedQuarter === 'current' ? null : this.selectedQuarter;

    // Load metrics
    const metricsUrl = quarter
      ? `${this.apiUrl}/quarterly-metrics?quarter=${quarter}`
      : `${this.apiUrl}/quarterly-metrics`;

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

    // Load top staff
    const staffUrl = quarter
      ? `${this.apiUrl}/top-staff-performance?period=quarter&quarter=${quarter}`
      : `${this.apiUrl}/top-staff-performance?period=quarter`;

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

    return Promise.all([metricsPromise, staffPromise]).then(() => {});
  }

  /**
   * Load monthly opportunity statistics
   * Includes: Total, Won, Lost, and Open Opportunities for the month
   */
  loadMonthlyOpportunities(): Promise<void> {
    const month = this.selectedMonth === 'current' ? null : this.selectedMonth;
    const url = month
      ? `${this.apiUrl}/monthly-opportunities?month=${month}`
      : `${this.apiUrl}/monthly-opportunities`;

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
   * Includes: Total, Won, Lost, and Open Opportunities for the quarter
   */
  loadQuarterOpportunities(): Promise<void> {
    const quarter =
      this.selectedOppQuarter === 'current' ? null : this.selectedOppQuarter;
    const url = quarter
      ? `${this.apiUrl}/quarterly-opportunities?quarter=${quarter}`
      : `${this.apiUrl}/quarterly-opportunities`;

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
   * Includes: Total, Won, Lost, and Open Opportunities for the fiscal year
   */
  loadFiscalYearOpportunities(): Promise<void> {
    const fiscalYear =
      this.selectedFiscalYearOpps === 'current'
        ? null
        : this.selectedFiscalYearOpps;
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
        'http://localhost:8080/api/activities/unreplied?maxHoursOld=168'
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
