import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';
import {
  ActivityService,
  Activity,
  UnrepliedEmail,
} from '../services/activity.service';
import { EmailReminder, EmailReminderCounts } from '../models';
import { DateUtilsService } from '../services/date-utils.service';

@Component({
  selector: 'app-activities',
  standalone: true,
  imports: [CommonModule, FormsModule, TableSkeletonComponent],
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.css'],
})
export class ActivitiesComponent implements OnInit, OnDestroy {
  activities: Activity[] = [];
  loading = true;
  error: string | null = null;
  selectedActivity: Activity | null = null;
  showModal = false;

  // Email reminders
  emailReminders: EmailReminder[] = [];
  emailReminderCount: number = 0;
  criticalRemindersCount: number = 0;
  showEmailReminders: boolean = false;
  loadingReminders: boolean = false;
  reminderEmailCodeFilter: 'all' | 'RE' | 'FW' | 'RFQ' | 'RFP' | 'OTHER' =
    'all';

  // Pagination for infinite scroll
  currentPage = 0;
  pageSize = 50;
  hasMoreActivities = true;
  loadingMore = false;

  // Filter options
  // prettier-ignore
  filterType: 'all' | 'recent' | 'emails' | 'staff' | 'account' | 'unreplied' | 'followup' = 'all';
  staffEmail = '';
  accountId = '';
  activityCount = 0;
  topLimit = 50;

  // Unreplied emails
  unrepliedEmails: UnrepliedEmail[] = [];
  private allUnrepliedEmails: UnrepliedEmail[] = [];
  unrepliedCount = 0;
  maxHoursOld: number | null = null; // null = all time, or specify hours
  unrepliedPage = 0;
  unrepliedPageSize = 20; // Reduced for better infinite scroll experience
  hasMoreUnreplied = true;
  loadingMoreUnreplied = false;

  // Auto-refresh settings
  private refreshInterval: any = null;
  autoRefreshEnabled = true;
  refreshIntervalMinutes = 2; // Refresh every 2 minutes

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
    this.loadEmailReminders(); // Load email reminders
    this.startAutoRefresh(); // Start auto-refresh for unreplied emails
  }

  ngOnDestroy() {
    // Clean up the refresh interval when component is destroyed
    this.stopAutoRefresh();
  }

  /**
   * Start auto-refresh timer for unreplied emails
   */
  startAutoRefresh() {
    if (!this.autoRefreshEnabled) return;

    this.stopAutoRefresh(); // Clear any existing interval

    const intervalMs = this.refreshIntervalMinutes * 60 * 1000;
    console.log(
      `🔄 Auto-refresh enabled: checking for unreplied emails every ${this.refreshIntervalMinutes} minutes`
    );

    this.refreshInterval = setInterval(() => {
      console.log('🔄 Auto-refreshing unreplied emails...');
      this.refreshUnrepliedEmails();
    }, intervalMs);
  }

  /**
   * Stop auto-refresh timer
   */
  stopAutoRefresh() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
      console.log('⏸️ Auto-refresh stopped');
    }
  }

  /**
   * Refresh unreplied emails in background (without showing loading state)
   */
  refreshUnrepliedEmails() {
    this.activityService.getUnrepliedEmails(this.maxHoursOld).subscribe({
      next: (response) => {
        if (response.success) {
          const previousCount = this.unrepliedCount;
          this.allUnrepliedEmails = response.data;
          this.unrepliedCount = this.allUnrepliedEmails.length;

          // If we're viewing unreplied emails, update the display
          if (this.filterType === 'unreplied') {
            // Reset pagination and reload
            this.unrepliedPage = 0;
            this.unrepliedEmails = [];
            this.hasMoreUnreplied = true;
            this.loadMoreUnrepliedEmails();
          }

          // Log if count changed
          if (previousCount !== this.unrepliedCount) {
            console.log(
              `📧 Unreplied count updated: ${previousCount} → ${this.unrepliedCount}`
            );
          }
        }
      },
      error: (err) => {
        console.error('❌ Auto-refresh failed:', err);
      },
    });
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
    console.log('🔄 Loading unreplied emails...');
    this.unrepliedPage = 0;
    this.unrepliedEmails = [];
    this.allUnrepliedEmails = [];
    this.hasMoreUnreplied = true;
    this.loading = true;

    // Fetch all unreplied emails from backend
    this.activityService.getUnrepliedEmails(this.maxHoursOld).subscribe({
      next: (response) => {
        console.log('✅ Unreplied emails response:', response);
        if (response.success) {
          this.allUnrepliedEmails = response.data;
          this.unrepliedCount = this.allUnrepliedEmails.length;
          console.log(`📧 Total unreplied emails: ${this.unrepliedCount}`);

          // Load first page
          this.loadMoreUnrepliedEmails();
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Error loading unreplied emails:', err);
        this.handleError('Failed to load unreplied emails', err);
        this.loading = false;
      },
    });
  }

  loadMoreUnrepliedEmails() {
    if (this.loadingMoreUnreplied || !this.hasMoreUnreplied) {
      console.log('⏸️ Cannot load more:', {
        loadingMore: this.loadingMoreUnreplied,
        hasMore: this.hasMoreUnreplied,
      });
      return;
    }

    console.log(`📄 Loading page ${this.unrepliedPage}...`);
    this.loadingMoreUnreplied = true;

    // Simulate async operation with setTimeout to show loading state
    setTimeout(() => {
      const skip = this.unrepliedPage * this.unrepliedPageSize;
      const pageEmails = this.allUnrepliedEmails.slice(
        skip,
        skip + this.unrepliedPageSize
      );

      console.log('📊 Pagination details:', {
        totalEmails: this.allUnrepliedEmails.length,
        currentPage: this.unrepliedPage,
        skip: skip,
        pageSize: this.unrepliedPageSize,
        loadedInThisPage: pageEmails.length,
        currentTotal: this.unrepliedEmails.length,
      });

      this.unrepliedEmails = [...this.unrepliedEmails, ...pageEmails];
      this.hasMoreUnreplied =
        skip + this.unrepliedPageSize < this.allUnrepliedEmails.length;
      this.unrepliedPage++;

      console.log(
        `✅ Loaded ${pageEmails.length} emails. Total displayed: ${this.unrepliedEmails.length}/${this.unrepliedCount}`
      );
      console.log(`📌 Has more: ${this.hasMoreUnreplied}`);

      this.loadingMoreUnreplied = false;
      this.cdr.detectChanges();
    }, 300); // Small delay to show loading indicator
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

  formatSentDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
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
    const threshold = 200; // pixels from bottom to trigger load (increased for better UX)
    const position = element.scrollTop + element.clientHeight;
    const height = element.scrollHeight;

    const nearBottom = position > height - threshold;

    // Only log when near bottom to reduce console noise
    if (nearBottom) {
      console.log('📜 Near bottom - scroll details:', {
        position,
        height,
        threshold,
        remaining: height - position,
        filterType: this.filterType,
        hasMore:
          this.filterType === 'unreplied'
            ? this.hasMoreUnreplied
            : this.hasMoreActivities,
        loading:
          this.filterType === 'unreplied'
            ? this.loadingMoreUnreplied
            : this.loadingMore,
      });
    }

    if (nearBottom) {
      if (
        this.filterType === 'unreplied' &&
        !this.loadingMoreUnreplied &&
        this.hasMoreUnreplied
      ) {
        console.log('🔄 Triggering load more unreplied emails...');
        this.loadMoreUnrepliedEmails();
      } else if (!this.loadingMore && this.hasMoreActivities) {
        console.log('🔄 Triggering load more activities...');
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
    console.log('Clicked unreplied email:', email);
    console.log('Fetching activity details for ID:', email.activityId);

    // Fetch full activity details and open modal
    this.activityService.getActivityById(email.activityId).subscribe({
      next: (response) => {
        console.log('Activity details response:', response);
        if (response.success && response.data) {
          this.selectedActivity = response.data;
          this.showModal = true;
          console.log('Modal opened with activity:', this.selectedActivity);
        } else {
          console.warn('Failed to load activity details:', response.message);
          alert(
            'Failed to load email details: ' +
              (response.message || 'Unknown error')
          );
        }
      },
      error: (err) => {
        console.error('Error loading email details:', err);
        alert('Error loading email details');
      },
    });
  }

  /**
   * View email reminder details
   */
  viewReminderDetails(reminder: EmailReminder): void {
    console.log('Clicked email reminder:', reminder);
    console.log('Fetching activity details for ID:', reminder.activityId);

    // Fetch full activity details and open modal
    this.activityService.getActivityById(reminder.activityId).subscribe({
      next: (response) => {
        console.log('Activity details response:', response);
        if (response.success && response.data) {
          this.selectedActivity = response.data;
          this.showModal = true;
          console.log('Modal opened with activity:', this.selectedActivity);
        } else {
          console.warn('Failed to load activity details:', response.message);
          alert(
            'Failed to load email details: ' +
              (response.message || 'Unknown error')
          );
        }
      },
      error: (err) => {
        console.error('Error loading email details:', err);
        alert('Error loading email details');
      },
    });
  }

  /**
   * Load email reminders
   */
  loadEmailReminders(): void {
    this.loadingReminders = true;
    console.log('Loading email follow-up reminders...');

    this.activityService.getEmailReminders().subscribe({
      next: (response) => {
        console.log('Email reminders response:', response);
        if (response.success && response.data) {
          this.emailReminders = response.data;
          this.emailReminderCount = this.emailReminders.length;
          this.criticalRemindersCount = this.emailReminders.filter(
            (r) => r.urgencyLevel === 'critical'
          ).length;
          console.log(
            `Loaded ${this.emailReminderCount} reminders (${this.criticalRemindersCount} critical)`
          );
        }
        this.loadingReminders = false;
      },
      error: (err) => {
        console.error('Error loading email reminders:', err);
        this.loadingReminders = false;
      },
    });
  }

  /**
   * Toggle email reminders section
   */
  toggleEmailReminders(): void {
    this.showEmailReminders = !this.showEmailReminders;
    console.log('Email reminders section toggled:', this.showEmailReminders);

    // If opening for the first time and no data, load it
    if (
      this.showEmailReminders &&
      this.emailReminders.length === 0 &&
      !this.loadingReminders
    ) {
      this.loadEmailReminders();
    }
  }

  /**
   * Show email follow-up reminders (from metric card click)
   */
  openEmailReminders(): void {
    this.filterType = 'followup';
    console.log('Showing follow-up email reminders');

    // Load reminders if not already loaded
    if (this.emailReminders.length === 0 && !this.loadingReminders) {
      this.loadEmailReminders();
    }
  }

  /**
   * Get urgency color as hex value
   */
  getUrgencyColor(color: string): string {
    const colorMap: Record<string, string> = {
      yellow: '#F59E0B',
      orange: '#F97316',
      red: '#EF4444',
    };
    return colorMap[color] || '#6B7280';
  }

  /**
   * Get filtered email reminders based on email code filter
   */
  getFilteredReminders(): EmailReminder[] {
    if (this.reminderEmailCodeFilter === 'all') {
      return this.emailReminders;
    }

    return this.emailReminders.filter((reminder) => {
      const code = this.getEmailCode(reminder.subject);
      return code === this.reminderEmailCodeFilter;
    });
  }

  /**
   * Get count of reminders for each email code category
   */
  getReminderEmailCodeCount(code: string): number {
    if (code === 'all') {
      return this.emailReminders.length;
    }

    return this.emailReminders.filter((reminder) => {
      return this.getEmailCode(reminder.subject) === code;
    }).length;
  }
}
