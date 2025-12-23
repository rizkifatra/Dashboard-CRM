import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  ActivityService,
  Activity,
  UnrepliedEmail,
} from '../services/activity.service';
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

  // Pagination for infinite scroll
  currentPage = 0;
  pageSize = 50;
  hasMoreActivities = true;
  loadingMore = false;

  // Filter options
  // prettier-ignore
  filterType: 'all' | 'recent' | 'emails' | 'staff' | 'account' | 'unreplied' = 'all';
  staffEmail = '';
  accountId = '';
  activityCount = 0;
  topLimit = 50;

  // Unreplied emails
  unrepliedEmails: UnrepliedEmail[] = [];
  unrepliedCount = 0;
  maxHoursOld = 720; // 30 days default (increased to capture more emails)
  unrepliedPage = 0;
  unrepliedPageSize = 50;
  hasMoreUnreplied = true;
  loadingMoreUnreplied = false;

  // Email code filter
  emailCodeFilter: 'all' | 'RE' | 'FW' | 'RFQ' | 'RFP' | 'OTHER' = 'all';

  // Status type filter for managing activity statuses
  statusTypeFilter:
    | 'all'
    | 'follow-up'
    | 'tender'
    | 'meeting'
    | 'call'
    | 'quote'
    | 'update'
    | 'empty' = 'all';

  constructor(
    private activityService: ActivityService,
    private dateUtils: DateUtilsService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Check for filter query parameter
    this.route.queryParams.subscribe((params) => {
      if (params['filter'] === 'unreplied') {
        this.filterType = 'unreplied';
      }
    });

    this.loadActivities();
    this.loadActivityCount();
    this.loadUnrepliedCount(); // Load unreplied count for the card
  }

  loadActivities() {
    console.log('Loading activities with filterType:', this.filterType);
    this.loading = true;
    this.error = null;
    this.currentPage = 0;
    this.activities = [];
    this.hasMoreActivities = true;

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
      case 'unreplied':
        this.loadUnrepliedEmails();
        break;
    }
  }

  loadAllActivities() {
    const skip = this.currentPage * this.pageSize;
    this.activityService.getAllActivities(this.pageSize, skip).subscribe({
      next: (response) => {
        if (response.success) {
          this.activities = [...this.activities, ...response.data];
          this.hasMoreActivities = response.data.length === this.pageSize;
          this.currentPage++;
        }
        this.loading = false;
        this.loadingMore = false;
      },
      error: (err) => {
        this.handleError('Failed to load activities', err);
        this.loadingMore = false;
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
    const skip = this.currentPage * this.pageSize;
    this.activityService.getEmailActivities(this.pageSize, skip).subscribe({
      next: (response) => {
        if (response.success) {
          this.activities = [...this.activities, ...response.data];
          this.hasMoreActivities = response.data.length === this.pageSize;
          this.currentPage++;
        }
        this.loading = false;
        this.loadingMore = false;
      },
      error: (err) => {
        this.handleError('Failed to load email activities', err);
        this.loadingMore = false;
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

  loadUnrepliedEmails() {
    console.log('Loading unreplied emails...');
    this.unrepliedPage = 0;
    this.unrepliedEmails = [];
    this.hasMoreUnreplied = true;
    this.loadMoreUnrepliedEmails();
  }

  loadMoreUnrepliedEmails() {
    if (this.loadingMoreUnreplied || !this.hasMoreUnreplied) {
      console.log('Cannot load more:', {
        loadingMore: this.loadingMoreUnreplied,
        hasMore: this.hasMoreUnreplied,
      });
      return;
    }

    const isInitialLoad = this.unrepliedPage === 0;
    console.log('Loading page:', this.unrepliedPage, 'Initial:', isInitialLoad);

    if (isInitialLoad) {
      this.loading = true;
    } else {
      this.loadingMoreUnreplied = true;
    }

    this.activityService.getUnrepliedEmails(this.maxHoursOld).subscribe({
      next: (response) => {
        console.log(' Unreplied emails response:', response);
        if (response.success) {
          const allEmails = response.data;
          const skip = this.unrepliedPage * this.unrepliedPageSize;
          const pageEmails = allEmails.slice(
            skip,
            skip + this.unrepliedPageSize
          );

          console.log('Pagination:', {
            totalEmails: allEmails.length,
            skip: skip,
            pageSize: this.unrepliedPageSize,
            loadedInThisPage: pageEmails.length,
            currentTotal: this.unrepliedEmails.length,
          });

          this.unrepliedEmails = [...this.unrepliedEmails, ...pageEmails];
          this.unrepliedCount = allEmails.length;
          this.hasMoreUnreplied =
            skip + this.unrepliedPageSize < allEmails.length;
          this.unrepliedPage++;

          console.log('Loaded unreplied emails:', this.unrepliedEmails.length);
          console.log('Has more:', this.hasMoreUnreplied);
        }
        this.loading = false;
        this.loadingMoreUnreplied = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Error loading unreplied emails:', err);
        this.handleError('Failed to load unreplied emails', err);
        this.loadingMoreUnreplied = false;
      },
    });
  }

  /**
   * Load unreplied emails count only (for the metric card)
   */
  loadUnrepliedCount() {
    this.activityService.getUnrepliedEmails(this.maxHoursOld).subscribe({
      next: (response) => {
        if (response.success) {
          this.unrepliedCount = response.data.length;
        }
      },
      error: (err) => {
        console.error('Failed to load unreplied count:', err);
        this.unrepliedCount = 0;
      },
    });
  }

  /**
   * Switch to unreplied emails view
   */
  showUnrepliedEmails() {
    console.log('🔄 Switching to unreplied view...');
    this.filterType = 'unreplied';
    console.log('✅ FilterType set to:', this.filterType);
    this.cdr.detectChanges(); // Force change detection
    console.log('✅ Change detection triggered');
    this.loadActivities();
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
   * Get filtered unreplied emails based on email code filter
   */
  getFilteredUnrepliedEmails(): UnrepliedEmail[] {
    if (this.emailCodeFilter === 'all') {
      return this.unrepliedEmails;
    }

    return this.unrepliedEmails.filter((email) => {
      const emailCode = this.getEmailCode(email.subject);
      return emailCode === this.emailCodeFilter;
    });
  }

  /**
   * Get count of unreplied emails for each email code
   */
  getUnrepliedEmailCodeCount(code: string): number {
    if (code === 'all') {
      return this.unrepliedEmails.length;
    }
    return this.unrepliedEmails.filter((email) => {
      const emailCode = this.getEmailCode(email.subject);
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

  /**
   * Detect activity status type from subject line
   * Helps categorize activities like "Follow Up", "Close Tender", etc.
   */
  getActivityStatusType(subject: string): string {
    if (!subject || subject.trim() === '') return 'empty';

    const lowerSubject = subject.toLowerCase();

    // Follow up activities
    if (lowerSubject.includes('follow') || lowerSubject.includes('followup')) {
      return 'follow-up';
    }

    // Tender related
    if (lowerSubject.includes('tender') || lowerSubject.includes('bid')) {
      return 'tender';
    }

    // Meeting related
    if (
      lowerSubject.includes('meeting') ||
      lowerSubject.includes('appointment')
    ) {
      return 'meeting';
    }

    // Call related
    if (lowerSubject.includes('call') || lowerSubject.includes('phone')) {
      return 'call';
    }

    // Quote/Quotation
    if (lowerSubject.includes('quote') || lowerSubject.includes('quotation')) {
      return 'quote';
    }

    // Update status
    if (lowerSubject.includes('update') || lowerSubject.includes('status')) {
      return 'update';
    }

    return 'other';
  }

  /**
   * Get status type badge label
   */
  getStatusTypeLabel(statusType: string): string {
    const labels: { [key: string]: string } = {
      'follow-up': 'Follow Up',
      tender: 'Tender',
      meeting: 'Meeting',
      call: 'Call',
      quote: 'Quote',
      update: 'Update',
      empty: 'No Subject',
      other: 'Other',
    };
    return labels[statusType] || 'Other';
  }

  /**
   * Get count of activities by status type
   */
  getStatusTypeCount(statusType: string): number {
    if (statusType === 'all') {
      return this.getFilteredActivities().length;
    }
    return this.getFilteredActivities().filter((activity) => {
      const activityStatusType = this.getActivityStatusType(activity.subject);
      return activityStatusType === statusType;
    }).length;
  }

  /**
   * Get filtered activities by both email code and status type
   */
  getDoubleFilteredActivities(): Activity[] {
    let activities = this.getFilteredActivities();

    // Apply status type filter
    if (this.statusTypeFilter !== 'all') {
      activities = activities.filter((activity) => {
        const statusType = this.getActivityStatusType(activity.subject);
        return statusType === this.statusTypeFilter;
      });
    }

    return activities;
  }

  /**
   * Get human-readable status description from state and status codes
   */
  getStatusDescription(stateCode?: number, statusCode?: number): string {
    // Dynamics 365 Activity State Codes
    // 0 = Open, 1 = Completed, 2 = Canceled, 3 = Scheduled

    if (stateCode === undefined && statusCode === undefined) {
      return 'Status update';
    }

    const stateLabels: { [key: number]: string } = {
      0: 'Open',
      1: 'Completed',
      2: 'Canceled',
      3: 'Scheduled',
    };

    const statusLabels: { [key: number]: string } = {
      1: 'Open',
      2: 'Completed',
      3: 'Canceled',
      4: 'Scheduled',
      5: 'Busy',
      6: 'Out of Office',
    };

    let description = stateLabels[stateCode || 0] || 'Unknown State';

    if (statusCode && statusLabels[statusCode]) {
      description += ` (${statusLabels[statusCode]})`;
    }

    return description;
  }

  /**
   * Handle scroll event on the table to implement infinite scrolling
   * @param event - The scroll event
   */
  onTableScroll(event: Event): void {
    const element = event.target as HTMLElement;
    const threshold = 100; // pixels from bottom to trigger load
    const position = element.scrollTop + element.clientHeight;
    const height = element.scrollHeight;

    console.log('📜 Scroll event:', {
      position,
      height,
      threshold,
      nearBottom: position > height - threshold,
      filterType: this.filterType,
    });

    if (position > height - threshold) {
      if (
        this.filterType === 'unreplied' &&
        !this.loadingMoreUnreplied &&
        this.hasMoreUnreplied
      ) {
        console.log('Triggering load more unreplied emails...');
        this.loadMoreUnrepliedEmails();
      } else if (!this.loadingMore && this.hasMoreActivities) {
        this.loadMoreActivities();
      }
    }
  }

  /**
   * Load more activities when scrolling to bottom
   */
  loadMoreActivities(): void {
    if (this.loadingMore || !this.hasMoreActivities) {
      return;
    }

    this.loadingMore = true;

    switch (this.filterType) {
      case 'all':
        this.loadAllActivities();
        break;
      case 'emails':
        this.loadEmailActivities();
        break;
      // Add other filter types as needed
      default:
        this.loadingMore = false;
    }
  }

  /**
   * Open reply modal for unreplied email
   */
  openReplyModal(email: UnrepliedEmail): void {
    // TODO: Implement reply modal - for now, open in new email client
    const subject = `RE: ${email.subject}`;
    const mailtoLink = `mailto:${email.fromEmail}?subject=${encodeURIComponent(
      subject
    )}`;
    window.open(mailtoLink, '_blank');
  }

  /**
   * View email details
   */
  viewEmailDetails(email: UnrepliedEmail): void {
    // Fetch full activity details and open modal
    this.activityService.getActivityById(email.activityId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.selectedActivity = response.data;
          this.showModal = true;
        }
      },
      error: (err) => {
        console.error('Error loading email details:', err);
      },
    });
  }
}
