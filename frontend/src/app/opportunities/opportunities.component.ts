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
  loading = false;
  error: string | null = null;

  // Date filter properties
  selectedMonth: string = '';
  fromDate: string = '';
  toDate: string = '';

  constructor(
    private opportunityService: OpportunityService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    // Set default to current month
    const today = new Date();
    this.selectedMonth = `${today.getFullYear()}-${String(
      today.getMonth() + 1
    ).padStart(2, '0')}`;
    this.onMonthChange();
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
        } else {
          this.error = response.message;
        }
      },
      error: (err) => {
        this.error = 'Failed to load opportunity statistics';
        console.error(err);
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

  onMonthChange() {
    if (this.selectedMonth) {
      // Parse the selected month (format: YYYY-MM)
      const [year, month] = this.selectedMonth.split('-').map(Number);

      // Set first day of month
      const firstDay = new Date(year, month - 1, 1);
      this.fromDate = this.dateUtils.formatDateToISO(firstDay);

      // Set last day of month
      const lastDay = new Date(year, month, 0);
      this.toDate = this.dateUtils.formatDateToISO(lastDay);

      this.loadData();
    }
  }

  onDateChange() {
    // When user manually changes dates, clear the month selector
    this.selectedMonth = '';
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
}
