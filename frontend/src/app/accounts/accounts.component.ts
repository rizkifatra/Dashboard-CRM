import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService, Account } from '../services/account.service';
import { StaffService, Staff } from '../services/staff.service';
import { DateUtilsService } from '../services/date-utils.service';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './accounts.component.html',
  styleUrls: ['./accounts.component.css'],
})
export class AccountsComponent implements OnInit {
  accounts: Account[] = [];
  loading = true;
  error: string | null = null;
  totalCount = 0;

  // Search and filter
  searchTerm = '';
  isSearching = false;
  topLimit = 50;

  // Month filter
  selectedMonth = 'current';
  monthlyData: { monthLabel: string; year: number; month: number }[] = [];
  fromDate?: string;
  toDate?: string;

  // Staff performance
  staffList: Staff[] = [];
  selectedStaffId = 'all';
  accountsByStaff: { [key: string]: number } = {};
  staffPerformance: {
    name: string;
    accountCount: number;
    percentage: number;
  }[] = [];

  // Data completeness warnings
  incompleteAccounts: Account[] = [];
  staffWithIncompleteData: {
    staffId: string;
    staffName: string;
    incompleteCount: number;
    totalCount: number;
    missingFields: string[];
  }[] = [];

  // Stats
  stats = {
    totalAccounts: 0,
    activeAccounts: 0,
    accountsThisMonth: 0,
    topPerformer: '',
    incompleteData: 0,
    dataCompletionRate: 100,
  };

  constructor(
    private accountService: AccountService,
    private staffService: StaffService,
    private dateUtils: DateUtilsService
  ) {}

  ngOnInit() {
    this.loadMonthlyData();
    this.loadStaff();
    this.loadAccounts();
    this.loadCount();
    this.calculateStats();
  }

  loadMonthlyData() {
    const months = this.dateUtils.getLast12Months();
    this.monthlyData = months.map(
      (m: { label: string; year: number; month: number }) => ({
        monthLabel: m.label,
        year: m.year,
        month: m.month,
      })
    );
  }

  loadStaff() {
    this.staffService.getAllStaff(false).subscribe({
      next: (response) => {
        if (response.success) {
          this.staffList = response.data;
        }
      },
      error: (err) => {
        console.error('Error loading staff:', err);
      },
    });
  }

  onMonthChange(monthValue: string) {
    this.selectedMonth = monthValue;

    if (monthValue === 'current') {
      this.fromDate = undefined;
      this.toDate = undefined;
    } else {
      const index = parseInt(monthValue);
      if (index >= 0 && index < this.monthlyData.length) {
        const selectedMonthData = this.monthlyData[index];
        const startDate = new Date(
          selectedMonthData.year,
          selectedMonthData.month,
          1
        );
        const endDate = new Date(
          selectedMonthData.year,
          selectedMonthData.month + 1,
          0
        );

        this.fromDate = this.formatDateForAPI(startDate);
        this.toDate = this.formatDateForAPI(endDate);
      }
    }

    this.loadAccounts();
  }

  formatDateForAPI(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  onStaffChange() {
    this.loadAccounts();
  }

  loadAccounts() {
    this.loading = true;
    this.error = null;

    this.accountService
      .getAccounts(this.topLimit, this.fromDate, this.toDate)
      .subscribe({
        next: (response: any) => {
          if (response.success) {
            let filteredAccounts = response.data;

            if (this.selectedStaffId !== 'all') {
              filteredAccounts = filteredAccounts.filter(
                (acc: Account) => acc._ownerid_value === this.selectedStaffId
              );
            }

            // Sort accounts by creation date - newest first
            filteredAccounts.sort((a: Account, b: Account) => {
              const dateA = new Date(a.createdon).getTime();
              const dateB = new Date(b.createdon).getTime();
              return dateB - dateA; // Descending order (newest first)
            });

            console.log(
              'First 3 accounts after sorting:',
              filteredAccounts.slice(0, 3).map((acc: Account) => ({
                name: acc.name,
                created: acc.createdon,
              }))
            );

            this.accounts = filteredAccounts;
            this.calculateStats();
            this.calculateStaffPerformance();
            this.checkDataCompleteness();
          } else {
            this.error = 'Failed to load accounts';
          }
          this.loading = false;
        },
        error: (err: any) => {
          console.error('Error loading accounts:', err);
          this.error = 'Failed to load accounts. Please try again.';
          this.loading = false;
        },
      });
  }

  checkDataCompleteness() {
    this.incompleteAccounts = this.accounts.filter((account) =>
      this.isAccountIncomplete(account)
    );

    const staffIncompleteMap: {
      [key: string]: {
        name: string;
        incomplete: number;
        total: number;
        missingFields: Set<string>;
      };
    } = {};

    this.accounts.forEach((account) => {
      const ownerId = account._ownerid_value;
      if (!ownerId) return;

      if (!staffIncompleteMap[ownerId]) {
        staffIncompleteMap[ownerId] = {
          name: this.getOwnerName(ownerId),
          incomplete: 0,
          total: 0,
          missingFields: new Set(),
        };
      }

      staffIncompleteMap[ownerId].total++;

      if (this.isAccountIncomplete(account)) {
        staffIncompleteMap[ownerId].incomplete++;
        const missingFields = this.getMissingFields(account);
        missingFields.forEach((field) =>
          staffIncompleteMap[ownerId].missingFields.add(field)
        );
      }
    });

    this.staffWithIncompleteData = Object.entries(staffIncompleteMap)
      .filter(([_, data]) => data.incomplete > 0)
      .map(([staffId, data]) => ({
        staffId,
        staffName: data.name,
        incompleteCount: data.incomplete,
        totalCount: data.total,
        missingFields: Array.from(data.missingFields),
      }));
  }

  isAccountIncomplete(account: Account): boolean {
    return (
      !account.telephone1 ||
      !account.emailaddress1 ||
      !account.address1_city ||
      !account.address1_stateorprovince ||
      !account.address1_country
    );
  }

  getMissingFields(account: Account): string[] {
    const missing: string[] = [];
    if (!account.telephone1) missing.push('Phone');
    if (!account.emailaddress1) missing.push('Email');
    if (!account.address1_city) missing.push('City');
    if (!account.address1_stateorprovince) missing.push('State');
    if (!account.address1_country) missing.push('Country');
    return missing;
  }

  loadCount() {
    this.accountService.getAccountCount().subscribe({
      next: (response) => {
        if (response.success) {
          this.totalCount = response.data;
        }
      },
      error: (err) => {
        console.error('Error loading count:', err);
      },
    });
  }

  calculateStats() {
    this.stats.totalAccounts = this.accounts.length;
    this.stats.activeAccounts = this.accounts.length;

    const currentDate = new Date();
    const currentMonth = currentDate.getMonth();
    const currentYear = currentDate.getFullYear();

    this.stats.accountsThisMonth = this.accounts.filter((account) => {
      if (!account.createdon) return false;
      const createdDate = new Date(account.createdon);
      return (ppppp
        createdDate.getMonth() === currentMonth &&
        createdDate.getFullYear() === currentYear
      );
    }).length;

    this.stats.incompleteData = this.incompleteAccounts.length;
    this.stats.dataCompletionRate =
      this.accounts.length > 0
        ? Math.round(
            ((this.accounts.length - this.incompleteAccounts.length) /
              this.accounts.length) *
              100
          )
        : 100;
  }

  calculateStaffPerformance() {
    const staffAccountCount: {
      [key: string]: { name: string; count: number };
    } = {};

    this.accounts.forEach((account) => {
      const ownerId = account._ownerid_value;
      if (ownerId) {
        if (!staffAccountCount[ownerId]) {
          staffAccountCount[ownerId] = {
            name: this.getOwnerName(ownerId),
            count: 0,
          };
        }
        staffAccountCount[ownerId].count++;
      }
    });

    const sortedStaff = Object.entries(staffAccountCount)
      .sort((a, b) => b[1].count - a[1].count)
      .slice(0, 5);

    const maxCount = sortedStaff.length > 0 ? sortedStaff[0][1].count : 1;

    this.staffPerformance = sortedStaff.map(([_, data]) => ({
      name: data.name,
      accountCount: data.count,
      percentage: (data.count / maxCount) * 100,
    }));
  }

  search() {
    if (!this.searchTerm.trim()) {
      this.loadAccounts();
      return;
    }

    this.isSearching = true;
    this.accountService.searchAccounts(this.searchTerm).subscribe({
      next: (response) => {
        if (response.success) {
          this.accounts = response.data;
          this.calculateStats();
          this.calculateStaffPerformance();
          this.checkDataCompleteness();
        }
        this.isSearching = false;
      },
      error: (err) => {
        console.error('Error searching accounts:', err);
        this.error = 'Search failed. Please try again.';
        this.isSearching = false;
      },
    });
  }

  onSearchChange() {
    if (!this.searchTerm.trim()) {
      this.loadAccounts();
    }
  }

  onTopLimitChange() {
    this.loadAccounts();
  }

  formatRevenue(revenue: number): string {
    return this.accountService.formatRevenue(revenue);
  }

  getInitials(name: string): string {
    return this.accountService.getInitials(name);
  }

  formatDate(dateString: string): string {
    return this.accountService.formatDate(dateString);
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date
      .toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
      .replace(',', '');
  }

  getOwnerInitials(ownerId: string): string {
    if (!ownerId) return '?';
    return ownerId.substring(0, 2).toUpperCase();
  }

  getOwnerName(ownerId: string): string {
    if (!ownerId) return 'Unknown';
    const staff = this.staffList.find((s) => s.systemUserId === ownerId);
    return staff ? staff.fullName : 'Unknown';
  }

  getPrimaryContact(account: Account): string {
    return '-';
  }
}
