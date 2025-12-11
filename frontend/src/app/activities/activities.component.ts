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

  // Filter options
  filterType: 'all' | 'recent' | 'emails' | 'staff' | 'account' = 'all';
  staffEmail = '';
  accountId = '';
  activityCount = 0;
  topLimit = 50;

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
}
