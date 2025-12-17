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
     * Get overall dashboard metrics
     * 
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return Dashboard metrics including total activities, opportunities, emails,
     *         response rate
     */
    public ApiResponse<?> getDashboardMetrics(String fromDate, String toDate) {
        try {
            log.info("Fetching dashboard metrics (FromDate: {}, ToDate: {})", fromDate, toDate);

            // Get all staff with email statistics
            List<Staff> staffList = staffService.getAllStaffUnfiltered(null, null, true, fromDate, toDate);

            if (staffList == null || staffList.isEmpty()) {
                log.warn("No staff data available");
                staffList = new ArrayList<>();
            }

            // Calculate aggregated metrics
            int totalActivities = 0;
            int totalEmailsSent = 0;
            int totalEmailsReceived = 0;
            int staffWithResponseData = 0;
            double totalResponseRate = 0.0;

            for (Staff staff : staffList) {
                // Count activities (incoming + outgoing emails)
                totalActivities += staff.getTotalEmailCount();

                // Count emails sent/received
                totalEmailsSent += staff.getOutgoingEmailCount();
                totalEmailsReceived += staff.getIncomingEmailCount();

                // Calculate response rate if staff has incoming emails
                if (staff.getIncomingEmailCount() > 0 && staff.getRespondedEmailCount() > 0) {
                    double responseRate = (double) staff.getRespondedEmailCount() / staff.getIncomingEmailCount() * 100;
                    totalResponseRate += responseRate;
                    staffWithResponseData++;
                }
            }

            // Calculate average response rate
            double avgResponseRate = staffWithResponseData > 0 ? Math.round(totalResponseRate / staffWithResponseData)
                    : 0;

            // Get actual active opportunities count from Dynamics 365
            int activeOpportunities = opportunityService.getActiveOpportunityCount(fromDate, toDate);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalActivities", totalActivities);
            metrics.put("activeOpportunities", activeOpportunities);
            metrics.put("totalEmailsSent", totalEmailsSent);
            metrics.put("averageResponseRate", Math.round(avgResponseRate));
            metrics.put("totalStaff", staffList.size());
            metrics.put("dateRange", Map.of(
                    "from", fromDate != null ? fromDate : "all",
                    "to", toDate != null ? toDate : "all"));

            log.info(
                    "Dashboard metrics calculated: {} activities, {} opportunities (from D365), {} emails, {}% response rate",
                    totalActivities, activeOpportunities, totalEmailsSent, Math.round(avgResponseRate));

            return ApiResponse.success("Dashboard metrics retrieved successfully", metrics);

        } catch (Exception e) {
            log.error("Error fetching dashboard metrics", e);
            return ApiResponse.error("Failed to fetch dashboard metrics: " + e.getMessage());
        }
    }

    /**
     * Get top performing staff members
     * 
     * @param top      Number of top performers to return
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return List of top performers with their metrics
     */
    public ApiResponse<?> getTopPerformers(Integer top, String fromDate, String toDate) {
        try {
            log.info("Fetching top {} performers (FromDate: {}, ToDate: {})", top, fromDate, toDate);

            // Get all staff with email statistics
            List<Staff> staffList = staffService.getAllStaffUnfiltered(null, null, true, fromDate, toDate);

            if (staffList == null || staffList.isEmpty()) {
                return ApiResponse.success("No staff data available", new ArrayList<>());
            }

            // Calculate performance score for each staff
            List<Map<String, Object>> performers = new ArrayList<>();

            for (Staff staff : staffList) {
                // Calculate performance score based on:
                // - Total emails (40%)
                // - Response rate (30%)
                // - Response time (30%)

                double emailScore = staff.getTotalEmailCount();

                double responseRateScore = 0;
                if (staff.getIncomingEmailCount() > 0 && staff.getRespondedEmailCount() > 0) {
                    responseRateScore = (double) staff.getRespondedEmailCount() / staff.getIncomingEmailCount() * 100;
                }

                double responseTimeScore = 0;
                if (staff.getAverageResponseTimeMinutes() != null && staff.getAverageResponseTimeMinutes() > 0) {
                    // Lower response time is better, inverse scoring (max 1440 minutes = 24 hours)
                    responseTimeScore = Math.max(0, 100 - (staff.getAverageResponseTimeMinutes() / 14.4));
                }

                // Weighted score
                double performanceScore = (emailScore * 0.4) + (responseRateScore * 0.3) + (responseTimeScore * 0.3);

                Map<String, Object> performer = new HashMap<>();
                performer.put("staffId", staff.getSystemUserId());
                performer.put("fullname", staff.getFullName());
                performer.put("title", staff.getTitle());
                performer.put("email", staff.getEmail());
                performer.put("totalEmails", staff.getTotalEmailCount());
                performer.put("incomingEmails", staff.getIncomingEmailCount());
                performer.put("outgoingEmails", staff.getOutgoingEmailCount());
                performer.put("responseRate", Math.round(responseRateScore));
                performer.put("averageResponseTimeMinutes", staff.getAverageResponseTimeMinutes());
                performer.put("performanceScore", Math.round(performanceScore * 100) / 100.0);

                performers.add(performer);
            }

            // Sort by performance score and get top N
            List<Map<String, Object>> topPerformers = performers.stream()
                    .sorted((a, b) -> Double.compare(
                            (Double) b.get("performanceScore"),
                            (Double) a.get("performanceScore")))
                    .limit(top)
                    .collect(Collectors.toList());

            log.info("Top {} performers retrieved", topPerformers.size());

            return ApiResponse.success("Top performers retrieved successfully", topPerformers);

        } catch (Exception e) {
            log.error("Error fetching top performers", e);
            return ApiResponse.error("Failed to fetch top performers: " + e.getMessage());
        }
    }

    /**
     * Get email performance data by staff for chart visualization
     * 
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return Email performance data grouped by staff
     */
    public ApiResponse<?> getEmailPerformanceByStaff(String fromDate, String toDate) {
        try {
            log.info("Fetching email performance by staff (FromDate: {}, ToDate: {})", fromDate, toDate);

            // Get all staff with email statistics
            List<Staff> staffList = staffService.getAllStaffUnfiltered(null, null, true, fromDate, toDate);

            if (staffList == null || staffList.isEmpty()) {
                return ApiResponse.success("No staff data available", new ArrayList<>());
            }

            // Filter staff with email activity and prepare chart data
            List<Map<String, Object>> chartData = staffList.stream()
                    .filter(staff -> staff.getTotalEmailCount() > 0)
                    .sorted((a, b) -> Integer.compare(b.getTotalEmailCount(), a.getTotalEmailCount()))
                    .map(staff -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("staffName", staff.getFullName());
                        data.put("incomingEmails", staff.getIncomingEmailCount());
                        data.put("outgoingEmails", staff.getOutgoingEmailCount());
                        data.put("totalEmails", staff.getTotalEmailCount());
                        return data;
                    })
                    .collect(Collectors.toList());

            log.info("Email performance data retrieved for {} staff members", chartData.size());

            return ApiResponse.success("Email performance data retrieved successfully", chartData);

        } catch (Exception e) {
            log.error("Error fetching email performance by staff", e);
            return ApiResponse.error("Failed to fetch email performance data: " + e.getMessage());
        }
    }

    /**
     * Get revenue metrics from opportunities
     * 
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return Revenue metrics including estimated, won, lost, and in-progress
     *         revenue
     */
    public ApiResponse<?> getRevenueMetrics(String fromDate, String toDate) {
        try {
            log.info("Fetching revenue metrics (FromDate: {}, ToDate: {})", fromDate, toDate);

            Map<String, Object> stats = opportunityService.getOpportunityStatistics(fromDate, toDate);

            // Extract revenue-related data
            double totalEstimatedRevenue = (double) stats.getOrDefault("totalEstimatedValue", 0.0);
            double wonRevenue = (double) stats.getOrDefault("wonValue", 0.0);
            int wonCount = (int) stats.getOrDefault("wonOpportunities", 0);
            int lostCount = (int) stats.getOrDefault("lostOpportunities", 0);
            int openCount = (int) stats.getOrDefault("openOpportunities", 0);
            double avgDealSize = (double) stats.getOrDefault("averageDealSize", 0.0);
            double winRate = (double) stats.getOrDefault("winRate", 0.0);

            // Calculate in-progress revenue (estimated value of open opportunities)
            double inProgressRevenue = totalEstimatedRevenue - wonRevenue;

            Map<String, Object> revenueMetrics = new HashMap<>();
            revenueMetrics.put("estimatedRevenue", Math.round(totalEstimatedRevenue * 100.0) / 100.0);
            revenueMetrics.put("wonRevenue", Math.round(wonRevenue * 100.0) / 100.0);
            revenueMetrics.put("inProgressRevenue", Math.round(inProgressRevenue * 100.0) / 100.0);
            revenueMetrics.put("wonCount", wonCount);
            revenueMetrics.put("lostCount", lostCount);
            revenueMetrics.put("openCount", openCount);
            revenueMetrics.put("averageDealSize", Math.round(avgDealSize * 100.0) / 100.0);
            revenueMetrics.put("winRate", Math.round(winRate * 10.0) / 10.0);
            revenueMetrics.put("dateRange", Map.of(
                    "from", fromDate != null ? fromDate : "all",
                    "to", toDate != null ? toDate : "all"));

            log.info("Revenue metrics calculated: Estimated=${}, Won=${}, InProgress=${}, WinRate={}%",
                    totalEstimatedRevenue, wonRevenue, inProgressRevenue, winRate);

            return ApiResponse.success("Revenue metrics retrieved successfully", revenueMetrics);

        } catch (Exception e) {
            log.error("Error fetching revenue metrics", e);
            return ApiResponse.error("Failed to fetch revenue metrics: " + e.getMessage());
        }
    }

    /**
     * Get revenue metrics grouped by month for the past 12 months
     * 
     * @return Monthly revenue breakdown
     */
    public ApiResponse<?> getRevenueByMonth() {
        try {
            log.info("Fetching revenue metrics by month for past 12 months");

            List<Map<String, Object>> monthlyRevenue = new ArrayList<>();

            // Calculate date ranges for past 12 months
            java.time.LocalDate now = java.time.LocalDate.now();

            for (int i = 11; i >= 0; i--) {
                java.time.LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
                java.time.LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

                String fromDate = monthStart.toString();
                String toDate = monthEnd.toString();

                // Get revenue stats for this month
                Map<String, Object> stats = opportunityService.getOpportunityStatistics(fromDate, toDate);

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", monthStart.getMonth().toString());
                monthData.put("year", monthStart.getYear());
                monthData.put("monthLabel", String.format("%s %d",
                        monthStart.getMonth().toString().substring(0, 3),
                        monthStart.getYear()));
                monthData.put("estimatedRevenue",
                        Math.round((double) stats.getOrDefault("totalEstimatedValue", 0.0) * 100.0) / 100.0);
                monthData.put("wonRevenue", Math.round((double) stats.getOrDefault("wonValue", 0.0) * 100.0) / 100.0);
                monthData.put("wonCount", stats.getOrDefault("wonOpportunities", 0));
                monthData.put("openCount", stats.getOrDefault("openOpportunities", 0));
                monthData.put("lostCount", stats.getOrDefault("lostOpportunities", 0));

                monthlyRevenue.add(monthData);
            }

            log.info("Monthly revenue data retrieved for {} months", monthlyRevenue.size());

            return ApiResponse.success("Monthly revenue data retrieved successfully", monthlyRevenue);

        } catch (Exception e) {
            log.error("Error fetching revenue by month", e);
            return ApiResponse.error("Failed to fetch revenue by month: " + e.getMessage());
        }
    }
}
