package com.example.backend.service;

import com.example.backend.model.ApiResponse;
import com.example.backend.model.Staff;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for dashboard-level data aggregation and analytics
 */
@Service
public class D365DashboardService {

    private static final Logger log = LoggerFactory.getLogger(D365DashboardService.class);

    @Autowired
    private D365StaffService staffService;

    @Autowired
    private D365ActivityService activityService;

    @Autowired
    private D365OpportunityService opportunityService;

    /**
     * Get fiscal year date range
     * Fiscal year: June (current year) to July (next year)
     */
    private Map<String, String> getFiscalYearDateRange(String fiscalYear) {
        int year = fiscalYear != null ? Integer.parseInt(fiscalYear) : java.time.LocalDate.now().getYear();

        // If current month is before June, use previous fiscal year
        if (fiscalYear == null && java.time.LocalDate.now().getMonthValue() < 6) {
            year = year - 1;
        }

        String fromDate = String.format("%d-06-01", year);
        String toDate = String.format("%d-07-31", year + 1);

        Map<String, String> range = new HashMap<>();
        range.put("fromDate", fromDate);
        range.put("toDate", toDate);
        range.put("fiscalYear", String.format("FY%d-%d", year, year + 1));
        return range;
    }

    /**
     * Get quarter date range within fiscal year
     * Q1: Jun-Aug, Q2: Sep-Nov, Q3: Dec-Feb, Q4: Mar-May
     */
    private Map<String, String> getQuarterDateRange(Integer quarter, String fiscalYear) {
        Map<String, String> fyRange = getFiscalYearDateRange(fiscalYear);
        int year = Integer.parseInt(fyRange.get("fromDate").substring(0, 4));

        int currentQuarter = quarter != null ? quarter : getCurrentFiscalQuarter();

        String fromDate, toDate;
        switch (currentQuarter) {
            case 1: // Jun-Aug
                fromDate = String.format("%d-06-01", year);
                toDate = String.format("%d-08-31", year);
                break;
            case 2: // Sep-Nov
                fromDate = String.format("%d-09-01", year);
                toDate = String.format("%d-11-30", year);
                break;
            case 3: // Dec-Feb
                fromDate = String.format("%d-12-01", year);
                toDate = String.format("%d-02-28", year + 1);
                break;
            case 4: // Mar-May
                fromDate = String.format("%d-03-01", year + 1);
                toDate = String.format("%d-05-31", year + 1);
                break;
            default:
                fromDate = fyRange.get("fromDate");
                toDate = fyRange.get("toDate");
        }

        Map<String, String> range = new HashMap<>();
        range.put("fromDate", fromDate);
        range.put("toDate", toDate);
        range.put("quarter", "Q" + currentQuarter);
        return range;
    }

    /**
     * Get current fiscal quarter (1-4)
     */
    private int getCurrentFiscalQuarter() {
        int month = java.time.LocalDate.now().getMonthValue();
        if (month >= 6 && month <= 8)
            return 1;
        if (month >= 9 && month <= 11)
            return 2;
        if (month >= 12 || month <= 2)
            return 3;
        return 4; // Mar-May
    }

    /**
     * Get fiscal year revenue metrics
     */
    public ApiResponse<?> getFiscalYearMetrics(String fiscalYear) {
        try {
            Map<String, String> range = getFiscalYearDateRange(fiscalYear);
            int currentQuarter = getCurrentFiscalQuarter();
            String today = java.time.LocalDate.now().toString();
            log.info("Fetching fiscal year metrics for {} (Q1 through Q{} up to {})",
                    range.get("fiscalYear"), currentQuarter, today);

            double totalWonRevenue = 0.0;
            double totalEstimatedRevenue = 0.0;
            double totalLostRevenue = 0.0;

            // Sum revenue from Q1 through current quarter
            for (int q = 1; q <= currentQuarter; q++) {
                Map<String, String> quarterRange = getQuarterDateRange(q, fiscalYear);

                // For the current quarter, use today's date instead of quarter end date
                String endDate = (q == currentQuarter) ? today : quarterRange.get("toDate");

                log.info("Loading Q{} data from {} to {}", q, quarterRange.get("fromDate"), endDate);

                Map<String, Object> quarterMetrics = opportunityService.getRevenueMetrics(
                        quarterRange.get("fromDate"), endDate);

                totalWonRevenue += (double) quarterMetrics.getOrDefault("wonRevenue", 0.0);
                totalEstimatedRevenue += (double) quarterMetrics.getOrDefault("estimatedRevenue", 0.0);
                totalLostRevenue += (double) quarterMetrics.getOrDefault("lostRevenue", 0.0);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fiscalYear", range.get("fiscalYear"));
            result.put("fromDate", range.get("fromDate"));
            result.put("toDate", range.get("toDate"));
            result.put("currentQuarter", currentQuarter);
            result.put("targetRevenue", 18000000.0); // Fixed target
            result.put("wonRevenue", totalWonRevenue);
            result.put("estimatedRevenue", totalEstimatedRevenue);
            result.put("lostRevenue", totalLostRevenue);

            return ApiResponse.success("Fiscal year metrics retrieved", result);
        } catch (Exception e) {
            log.error("Error fetching fiscal year metrics", e);
            return ApiResponse.error("Failed to fetch fiscal year metrics: " + e.getMessage());
        }
    }

    /**
     * Get quarterly revenue metrics
     */
    public ApiResponse<?> getQuarterlyMetrics(Integer quarter, String fiscalYear) {
        try {
            Map<String, String> range = getQuarterDateRange(quarter, fiscalYear);
            log.info("Fetching quarterly metrics for {}", range.get("quarter"));

            Map<String, Object> revenueMetrics = opportunityService.getRevenueMetrics(
                    range.get("fromDate"), range.get("toDate"));

            Map<String, Object> result = new HashMap<>();
            result.put("quarter", range.get("quarter"));
            result.put("fromDate", range.get("fromDate"));
            result.put("toDate", range.get("toDate"));
            result.put("targetRevenue", 4500000.0); // 18M / 4 quarters
            result.put("wonRevenue", revenueMetrics.getOrDefault("wonRevenue", 0.0));
            result.put("estimatedRevenue", revenueMetrics.getOrDefault("estimatedRevenue", 0.0));
            result.put("lostRevenue", revenueMetrics.getOrDefault("lostRevenue", 0.0));

            return ApiResponse.success("Quarterly metrics retrieved", result);
        } catch (Exception e) {
            log.error("Error fetching quarterly metrics", e);
            return ApiResponse.error("Failed to fetch quarterly metrics: " + e.getMessage());
        }
    }

    /**
     * Get monthly opportunity statistics
     */
    public ApiResponse<?> getMonthlyOpportunities(Integer month, Integer year) {
        try {
            int currentMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();
            int currentYear = year != null ? year : java.time.LocalDate.now().getYear();

            String fromDate = String.format("%d-%02d-01", currentYear, currentMonth);
            java.time.LocalDate endDate = java.time.LocalDate.of(currentYear, currentMonth, 1)
                    .plusMonths(1).minusDays(1);
            String toDate = endDate.toString();

            Map<String, Object> stats = opportunityService.getOpportunityStats(fromDate, toDate);

            Map<String, Object> result = new HashMap<>();
            result.put("month", currentMonth);
            result.put("year", currentYear);
            result.put("totalOpportunities", stats.getOrDefault("totalOpportunities", 0));
            result.put("wonOpportunities", stats.getOrDefault("wonOpportunities", 0));
            result.put("lostOpportunities", stats.getOrDefault("lostOpportunities", 0));
            result.put("openOpportunities", stats.getOrDefault("openOpportunities", 0));

            return ApiResponse.success("Monthly opportunities retrieved", result);
        } catch (Exception e) {
            log.error("Error fetching monthly opportunities", e);
            return ApiResponse.error("Failed to fetch monthly opportunities: " + e.getMessage());
        }
    }

    /**
     * Get quarterly opportunity statistics
     */
    public ApiResponse<?> getQuarterlyOpportunities(Integer quarter, String fiscalYear) {
        try {
            Map<String, String> range = getQuarterDateRange(quarter, fiscalYear);
            int currentQuarter = getCurrentFiscalQuarter();
            String today = java.time.LocalDate.now().toString();

            // For the current quarter, use today's date instead of quarter end date
            String endDate = (quarter == null || quarter == currentQuarter) ? today : range.get("toDate");

            log.info("Fetching quarterly opportunities from {} to {}", range.get("fromDate"), endDate);

            Map<String, Object> stats = opportunityService.getOpportunityStats(
                    range.get("fromDate"), endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("quarter", range.get("quarter"));
            result.put("totalOpportunities", stats.getOrDefault("totalOpportunities", 0));
            result.put("wonOpportunities", stats.getOrDefault("wonOpportunities", 0));
            result.put("lostOpportunities", stats.getOrDefault("lostOpportunities", 0));
            result.put("openOpportunities", stats.getOrDefault("openOpportunities", 0));

            return ApiResponse.success("Quarterly opportunities retrieved", result);
        } catch (Exception e) {
            log.error("Error fetching quarterly opportunities", e);
            return ApiResponse.error("Failed to fetch quarterly opportunities: " + e.getMessage());
        }
    }

    /**
     * Get fiscal year opportunity statistics
     * Sums Q1 through current quarter up to today
     */
    public ApiResponse<?> getFiscalYearOpportunities(String fiscalYear) {
        try {
            Map<String, String> range = getFiscalYearDateRange(fiscalYear);
            int currentQuarter = getCurrentFiscalQuarter();
            String today = java.time.LocalDate.now().toString();

            log.info("Fetching fiscal year opportunities for {} (Q1 through Q{} up to {})",
                    range.get("fiscalYear"), currentQuarter, today);

            int totalOpportunities = 0;
            int wonOpportunities = 0;
            int lostOpportunities = 0;
            int openOpportunities = 0;

            // Sum opportunities from Q1 through current quarter
            for (int q = 1; q <= currentQuarter; q++) {
                Map<String, String> quarterRange = getQuarterDateRange(q, fiscalYear);

                // For the current quarter, use today's date instead of quarter end date
                String endDate = (q == currentQuarter) ? today : quarterRange.get("toDate");

                log.info("Loading Q{} opportunities from {} to {}", q, quarterRange.get("fromDate"), endDate);

                Map<String, Object> stats = opportunityService.getOpportunityStats(
                        quarterRange.get("fromDate"), endDate);

                totalOpportunities += (int) stats.getOrDefault("totalOpportunities", 0);
                wonOpportunities += (int) stats.getOrDefault("wonOpportunities", 0);
                lostOpportunities += (int) stats.getOrDefault("lostOpportunities", 0);
                openOpportunities += (int) stats.getOrDefault("openOpportunities", 0);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fiscalYear", range.get("fiscalYear"));
            result.put("currentQuarter", currentQuarter);
            result.put("totalOpportunities", totalOpportunities);
            result.put("wonOpportunities", wonOpportunities);
            result.put("lostOpportunities", lostOpportunities);
            result.put("openOpportunities", openOpportunities);

            return ApiResponse.success("Fiscal year opportunities retrieved", result);
        } catch (Exception e) {
            log.error("Error fetching fiscal year opportunities", e);
            return ApiResponse.error("Failed to fetch fiscal year opportunities: " + e.getMessage());
        }
    }

    /**
     * Get top staff performance based on won opportunities
     */
    public ApiResponse<?> getTopStaffPerformance(String period, String fiscalYear,
            Integer quarter, Integer month, Integer year) {
        try {
            String fromDate, toDate;

            switch (period.toLowerCase()) {
                case "quarter":
                    Map<String, String> qRange = getQuarterDateRange(quarter, fiscalYear);
                    fromDate = qRange.get("fromDate");
                    toDate = qRange.get("toDate");
                    break;
                case "month":
                    int m = month != null ? month : java.time.LocalDate.now().getMonthValue();
                    int y = year != null ? year : java.time.LocalDate.now().getYear();
                    fromDate = String.format("%d-%02d-01", y, m);
                    java.time.LocalDate endDate = java.time.LocalDate.of(y, m, 1)
                            .plusMonths(1).minusDays(1);
                    toDate = endDate.toString();
                    break;
                default: // fiscal-year
                    Map<String, String> fyRange = getFiscalYearDateRange(fiscalYear);
                    fromDate = fyRange.get("fromDate");
                    toDate = fyRange.get("toDate");
            }

            // Get staff with won opportunities
            List<Map<String, Object>> topStaff = opportunityService.getTopStaffByWonOpportunities(
                    fromDate, toDate, 5);

            return ApiResponse.success("Top staff performance retrieved", topStaff);
        } catch (Exception e) {
            log.error("Error fetching top staff performance", e);
            return ApiResponse.error("Failed to fetch top staff performance: " + e.getMessage());
        }
    }
}
