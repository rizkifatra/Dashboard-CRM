package com.example.backend.service;

import com.example.backend.model.Activity;
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
 * Service for interacting with Dynamics 365 Activities (Emails, Calls,
 * Meetings, Tasks)
 */
@Slf4j
@Service
public class D365ActivityService {

    private final WebClient webClient;
    private final D365AuthService authService;
    private final D365Config d365Config;
    private final ObjectMapper objectMapper;

    public D365ActivityService(WebClient.Builder webClientBuilder,
            D365AuthService authService,
            D365Config d365Config,
            ObjectMapper objectMapper) {
        this.authService = authService;
        this.d365Config = d365Config;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(d365Config.getBaseUrl())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer size
                .build();
    }

    /**
     * Get all activities (emails, calls, meetings, tasks)
     * 
     * @param top    Optional limit for number of results
     * @param filter Optional OData filter string
     * @return List of activities
     */
    public List<Activity> getAllActivities(Integer top, String filter) {
        try {
            log.info("Fetching activities from Dynamics 365. Top: {}, Filter: {}", top, filter);

            String token = authService.getAccessToken();

            // Build query parameters
            StringBuilder queryParams = new StringBuilder("?");

            // Select fields - only fields available in activitypointers table
            queryParams.append("$select=activityid,subject,description,activitytypecode,")
                    .append("statecode,statuscode,_owninguser_value,_regardingobjectid_value,")
                    .append("createdon,modifiedon,actualstart,actualend,")
                    .append("scheduledstart,scheduledend,actualdurationminutes,scheduleddurationminutes,")
                    .append("prioritycode&");

            if (filter != null && !filter.isEmpty()) {
                queryParams.append("$filter=").append(filter).append("&");
            }

            if (top != null && top > 0) {
                queryParams.append("$top=").append(top).append("&");
            }

            // Order by created date descending
            queryParams.append("$orderby=createdon desc&");
            queryParams.append("$count=true");

            String uri = "/activitypointers" + queryParams.toString();

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode valueArray = root.get("value");

            List<Activity> activities = new ArrayList<>();
            if (valueArray != null && valueArray.isArray()) {
                for (JsonNode node : valueArray) {
                    Activity activity = objectMapper.treeToValue(node, Activity.class);
                    activities.add(activity);
                }
            }

            // Filter to include only activities from tracked staff
            List<Activity> filteredActivities = filterTrackedStaffActivities(activities);

            log.info("Successfully fetched {} activities ({} from tracked staff)",
                    activities.size(), filteredActivities.size());
            return filteredActivities;

        } catch (WebClientResponseException e) {
            log.error("Error fetching activities: Status={}, Body={}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch activities: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching activities", e);
            throw new RuntimeException("Failed to fetch activities: " + e.getMessage(), e);
        }
    }

    /**
     * Get activities by staff email (owner)
     * 
     * @param staffEmail The staff member's email
     * @param top        Optional limit for number of results
     * @return List of activities owned by the staff member
     */
    public List<Activity> getActivitiesByStaffEmail(String staffEmail, Integer top) {
        try {
            log.info("Fetching activities for staff: {}", staffEmail);

            String token = authService.getAccessToken();

            // First, get the systemuser by email to get their ID
            String userResponse = webClient.get()
                    .uri("/systemusers?$filter=internalemailaddress eq '" + staffEmail
                            + "'&$select=systemuserid,fullname")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode userRoot = objectMapper.readTree(userResponse);
            JsonNode userArray = userRoot.get("value");

            if (userArray == null || !userArray.isArray() || userArray.size() == 0) {
                log.warn("Staff not found with email: {}", staffEmail);
                return new ArrayList<>();
            }

            String userId = userArray.get(0).get("systemuserid").asText();
            log.info("Found staff with ID: {}", userId);

            // Now fetch activities for this user
            String filter = "_owninguser_value eq " + userId;
            return getAllActivities(top, filter);

        } catch (Exception e) {
            log.error("Error fetching activities by staff email", e);
            throw new RuntimeException("Failed to fetch activities by staff email: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific activity by ID
     * 
     * @param activityId The activity ID
     * @return Optional containing the activity if found
     */
    public Optional<Activity> getActivityById(String activityId) {
        try {
            log.info("Fetching activity with ID: {}", activityId);

            String token = authService.getAccessToken();

            String response = webClient.get()
                    .uri("/activitypointers(" + activityId + ")?$select=activityid,subject,description," +
                            "activitytypecode,statecode,statuscode,_owninguser_value," +
                            "_regardingobjectid_value,regardingobjecttypecode,createdon,modifiedon," +
                            "actualstart,actualend,scheduledstart,scheduledend,actualdurationminutes," +
                            "scheduleddurationminutes,prioritycode")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            Activity activity = objectMapper.readValue(response, Activity.class);
            log.info("Successfully fetched activity: {}", activity.getSubject());
            return Optional.of(activity);

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Activity not found with ID: {}", activityId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching activity by ID", e);
            throw new RuntimeException("Failed to fetch activity: " + e.getMessage(), e);
        }
    }

    /**
     * Get activities related to a specific account (client)
     * 
     * @param accountId The account ID
     * @param top       Optional limit for number of results
     * @return List of activities related to the account
     */
    public List<Activity> getActivitiesByAccount(String accountId, Integer top) {
        try {
            log.info("Fetching activities for account: {}", accountId);

            String filter = "_regardingobjectid_value eq " + accountId;
            return getAllActivities(top, filter);

        } catch (Exception e) {
            log.error("Error fetching activities by account", e);
            throw new RuntimeException("Failed to fetch activities by account: " + e.getMessage(), e);
        }
    }

    /**
     * Get email activities only (sent and received)
     * 
     * @param top Optional limit for number of results
     * @return List of email activities
     */
    public List<Activity> getEmailActivities(Integer top) {
        try {
            log.info("Fetching email activities from Dynamics 365");

            String filter = "activitytypecode eq 'email'";
            return getAllActivities(top, filter);

        } catch (Exception e) {
            log.error("Error fetching email activities", e);
            throw new RuntimeException("Failed to fetch email activities: " + e.getMessage(), e);
        }
    }

    /**
     * Get sent emails by staff
     * 
     * @param staffEmail The staff member's email
     * @param top        Optional limit for number of results
     * @return List of sent email activities
     */
    public List<Activity> getSentEmailsByStaff(String staffEmail, Integer top) {
        try {
            log.info("Fetching sent emails for staff: {}", staffEmail);

            String token = authService.getAccessToken();

            // Get user ID first
            String userResponse = webClient.get()
                    .uri("/systemusers?$filter=internalemailaddress eq '" + staffEmail
                            + "'&$select=systemuserid")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode userRoot = objectMapper.readTree(userResponse);
            JsonNode userArray = userRoot.get("value");

            if (userArray == null || !userArray.isArray() || userArray.size() == 0) {
                log.warn("Staff not found with email: {}", staffEmail);
                return new ArrayList<>();
            }

            String userId = userArray.get(0).get("systemuserid").asText();

            // Fetch sent emails (directioncode = true means outgoing/sent)
            String filter = "_owninguser_value eq " + userId
                    + " and activitytypecode eq 'email' and directioncode eq true";
            return getAllActivities(top, filter);

        } catch (Exception e) {
            log.error("Error fetching sent emails by staff", e);
            throw new RuntimeException("Failed to fetch sent emails: " + e.getMessage(), e);
        }
    }

    /**
     * Get received emails by staff
     * 
     * @param staffEmail The staff member's email
     * @param top        Optional limit for number of results
     * @return List of received email activities
     */
    public List<Activity> getReceivedEmailsByStaff(String staffEmail, Integer top) {
        try {
            log.info("Fetching received emails for staff: {}", staffEmail);

            String token = authService.getAccessToken();

            // Get user ID first
            String userResponse = webClient.get()
                    .uri("/systemusers?$filter=internalemailaddress eq '" + staffEmail
                            + "'&$select=systemuserid")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode userRoot = objectMapper.readTree(userResponse);
            JsonNode userArray = userRoot.get("value");

            if (userArray == null || !userArray.isArray() || userArray.size() == 0) {
                log.warn("Staff not found with email: {}", staffEmail);
                return new ArrayList<>();
            }

            String userId = userArray.get(0).get("systemuserid").asText();

            // Fetch received emails (directioncode = false means incoming/received)
            String filter = "_owninguser_value eq " + userId
                    + " and activitytypecode eq 'email' and directioncode eq false";
            return getAllActivities(top, filter);

        } catch (Exception e) {
            log.error("Error fetching received emails by staff", e);
            throw new RuntimeException("Failed to fetch received emails: " + e.getMessage(), e);
        }
    }

    /**
     * Get activity count
     * 
     * @param filter Optional OData filter string
     * @return Total count of activities
     */
    public int getActivityCount(String filter) {
        try {
            log.info("Fetching activity count with filter: {}", filter);

            String token = authService.getAccessToken();

            // Build query with $count=true to get @odata.count in response
            StringBuilder queryParams = new StringBuilder("?");
            queryParams.append("$select=activityid&");
            queryParams.append("$top=1&"); // We only need the count, not the data

            if (filter != null && !filter.isEmpty()) {
                queryParams.append("$filter=").append(filter).append("&");
            }

            queryParams.append("$count=true");

            String uri = "/activitypointers" + queryParams.toString();
            log.info("Requesting URI: {}", d365Config.getBaseUrl() + uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            log.info("Response received: {}", response);

            // Parse the @odata.count from response
            JsonNode root = objectMapper.readTree(response);
            int count = root.has("@odata.count") ? root.get("@odata.count").asInt() : 0;

            log.info("Successfully fetched activity count: {} for filter: {}", count, filter);
            return count;

        } catch (WebClientResponseException e) {
            log.error("Error fetching activity count: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch activity count: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching activity count with filter: {}", filter, e);
            throw new RuntimeException("Failed to fetch activity count: " + e.getMessage(), e);
        }
    }

    /**
     * Get accounts owned by a staff member
     * 
     * @param userId Staff member's system user ID
     * @return List of account IDs owned by the staff member
     */
    public java.util.List<String> getAccountsByOwner(String userId) {
        try {
            log.info("Fetching accounts owned by user: {}", userId);

            String token = authService.getAccessToken();

            String uri = "/accounts?$filter=_ownerid_value eq " + userId + "&$select=accountid";

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode accounts = root.get("value");

            java.util.List<String> accountIds = new java.util.ArrayList<>();
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    String accountId = account.get("accountid").asText();
                    accountIds.add(accountId);
                }
            }

            log.info("Found {} accounts owned by user: {}", accountIds.size(), userId);
            return accountIds;

        } catch (Exception e) {
            log.error("Error fetching accounts for user: {}", userId, e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Count emails from the emails table related to specific accounts
     * 
     * @param accountIds List of account IDs to count emails for
     * @param isIncoming true for incoming emails, false for outgoing emails
     * @return Count of emails matching the criteria
     */
    public int getEmailCountForAccounts(java.util.List<String> accountIds, boolean isIncoming) {
        try {
            if (accountIds == null || accountIds.isEmpty()) {
                log.info("No accounts provided, returning 0 count");
                return 0;
            }

            log.info("Fetching email count for {} accounts, incoming={}", accountIds.size(), isIncoming);

            // Split into batches to avoid URL length limitation
            int batchSize = 10; // Process 10 accounts at a time to keep URL manageable
            int totalCount = 0;

            for (int i = 0; i < accountIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, accountIds.size());
                java.util.List<String> batchAccountIds = accountIds.subList(i, endIndex);

                log.info("Processing batch {}/{}: accounts {} to {}",
                        (i / batchSize) + 1,
                        (accountIds.size() + batchSize - 1) / batchSize,
                        i + 1,
                        endIndex);

                int batchCount = getEmailCountForAccountsBatch(batchAccountIds, isIncoming);
                totalCount += batchCount;

                log.info("Batch count: {}, Running total: {}", batchCount, totalCount);
            }

            log.info("Successfully fetched total email count: {} for {} accounts (incoming={})",
                    totalCount, accountIds.size(), isIncoming);
            return totalCount;

        } catch (Exception e) {
            log.error("Error fetching email count for accounts", e);
            return 0;
        }
    }

    /**
     * Count emails for a single batch of accounts (internal helper method)
     * 
     * @param accountIds List of account IDs (should be <= 25)
     * @param isIncoming true for incoming emails, false for outgoing emails
     * @return Count of emails matching the criteria
     */
    private int getEmailCountForAccountsBatch(java.util.List<String> accountIds, boolean isIncoming) {
        try {
            String token = authService.getAccessToken();

            // Build filter for multiple accounts using 'or' conditions
            StringBuilder accountFilter = new StringBuilder();
            for (int i = 0; i < accountIds.size(); i++) {
                if (i > 0) {
                    accountFilter.append(" or ");
                }
                accountFilter.append("_regardingobjectid_value eq ").append(accountIds.get(i));
            }

            // Add direction filter
            String directionFilter = isIncoming ? "directioncode eq false" : "directioncode eq true";
            String fullFilter = "(" + accountFilter.toString() + ") and " + directionFilter;

            // Build query with $count=true to get @odata.count in response
            StringBuilder queryParams = new StringBuilder("?");
            queryParams.append("$select=activityid&");
            queryParams.append("$top=1&"); // We only need the count, not the data
            queryParams.append("$filter=").append(fullFilter).append("&");
            queryParams.append("$count=true");

            String uri = "/emails" + queryParams.toString();

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            // Parse the @odata.count from response
            JsonNode root = objectMapper.readTree(response);
            int count = root.has("@odata.count") ? root.get("@odata.count").asInt() : 0;

            return count;

        } catch (WebClientResponseException e) {
            log.error("Error fetching email count batch: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return 0;
        } catch (Exception e) {
            log.error("Error fetching email count for accounts batch", e);
            return 0;
        }
    }

    /**
     * Count emails from the emails table with a filter
     * 
     * @param filter OData filter string
     * @return Count of emails matching the filter
     */
    public int getEmailCount(String filter) {
        try {
            String token = authService.getAccessToken();

            // Fetch emails with activity parties to filter external emails only
            // We need to check if any participant is NOT from bintara.com.my
            StringBuilder queryParams = new StringBuilder("?");
            queryParams.append("$select=activityid&");
            queryParams.append("$expand=email_activity_parties($select=participationtypemask,partyid,addressused)&");
            queryParams.append("$filter=").append(filter).append("&");
            queryParams.append("$top=500"); // Get up to 500 emails to count external ones

            String uri = "/emails" + queryParams.toString();
            log.info("Querying D365 for external email count - URI: {}", uri);
            log.info("Filter applied: {}", filter);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            // Parse emails and count only external ones (non-bintara.com.my)
            JsonNode root = objectMapper.readTree(response);
            JsonNode emails = root.get("value");

            int externalCount = 0;
            int totalEmails = 0;
            if (emails != null && emails.isArray()) {
                totalEmails = emails.size();

                // Debug: Log first email structure to understand the data
                if (totalEmails > 0) {
                    JsonNode firstEmail = emails.get(0);
                    log.info("Sample email structure (first email): {}", firstEmail.toString());
                    if (firstEmail.has("email_activity_parties")) {
                        log.info("First email has {} parties", firstEmail.get("email_activity_parties").size());
                    } else {
                        log.warn("First email does NOT have email_activity_parties field!");
                    }
                }

                for (JsonNode email : emails) {
                    if (isExternalEmail(email)) {
                        externalCount++;
                    }
                }
            }

            log.info("D365 returned {} external emails (filtered from {} total) for filter: {}",
                    externalCount, totalEmails, filter);

            return externalCount;

        } catch (WebClientResponseException e) {
            log.error("Error fetching email count: Status={}, Body={}, Filter={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), filter);
            return 0;
        } catch (Exception e) {
            log.error("Error fetching email count with filter: {}", filter, e);
            return 0;
        }
    }

    /**
     * Check if an email is external (has at least one non-Bintara participant)
     * 
     * @param emailNode The email JSON node with expanded activity parties
     * @return true if email has external participants
     */
    private boolean isExternalEmail(JsonNode emailNode) {
        try {
            JsonNode parties = emailNode.get("email_activity_parties");
            if (parties == null || !parties.isArray() || parties.size() == 0) {
                // If no parties info, we can't determine, so count it
                log.warn("No email_activity_parties found for email, counting as external");
                return true;
            }

            log.debug("Checking email with {} parties", parties.size());
            boolean hasExternalParty = false;
            int partiesChecked = 0;

            // Check if any party has a non-Bintara email address
            for (JsonNode party : parties) {
                partiesChecked++;

                // D365 can use different field names: addressused, address, emailaddress
                JsonNode addressUsed = party.has("addressused") ? party.get("addressused") : null;
                if (addressUsed == null || addressUsed.isNull()) {
                    addressUsed = party.has("address") ? party.get("address") : null;
                }
                if (addressUsed == null || addressUsed.isNull()) {
                    addressUsed = party.has("emailaddress") ? party.get("emailaddress") : null;
                }

                if (addressUsed != null && !addressUsed.isNull()) {
                    String email = addressUsed.asText().toLowerCase().trim();
                    log.debug("Party {}: email={}", partiesChecked, email);

                    // Check if email is NOT from bintara.com.my domain
                    if (!email.isEmpty() && !email.contains("@bintara.com.my")) {
                        log.debug("External email detected with participant: {}", email);
                        hasExternalParty = true;
                        break; // Found external party, no need to check more
                    }
                } else {
                    log.debug("Party {}: No email address found in fields (addressused/address/emailaddress)",
                            partiesChecked);
                }
            }

            if (!hasExternalParty) {
                log.debug("Internal Bintara email detected (all {} parties are @bintara.com.my), excluding from count",
                        partiesChecked);
            }

            return hasExternalParty;

        } catch (Exception e) {
            log.warn("Error checking if email is external, counting it anyway: {}", e.getMessage(), e);
            return true; // Count it if we can't determine
        }
    }

    /**
     * Get email statistics (incoming/outgoing counts) for a staff member
     * 
     * @param staffEmail Staff member's email
     * @return Email statistics with incoming and outgoing counts
     */
    public com.example.backend.model.EmailStats getEmailStatsByStaff(String staffEmail) {
        try {
            log.info("Fetching email statistics for staff: {}", staffEmail);

            String token = authService.getAccessToken();

            // First, get the systemuser by email
            String userResponse = webClient.get()
                    .uri("/systemusers?$filter=internalemailaddress eq '" + staffEmail
                            + "'&$select=systemuserid,fullname")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode userRoot = objectMapper.readTree(userResponse);
            JsonNode userArray = userRoot.get("value");

            if (userArray == null || !userArray.isArray() || userArray.size() == 0) {
                log.warn("Staff not found with email: {}", staffEmail);
                return com.example.backend.model.EmailStats.builder()
                        .staffEmail(staffEmail)
                        .staffName(null)
                        .incomingEmailCount(0)
                        .outgoingEmailCount(0)
                        .totalEmailCount(0)
                        .build();
            }

            String userId = userArray.get(0).get("systemuserid").asText();
            String userName = userArray.get(0).get("fullname").asText();
            log.info("Found staff: {} with ID: {}", userName, userId);

            // Count incoming emails (directioncode = false) from emails table
            String incomingFilter = "_owninguser_value eq " + userId + " and directioncode eq false";
            int incomingCount = getEmailCount(incomingFilter);

            // Count outgoing emails (directioncode = true) from emails table
            String outgoingFilter = "_owninguser_value eq " + userId + " and directioncode eq true";
            int outgoingCount = getEmailCount(outgoingFilter);

            log.info("Email stats for {}: Incoming={}, Outgoing={}", userName, incomingCount, outgoingCount);

            return com.example.backend.model.EmailStats.builder()
                    .staffEmail(staffEmail)
                    .staffName(userName)
                    .incomingEmailCount(incomingCount)
                    .outgoingEmailCount(outgoingCount)
                    .totalEmailCount(incomingCount + outgoingCount)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching email statistics", e);
            throw new RuntimeException("Failed to fetch email statistics: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate email response time statistics for a staff member's accounts
     * Measures time between incoming emails and their corresponding outgoing
     * responses for accounts managed by the staff
     * 
     * @param accountIds List of account IDs managed by the staff member
     * @return Map containing response time metrics (average, fastest, slowest in
     *         minutes)
     */
    public java.util.Map<String, Object> calculateResponseTimeForAccounts(java.util.List<String> accountIds) {
        try {
            if (accountIds == null || accountIds.isEmpty()) {
                log.info("No accounts provided for response time calculation");
                return createEmptyResponseTimeStats();
            }

            // Skip response time calculation for staff with many accounts to avoid URL
            // length errors
            if (accountIds.size() > 10) {
                log.info("Skipping response time calculation for {} accounts (limit is 10 to avoid URL length errors)",
                        accountIds.size());
                return createEmptyResponseTimeStats();
            }

            log.info("Calculating response time for {} accounts", accountIds.size());

            // Split into batches to avoid URL length limitation
            // Use smaller batch size (3) for response time due to longer URLs with $select,
            // $orderby, $top
            int batchSize = 3;
            java.util.List<Double> allResponseTimes = new java.util.ArrayList<>();

            for (int i = 0; i < accountIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, accountIds.size());
                java.util.List<String> batchAccountIds = accountIds.subList(i, endIndex);

                log.info("Processing response time batch {}/{}: accounts {} to {}",
                        (i / batchSize) + 1,
                        (accountIds.size() + batchSize - 1) / batchSize,
                        i + 1,
                        endIndex);

                java.util.List<Double> batchResponseTimes = calculateResponseTimeForAccountsBatch(batchAccountIds);
                allResponseTimes.addAll(batchResponseTimes);
            }

            return calculateResponseTimeStats(allResponseTimes, accountIds.size());

        } catch (Exception e) {
            log.error("Error calculating response time for {} accounts", accountIds.size(), e);
            return createEmptyResponseTimeStats();
        }
    }

    /**
     * Calculate response times for a batch of accounts
     * 
     * @param accountIds List of account IDs (should be <= 25)
     * @return List of response times in minutes
     */
    private java.util.List<Double> calculateResponseTimeForAccountsBatch(java.util.List<String> accountIds) {
        try {
            String token = authService.getAccessToken();

            // Build filter for multiple accounts using 'or' conditions
            StringBuilder accountFilter = new StringBuilder();
            for (int i = 0; i < accountIds.size(); i++) {
                if (i > 0) {
                    accountFilter.append(" or ");
                }
                accountFilter.append("_regardingobjectid_value eq ").append(accountIds.get(i));
            }

            // Fetch incoming emails with their timestamps and conversation IDs
            String incomingUri = "/emails?$filter=(" + accountFilter.toString() + ") and directioncode eq false"
                    + "&$select=activityid,createdon,subject,regardingobjectid"
                    + "&$orderby=createdon desc"
                    + "&$top=100"; // Last 100 incoming emails

            String incomingResponse = webClient.get()
                    .uri(incomingUri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode incomingRoot = objectMapper.readTree(incomingResponse);
            JsonNode incomingEmails = incomingRoot.get("value");

            if (incomingEmails == null || incomingEmails.size() == 0) {
                log.info("No incoming emails found for account batch");
                return new java.util.ArrayList<>();
            }

            // Fetch outgoing emails with their timestamps
            String outgoingUri = "/emails?$filter=(" + accountFilter.toString() + ") and directioncode eq true"
                    + "&$select=activityid,createdon,subject,regardingobjectid"
                    + "&$orderby=createdon desc"
                    + "&$top=100"; // Last 100 outgoing emails

            String outgoingResponse = webClient.get()
                    .uri(outgoingUri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode outgoingRoot = objectMapper.readTree(outgoingResponse);
            JsonNode outgoingEmails = outgoingRoot.get("value");

            if (outgoingEmails == null || outgoingEmails.size() == 0) {
                log.info("No outgoing emails found for account batch");
                return new java.util.ArrayList<>();
            }

            // Calculate response times by matching incoming and outgoing emails
            java.util.List<Double> responseTimes = new java.util.ArrayList<>();

            for (JsonNode incoming : incomingEmails) {
                String incomingSubject = incoming.has("subject") ? incoming.get("subject").asText() : "";
                String incomingTime = incoming.get("createdon").asText();
                String regardingId = incoming.has("regardingobjectid") ? incoming.get("regardingobjectid").asText()
                        : null;

                // Find matching outgoing email (by subject similarity or regarding object)
                for (JsonNode outgoing : outgoingEmails) {
                    String outgoingSubject = outgoing.has("subject") ? outgoing.get("subject").asText() : "";
                    String outgoingTime = outgoing.get("createdon").asText();
                    String outgoingRegardingId = outgoing.has("regardingobjectid")
                            ? outgoing.get("regardingobjectid").asText()
                            : null;

                    // Match by regarding object (related to same account/contact)
                    boolean regardingMatch = regardingId != null && regardingId.equals(outgoingRegardingId);

                    // Match by subject (responses often start with "RE:" or contain original
                    // subject)
                    boolean subjectMatch = !outgoingSubject.isEmpty()
                            && (outgoingSubject.toLowerCase().contains(incomingSubject.toLowerCase())
                                    || outgoingSubject.toLowerCase().startsWith("re:")
                                            && incomingSubject.toLowerCase()
                                                    .contains(outgoingSubject.toLowerCase().replace("re:", "")
                                                            .trim()));

                    // Check if outgoing is after incoming (response comes after the request)
                    try {
                        java.time.Instant incomingInstant = java.time.Instant.parse(incomingTime);
                        java.time.Instant outgoingInstant = java.time.Instant.parse(outgoingTime);

                        if ((regardingMatch || subjectMatch) && outgoingInstant.isAfter(incomingInstant)) {
                            // Calculate time difference in minutes
                            long minutes = java.time.Duration.between(incomingInstant, outgoingInstant).toMinutes();
                            if (minutes >= 0 && minutes < 10080) { // Exclude responses > 1 week (likely not related)
                                responseTimes.add((double) minutes);
                                log.debug("Found response: {} minutes for subject: {}", minutes, incomingSubject);
                                break; // Found a match, move to next incoming email
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing timestamps: {}", e.getMessage());
                    }
                }
            }

            return responseTimes;

        } catch (Exception e) {
            log.warn("Error calculating response time for account batch: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Calculate statistics from collected response times
     * 
     * @param responseTimes List of response times in minutes
     * @param accountCount  Total number of accounts being analyzed
     * @return Map containing response time statistics
     */
    private java.util.Map<String, Object> calculateResponseTimeStats(java.util.List<Double> responseTimes,
            int accountCount) {
        if (responseTimes.isEmpty()) {
            log.info("No matched email responses found for {} accounts", accountCount);
            return createEmptyResponseTimeStats();
        }

        double sum = responseTimes.stream().mapToDouble(Double::doubleValue).sum();
        double average = sum / responseTimes.size();
        double fastest = responseTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double slowest = responseTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        log.info("Response time stats - Average: {} min, Fastest: {} min, Slowest: {} min, Count: {}",
                String.format("%.2f", average), String.format("%.2f", fastest),
                String.format("%.2f", slowest), responseTimes.size());

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("averageResponseTimeMinutes", Math.round(average * 100.0) / 100.0);
        stats.put("fastestResponseTimeMinutes", Math.round(fastest * 100.0) / 100.0);
        stats.put("slowestResponseTimeMinutes", Math.round(slowest * 100.0) / 100.0);
        stats.put("respondedEmailCount", responseTimes.size());

        return stats;
    }

    /**
     * Calculate response time statistics for emails owned by a specific user
     * 
     * @param userId System user ID
     * @return Map with response time statistics
     */
    public java.util.Map<String, Object> calculateResponseTimeForUser(String userId, String fromDate, String toDate) {
        try {
            log.info("Calculating response time for user: {} (FromDate: {}, ToDate: {})", userId, fromDate, toDate);
            String token = authService.getAccessToken();

            // Build date filter component
            String dateFilter = buildDateFilter(fromDate, toDate);

            // Fetch last 100 incoming emails owned by this user
            // Note: D365 does NOT support $expand on email_activity_parties, so we skip
            // external filtering here
            // External filtering is done in getEmailCount() which uses a different approach
            String incomingUri = "/emails?$filter=_owninguser_value eq " + userId + " and directioncode eq false"
                    + dateFilter
                    + "&$select=activityid,createdon,subject,regardingobjectid"
                    + "&$top=100";

            String incomingResponse = webClient.get()
                    .uri(incomingUri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode incomingRoot = objectMapper.readTree(incomingResponse);
            JsonNode incomingEmails = incomingRoot.get("value");

            int incomingCount = (incomingEmails != null && incomingEmails.isArray()) ? incomingEmails.size() : 0;
            log.info("Found {} incoming emails for response time calculation", incomingCount);

            if (incomingCount == 0) {
                log.info("No incoming emails found for user: {}", userId);
                return createEmptyResponseTimeStats();
            }

            // Fetch last 100 outgoing emails owned by this user
            String outgoingUri = "/emails?$filter=_owninguser_value eq " + userId + " and directioncode eq true"
                    + dateFilter
                    + "&$select=activityid,createdon,subject,regardingobjectid"
                    + "&$top=100";

            String outgoingResponse = webClient.get()
                    .uri(outgoingUri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode outgoingRoot = objectMapper.readTree(outgoingResponse);
            JsonNode outgoingEmails = outgoingRoot.get("value");

            int outgoingCount = (outgoingEmails != null && outgoingEmails.isArray()) ? outgoingEmails.size() : 0;
            log.info("Found {} outgoing emails for response time calculation", outgoingCount);

            if (outgoingCount == 0) {
                log.info("No outgoing emails found for user: {}", userId);
                return createEmptyResponseTimeStats();
            }

            // Calculate response times by matching incoming and outgoing emails
            java.util.List<Double> responseTimes = new java.util.ArrayList<>();

            for (JsonNode incoming : incomingEmails) {
                String incomingSubject = incoming.has("subject") ? incoming.get("subject").asText() : "";
                String incomingTime = incoming.get("createdon").asText();
                String regardingId = incoming.has("regardingobjectid") ? incoming.get("regardingobjectid").asText()
                        : null;

                // Find matching outgoing email (by subject similarity or regarding object)
                for (JsonNode outgoing : outgoingEmails) {
                    String outgoingSubject = outgoing.has("subject") ? outgoing.get("subject").asText() : "";
                    String outgoingTime = outgoing.get("createdon").asText();
                    String outgoingRegardingId = outgoing.has("regardingobjectid")
                            ? outgoing.get("regardingobjectid").asText()
                            : null;

                    // Match by regarding object OR by similar subject
                    boolean isMatch = false;
                    if (regardingId != null && regardingId.equals(outgoingRegardingId)) {
                        isMatch = true;
                    } else if (!incomingSubject.isBlank() && !outgoingSubject.isBlank()) {
                        // Simple subject matching (could be improved)
                        String normalizedIncoming = incomingSubject.toLowerCase().replaceAll("^(re:|fw:)\\s*", "");
                        String normalizedOutgoing = outgoingSubject.toLowerCase().replaceAll("^(re:|fw:)\\s*", "");
                        if (normalizedIncoming.equals(normalizedOutgoing)) {
                            isMatch = true;
                        }
                    }

                    if (isMatch) {
                        // Calculate response time in minutes
                        try {
                            java.time.LocalDateTime incomingDateTime = java.time.LocalDateTime.parse(incomingTime,
                                    java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                            java.time.LocalDateTime outgoingDateTime = java.time.LocalDateTime.parse(outgoingTime,
                                    java.time.format.DateTimeFormatter.ISO_DATE_TIME);

                            // Only count if outgoing is after incoming (response, not proactive email)
                            if (outgoingDateTime.isAfter(incomingDateTime)) {
                                long minutes = java.time.Duration.between(incomingDateTime, outgoingDateTime)
                                        .toMinutes();
                                if (minutes > 0) {
                                    responseTimes.add((double) minutes);
                                    break; // Found a match, move to next incoming email
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error parsing timestamps: {} - {}", incomingTime, outgoingTime, e);
                        }
                    }
                }
            }

            return calculateResponseTimeStats(responseTimes, 1);

        } catch (Exception e) {
            log.error("Error calculating response time for user: {}", userId, e);
            return createEmptyResponseTimeStats();
        }
    }

    /**
     * Create empty response time statistics
     */
    private java.util.Map<String, Object> createEmptyResponseTimeStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("averageResponseTimeMinutes", null);
        stats.put("fastestResponseTimeMinutes", null);
        stats.put("slowestResponseTimeMinutes", null);
        stats.put("respondedEmailCount", 0);
        return stats;
    }

    /**
     * Build date filter for OData queries
     */
    private String buildDateFilter(String fromDate, String toDate) {
        StringBuilder filter = new StringBuilder();
        if (fromDate != null && !fromDate.isBlank()) {
            filter.append(" and createdon ge ").append(fromDate).append("T00:00:00Z");
        }
        if (toDate != null && !toDate.isBlank()) {
            filter.append(" and createdon le ").append(toDate).append("T23:59:59Z");
        }
        return filter.toString();
    }

    /**
     * Get recent activities across all staff for dashboard timeline
     * Returns activities with staff information for visualization
     * 
     * @param top      Number of recent activities to return
     * @param fromDate Optional start date filter
     * @param toDate   Optional end date filter
     * @return List of recent activities with enriched staff data
     */
    public List<java.util.Map<String, Object>> getRecentActivities(Integer top, String fromDate, String toDate) {
        try {
            log.info("Fetching {} recent activities (FromDate: {}, ToDate: {})", top, fromDate, toDate);
            String token = authService.getAccessToken();

            // Build date filter
            String dateFilter = buildDateFilter(fromDate, toDate);

            // Query recent emails with owner information
            String uri = "/emails?$filter=statecode eq 0" + dateFilter +
                    "&$select=activityid,subject,createdon,modifiedon,directioncode,_owninguser_value" +
                    "&$expand=owninguser($select=fullname,internalemailaddress,title)" +
                    "&$orderby=createdon desc" +
                    "&$top=" + top;

            log.debug("Recent activities URI: {}", uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode activities = root.get("value");

            List<java.util.Map<String, Object>> recentActivities = new ArrayList<>();

            if (activities != null && activities.isArray()) {
                for (JsonNode activity : activities) {
                    java.util.Map<String, Object> activityData = new java.util.HashMap<>();

                    activityData.put("activityId",
                            activity.has("activityid") ? activity.get("activityid").asText() : null);
                    activityData.put("subject",
                            activity.has("subject") ? activity.get("subject").asText() : "No Subject");
                    activityData.put("createdOn",
                            activity.has("createdon") ? activity.get("createdon").asText() : null);
                    activityData.put("modifiedOn",
                            activity.has("modifiedon") ? activity.get("modifiedon").asText() : null);

                    // Direction: false = incoming, true = outgoing
                    boolean isOutgoing = activity.has("directioncode") && activity.get("directioncode").asBoolean();
                    activityData.put("direction", isOutgoing ? "outgoing" : "incoming");
                    activityData.put("activityType", "email");

                    // Extract owner information
                    if (activity.has("owninguser")) {
                        JsonNode owner = activity.get("owninguser");
                        activityData.put("staffName",
                                owner.has("fullname") ? owner.get("fullname").asText() : "Unknown");
                        activityData.put("staffEmail",
                                owner.has("internalemailaddress") ? owner.get("internalemailaddress").asText() : null);
                        activityData.put("staffTitle", owner.has("title") ? owner.get("title").asText() : null);
                    } else if (activity.has("_owninguser_value")) {
                        activityData.put("staffId", activity.get("_owninguser_value").asText());
                        activityData.put("staffName", "Loading...");
                    }

                    recentActivities.add(activityData);
                }
            }

            log.info("Retrieved {} recent activities", recentActivities.size());
            return recentActivities;

        } catch (Exception e) {
            log.error("Error fetching recent activities", e);
            return new ArrayList<>();
        }
    }

    /**
     * Filter activities to include only those from tracked staff members
     * Note: Since Activity doesn't contain staff title, we skip filtering here.
     * Activities are effectively filtered when staff list is filtered by title,
     * as activities without valid staff owners won't appear in reports.
     * 
     * @param activities List of activities to filter
     * @return List of activities (currently unfiltered at this level)
     */
    private List<Activity> filterTrackedStaffActivities(List<Activity> activities) {
        // Note: Activity filtering by staff title requires additional database lookup
        // which would be inefficient here. Instead, rely on staff-level filtering
        // in dashboard and report generation where staff are already loaded with
        // titles.
        log.debug("Activity filtering by staff title is handled at the staff service level");
        return activities;
    }
}
