package com.example.backend.controller;

import com.example.backend.model.ApiResponse;
import com.example.backend.model.Opportunity;
import com.example.backend.service.D365OpportunityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Opportunity endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final D365OpportunityService opportunityService;

    public OpportunityController(D365OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    /**
     * Get all opportunities
     * 
     * @param top      Maximum number of records (optional)
     * @param fromDate Start date filter YYYY-MM-DD (optional)
     * @param toDate   End date filter YYYY-MM-DD (optional)
     * @return List of opportunities
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Opportunity>>> getAllOpportunities(
            @RequestParam(required = false) Integer top,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            log.info("GET /api/opportunities/all - Top: {}, FromDate: {}, ToDate: {}", top, fromDate, toDate);

            List<Opportunity> opportunities = opportunityService.getAllOpportunities(top, null, fromDate, toDate);

            return ResponseEntity.ok(ApiResponse.success(
                    "Opportunities fetched successfully",
                    opportunities));

        } catch (Exception e) {
            log.error("Error fetching opportunities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch opportunities: " + e.getMessage()));
        }
    }

    /**
     * Get opportunity statistics
     * 
     * @param fromDate Start date filter YYYY-MM-DD (optional)
     * @param toDate   End date filter YYYY-MM-DD (optional)
     * @return Statistics map
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOpportunityStatistics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            log.info("GET /api/opportunities/stats - FromDate: {}, ToDate: {}", fromDate, toDate);

            Map<String, Object> stats = opportunityService.getOpportunityStatistics(fromDate, toDate);

            return ResponseEntity.ok(ApiResponse.success(
                    "Statistics calculated successfully",
                    stats));

        } catch (Exception e) {
            log.error("Error calculating opportunity statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to calculate statistics: " + e.getMessage()));
        }
    }

    /**
     * Get opportunities grouped by staff member
     * 
     * @param fromDate Start date filter YYYY-MM-DD (optional)
     * @param toDate   End date filter YYYY-MM-DD (optional)
     * @return Map of staff ID to their opportunity statistics
     */
    @GetMapping("/by-staff")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> getOpportunitiesByStaff(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            log.info("GET /api/opportunities/by-staff - FromDate: {}, ToDate: {}", fromDate, toDate);

            Map<String, Map<String, Object>> staffStats = opportunityService.getOpportunitiesByStaff(fromDate,
                    toDate);

            return ResponseEntity.ok(ApiResponse.success(
                    "Staff opportunities fetched successfully",
                    staffStats));

        } catch (Exception e) {
            log.error("Error fetching opportunities by staff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch staff opportunities: " + e.getMessage()));
        }
    }

    /**
     * Get top opportunities by estimated value
     * 
     * @param top      Number of top opportunities (default: 10)
     * @param fromDate Start date filter YYYY-MM-DD (optional)
     * @param toDate   End date filter YYYY-MM-DD (optional)
     * @return List of top opportunities
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<Opportunity>>> getTopOpportunities(
            @RequestParam(required = false, defaultValue = "10") Integer top,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            log.info("GET /api/opportunities/top - Top: {}, FromDate: {}, ToDate: {}", top, fromDate, toDate);

            List<Opportunity> opportunities = opportunityService.getTopOpportunities(top, fromDate, toDate);

            return ResponseEntity.ok(ApiResponse.success(
                    "Top opportunities fetched successfully",
                    opportunities));

        } catch (Exception e) {
            log.error("Error fetching top opportunities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch top opportunities: " + e.getMessage()));
        }
    }

    /**
     * Get a single opportunity by ID
     * 
     * @param id Opportunity ID
     * @return Opportunity details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Opportunity>> getOpportunityById(@PathVariable String id) {
        try {
            log.info("GET /api/opportunities/{}", id);

            Opportunity opportunity = opportunityService.getOpportunityById(id);

            return ResponseEntity.ok(ApiResponse.success(
                    "Opportunity fetched successfully",
                    opportunity));

        } catch (Exception e) {
            log.error("Error fetching opportunity {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch opportunity: " + e.getMessage()));
        }
    }

    /**
     * Get count of active opportunities
     * 
     * @param fromDate Start date filter YYYY-MM-DD (optional)
     * @param toDate   End date filter YYYY-MM-DD (optional)
     * @return Count of active opportunities
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getActiveOpportunityCount(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            log.info("GET /api/opportunities/count - FromDate: {}, ToDate: {}", fromDate, toDate);

            int count = opportunityService.getActiveOpportunityCount(fromDate, toDate);

            return ResponseEntity.ok(ApiResponse.success(
                    "Count fetched successfully",
                    count));

        } catch (Exception e) {
            log.error("Error fetching opportunity count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch count: " + e.getMessage()));
        }
    }

    /**
     * Get monthly trend data for opportunities
     * 
     * @param months Number of months to fetch (default: 6)
     * @return Monthly aggregated statistics
     */
    @GetMapping("/monthly-trends")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int months) {
        try {
            log.info("GET /api/opportunities/monthly-trends - Months: {}", months);

            List<Map<String, Object>> trends = opportunityService.getMonthlyTrends(months);

            return ResponseEntity.ok(ApiResponse.success(
                    "Monthly trends fetched successfully",
                    trends));

        } catch (Exception e) {
            log.error("Error fetching monthly trends", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch monthly trends: " + e.getMessage()));
        }
    }
}
