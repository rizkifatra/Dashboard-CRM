package com.example.backend.controller;

import com.example.backend.model.ApiResponse;
import com.example.backend.service.D365DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for fiscal dashboard endpoints
 * Provides fiscal year, quarterly, and monthly metrics for the CRM dashboard
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private D365DashboardService dashboardService;

    /**
     * Get fiscal year revenue metrics
     * Fiscal year runs from June (current year) to July (next year)
     * 
     * @param fiscalYear Optional fiscal year (e.g., "2025" for FY2025-2026)
     * @return ApiResponse with fiscal year metrics
     */
    @GetMapping("/fiscal-year-metrics")
    public ApiResponse<?> getFiscalYearMetrics(
            @RequestParam(required = false) String fiscalYear) {
        return dashboardService.getFiscalYearMetrics(fiscalYear);
    }

    /**
     * Get quarterly revenue metrics
     * Returns metrics for a specific quarter within the fiscal year
     * 
     * @param quarter    Quarter number (1-4)
     * @param fiscalYear Optional fiscal year
     * @return ApiResponse with quarterly metrics
     */
    @GetMapping("/quarterly-metrics")
    public ApiResponse<?> getQuarterlyMetrics(
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String fiscalYear) {
        return dashboardService.getQuarterlyMetrics(quarter, fiscalYear);
    }

    /**
     * Get monthly opportunity statistics
     * 
     * @param month      Month (1-12)
     * @param quarter    Quarter number (1-4) - optional, for filtering within
     *                   quarter
     * @param fiscalYear Fiscal year - optional, for filtering within fiscal year
     * @return ApiResponse with monthly opportunity stats
     */
    @GetMapping("/monthly-opportunities")
    public ApiResponse<?> getMonthlyOpportunities(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String fiscalYear) {
        return dashboardService.getMonthlyOpportunities(month, quarter, fiscalYear);
    }

    /**
     * Get quarterly opportunity statistics
     * 
     * @param quarter    Quarter number (1-4)
     * @param fiscalYear Optional fiscal year
     * @return ApiResponse with quarterly opportunity stats
     */
    @GetMapping("/quarterly-opportunities")
    public ApiResponse<?> getQuarterlyOpportunities(
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String fiscalYear) {
        return dashboardService.getQuarterlyOpportunities(quarter, fiscalYear);
    }

    /**
     * Get fiscal year opportunity statistics
     * 
     * @param fiscalYear Optional fiscal year
     * @return ApiResponse with fiscal year opportunity stats
     */
    @GetMapping("/fiscal-year-opportunities")
    public ApiResponse<?> getFiscalYearOpportunities(
            @RequestParam(required = false) String fiscalYear) {
        return dashboardService.getFiscalYearOpportunities(fiscalYear);
    }

    /**
     * Get top staff performance based on won opportunities
     * 
     * @param period     Period type: "fiscal-year", "quarter", "month"
     * @param fiscalYear Optional fiscal year
     * @param quarter    Optional quarter
     * @param month      Optional month
     * @param year       Optional year
     * @return ApiResponse with top staff performance
     */
    @GetMapping("/top-staff-performance")
    public ApiResponse<?> getTopStaffPerformance(
            @RequestParam(defaultValue = "fiscal-year") String period,
            @RequestParam(required = false) String fiscalYear,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return dashboardService.getTopStaffPerformance(period, fiscalYear, quarter, month, year);
    }
}
