import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService, Account } from '../services/account.service';

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

  constructor(private accountService: AccountService) {}

  ngOnInit() {
    this.loadAccounts();
    this.loadCount();
  }

  loadAccounts() {
    this.loading = true;
    this.error = null;

    this.accountService.getAccounts(this.topLimit).subscribe({
      next: (response) => {
        if (response.success) {
          this.accounts = response.data;
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = 'Failed to load accounts';
        this.loading = false;
        console.error('Error loading accounts:', err);
      },
    });
  }

  loadCount() {
    this.accountService.getAccountCount().subscribe({
      next: (response) => {
        if (response.success) {
          this.totalCount = response.data;
        }
      },
      error: (err) => {
        console.error('Error loading account count:', err);
      },
    });
  }

  searchAccounts() {
    if (!this.searchTerm || this.searchTerm.trim().length < 2) {
      this.loadAccounts();
      return;
    }

    this.loading = true;
    this.isSearching = true;
    this.error = null;

    this.accountService
      .searchAccounts(this.searchTerm, this.topLimit)
      .subscribe({
        next: (response) => {
          if (response.success) {
            this.accounts = response.data;
            this.loading = false;
          }
        },
        error: (err) => {
          this.error = 'Failed to search accounts';
          this.loading = false;
          console.error('Error searching accounts:', err);
        },
      });
  }

  clearSearch() {
    this.searchTerm = '';
    this.isSearching = false;
    this.loadAccounts();
  }

  onTopLimitChange() {
    if (this.isSearching && this.searchTerm) {
      this.searchAccounts();
    } else {
      this.loadAccounts();
    }
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
}
