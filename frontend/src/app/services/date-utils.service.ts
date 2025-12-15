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

  /**
   * Get date range for last 7 days
   */
  getLast7Days(): { from: string; to: string } {
    const today = new Date();
    const from = new Date();
    from.setDate(today.getDate() - 7);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for last 30 days
   */
  getLast30Days(): { from: string; to: string } {
    const today = new Date();
    const from = new Date();
    from.setDate(today.getDate() - 30);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for last 90 days
   */
  getLast90Days(): { from: string; to: string } {
    const today = new Date();
    const from = new Date();
    from.setDate(today.getDate() - 90);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for last 6 months
   */
  getLast6Months(): { from: string; to: string } {
    const today = new Date();
    const from = new Date();
    from.setMonth(today.getMonth() - 6);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for last year
   */
  getLastYear(): { from: string; to: string } {
    const today = new Date();
    const from = new Date();
    from.setFullYear(today.getFullYear() - 1);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for this year
   */
  getThisYear(): { from: string; to: string } {
    const today = new Date();
    const from = new Date(today.getFullYear(), 0, 1);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for this quarter
   */
  getThisQuarter(): { from: string; to: string } {
    const today = new Date();
    const quarter = Math.floor(today.getMonth() / 3);
    const from = new Date(today.getFullYear(), quarter * 3, 1);
    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(today),
    };
  }

  /**
   * Get date range for last quarter
   */
  getLastQuarter(): { from: string; to: string } {
    const today = new Date();
    const currentQuarter = Math.floor(today.getMonth() / 3);
    const lastQuarter = currentQuarter === 0 ? 3 : currentQuarter - 1;
    const year =
      currentQuarter === 0 ? today.getFullYear() - 1 : today.getFullYear();

    const from = new Date(year, lastQuarter * 3, 1);
    const to = new Date(year, lastQuarter * 3 + 3, 0);

    return {
      from: this.formatDateToISO(from),
      to: this.formatDateToISO(to),
    };
  }

  /**
   * Get all time (no date filter - returns empty strings)
   */
  getAllTime(): { from: string; to: string } {
    return {
      from: '',
      to: '',
    };
  }
}
