package com.example.backend.service;

import com.example.backend.model.Staff;
import com.example.backend.config.D365Config;
import com.example.backend.config.StaffFilterConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for interacting with Dynamics 365 System Users (Staff)
 */
@Slf4j
@Service
public class D365StaffService {

    private final WebClient webClient;
    private final D365AuthService authService;
    private final D365Config d365Config;
    private final ObjectMapper objectMapper;
    private D365ActivityService activityService; // Will be injected via setter to avoid circular dependency

    public D365StaffService(WebClient.Builder webClientBuilder,
            D365AuthService authService,
            D365Config d365Config,
            ObjectMapper objectMapper) {
        this.authService = authService;
        this.d365Config = d365Config;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(d365Config.getBaseUrl())
                .build();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setActivityService(D365ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Get all staff members from Dynamics 365 (UNFILTERED - shows all staff
     * regardless of job title)
     * Use this for Staff Management page
     * 
     * @param top               Optional limit for number of results
     * @param select            Optional comma-separated list of fields to select
     * @param includeEmailStats Whether to include email statistics (default: false)
     * @param fromDate          Optional start date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @param toDate            Optional end date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @return List of all staff members (unfiltered)
     */
    public List<Staff> getAllStaffUnfiltered(Integer top, String select, Boolean includeEmailStats, String fromDate,
            String toDate) {
        return fetchStaffFromD365(top, select, includeEmailStats, fromDate, toDate, false);
    }

    /**
     * Get tracked staff members from Dynamics 365 (FILTERED by configured job
     * titles)
     * Use this for Dashboard/Ranking page
     * 
     * @param top               Optional limit for number of results
     * @param select            Optional comma-separated list of fields to select
     * @param includeEmailStats Whether to include email statistics (default: false)
     * @param fromDate          Optional start date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @param toDate            Optional end date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @return List of tracked staff members only
     */
    public List<Staff> getAllStaff(Integer top, String select, Boolean includeEmailStats, String fromDate,
            String toDate) {
        return fetchStaffFromD365(top, select, includeEmailStats, fromDate, toDate, true);
    }

    /**
     * Internal method to fetch staff from Dynamics 365 with optional filtering
     * 
     * @param top               Optional limit for number of results
     * @param select            Optional comma-separated list of fields to select
     * @param includeEmailStats Whether to include email statistics
     * @param fromDate          Optional start date for email statistics filter
     * @param toDate            Optional end date for email statistics filter
     * @param applyFilter       Whether to apply job title filtering
     * @return List of staff members
     */
    private List<Staff> fetchStaffFromD365(Integer top, String select, Boolean includeEmailStats, String fromDate,
            String toDate, boolean applyFilter) {
        try {
            log.info(
                    "Fetching staff from Dynamics 365. Top: {}, Select: {}, IncludeEmailStats: {}, FromDate: {}, ToDate: {}",
                    top, select, includeEmailStats, fromDate, toDate);

            String token = authService.getAccessToken();

            // Build query parameters
            String uri = "/systemusers";
            StringBuilder queryParams = new StringBuilder("?");

            if (top != null && top > 0) {
                queryParams.append("$top=").append(top).append("&");
            }

            if (select != null && !select.isEmpty()) {
                queryParams.append("$select=").append(select).append("&");
            } else {
                // Default fields to select
                queryParams.append("$select=systemuserid,fullname,internalemailaddress,domainname,")
                        .append("title,mobilephone,address1_telephone1,createdon,modifiedon&");
            }

            // Filter for active users with bintara email domain only
            queryParams.append("$filter=isdisabled eq false and contains(internalemailaddress,'@bintara')&");

            // Add count
            queryParams.append("$count=true");

            uri += queryParams.toString();

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode valueArray = root.get("value");

            List<Staff> staffList = new ArrayList<>();
            if (valueArray != null && valueArray.isArray()) {
                for (JsonNode node : valueArray) {
                    Staff staff = objectMapper.treeToValue(node, Staff.class);

                    // Apply filter only if requested
                    if (applyFilter && !StaffFilterConfig.shouldTrackStaff(staff.getTitle())) {
                        log.debug("Skipping non-tracked staff (title: {}): {}", staff.getTitle(), staff.getFullName());
                        continue;
                    }

                    // Conditionally enrich with email statistics
                    if (includeEmailStats != null && includeEmailStats) {
                        enrichStaffWithEmailStats(staff, fromDate, toDate);
                    }
                    staffList.add(staff);
                }
            }

            String filterMsg = applyFilter ? " tracked staff members (filtered)" : " staff members (unfiltered)";
            log.info("Successfully fetched {}{}", staffList.size(), filterMsg);
            return staffList;

        } catch (WebClientResponseException e) {
            log.error("Error fetching staff: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch staff: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching staff", e);
            throw new RuntimeException("Failed to fetch staff: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific staff member by ID
     * 
     * @param systemUserId      The system user ID
     * @param includeEmailStats Whether to include email statistics (default: false)
     * @param fromDate          Optional start date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @param toDate            Optional end date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @return Optional containing the staff member if found
     */
    public Optional<Staff> getStaffById(String systemUserId, Boolean includeEmailStats, String fromDate,
            String toDate) {
        try {
            log.info("Fetching staff with ID: {}, IncludeEmailStats: {}, FromDate: {}, ToDate: {}",
                    systemUserId, includeEmailStats, fromDate, toDate);

            String token = authService.getAccessToken();

            String response = webClient.get()
                    .uri("/systemusers(" + systemUserId + ")?$select=systemuserid,fullname,internalemailaddress," +
                            "domainname,title,mobilephone,address1_telephone1,createdon,modifiedon")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            Staff staff = objectMapper.readValue(response, Staff.class);
            // Conditionally enrich with email statistics
            if (includeEmailStats != null && includeEmailStats) {
                enrichStaffWithEmailStats(staff, fromDate, toDate);
            }
            log.info("Successfully fetched staff: {}", staff.getFullName());
            return Optional.of(staff);

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Staff not found with ID: {}", systemUserId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching staff by ID", e);
            throw new RuntimeException("Failed to fetch staff: " + e.getMessage(), e);
        }
    }

    /**
     * Search for staff by name or email (UNFILTERED - shows all staff)
     * 
     * @param name  Optional name to search for
     * @param email Optional email to search for
     * @param top   Optional limit for number of results
     * @return List of all matching staff members
     */
    public List<Staff> searchStaffUnfiltered(String name, String email, Integer top) {
        return performStaffSearch(name, email, top, false);
    }

    /**
     * Search for tracked staff by name or email (FILTERED by configured job titles)
     * 
     * @param name  Optional name to search for
     * @param email Optional email to search for
     * @param top   Optional limit for number of results
     * @return List of matching tracked staff members
     */
    public List<Staff> searchStaff(String name, String email, Integer top) {
        return performStaffSearch(name, email, top, true);
    }

    /**
     * Internal method to search for staff with optional filtering
     * 
     * @param name        Optional name to search for
     * @param email       Optional email to search for
     * @param top         Optional limit for number of results
     * @param applyFilter Whether to apply job title filtering
     * @return List of matching staff members
     */
    private List<Staff> performStaffSearch(String name, String email, Integer top, boolean applyFilter) {
        try {
            log.info("Searching staff - Name: {}, Email: {}, Top: {}", name, email, top);

            String token = authService.getAccessToken();

            // Build OData filter - only Bintara email domain
            StringBuilder filterBuilder = new StringBuilder(
                    "isdisabled eq false and contains(internalemailaddress,'@bintara')");

            if (name != null && !name.isBlank()) {
                filterBuilder.append(" and contains(fullname,'").append(name.trim()).append("')");
            }

            if (email != null && !email.isBlank()) {
                filterBuilder.append(" and contains(internalemailaddress,'").append(email.trim()).append("')");
            }

            // Build URI with OData query
            StringBuilder uriBuilder = new StringBuilder("/systemusers?");
            uriBuilder.append("$filter=").append(filterBuilder.toString()).append("&");
            uriBuilder.append("$select=systemuserid,fullname,internalemailaddress,domainname,")
                    .append("title,mobilephone,address1_telephone1,createdon,modifiedon&");

            if (top != null && top > 0) {
                uriBuilder.append("$top=").append(top).append("&");
            }

            uriBuilder.append("$count=true");

            String response = webClient.get()
                    .uri(uriBuilder.toString())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode valueArray = root.get("value");

            List<Staff> staffList = new ArrayList<>();
            if (valueArray != null && valueArray.isArray()) {
                for (JsonNode node : valueArray) {
                    Staff staff = objectMapper.treeToValue(node, Staff.class);

                    // Apply filter only if requested
                    if (applyFilter && !StaffFilterConfig.shouldTrackStaff(staff.getTitle())) {
                        continue;
                    }

                    staffList.add(staff);
                }
            }

            String filterMsg = applyFilter ? " tracked staff members" : " staff members (unfiltered)";
            log.info("Found {}{} matching search criteria", staffList.size(), filterMsg);
            return staffList;

        } catch (Exception e) {
            log.error("Error searching staff", e);
            throw new RuntimeException("Failed to search staff: " + e.getMessage(), e);
        }
    }

    /**
     * Get count of all active staff
     * 
     * @return Total count of active staff
     */
    public int getStaffCount() {
        try {
            log.info("Fetching active staff count from Dynamics 365");

            String token = authService.getAccessToken();

            String response = webClient.get()
                    .uri("/systemusers/$count?$filter=isdisabled eq false and contains(internalemailaddress,'@bintara')")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            // Remove quotes and any non-numeric characters
            String cleanResponse = response.replaceAll("[^0-9]", "");
            int count = Integer.parseInt(cleanResponse);

            log.info("Successfully fetched staff count: {}", count);
            return count;

        } catch (Exception e) {
            log.error("Error fetching staff count", e);
            throw new RuntimeException("Failed to fetch staff count: " + e.getMessage(), e);
        }
    }

    /**
     * Enrich staff object with email statistics (incoming/outgoing counts)
     * Counts all emails owned by the staff member (by _owninguser_value)
     * 
     * @param staff    Staff object to enrich
     * @param fromDate Optional start date for filtering (ISO format: YYYY-MM-DD)
     * @param toDate   Optional end date for filtering (ISO format: YYYY-MM-DD)
     */
    private void enrichStaffWithEmailStats(Staff staff, String fromDate, String toDate) {
        try {
            if (staff.getEmail() != null && activityService != null) {
                log.info("Enriching staff {} (ID: {}) with email statistics (FromDate: {}, ToDate: {})",
                        staff.getEmail(), staff.getSystemUserId(), fromDate, toDate);

                String userId = staff.getSystemUserId();

                // Get email statistics using the new participant-based method
                log.info("Getting email statistics for staff: {}", staff.getEmail());

                com.example.backend.model.EmailStats emailStats = activityService
                        .getEmailStatsByStaff(staff.getEmail());

                log.info("Email counts for {}: Incoming={}, Outgoing={}", staff.getEmail(),
                        emailStats.getIncomingEmailCount(), emailStats.getOutgoingEmailCount());

                staff.setIncomingEmailCount(emailStats.getIncomingEmailCount());
                staff.setOutgoingEmailCount(emailStats.getOutgoingEmailCount());
                staff.setTotalEmailCount(emailStats.getTotalEmailCount());

                // Calculate response time statistics for emails owned by this user
                log.info("Calculating response time for staff: {}", staff.getEmail());
                java.util.Map<String, Object> responseTimeStats = activityService
                        .calculateResponseTimeForUser(userId, fromDate, toDate);

                staff.setAverageResponseTimeMinutes(
                        responseTimeStats.get("averageResponseTimeMinutes") != null
                                ? (Double) responseTimeStats.get("averageResponseTimeMinutes")
                                : null);
                staff.setFastestResponseTimeMinutes(
                        responseTimeStats.get("fastestResponseTimeMinutes") != null
                                ? (Double) responseTimeStats.get("fastestResponseTimeMinutes")
                                : null);
                staff.setSlowestResponseTimeMinutes(
                        responseTimeStats.get("slowestResponseTimeMinutes") != null
                                ? (Double) responseTimeStats.get("slowestResponseTimeMinutes")
                                : null);
                staff.setRespondedEmailCount(
                        responseTimeStats.get("respondedEmailCount") != null
                                ? (Integer) responseTimeStats.get("respondedEmailCount")
                                : 0);

                // Calculate conversation thread statistics
                log.info("Calculating conversation statistics for staff: {}", staff.getEmail());
                java.util.Map<String, Object> conversationStats = activityService
                        .calculateConversationStats(userId, fromDate, toDate);

                staff.setTotalConversations(
                        conversationStats.get("totalConversations") != null
                                ? (Integer) conversationStats.get("totalConversations")
                                : 0);
                staff.setAverageEmailsPerConversation(
                        conversationStats.get("averageEmailsPerConversation") != null
                                ? (Double) conversationStats.get("averageEmailsPerConversation")
                                : 0.0);

                log.info(
                        "Staff {} email stats: Incoming={}, Outgoing={}, Total={}, Conversations={}, Avg Emails/Conv={}, Avg Response Time={} min",
                        staff.getEmail(),
                        staff.getIncomingEmailCount(),
                        staff.getOutgoingEmailCount(),
                        staff.getTotalEmailCount(),
                        staff.getTotalConversations(),
                        String.format("%.2f", staff.getAverageEmailsPerConversation()),
                        staff.getAverageResponseTimeMinutes() != null
                                ? String.format("%.2f", staff.getAverageResponseTimeMinutes())
                                : "N/A");
            } else {
                log.warn("Cannot enrich staff - Email: {}, ActivityService: {}",
                        staff.getEmail(), activityService != null ? "available" : "null");
            }
        } catch (Exception e) {
            log.error("Error enriching staff {} with email stats: {}", staff.getEmail(), e.getMessage(), e);
            // Set to 0 if there's an error
            staff.setIncomingEmailCount(0);
            staff.setOutgoingEmailCount(0);
            staff.setTotalEmailCount(0);
            staff.setAverageResponseTimeMinutes(null);
            staff.setFastestResponseTimeMinutes(null);
            staff.setSlowestResponseTimeMinutes(null);
            staff.setTotalConversations(0);
            staff.setAverageEmailsPerConversation(0.0);
            staff.setRespondedEmailCount(0);
        }
    }

    /**
     * Build OData filter with optional date range
     * 
     * @param baseFilter Base filter condition
     * @param fromDate   Optional start date (ISO format: YYYY-MM-DD)
     * @param toDate     Optional end date (ISO format: YYYY-MM-DD)
     * @return Complete filter string with date range if provided
     */
    private String buildFilterWithDateRange(String baseFilter, String fromDate, String toDate) {
        StringBuilder filter = new StringBuilder(baseFilter);

        if (fromDate != null && !fromDate.isBlank()) {
            // Add time component to make it start of day
            filter.append(" and createdon ge ").append(fromDate).append("T00:00:00Z");
        }

        if (toDate != null && !toDate.isBlank()) {
            // Add time component to make it end of day
            filter.append(" and createdon le ").append(toDate).append("T23:59:59Z");
        }

        log.debug("Built filter with date range: {}", filter.toString());
        return filter.toString();
    }
}
