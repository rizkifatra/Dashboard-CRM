package com.example.backend.controller;

import com.example.backend.model.ApiResponse;
import com.example.backend.service.D365DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for dashboard aggregation endpoints
 * Provides summary statistics and metrics for the CRM dashboard
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private D365DashboardService dashboardService;

    /**
     * Get overall dashboard metrics
     * Returns: total activities, active opportunities, total emails, response rate
     * 
     * @param fromDate Optional start date (YYYY-MM-DD)
     * @param toDate   Optional end date (YYYY-MM-DD)
     * @return ApiResponse with dashboard metrics
     */
    @GetMapping("/metrics")
    public ApiResponse<?> getDashboardMetrics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return dashboardService.getDashboardMetrics(fromDate, toDate);
    }

    /**
     * Get top performing staff members
     * Returns ranked list based on email activity and response times
     * 
     * @param top      Number of top performers to return (default: 10)
     * @param fromDate Optional start date (YYYY-MM-DD)
     * @param toDate   Optional end date (YYYY-MM-DD)
     * @return ApiResponse with top performers
     */
    @GetMapping("/top-performers")
    public ApiResponse<?> getTopPerformers(
            @RequestParam(defaultValue = "10") Integer top,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return dashboardService.getTopPerformers(top, fromDate, toDate);
    }

    /**
     * Get email performance data for all staff
     * Returns data suitable for bar chart visualization
     * 
     * @param fromDate Optional start date (YYYY-MM-DD)
     * @param toDate   Optional end date (YYYY-MM-DD)
     * @return ApiResponse with email performance by staff
     */
    @GetMapping("/email-performance")
    public ApiResponse<?> getEmailPerformance(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return dashboardService.getEmailPerformanceByStaff(fromDate, toDate);
    }
}
