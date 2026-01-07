import {
  Component,
  OnInit,
  AfterViewInit,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService, Account } from '../services/account.service';
import { StaffService, Staff } from '../services/staff.service';
import { DateUtilsService } from '../services/date-utils.service';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule, TableSkeletonComponent],
  templateUrl: './accounts.component.html',
  styleUrls: ['./accounts.component.css'],
})
export class AccountsComponent implements OnInit, AfterViewInit {
  accounts: Account[] = [];
  filteredAccounts: Account[] = [];
  allAccounts: Account[] = []; // All accounts for stats calculation
  loading = true;
  error: string | null = null;
  totalCount = 0;
  lastUpdated: Date | null = null;

  searchTerm = '';
  statusFilter = 'all';
  currentPage = 0;
  pageSize = 100; // Load 100 accounts initially, then 50 more
  hasMore = true; // Enable infinite scroll
  loadingMore = false;

  staffList: Staff[] = [];
  selectedStaffId = 'all';
  incompleteAccountsByStaff: { staff: Staff; count: number }[] = [];
  showingIncompleteOnly = false;

  // Account detail modal
  selectedAccount: Account | null = null;
  showAccountDetail = false;

  // Search debounce subject
  private searchSubject = new Subject<string>();

  stats = {
    totalAccounts: 0,
    activeAccounts: 0,
    accountsThisMonth: 0,
    incompleteData: 0,
  };

  constructor(
    private accountService: AccountService,
    private staffService: StaffService,
    private dateUtils: DateUtilsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadStaff();
    this.loadCount();

    // Setup search debounce - wait 500ms after user stops typing
    this.searchSubject
      .pipe(debounceTime(500), distinctUntilChanged())
      .subscribe((searchTerm) => {
        // Reset pagination and reload accounts
        this.currentPage = 0;
        this.hasMore = true;
        this.accounts = [];
        this.filteredAccounts = [];
        this.loadAccounts();
      });
  }

  loadStaff() {
    this.staffService.getAllStaff(false).subscribe({
      next: (response) => {
        if (response.success) {
          this.staffList = response.data;
          // Load all accounts for stats after staff is loaded
          this.loadAllAccountsForStats();
          // Load accounts after staff is loaded
          this.loadAccounts();
        } else {
          console.error('Failed to load staff');
          // Still load all accounts and table accounts even if staff fails
          this.loadAllAccountsForStats();
          this.loadAccounts();
        }
      },
      error: (err) => {
        console.error('Error loading staff:', err);
        // Still load all accounts and table accounts even if staff fails
        this.loadAllAccountsForStats();
        this.loadAccounts();
      },
    });
  }

  loadAccounts() {
    this.loading = true;
    this.error = null;
    this.currentPage = 0;
    this.accounts = [];
    this.filteredAccounts = [];

    this.accountService
      .getAccounts(
        this.pageSize,
        0,
        this.searchTerm,
        this.selectedStaffId,
        this.statusFilter
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.accounts = response.data;
            this.filteredAccounts = response.data;
            this.hasMore = response.data.length === this.pageSize;
            this.currentPage = 1;
            this.lastUpdated = new Date();
            this.pageSize = 50; // After initial load, load 50 more each time
          } else {
            this.error =
              response.message || 'Failed to load accounts from server';
            console.error('API returned error:', response.message);
          }
          this.loading = false;
        },
        error: (err) => {
          console.error('HTTP Error loading accounts:', err);
          console.error('Error details:', {
            status: err.status,
            statusText: err.statusText,
            url: err.url,
            message: err.message,
          });

          // Better error messages based on error type
          if (err.status === 0) {
            this.error =
              'Cannot connect to server. Please check your connection and try again.';
          } else if (err.status === 401 || err.status === 403) {
            this.error =
              'Authentication failed. Please refresh the page and login again.';
          } else if (err.status >= 500) {
            this.error = 'Server error. Please try again in a few moments.';
          } else {
            this.error = `Failed to load accounts (Error ${err.status}). Please try again.`;
          }
          this.loading = false;
        },
      });
  }

  loadAllAccountsForStats() {
    // Load all accounts without filters for accurate stats
    console.log('Loading all accounts for stats calculation...');
    this.accountService
      .getAccounts(10000, 0, '', 'all', 'all') // Load up to backend limit for all accounts
      .subscribe({
        next: (response) => {
          console.log('Stats API response:', response);
          if (response.success) {
            this.allAccounts = response.data;
            console.log(
              'All accounts loaded for stats:',
              this.allAccounts.length
            );
            this.calculateStats();
          } else {
            console.error(
              'Stats API returned success=false:',
              response.message
            );
            // Use empty array if request fails
            this.allAccounts = [];
            this.calculateStats();
          }
        },
        error: (err) => {
          console.error('Error loading all accounts for stats:', err);
          // Fallback to using loaded accounts if all accounts fetch fails
          this.allAccounts = this.accounts;
          console.log(
            'Using fallback accounts for stats:',
            this.allAccounts.length
          );
          this.calculateStats();
        },
      });
  }

  loadCount() {
    this.accountService.getAccountCount().subscribe({
      next: (response) => {
        if (response.success) {
          this.totalCount = response.data || 0;
        }
      },
      error: (err) => {
        console.error('Error loading account count:', err);
      },
    });
  }

  onSearchChange() {
    // Use debounced search to avoid too many API calls while typing
    this.searchSubject.next(this.searchTerm);
  }

  clearSearch() {
    this.searchTerm = '';
    this.currentPage = 0;
    this.hasMore = true;
    this.accounts = [];
    this.filteredAccounts = [];
    this.loadAccounts();
  }

  onStaffChange() {
    // Reset pagination when staff filter changes
    this.currentPage = 0;
    this.hasMore = true;
    this.accounts = [];
    this.filteredAccounts = [];
    this.loadAccounts();
  }

  onStatusChange() {
    // Reset pagination when status filter changes
    this.currentPage = 0;
    this.hasMore = true;
    this.accounts = [];
    this.filteredAccounts = [];
    this.loadAccounts();
  }

  onTableScroll(event: Event): void {
    const element = event.target as HTMLElement;
    const threshold = 200; // pixels from bottom to trigger load
    const position = element.scrollTop + element.clientHeight;
    const height = element.scrollHeight;

    const nearBottom = position > height - threshold;

    if (nearBottom && !this.loadingMore && this.hasMore) {
      this.loadMoreAccounts();
    }
  }

  loadMoreAccounts(): void {
    if (this.loadingMore || !this.hasMore) {
      return;
    }

    this.loadingMore = true;
    // D365 doesn't support $skip, so we load cumulatively with increasing $top
    // Load all previous + next batch, then slice to get only new items
    const previousCount = this.accounts.length;
    const newTop = previousCount + this.pageSize;

    this.accountService
      .getAccounts(
        newTop,
        0, // skip is ignored by backend
        this.searchTerm,
        this.selectedStaffId,
        this.statusFilter
      )
      .subscribe({
        next: (response) => {
          if (response.success) {
            // Get only the new accounts (after previousCount)
            const newAccounts = response.data.slice(previousCount);

            if (newAccounts.length > 0) {
              this.accounts = [...this.accounts, ...newAccounts];
              this.filteredAccounts = [
                ...this.filteredAccounts,
                ...newAccounts,
              ];
              this.hasMore = response.data.length === newTop;
              this.currentPage++;
            } else {
              // No new accounts received
              this.hasMore = false;
            }
          }
          this.loadingMore = false;
        },
        error: (err: any) => {
          console.error('Error loading more accounts:', err);
          this.loadingMore = false;
          this.hasMore = false;
        },
      });
  }

  openAccountDetails(account: Account) {
    this.selectedAccount = account;
    this.showAccountDetail = true;
    this.cdr.detectChanges();
  }

  closeAccountDetail() {
    this.showAccountDetail = false;
    this.selectedAccount = null;
  }

  getLastUpdatedText(): string {
    if (!this.lastUpdated) return '';

    const now = new Date();
    const diffMs = now.getTime() - this.lastUpdated.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return 'Just now';
    if (diffMins === 1) return '1 minute ago';
    if (diffMins < 60) return `${diffMins} minutes ago`;

    const diffHours = Math.floor(diffMins / 60);
    if (diffHours === 1) return '1 hour ago';
    if (diffHours < 24) return `${diffHours} hours ago`;

    return this.lastUpdated.toLocaleTimeString();
  }

  getStaffInitials(fullName: string): string {
    return fullName
      .split(' ')
      .map((name) => name.charAt(0))
      .join('')
      .toUpperCase();
  }

  getFilteredAccounts(): Account[] {
    return this.filteredAccounts;
  }

  getPercentage(value: number, total: number): number {
    if (!total || total === 0) return 0;
    return Math.round((value / total) * 100);
  }

  calculateStats() {
    const total = this.allAccounts.length;
    const incomplete = this.allAccounts.filter((acc) =>
      this.isAccountIncomplete(acc)
    ).length;
    const active = total - incomplete;

    const currentMonth = new Date().getMonth();
    const currentYear = new Date().getFullYear();
    const thisMonth = this.allAccounts.filter((acc) => {
      const created = new Date(acc.createdon);
      return (
        created.getMonth() === currentMonth &&
        created.getFullYear() === currentYear
      );
    }).length;

    this.stats = {
      totalAccounts: total,
      activeAccounts: active,
      accountsThisMonth: thisMonth,
      incompleteData: incomplete,
    };

    // Calculate incomplete accounts per staff
    this.calculateIncompleteByStaff();
  }

  calculateIncompleteByStaff() {
    const incompleteByStaffMap = new Map<string, number>();

    console.log(
      'Calculating incomplete by staff. All accounts:',
      this.allAccounts.length
    );

    // Count incomplete accounts per staff
    this.allAccounts
      .filter((acc) => this.isAccountIncomplete(acc))
      .forEach((acc) => {
        const ownerId = acc._ownerid_value;
        if (ownerId) {
          incompleteByStaffMap.set(
            ownerId,
            (incompleteByStaffMap.get(ownerId) || 0) + 1
          );
        }
      });

    // Map to staff objects with counts, sorted by count descending
    this.incompleteAccountsByStaff = this.staffList
      .map((staff) => ({
        staff,
        count: incompleteByStaffMap.get(staff.systemUserId) || 0,
      }))
      .filter((item) => item.count > 0)
      .sort((a, b) => b.count - a.count);

    console.log(
      'Incomplete accounts by staff:',
      this.incompleteAccountsByStaff.length,
      this.incompleteAccountsByStaff
    );
  }

  filterByIncompleteStaff(staffId: string, staffName: string) {
    this.selectedStaffId = staffId;
    this.statusFilter = 'all';
    this.searchTerm = '';
    this.showingIncompleteOnly = true;

    // Filter to show only incomplete accounts for this staff
    this.filteredAccounts = this.accounts.filter(
      (acc) => acc._ownerid_value === staffId && this.isAccountIncomplete(acc)
    );
  }

  clearIncompleteFilter() {
    this.showingIncompleteOnly = false;
    this.selectedStaffId = 'all';
    this.statusFilter = 'all';
    this.searchTerm = '';
    this.currentPage = 0;
    this.hasMore = true;
    this.accounts = [];
    this.filteredAccounts = [];
    this.loadAccounts();
  }

  isAccountIncomplete(account: Account): boolean {
    return (
      !account.telephone1 || !account.emailaddress1 || !account.address1_city
    );
  }

  getOwnerName(account: Account): string {
    // First try to get owner name from expanded ownerid field
    if (account.ownerid?.fullname) {
      return account.ownerid.fullname;
    }

    // Fallback to staff list lookup using _ownerid_value
    if (account._ownerid_value) {
      const staff = this.staffList.find(
        (s) => s.systemUserId === account._ownerid_value
      );
      if (staff) return staff.fullName;
    }

    return 'Unknown';
  }

  getOwnerInitials(account: Account): string {
    const name = this.getOwnerName(account);
    if (name === 'Unknown') return '--';

    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return 'N/A';
    const formatted = this.dateUtils.formatDate(dateString, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
    return `Created: ${formatted}`;
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const day = date.getDate();
    const month = date.getMonth() + 1;
    const year = date.getFullYear();
    const hours = date.getHours();
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  }

  getPrimaryContactEmail(account: Account): string {
    // Return emailaddress2 if available (typically used for primary contact)
    // Otherwise return emailaddress1
    return account.emailaddress2 || account.emailaddress1 || '';
  }

  ngAfterViewInit() {
    this.makeColumnsResizable();
  }

  makeColumnsResizable() {
    const table = document.querySelector('.data-table') as HTMLTableElement;
    if (!table) return;

    const cols = table.querySelectorAll('th');
    const tableHeight = table.offsetHeight;

    cols.forEach((col) => {
      const resizer = document.createElement('div');
      resizer.classList.add('resizer');
      resizer.style.height = `${tableHeight}px`;

      col.appendChild(resizer);

      let isResizing = false;
      let startX = 0;
      let startWidth = 0;

      resizer.addEventListener('mousedown', (e: MouseEvent) => {
        isResizing = true;
        startX = e.pageX;
        startWidth = col.offsetWidth;

        document.body.style.cursor = 'col-resize';
        e.preventDefault();
      });

      document.addEventListener('mousemove', (e: MouseEvent) => {
        if (!isResizing) return;

        const width = startWidth + (e.pageX - startX);
        if (width > 50) {
          col.style.width = `${width}px`;
        }
      });

      document.addEventListener('mouseup', () => {
        if (isResizing) {
          isResizing = false;
          document.body.style.cursor = 'default';
        }
      });
    });
  }
}
