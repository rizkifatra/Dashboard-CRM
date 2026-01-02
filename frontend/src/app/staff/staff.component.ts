import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StaffService, Staff } from '../services/staff.service';
import { DateUtilsService } from '../services/date-utils.service';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';

@Component({
  selector: 'app-staff',
  standalone: true,
  imports: [CommonModule, FormsModule, TableSkeletonComponent],
  templateUrl: './staff.component.html',
  styleUrls: ['./staff.component.css'],
})
export class StaffComponent implements OnInit {
  staffList: Staff[] = [];
  loading = true;
  error: string | null = null;
  totalCount = 0;

  // Search and filters
  searchTerm = '';
  isSearching = false;
  includeStats = true;
  dateRange: 'current-month' | 'last-7-days' | 'last-30-days' | 'last-90-days' =
    'current-month';

  constructor(
    private staffService: StaffService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    this.loadStaff();
    this.loadStaffCount();
  }

  loadStaff() {
    this.loading = true;
    this.error = null;

    const { from, to } = this.getDateRange();

    // Use getAllStaff (unfiltered) to show ALL staff members
    this.staffService.getAllStaff(this.includeStats, from, to).subscribe({
      next: (response) => {
        if (response.success) {
          this.staffList = response.data;
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = 'Failed to load staff data';
        this.loading = false;
        console.error('Error loading staff:', err);
      },
    });
  }

  loadStaffCount() {
    this.staffService.getStaffCount().subscribe({
      next: (response) => {
        if (response.success) {
          this.totalCount = response.data;
        }
      },
      error: (err) => {
        console.error('Error loading staff count:', err);
      },
    });
  }

  searchStaff() {
    if (!this.searchTerm || this.searchTerm.trim().length < 2) {
      this.loadStaff();
      return;
    }

    this.loading = true;
    this.isSearching = true;
    this.error = null;

    this.staffService.searchStaff(this.searchTerm, 50).subscribe({
      next: (response) => {
        if (response.success) {
          this.staffList = response.data;
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = 'Failed to search staff';
        this.loading = false;
        console.error('Error searching staff:', err);
      },
    });
  }

  clearSearch() {
    this.searchTerm = '';
    this.isSearching = false;
    this.loadStaff();
  }

  onDateRangeChange() {
    if (!this.isSearching) {
      this.loadStaff();
    }
  }

  onStatsToggle() {
    this.loadStaff();
  }

  getDateRange(): { from: string; to: string } {
    switch (this.dateRange) {
      case 'last-7-days':
        return this.dateUtils.getLast7Days();
      case 'last-30-days':
        return this.dateUtils.getLast30Days();
      case 'last-90-days':
        return this.dateUtils.getLast90Days();
      case 'current-month':
      default:
        return {
          from: this.dateUtils.getCurrentMonthStart(),
          to: this.dateUtils.getCurrentMonthEnd(),
        };
    }
  }

  getInitials(name: string): string {
    return this.staffService.getInitials(name);
  }

  formatResponseTime(minutes: number): string {
    return this.staffService.formatResponseTime(minutes);
  }

  calculateResponseRate(responded: number, total: number): number {
    return this.staffService.calculateResponseRate(responded, total);
  }
}
