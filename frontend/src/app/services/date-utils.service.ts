import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class DateUtilsService {
  /**
   * Get the first day of the current month
   */
  getCurrentMonthStart(): string {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    return this.formatDateToISO(firstDay);
  }

  /**
   * Get the last day of the current month
   */
  getCurrentMonthEnd(): string {
    const today = new Date();
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    return this.formatDateToISO(lastDay);
  }

  /**
   * Get the date for N days ago
   * @param days Number of days in the past
   */
  getDaysAgo(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return this.formatDateToISO(date);
  }

  /**
   * Format a Date object to ISO date string (YYYY-MM-DD)
   * @param date The date to format
   */
  formatDateToISO(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  /**
   * Format date string to readable format
   * @param dateString ISO date string
   * @param options Intl.DateTimeFormatOptions
   */
  formatDate(
    dateString: string,
    options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }
  ): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', options);
  }

  /**
   * Get the month name
   * @param monthIndex Month index (0-11)
   */
  getMonthName(monthIndex: number): string {
    const months = [
      'January',
      'February',
      'March',
      'April',
      'May',
      'June',
      'July',
      'August',
      'September',
      'October',
      'November',
      'December',
    ];
    return months[monthIndex];
  }

  /**
   * Get current month and year as string
   */
  getCurrentMonthYear(): string {
    const today = new Date();
    return `${this.getMonthName(today.getMonth())} ${today.getFullYear()}`;
  }
}
