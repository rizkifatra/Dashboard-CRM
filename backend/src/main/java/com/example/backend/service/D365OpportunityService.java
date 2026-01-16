package com.example.backend.service;

import com.example.backend.config.D365Config;
import com.example.backend.model.D365Response;
import com.example.backend.model.Opportunity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Service for interacting with Dynamics 365 Opportunity entity
 */
@Service
@Slf4j
public class D365OpportunityService {

    private final D365Config d365Config;
    private final D365AuthService authService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final D365StaffService staffService;

    public D365OpportunityService(D365Config d365Config, D365AuthService authService, D365StaffService staffService) {
        this.d365Config = d365Config;
        this.authService = authService;
        this.staffService = staffService;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(d365Config.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .defaultHeader("OData-MaxVersion", "4.0")
                .defaultHeader("OData-Version", "4.0")
                .defaultHeader("Prefer", "return=representation")
                .build();
    }

    /**
     * Get count of active (open) opportunities
     * 
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     * @param toDate   Optional end date filter (YYYY-MM-DD)
     * @return Count of active opportunities
     */
    public int getActiveOpportunityCount(String fromDate, String toDate) {
        try {
            log.info("Fetching active opportunity count. FromDate: {}, ToDate: {}", fromDate, toDate);

            String token = authService.getAccessToken();

            // Build query - only count, no data needed
            StringBuilder uri = new StringBuilder("/opportunities?$count=true&$top=1");

            // Filter for open opportunities (statecode = 0)
            StringBuilder filter = new StringBuilder("$filter=statecode eq 0");

            // Add date filters if provided
            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append(" and createdon ge ").append(fromDate);
            }

            if (toDate != null && !toDate.isEmpty()) {
                filter.append(" and createdon le ").append(toDate);
            }

            uri.append("&").append(filter.toString());

            String response = webClient.get()
                    .uri(uri.toString())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response,
                    new TypeReference<D365Response<Opportunity>>() {
                    });

            int count = d365Response.getCount() != null ? d365Response.getCount() : 0;
            log.info("Active opportunity count: {}", count);
            return count;

        } catch (WebClientResponseException e) {
            log.error("Error fetching opportunity count. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return 0;
        } catch (Exception e) {
            log.error("Error fetching opportunity count", e);
            return 0;
        }
    }

    /**
     * Get all opportunities from Dynamics 365
     * 
     * @param top      Maximum number of records to return (optional)
     * @param skip     Number of records to skip for pagination (optional)
     * @param search   Search term for filtering (optional)
     * @param select   Comma-separated list of fields to return (optional)
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     * @param toDate   Optional end date filter (YYYY-MM-DD)
     * @return List of opportunities
     */
    public List<Opportunity> getAllOpportunities(Integer top, Integer skip, String search, String select,
            String fromDate,
            String toDate) {
        try {
            log.info(
                    "Fetching opportunities from Dynamics 365. Top: {}, Skip: {}, Search: {}, Select: {}, FromDate: {}, ToDate: {}",
                    top, skip, search, select, fromDate, toDate);

            String token = authService.getAccessToken();

            // Build query parameters - Use $top to limit batch size, ignore $skip (same as
            // AccountService)
            // Fetch limited records from D365 and do pagination in-memory
            StringBuilder uri = new StringBuilder("/opportunities?");
            StringBuilder filter = new StringBuilder();

            // Use $top to limit the batch size fetched from D365
            if (top != null && top > 0) {
                uri.append("$top=").append(top).append("&");
            }

            if (select != null && !select.isEmpty()) {
                uri.append("$select=").append(select).append("&");
            } else {
                // Default fields to select with expanded navigation properties
                uri.append("$select=opportunityid,name,description,estimatedvalue,estimatedclosedate,")
                        .append("actualvalue,actualclosedate,closeprobability,salesstage,stepname,budgetamount,")
                        .append("createdon,modifiedon,statecode,statuscode,")
                        .append("_ownerid_value,_createdby_value,_modifiedby_value,_customerid_value,_parentaccountid_value&");

                // Expand navigation properties to get related entity details
                // Note: ownerid navigation property is not directly expandable for opportunity
                uri.append("$expand=customerid_account($select=name,accountid),")
                        .append("customerid_contact($select=fullname,contactid),")
                        .append("parentaccountid($select=name,accountid)&");
            }

            // Add search filter - search across opportunity name, description, and step
            // name
            if (search != null && !search.isEmpty()) {
                String searchTerm = search.trim().replace("'", "''"); // Escape single quotes
                filter.append("(contains(name, '").append(searchTerm).append("')");
                filter.append(" or contains(description, '").append(searchTerm).append("')");
                filter.append(" or contains(stepname, '").append(searchTerm).append("'))");
            }

            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append(filter.length() > 0 ? " and " : "")
                        .append("createdon ge ").append(fromDate);
            }

            if (toDate != null && !toDate.isEmpty()) {
                filter.append(filter.length() > 0 ? " and " : "")
                        .append("createdon le ").append(toDate);
            }

            if (filter.length() > 0) {
                uri.append("$filter=").append(filter.toString()).append("&");
            }

            // Add ordering
            uri.append("$orderby=createdon desc&");

            // Add count
            uri.append("$count=true");

            String response = webClient.get()
                    .uri(uri.toString())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            log.debug("D365 Opportunities Response (first 500 chars): {}",
                    response != null && response.length() > 500 ? response.substring(0, 500) : response);

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response,
                    new TypeReference<D365Response<Opportunity>>() {
                    });

            List<Opportunity> opportunities = d365Response.getValue();
            if (opportunities == null) {
                log.warn("D365 returned null opportunities list, returning empty list");
                return new java.util.ArrayList<>();
            }

            log.info("Successfully fetched {} opportunities from D365", opportunities.size());
            return opportunities;

        } catch (WebClientResponseException e) {
            log.error("Error fetching opportunities. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching opportunities. Exception: {}, Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Get a single opportunity by ID
     * 
     * @param opportunityId The ID of the opportunity
     * @return The opportunity if found
     */
    public Opportunity getOpportunityById(String opportunityId) {
        try {
            log.info("Fetching opportunity: {}", opportunityId);

            String token = authService.getAccessToken();

            String uri = String.format("/opportunities(%s)", opportunityId);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            Opportunity opportunity = objectMapper.readValue(response, Opportunity.class);
            log.info("Successfully fetched opportunity: {}", opportunity.getName());
            return opportunity;

        } catch (WebClientResponseException e) {
            log.error("Error fetching opportunity. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch opportunity: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching opportunity", e);
            throw new RuntimeException("Failed to fetch opportunity: " + e.getMessage(), e);
        }
    }

    /**
     * Get opportunity statistics
     * 
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return Map with statistics (total, open, won, lost, total value, etc.)
     */
    public java.util.Map<String, Object> getOpportunityStatistics(String fromDate, String toDate) {
        try {
            log.info("Calculating opportunity statistics. FromDate: {}, ToDate: {}", fromDate, toDate);

            List<Opportunity> allOpportunities = getAllOpportunities(null, null, null, null, fromDate, toDate);

            int totalOpportunities = allOpportunities.size();
            int openOpportunities = 0;
            int wonOpportunities = 0;
            int lostOpportunities = 0;
            double totalEstimatedValue = 0.0;
            double totalActualValue = 0.0;
            double wonValue = 0.0;

            for (Opportunity opp : allOpportunities) {
                // Count by state: 0=Open, 1=Won, 2=Lost
                if (opp.getStateCode() != null) {
                    switch (opp.getStateCode()) {
                        case 0:
                            openOpportunities++;
                            break;
                        case 1:
                            wonOpportunities++;
                            if (opp.getActualValue() != null) {
                                wonValue += opp.getActualValue().doubleValue();
                            }
                            break;
                        case 2:
                            lostOpportunities++;
                            break;
                    }
                }

                // Sum values
                if (opp.getEstimatedValue() != null) {
                    totalEstimatedValue += opp.getEstimatedValue().doubleValue();
                }
                if (opp.getActualValue() != null) {
                    totalActualValue += opp.getActualValue().doubleValue();
                }
            }

            // Calculate win rate
            double winRate = (wonOpportunities + lostOpportunities) > 0
                    ? (double) wonOpportunities / (wonOpportunities + lostOpportunities) * 100
                    : 0.0;

            // Calculate average deal size
            double avgDealSize = wonOpportunities > 0 ? wonValue / wonOpportunities : 0.0;

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalOpportunities", totalOpportunities);
            stats.put("openOpportunities", openOpportunities);
            stats.put("wonOpportunities", wonOpportunities);
            stats.put("lostOpportunities", lostOpportunities);
            stats.put("totalEstimatedValue", totalEstimatedValue);
            stats.put("totalActualValue", totalActualValue);
            stats.put("wonValue", wonValue);
            stats.put("winRate", winRate);
            stats.put("averageDealSize", avgDealSize);

            log.info("Opportunity statistics: Total={}, Open={}, Won={}, Lost={}, WinRate={:.2f}%, AvgDealSize={:.2f}",
                    totalOpportunities, openOpportunities, wonOpportunities, lostOpportunities, winRate, avgDealSize);

            return stats;

        } catch (Exception e) {
            log.error("Error calculating opportunity statistics", e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * Get opportunities grouped by staff member
     * 
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return Map with staff ID as key and their opportunity stats as value
     */
    public java.util.Map<String, java.util.Map<String, Object>> getOpportunitiesByStaff(String fromDate,
            String toDate) {
        try {
            log.info("Fetching opportunities by staff. FromDate: {}, ToDate: {}", fromDate, toDate);

            List<Opportunity> allOpportunities = getAllOpportunities(null, null, null, null, fromDate, toDate);

            java.util.Map<String, java.util.Map<String, Object>> staffStats = new java.util.HashMap<>();

            for (Opportunity opp : allOpportunities) {
                if (opp.getOwnerId() == null)
                    continue;

                String ownerId = opp.getOwnerId();

                // Initialize stats for this staff member if not exists
                if (!staffStats.containsKey(ownerId)) {
                    java.util.Map<String, Object> stats = new java.util.HashMap<>();
                    stats.put("ownerId", ownerId);
                    stats.put("ownerName", null); // Will be fetched later
                    stats.put("totalOpportunities", 0);
                    stats.put("openOpportunities", 0);
                    stats.put("wonOpportunities", 0);
                    stats.put("lostOpportunities", 0);
                    stats.put("totalEstimatedValue", 0.0);
                    stats.put("wonValue", 0.0);
                    stats.put("winRate", 0.0);
                    stats.put("averageDealSize", 0.0);
                    staffStats.put(ownerId, stats);
                }

                java.util.Map<String, Object> stats = staffStats.get(ownerId);

                // Update counts
                stats.put("totalOpportunities", (Integer) stats.get("totalOpportunities") + 1);

                if (opp.getStateCode() != null) {
                    switch (opp.getStateCode()) {
                        case 0:
                            stats.put("openOpportunities", (Integer) stats.get("openOpportunities") + 1);
                            break;
                        case 1:
                            stats.put("wonOpportunities", (Integer) stats.get("wonOpportunities") + 1);
                            if (opp.getActualValue() != null) {
                                stats.put("wonValue",
                                        (Double) stats.get("wonValue") + opp.getActualValue().doubleValue());
                            }
                            break;
                        case 2:
                            stats.put("lostOpportunities", (Integer) stats.get("lostOpportunities") + 1);
                            break;
                    }
                }

                if (opp.getEstimatedValue() != null) {
                    stats.put("totalEstimatedValue",
                            (Double) stats.get("totalEstimatedValue") + opp.getEstimatedValue().doubleValue());
                }
            }

            // Calculate derived metrics for each staff
            for (java.util.Map<String, Object> stats : staffStats.values()) {
                int won = (Integer) stats.get("wonOpportunities");
                int lost = (Integer) stats.get("lostOpportunities");
                double wonValue = (Double) stats.get("wonValue");

                // Win rate
                double winRate = (won + lost) > 0 ? (double) won / (won + lost) * 100 : 0.0;
                stats.put("winRate", winRate);

                // Average deal size
                double avgDealSize = won > 0 ? wonValue / won : 0.0;
                stats.put("averageDealSize", avgDealSize);
            }

            // Fetch actual staff names from D365
            for (java.util.Map<String, Object> stats : staffStats.values()) {
                String ownerId = (String) stats.get("ownerId");
                try {
                    // Fetch staff details by ID
                    var staffOptional = staffService.getStaffById(ownerId, false, null, null);
                    if (staffOptional.isPresent()) {
                        stats.put("ownerName", staffOptional.get().getFullName());
                    } else {
                        // Fallback to ID if staff not found
                        stats.put("ownerName", "Staff " + ownerId.substring(0, Math.min(8, ownerId.length())));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch name for staff ID: {}, using default", ownerId);
                    stats.put("ownerName", "Staff " + ownerId.substring(0, Math.min(8, ownerId.length())));
                }
            }

            log.info("Fetched opportunity stats for {} staff members", staffStats.size());
            return staffStats;

        } catch (Exception e) {
            log.error("Error fetching opportunities by staff", e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * Get top opportunities by estimated value
     * 
     * @param top      Number of top opportunities to return
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return List of top opportunities
     */
    public List<Opportunity> getTopOpportunities(Integer top, String fromDate, String toDate) {
        try {
            log.info("Fetching top {} opportunities. FromDate: {}, ToDate: {}", top, fromDate, toDate);

            List<Opportunity> opportunities = getAllOpportunities(null, null, null, null, fromDate, toDate);

            // Sort by estimated value (descending) and filter open opportunities
            return opportunities.stream()
                    .filter(opp -> opp.getStateCode() != null && opp.getStateCode() == 0) // Only open
                    .sorted((a, b) -> {
                        double valA = a.getEstimatedValue() != null ? a.getEstimatedValue().doubleValue() : 0.0;
                        double valB = b.getEstimatedValue() != null ? b.getEstimatedValue().doubleValue() : 0.0;
                        return Double.compare(valB, valA); // Descending
                    })
                    .limit(top != null ? top : 10)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching top opportunities", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Get monthly trends for opportunities
     * 
     * @param months Number of months to include in trends
     * @return List of monthly statistics
     */
    public List<java.util.Map<String, Object>> getMonthlyTrends(int months) {
        try {
            log.info("Fetching monthly trends for {} months", months);

            java.util.List<java.util.Map<String, Object>> trends = new java.util.ArrayList<>();
            java.time.LocalDate endDate = java.time.LocalDate.now();

            for (int i = months - 1; i >= 0; i--) {
                java.time.LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
                java.time.LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

                String fromDate = monthStart.toString();
                String toDate = monthEnd.toString();

                // Fetch opportunities for this month
                List<Opportunity> opportunities = getAllOpportunities(null, null, null, null, fromDate, toDate);

                // Calculate statistics
                long totalCount = opportunities.size();
                long openCount = opportunities.stream()
                        .filter(opp -> opp.getStateCode() != null && opp.getStateCode() == 0)
                        .count();
                long wonCount = opportunities.stream()
                        .filter(opp -> opp.getStateCode() != null && opp.getStateCode() == 1)
                        .count();
                long lostCount = opportunities.stream()
                        .filter(opp -> opp.getStateCode() != null && opp.getStateCode() == 2)
                        .count();

                double totalValue = opportunities.stream()
                        .filter(opp -> opp.getEstimatedValue() != null)
                        .mapToDouble(opp -> opp.getEstimatedValue().doubleValue())
                        .sum();

                double wonValue = opportunities.stream()
                        .filter(opp -> opp.getStateCode() != null && opp.getStateCode() == 1
                                && opp.getActualValue() != null)
                        .mapToDouble(opp -> opp.getActualValue().doubleValue())
                        .sum();

                double winRate = (totalCount > 0) ? ((double) wonCount / totalCount * 100) : 0.0;

                // Build month data
                java.util.Map<String, Object> monthData = new java.util.HashMap<>();
                monthData.put("month", monthStart.getMonth().toString().substring(0, 3) + " " + monthStart.getYear());
                monthData.put("monthKey", monthStart.toString().substring(0, 7)); // YYYY-MM
                monthData.put("total", totalCount);
                monthData.put("open", openCount);
                monthData.put("won", wonCount);
                monthData.put("lost", lostCount);
                monthData.put("totalValue", Math.round(totalValue));
                monthData.put("wonValue", Math.round(wonValue));
                monthData.put("winRate", Math.round(winRate * 10) / 10.0);

                trends.add(monthData);
            }

            log.info("Generated monthly trends for {} months", trends.size());
            return trends;

        } catch (Exception e) {
            log.error("Error generating monthly trends", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Get top staff by won opportunities
     * 
     * @param fromDate Start date filter
     * @param toDate   End date filter
     * @param top      Number of top staff to return
     * @return List of staff with won opportunity counts and revenue
     */
    public List<java.util.Map<String, Object>> getTopStaffByWonOpportunities(
            String fromDate, String toDate, int top) {
        try {
            log.info("Fetching top {} staff by won opportunities", top);

            // Build filter for won opportunities
            StringBuilder filter = new StringBuilder();
            filter.append("statecode eq 1 and statuscode eq 3"); // Won opportunities

            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append(" and createdon ge ").append(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                filter.append(" and createdon le ").append(toDate);
            }

            String token = authService.getAccessToken();
            String uri = String.format("/opportunities?$filter=%s&$select=_ownerid_value,estimatedvalue",
                    java.net.URLEncoder.encode(filter.toString(), "UTF-8"));

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response, new TypeReference<D365Response<Opportunity>>() {
                    });

            // Group by owner and calculate stats
            java.util.Map<String, java.util.Map<String, Object>> staffStats = new java.util.HashMap<>();

            for (Opportunity opp : d365Response.getValue()) {
                String ownerId = opp.getOwnerId();
                if (ownerId == null)
                    continue;

                staffStats.putIfAbsent(ownerId, new java.util.HashMap<>());
                java.util.Map<String, Object> stats = staffStats.get(ownerId);

                stats.put("ownerId", ownerId);
                stats.put("wonCount", (int) stats.getOrDefault("wonCount", 0) + 1);
                double currentRevenue = ((Number) stats.getOrDefault("wonRevenue", 0.0)).doubleValue();
                double oppValue = opp.getEstimatedValue() != null ? opp.getEstimatedValue().doubleValue() : 0.0;
                stats.put("wonRevenue", currentRevenue + oppValue);
            }

            // Get staff names
            List<com.example.backend.model.Staff> allStaff = staffService.getAllStaffUnfiltered(
                    null, null, false, null, null);
            java.util.Map<String, String> staffNames = new java.util.HashMap<>();
            for (com.example.backend.model.Staff staff : allStaff) {
                staffNames.put(staff.getSystemUserId(), staff.getFullName());
            }

            // Convert to list and add names
            List<java.util.Map<String, Object>> result = new java.util.ArrayList<>(staffStats.values());
            for (java.util.Map<String, Object> stats : result) {
                String ownerId = (String) stats.get("ownerId");
                stats.put("ownerName", staffNames.getOrDefault(ownerId, "Unknown"));
            }

            // Sort by won count descending
            result.sort((a, b) -> Integer.compare((int) b.getOrDefault("wonCount", 0),
                    (int) a.getOrDefault("wonCount", 0)));

            // Return top N
            return result.size() > top ? result.subList(0, top) : result;

        } catch (Exception e) {
            log.error("Error fetching top staff by won opportunities", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Get opportunity statistics for a date range
     * 
     * @param fromDate Start date
     * @param toDate   End date
     * @return Map with opportunity counts
     */
    public java.util.Map<String, Object> getOpportunityStats(String fromDate, String toDate) {
        try {
            log.info("Fetching opportunity stats from {} to {}", fromDate, toDate);

            StringBuilder filter = new StringBuilder();
            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append("createdon ge ").append(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                if (filter.length() > 0)
                    filter.append(" and ");
                filter.append("createdon le ").append(toDate);
            }

            String token = authService.getAccessToken();
            String uri = "/opportunities?$select=statecode,statuscode,estimatedvalue";
            if (filter.length() > 0) {
                uri += "&$filter=" + java.net.URLEncoder.encode(filter.toString(), "UTF-8");
            }

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response, new TypeReference<D365Response<Opportunity>>() {
                    });

            int totalCount = d365Response.getValue().size();
            int wonCount = 0, lostCount = 0, openCount = 0;

            for (Opportunity opp : d365Response.getValue()) {
                if (opp.getStateCode() != null) {
                    if (opp.getStateCode() == 1 && opp.getStatusCode() == 3) {
                        // Won: StateCode=1, StatusCode=3
                        wonCount++;
                    } else if (opp.getStateCode() == 2) {
                        // Lost: StateCode=2 (any statuscode like 4, 5, etc.)
                        lostCount++;
                    } else if (opp.getStateCode() == 0) {
                        // Open: StateCode=0
                        openCount++;
                    }
                }
            }

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalOpportunities", totalCount);
            stats.put("wonOpportunities", wonCount);
            stats.put("lostOpportunities", lostCount);
            stats.put("openOpportunities", openCount);

            return stats;

        } catch (Exception e) {
            log.error("Error fetching opportunity stats", e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * Get revenue metrics for a date range
     * 
     * @param fromDate Start date
     * @param toDate   End date
     * @return Map with revenue metrics
     */
    public java.util.Map<String, Object> getRevenueMetrics(String fromDate, String toDate) {
        try {
            log.info("Fetching revenue metrics from {} to {}", fromDate, toDate);

            StringBuilder filter = new StringBuilder();
            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append("createdon ge ").append(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                if (filter.length() > 0)
                    filter.append(" and ");
                filter.append("createdon le ").append(toDate);
            }

            String token = authService.getAccessToken();
            String uri = "/opportunities?$select=statecode,statuscode,estimatedvalue";
            if (filter.length() > 0) {
                uri += "&$filter=" + java.net.URLEncoder.encode(filter.toString(), "UTF-8");
            }

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response, new TypeReference<D365Response<Opportunity>>() {
                    });

            double estimatedRevenue = 0, wonRevenue = 0, lostRevenue = 0;

            for (Opportunity opp : d365Response.getValue()) {
                double value = opp.getEstimatedValue() != null ? opp.getEstimatedValue().doubleValue() : 0.0;
                estimatedRevenue += value;

                if (opp.getStateCode() != null) {
                    if (opp.getStateCode() == 1 && opp.getStatusCode() == 3) {
                        wonRevenue += value;
                    } else if (opp.getStateCode() == 1 && opp.getStatusCode() == 4) {
                        lostRevenue += value;
                    }
                }
            }

            java.util.Map<String, Object> metrics = new java.util.HashMap<>();
            metrics.put("estimatedRevenue", Math.round(estimatedRevenue * 100.0) / 100.0);
            metrics.put("wonRevenue", Math.round(wonRevenue * 100.0) / 100.0);
            metrics.put("lostRevenue", Math.round(lostRevenue * 100.0) / 100.0);

            return metrics;

        } catch (Exception e) {
            log.error("Error fetching revenue metrics", e);
            return new java.util.HashMap<>();
        }
    }
}
