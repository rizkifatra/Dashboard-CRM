import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OpportunitiesSkeletonComponent } from '../shared/opportunities-skeleton.component';
import {
  OpportunityService,
  OpportunityStats,
  Opportunity,
} from '../services/opportunity.service';
import { DateUtilsService } from '../services/date-utils.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-opportunities',
  standalone: true,
  imports: [CommonModule, FormsModule, OpportunitiesSkeletonComponent],
  templateUrl: './opportunities.component.html',
  styleUrls: ['./opportunities.component.css'],
})
export class OpportunitiesComponent implements OnInit {
  // Data properties
  opportunities: Opportunity[] = [];
  stats: OpportunityStats | null = null;

  // Loading states
  loading = false;
  loadingMore = false;
  error: string | null = null;

  // Search
  searchTerm = '';
  private searchSubject = new Subject<string>();

  // Pagination
  private skip = 0;
  private readonly pageSize = 50;
  hasMore = true; // Enable infinite scroll like Accounts page
  totalCount = 0;

  // Last updated tracking
  lastUpdated: Date | null = null;

  constructor(
    private opportunityService: OpportunityService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    this.loadStats();
    this.loadOpportunities();

    // Setup search debounce - wait 500ms after user stops typing
    this.searchSubject
      .pipe(debounceTime(500), distinctUntilChanged())
      .subscribe((searchTerm) => {
        // Reset pagination and reload opportunities
        this.skip = 0;
        this.hasMore = true;
        this.opportunities = [];
        this.loadOpportunities();
      });
  }

  loadStats() {
    this.opportunityService.getOpportunityStatistics().subscribe({
      next: (response) => {
        if (response.success) {
          this.stats = response.data;
        }
      },
      error: (err) => {
        console.error('Failed to load stats:', err);
      },
    });
  }

  loadOpportunities(append = false) {
    if (append) {
      this.loadingMore = true;
    } else {
      this.loading = true;
      this.skip = 0;
      this.opportunities = [];
    }

    this.error = null;

    this.opportunityService
      .getAllOpportunities(this.skip, this.pageSize, this.searchTerm)
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            const newOpportunities = response.data;
            this.opportunities = append
              ? [...this.opportunities, ...newOpportunities]
              : newOpportunities;
            this.hasMore = newOpportunities.length === this.pageSize;
            this.skip += newOpportunities.length;
            this.lastUpdated = new Date();
          } else {
            this.error = response.message;
          }

          this.loading = false;
          this.loadingMore = false;
        },
        error: (err) => {
          this.error = 'Failed to load opportunities';
          this.loading = false;
          this.loadingMore = false;
          console.error(err);
        },
      });
  }

  onTableScroll(event: Event) {
    if (this.loadingMore || !this.hasMore) {
      return;
    }

    const element = event.target as HTMLElement;
    const threshold = 200;
    const position = element.scrollTop + element.clientHeight;
    const height = element.scrollHeight;

    if (position > height - threshold) {
      this.loadOpportunities(true);
    }
  }

  openOpportunityDetails(opportunity: Opportunity) {
    // TODO: Implement opportunity details modal or navigation
    console.log('Opening opportunity details:', opportunity);
  }

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((value / total) * 100);
  }

  onSearchChange() {
    // Use debounced search to avoid too many API calls while typing
    this.searchSubject.next(this.searchTerm);
  }

  clearSearch() {
    this.searchTerm = '';
    this.skip = 0;
    this.hasMore = true;
    this.opportunities = [];
    this.loadOpportunities();
  }

  formatCurrency(value: number): string {
    return this.opportunityService.formatCurrency(value);
  }

  formatDate(dateString: string): string {
    return this.dateUtils.formatDate(dateString);
  }

  getOwnerInitials(ownerName: string): string {
    if (!ownerName) return '?';
    const names = ownerName.trim().split(' ');
    if (names.length >= 2) {
      return (names[0][0] + names[names.length - 1][0]).toUpperCase();
    }
    return names[0][0].toUpperCase();
  }

  getStatusLabel(stateCode: number | undefined): string {
    switch (stateCode) {
      case 0:
        return 'Open';
      case 1:
        return 'Won';
      case 2:
        return 'Lost';
      default:
        return 'Unknown';
    }
  }

  getStatusClass(stateCode: number | undefined): string {
    switch (stateCode) {
      case 0:
        return 'status-open';
      case 1:
        return 'status-won';
      case 2:
        return 'status-lost';
      default:
        return 'status-unknown';
    }
  }

  getLastUpdatedText(): string {
    if (!this.lastUpdated) return '';

    const now = new Date();
    const diff = now.getTime() - this.lastUpdated.getTime();
    const minutes = Math.floor(diff / 60000);

    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} hour${hours > 1 ? 's' : ''} ago`;

    return this.dateUtils.formatDate(this.lastUpdated.toISOString());
  }
}
