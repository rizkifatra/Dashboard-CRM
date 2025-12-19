import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivityService, Activity } from '../services/activity.service';
import { DateUtilsService } from '../services/date-utils.service';

@Component({
  selector: 'app-activities',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.css'],
})
export class ActivitiesComponent implements OnInit {
  activities: Activity[] = [];
  loading = true;
  error: string | null = null;
  selectedActivity: Activity | null = null;
  showModal = false;

  // Filter options
  filterType: 'all' | 'recent' | 'emails' | 'staff' | 'account' = 'all';
  staffEmail = '';
  accountId = '';
  activityCount = 0;
  topLimit = 50;

  // Email code filter
  emailCodeFilter: 'all' | 'RE' | 'FW' | 'RFQ' | 'RFP' | 'OTHER' = 'all';

  constructor(
    private activityService: ActivityService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    this.loadActivities();
    this.loadActivityCount();
  }

  loadActivities() {
    this.loading = true;
    this.error = null;

    switch (this.filterType) {
      case 'all':
        this.loadAllActivities();
        break;
      case 'recent':
        this.loadRecentActivities();
        break;
      case 'emails':
        this.loadEmailActivities();
        break;
      case 'staff':
        if (this.staffEmail) {
          this.loadActivitiesByStaff();
        } else {
          this.error = 'Please enter a staff email';
          this.loading = false;
        }
        break;
      case 'account':
        if (this.accountId) {
          this.loadActivitiesByAccount();
        } else {
          this.error = 'Please enter an account ID';
          this.loading = false;
        }
        break;
    }
  }

  loadAllActivities() {
    this.activityService.getAllActivities(this.topLimit).subscribe({
      next: (response) => {
        if (response.success) {
          this.activities = response.data;
        }
        this.loading = false;
      },
      error: (err) => {
        this.handleError('Failed to load activities', err);
      },
    });
  }

  loadRecentActivities() {
    const fromDate = this.dateUtils.getLast30Days().from;
    const toDate = this.dateUtils.getLast30Days().to;

    this.activityService
      .getRecentActivities(this.topLimit, fromDate, toDate)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.activities = response.data;
          }
          this.loading = false;
        },
        error: (err) => {
          this.handleError('Failed to load recent activities', err);
        },
      });
  }

  loadEmailActivities() {
    this.activityService.getEmailActivities(this.topLimit).subscribe({
      next: (response) => {
        if (response.success) {
          this.activities = response.data;
        }
        this.loading = false;
      },
      error: (err) => {
        this.handleError('Failed to load email activities', err);
      },
    });
  }

  loadActivitiesByStaff() {
    this.activityService
      .getActivitiesByStaff(this.staffEmail, this.topLimit)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.activities = response.data;
          }
          this.loading = false;
        },
        error: (err) => {
          this.handleError('Failed to load activities by staff', err);
        },
      });
  }

  loadActivitiesByAccount() {
    this.activityService
      .getActivitiesByAccount(this.accountId, this.topLimit)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.activities = response.data;
          }
          this.loading = false;
        },
        error: (err) => {
          this.handleError('Failed to load activities by account', err);
        },
      });
  }

  loadActivityCount() {
    this.activityService.getActivityCount().subscribe({
      next: (response) => {
        if (response.success) {
          this.activityCount = response.data;
        }
      },
      error: (err) => {
        console.error('Error loading activity count:', err);
      },
    });
  }

  onFilterChange() {
    this.loadActivities();
  }

  handleError(message: string, err: any) {
    this.error = message;
    this.loading = false;
    console.error(message, err);
  }

  getInitials(name: string): string {
    return this.activityService.getInitials(name);
  }

  getRelativeTime(timestamp: string): string {
    return this.activityService.getRelativeTime(timestamp);
  }

  getEmailCount(): number {
    return this.activities.filter((activity) =>
      activity.activityType?.toLowerCase().includes('email')
    ).length;
  }

  getOutgoingCount(): number {
    return this.activities.filter(
      (activity) => activity.direction === 'outgoing'
    ).length;
  }

  getIncomingCount(): number {
    return this.activities.filter(
      (activity) => activity.direction === 'incoming'
    ).length;
  }

  openActivityDetails(activity: Activity) {
    this.selectedActivity = activity;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    setTimeout(() => {
      this.selectedActivity = null;
    }, 300);
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((value / total) * 100);
  }

  /**
   * Extract email code from activity subject line
   * @param subject - The activity subject line
   * @returns Email code: 'RE', 'FW', 'RFQ', 'RFP', or 'OTHER'
   */
  getEmailCode(subject: string): string {
    if (!subject) return 'OTHER';

    const upperSubject = subject.toUpperCase().trim();

    // Check for standard email prefixes (RE:, FW:, Fw:, Re:)
    if (upperSubject.startsWith('RE:') || upperSubject.startsWith('RE ')) {
      return 'RE';
    }
    if (
      upperSubject.startsWith('FW:') ||
      upperSubject.startsWith('FWD:') ||
      upperSubject.startsWith('FW ')
    ) {
      return 'FW';
    }

    // Check for business request types (can be anywhere in subject)
    if (
      upperSubject.includes('RFQ') ||
      upperSubject.includes('REQUEST FOR QUOTE')
    ) {
      return 'RFQ';
    }
    if (
      upperSubject.includes('RFP') ||
      upperSubject.includes('REQUEST FOR PROPOSAL')
    ) {
      return 'RFP';
    }

    return 'OTHER';
  }

  /**
   * Get filtered activities based on email code filter
   */
  getFilteredActivities(): Activity[] {
    if (this.emailCodeFilter === 'all') {
      return this.activities;
    }

    return this.activities.filter((activity) => {
      const emailCode = this.getEmailCode(activity.subject);
      return emailCode === this.emailCodeFilter;
    });
  }

  /**
   * Get count of activities for each email code
   */
  getEmailCodeCount(code: string): number {
    if (code === 'all') {
      return this.activities.length;
    }
    return this.activities.filter((activity) => {
      const emailCode = this.getEmailCode(activity.subject);
      return emailCode === code;
    }).length;
  }

  /**
   * Get status badge class based on state and status codes
   */
  getStatusBadgeClass(activity: Activity): string {
    if (activity.stateCode === 0) return 'status-open';
    if (activity.stateCode === 1) return 'status-completed';
    if (activity.stateCode === 2) return 'status-cancelled';
    return 'status-unknown';
  }

  /**
   * Get status text based on state and status codes
   */
  getStatusText(activity: Activity): string {
    if (activity.stateCode === 0) return 'Open';
    if (activity.stateCode === 1) return 'Completed';
    if (activity.stateCode === 2) return 'Cancelled';
    return 'Unknown';
  }
}
