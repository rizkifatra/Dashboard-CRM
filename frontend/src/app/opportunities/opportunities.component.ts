import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  OpportunityService,
  OpportunityStats,
  Opportunity,
} from '../services/opportunity.service';
import { DateUtilsService } from '../services/date-utils.service';

@Component({
  selector: 'app-opportunities',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './opportunities.component.html',
  styleUrls: ['./opportunities.component.css'],
})
export class OpportunitiesComponent implements OnInit {
  stats: OpportunityStats | null = null;
  topOpportunities: Opportunity[] = [];
  wonOpportunities: Opportunity[] = [];
  openOpportunities: Opportunity[] = [];
  lostOpportunities: Opportunity[] = [];
  staffStats: any[] = []; // Array of staff with their opportunity stats
  loading = false;
  error: string | null = null;

  // Date filter properties
  selectedMonth: string = 'current';
  selectedMonthLabel: string = 'This Month';
  monthlyData: any[] = [];
  fromDate: string = '';
  toDate: string = '';

  // View toggles
  showWonOpportunities = false;
  showOpenOpportunities = false;
  showLostOpportunities = false;
  activeView: 'top' | 'won' | 'open' | 'lost' = 'top';

  // For revenue by status chart
  statusData: any = {
    open: 0,
    won: 0,
    lost: 0,
  };

  constructor(
    private opportunityService: OpportunityService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    // Load monthly data first, then load current month data
    this.loadMonthlyData().then(() => {
      this.onMonthChange('current');
    });
  }

  loadData() {
    this.loading = true;
    this.error = null;

    const from = this.fromDate;
    const to = this.toDate;

    // Load statistics
    this.opportunityService.getOpportunityStatistics(from, to).subscribe({
      next: (response) => {
        if (response.success) {
          this.stats = response.data;
          // Update status data for chart
          this.statusData = {
            open:
              response.data.totalEstimatedValue *
              (response.data.openOpportunities /
                Math.max(response.data.totalOpportunities, 1)),
            won: response.data.wonValue || 0,
            lost:
              response.data.totalEstimatedValue -
              (response.data.wonValue || 0) -
              response.data.totalEstimatedValue *
                (response.data.openOpportunities /
                  Math.max(response.data.totalOpportunities, 1)),
          };
        } else {
          this.error = response.message;
        }
      },
      error: (err) => {
        this.error = 'Failed to load opportunity statistics';
        console.error(err);
      },
    });

    // Load staff statistics
    this.opportunityService.getOpportunitiesByStaff(from, to).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('Staff stats response:', response.data);
          // Convert map to array, filter for staff with won opportunities, and sort by won value
          this.staffStats = Object.values(response.data)
            .filter((s: any) => s.wonOpportunities > 0 && s.wonValue > 0) // Only show staff who closed deals
            .sort((a: any, b: any) => b.wonValue - a.wonValue); // Sort by actual won revenue

          console.log('Filtered staff stats:', this.staffStats);
        }
      },
      error: (err) => {
        console.error('Failed to load staff statistics:', err);
      },
    });

    // Load top opportunities
    this.opportunityService.getTopOpportunities(10, from, to).subscribe({
      next: (response) => {
        if (response.success) {
          this.topOpportunities = response.data;
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load top opportunities';
        this.loading = false;
        console.error(err);
      },
    });
  }

  loadWonOpportunities() {
    const from = this.fromDate ? this.fromDate : undefined;
    const to = this.toDate ? this.toDate : undefined;

    this.loading = true;
    this.opportunityService.getAllOpportunities(undefined, from, to).subscribe({
      next: (response) => {
        if (response.success) {
          // Filter for won opportunities (stateCode = 1)
          this.wonOpportunities = response.data
            .filter((opp: Opportunity) => opp.stateCode === 1)
            .sort((a: Opportunity, b: Opportunity) => {
              const valA = a.actualValue || 0;
              const valB = b.actualValue || 0;
              return valB - valA; // Sort by actual value descending
            });
          this.showWonOpportunities = true;
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load won opportunities';
        this.loading = false;
        console.error(err);
      },
    });
  }

  toggleWonOpportunities() {
    if (!this.showWonOpportunities && this.wonOpportunities.length === 0) {
      this.loadWonOpportunities();
    } else {
      this.showWonOpportunities = !this.showWonOpportunities;
    }
  }

  loadOpenOpportunities() {
    const from = this.fromDate ? this.fromDate : undefined;
    const to = this.toDate ? this.toDate : undefined;

    this.loading = true;
    this.opportunityService.getAllOpportunities(undefined, from, to).subscribe({
      next: (response) => {
        if (response.success) {
          // Filter for open opportunities (stateCode = 0)
          this.openOpportunities = response.data
            .filter((opp: Opportunity) => opp.stateCode === 0)
            .sort((a: Opportunity, b: Opportunity) => {
              const valA = a.estimatedValue || 0;
              const valB = b.estimatedValue || 0;
              return valB - valA;
            });
          this.activeView = 'open';
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load open opportunities';
        this.loading = false;
        console.error(err);
      },
    });
  }

  loadLostOpportunities() {
    const from = this.fromDate ? this.fromDate : undefined;
    const to = this.toDate ? this.toDate : undefined;

    this.loading = true;
    this.opportunityService.getAllOpportunities(undefined, from, to).subscribe({
      next: (response) => {
        if (response.success) {
          // Filter for lost opportunities (stateCode = 2)
          this.lostOpportunities = response.data
            .filter((opp: Opportunity) => opp.stateCode === 2)
            .sort((a: Opportunity, b: Opportunity) => {
              const valA = a.estimatedValue || 0;
              const valB = b.estimatedValue || 0;
              return valB - valA;
            });
          this.activeView = 'lost';
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load lost opportunities';
        this.loading = false;
        console.error(err);
      },
    });
  }

  switchView(view: 'top' | 'won' | 'open' | 'lost') {
    this.activeView = view;

    switch (view) {
      case 'won':
        if (this.wonOpportunities.length === 0) {
          this.loadWonOpportunities();
        }
        break;
      case 'open':
        if (this.openOpportunities.length === 0) {
          this.loadOpenOpportunities();
        }
        break;
      case 'lost':
        if (this.lostOpportunities.length === 0) {
          this.loadLostOpportunities();
        }
        break;
      case 'top':
        // Already loaded
        break;
    }
  }

  private loadMonthlyData(): Promise<void> {
    return new Promise((resolve) => {
      // Load the last 12 months of data
      const months = [];
      const today = new Date();
      for (let i = 1; i <= 12; i++) {
        const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
        const monthName = date.toLocaleDateString('en-US', {
          month: 'long',
          year: 'numeric',
        });
        months.push({
          monthLabel: monthName,
          year: date.getFullYear(),
          month: date.getMonth() + 1,
        });
      }
      this.monthlyData = months;
      resolve();
    });
  }

  onMonthChange(monthValue: string): void {
    this.selectedMonth = monthValue;

    if (monthValue === 'current') {
      this.selectedMonthLabel = 'This Month';
      // Set to current month
      const today = new Date();
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
      const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      this.fromDate = this.dateUtils.formatDateToISO(firstDay);
      this.toDate = this.dateUtils.formatDateToISO(lastDay);
    } else {
      // Selected a specific month from dropdown
      const monthIndex = parseInt(monthValue);
      if (monthIndex >= 0 && monthIndex < this.monthlyData.length) {
        const selectedMonthData = this.monthlyData[monthIndex];
        this.selectedMonthLabel = selectedMonthData.monthLabel;
        const firstDay = new Date(
          selectedMonthData.year,
          selectedMonthData.month - 1,
          1
        );
        const lastDay = new Date(
          selectedMonthData.year,
          selectedMonthData.month,
          0
        );
        this.fromDate = this.dateUtils.formatDateToISO(firstDay);
        this.toDate = this.dateUtils.formatDateToISO(lastDay);
      }
    }

    this.loadData();
  }

  formatCurrency(value: number): string {
    return this.opportunityService.formatCurrency(value);
  }

  formatPercentage(value: number): string {
    return this.opportunityService.formatPercentage(value);
  }

  getStatusLabel(stateCode: number): string {
    return this.opportunityService.getStatusLabel(stateCode);
  }

  getStatusColorClass(stateCode: number): string {
    return this.opportunityService.getStatusColorClass(stateCode);
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-MY', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  getDaysUntilClose(dateString: string): number {
    if (!dateString) return 0;
    const closeDate = new Date(dateString);
    const today = new Date();
    const diffTime = closeDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  }

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((value / total) * 100);
  }

  // Format large numbers to millions (M) or thousands (K)
  formatCompactCurrency(value: number): string {
    if (value >= 1000000) {
      return (value / 1000000).toFixed(1) + 'M';
    } else if (value >= 1000) {
      return (value / 1000).toFixed(1) + 'K';
    }
    return value.toFixed(0);
  }

  // Calculate width percentage for bar charts
  getBarWidth(value: number, maxValue: number): number {
    if (!maxValue || maxValue === 0) return 0;
    return Math.min((value / maxValue) * 100, 100);
  }

  // Get max value for scaling bars (based on won revenue)
  getMaxRevenue(): number {
    if (this.staffStats.length === 0) return 0;
    return Math.max(...this.staffStats.map((s: any) => s.wonValue || 0));
  }

  // Get max value for status chart
  getMaxStatusRevenue(): number {
    return Math.max(
      this.statusData.open,
      this.statusData.won,
      this.statusData.lost
    );
  }
}
