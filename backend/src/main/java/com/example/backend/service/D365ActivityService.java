package com.example.backend.service;

import com.example.backend.model.Activity;
import com.example.backend.config.D365Config;
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

            // Expand to get related object details (account, contact, etc.)
            queryParams.append("$expand=regardingobjectid_account($select=name),")
                    .append("regardingobjectid_contact($select=fullname),")
                    .append("owninguser($select=fullname,internalemailaddress,title)&");

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

            // Enrich activities with staff information and friendly names
            List<Activity> enrichedActivities = enrichActivitiesWithStaffInfo(filteredActivities);

            log.info("About to call enrichEmailActivitiesSimple with {} activities", enrichedActivities.size());

            // Enrich email activities with email-specific details using simplified approach
            List<Activity> fullyEnrichedActivities = enrichEmailActivitiesSimple(enrichedActivities);

            log.info("Successfully fetched {} activities ({} from tracked staff)",
                    activities.size(), fullyEnrichedActivities.size());
            return fullyEnrichedActivities;

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

            // Build query with proper $select and $expand to match getAllActivities
            String query = "?$select=activityid,subject,description,activitytypecode," +
                    "statecode,statuscode,_owninguser_value,_regardingobjectid_value," +
                    "createdon,modifiedon,actualstart,actualend," +
                    "scheduledstart,scheduledend,actualdurationminutes,scheduleddurationminutes," +
                    "prioritycode&" +
                    "$expand=regardingobjectid_account($select=name)," +
                    "regardingobjectid_contact($select=fullname)," +
                    "owninguser($select=fullname,internalemailaddress,title)";

            String response = webClient.get()
                    .uri("/activitypointers(" + activityId + ")" + query)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            Activity activity = objectMapper.readValue(response, Activity.class);

            // Enrich with staff information
            List<Activity> enriched = enrichActivitiesWithStaffInfo(List.of(activity));
            if (!enriched.isEmpty()) {
                activity = enriched.get(0);
            }

            // Enrich email-specific details if it's an email activity
            if ("email".equalsIgnoreCase(activity.getActivityType())) {
                enriched = enrichEmailActivitiesSimple(List.of(activity));
                if (!enriched.isEmpty()) {
                    activity = enriched.get(0);
                }
            }

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
     * Get email activities with full email addresses (from activity parties)
     * This method queries the /emails endpoint directly and expands
     * email_activity_parties
     * to get actual email addresses which are not available in activitypointers
     * 
     * @param top Optional limit for number of results
     * @return List of email activities with fromEmail and toEmail populated
     */
    /**
     * Get emails with addresses (from/to/cc extracted from activity parties)
     * Supports pagination with skip parameter
     * 
     * @param top  Number of emails to fetch
     * @param skip Number of emails to skip (for pagination)
     * @return List of emails with populated email addresses
     */
    public List<Activity> getEmailsWithAddresses(Integer top, Integer skip) {
        try {
            log.info("Fetching emails with addresses from Dynamics 365 (top: {}, skip: {})", top, skip);

            String token = authService.getAccessToken();

            // Query /emails endpoint with activity parties expansion
            StringBuilder queryParams = new StringBuilder("?");
            queryParams.append("$select=activityid,subject,description,statecode,statuscode,")
                    .append("directioncode,_owninguser_value,_regardingobjectid_value,")
                    .append("createdon,modifiedon&");
            queryParams.append("$expand=email_activity_parties($select=participationtypemask,addressused),")
                    .append("regardingobjectid_account($select=name),")
                    .append("regardingobjectid_contact($select=fullname),")
                    .append("owninguser($select=fullname,internalemailaddress,title)&");
            queryParams.append("$orderby=createdon desc&");
            queryParams.append("$top=").append(top != null ? top : 50);

            // Add skip parameter for pagination
            if (skip != null && skip > 0) {
                queryParams.append("&$skip=").append(skip);
            }

            String uri = "/emails" + queryParams.toString();
            log.info("Querying D365 emails with addresses - URI: {}", uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode emailsNode = root.get("value");

            List<Activity> emails = new ArrayList<>();
            if (emailsNode != null && emailsNode.isArray()) {
                for (JsonNode emailNode : emailsNode) {
                    Activity email = parseEmailWithParties(emailNode);
                    emails.add(email);
                }
            }

            log.info("Fetched {} emails with addresses", emails.size());
            return emails;

        } catch (Exception e) {
            log.error("Error fetching emails with addresses", e);
            throw new RuntimeException("Failed to fetch emails with addresses: " + e.getMessage(), e);
        }
    }

    /**
     * Get emails with addresses (from/to/cc extracted from activity parties)
     * 
     * @param top Number of emails to fetch
     * @return List of emails with populated email addresses
     */
    public List<Activity> getEmailsWithAddresses(Integer top) {
        return getEmailsWithAddresses(top, null);
    }

    /**
     * Parse email JSON with activity parties to extract email addresses
     */
    private Activity parseEmailWithParties(JsonNode emailNode) {
        Activity email = new Activity();

        // Basic fields
        email.setActivityId(getStringValue(emailNode, "activityid"));
        email.setSubject(getStringValue(emailNode, "subject"));
        email.setDescription(getStringValue(emailNode, "description"));
        email.setActivityTypeCode("email");
        email.setStateCode(emailNode.has("statecode") ? emailNode.get("statecode").asInt() : null);
        email.setStatusCode(emailNode.has("statuscode") ? emailNode.get("statuscode").asInt() : null);
        email.setCreatedOn(getStringValue(emailNode, "createdon"));
        email.setModifiedOn(getStringValue(emailNode, "modifiedon"));

        // Direction code: true = outgoing, false = incoming
        if (emailNode.has("directioncode")) {
            boolean directionCode = emailNode.get("directioncode").asBoolean();
            email.setDirectionCode(directionCode);
            email.setDirection(directionCode ? "outgoing" : "incoming");
        }

        // Owner (staff)
        email.setOwningUserId(getStringValue(emailNode, "_owninguser_value"));
        if (emailNode.has("owninguser")) {
            JsonNode ownerNode = emailNode.get("owninguser");
            email.setOwningUserName(getStringValue(ownerNode, "fullname"));
            email.setStaffName(getStringValue(ownerNode, "fullname"));
            email.setStaffEmail(getStringValue(ownerNode, "internalemailaddress"));
        } else {
            email.setOwningUserName(
                    getStringValue(emailNode, "_owninguser_value@OData.Community.Display.V1.FormattedValue"));
            email.setStaffName(
                    getStringValue(emailNode, "_owninguser_value@OData.Community.Display.V1.FormattedValue"));
        }

        // Regarding (account/contact)
        email.setRegardingObjectId(getStringValue(emailNode, "_regardingobjectid_value"));
        if (emailNode.has("regardingobjectid_account")) {
            JsonNode accountNode = emailNode.get("regardingobjectid_account");
            email.setRegardingObjectName(getStringValue(accountNode, "name"));
        } else if (emailNode.has("regardingobjectid_contact")) {
            JsonNode contactNode = emailNode.get("regardingobjectid_contact");
            email.setRegardingObjectName(getStringValue(contactNode, "fullname"));
        } else {
            email.setRegardingObjectName(
                    getStringValue(emailNode, "_regardingobjectid_value@OData.Community.Display.V1.FormattedValue"));
        }

        // Parse email_activity_parties to get from/to addresses
        if (emailNode.has("email_activity_parties")) {
            JsonNode parties = emailNode.get("email_activity_parties");
            if (parties.isArray()) {
                for (JsonNode party : parties) {
                    int participationType = party.has("participationtypemask")
                            ? party.get("participationtypemask").asInt()
                            : 0;
                    String address = getStringValue(party, "addressused");

                    if (address != null && !address.isEmpty()) {
                        // participationtypemask: 1=From, 2=To, 3=CC, 4=BCC
                        if (participationType == 1) {
                            email.setFromEmail(address);
                        } else if (participationType == 2) {
                            // For To, concatenate if multiple recipients
                            if (email.getToEmail() == null || email.getToEmail().isEmpty()) {
                                email.setToEmail(address);
                            } else {
                                email.setToEmail(email.getToEmail() + "; " + address);
                            }
                        } else if (participationType == 3) {
                            // For CC
                            if (email.getCcEmail() == null || email.getCcEmail().isEmpty()) {
                                email.setCcEmail(address);
                            } else {
                                email.setCcEmail(email.getCcEmail() + "; " + address);
                            }
                        }
                    }
                }
            }
        }

        return email;
    }

    /**
     * Helper method to safely get string value from JsonNode
     */
    private String getStringValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }

    /**
     * Get email details for specific activity IDs
     * This is a standalone method that queries the emails table directly
     * without any enrichment dependencies
     * 
     * @param activityIds List of activity IDs to get email details for
     * @return Map of activityId -> email details (from, to, cc, sender)
     */
    public java.util.Map<String, java.util.Map<String, String>> getEmailDetailsByIds(
            java.util.List<String> activityIds) {
        log.info("Fetching email details for {} activity IDs", activityIds != null ? activityIds.size() : 0);

        java.util.Map<String, java.util.Map<String, String>> emailDetailsMap = new java.util.HashMap<>();

        try {
            if (activityIds == null || activityIds.isEmpty()) {
                log.info("No activity IDs provided");
                return emailDetailsMap;
            }

            String token = authService.getAccessToken();

            // Process in batches of 10 to avoid URL length issues
            int batchSize = 10;
            for (int i = 0; i < activityIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, activityIds.size());
                java.util.List<String> batch = activityIds.subList(i, endIndex);

                log.info("Processing email details batch {}/{}: IDs {} to {}",
                        (i / batchSize) + 1,
                        (activityIds.size() + batchSize - 1) / batchSize,
                        i + 1,
                        endIndex);

                // Build filter for this batch
                String filter = batch.stream()
                        .map(id -> "activityid eq '" + id + "'")
                        .collect(Collectors.joining(" or "));

                // Query emails with description which might contain email addresses
                String uri = "/emails?$filter=" + filter +
                        "&$select=activityid,description";

                log.info("Fetching email details for {} emails", batch.size());

                String response = webClient.get()
                        .uri(uri)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(d365Config.getTimeout()))
                        .block();

                JsonNode root = objectMapper.readTree(response);
                JsonNode emailRecords = root.get("value");

                if (emailRecords != null && emailRecords.isArray()) {
                    log.debug("Received {} email records from D365", emailRecords.size());

                    for (JsonNode emailNode : emailRecords) {
                        String activityId = emailNode.has("activityid") ? emailNode.get("activityid").asText() : null;
                        if (activityId != null) {
                            java.util.Map<String, String> details = new java.util.HashMap<>();

                            // Extract email addresses from description if available
                            if (emailNode.has("description") && !emailNode.get("description").isNull()) {
                                String description = emailNode.get("description").asText();

                                // Parse email addresses from description
                                // Look for patterns like "From:", "To:", "Cc:"
                                details.put("from", extractEmailFromDescription(description, "From:"));
                                details.put("to", extractEmailFromDescription(description, "To:"));
                                details.put("cc", extractEmailFromDescription(description, "Cc:"));
                                details.put("sender", extractEmailFromDescription(description, "From:"));
                            }

                            emailDetailsMap.put(activityId, details);

                            log.debug("Email {} details: from={}, to={}, cc={}",
                                    activityId,
                                    details.get("from"),
                                    details.get("to"),
                                    details.get("cc"));
                        }
                    }
                } else {
                    log.warn("No email records returned from D365 for batch");
                }
            }

            log.info("Successfully fetched email details for {} activities", emailDetailsMap.size());
            return emailDetailsMap;

        } catch (Exception e) {
            log.error("Error fetching email details by IDs", e);
            return emailDetailsMap;
        }
    }

    /**
     * Extract email address from description field
     * Looks for patterns like "From: email@example.com" or "To: email@example.com"
     */
    private String extractEmailFromDescription(String description, String prefix) {
        if (description == null || description.isEmpty()) {
            return null;
        }

        try {
            int startIndex = description.indexOf(prefix);
            if (startIndex == -1) {
                return null;
            }

            // Find email pattern after the prefix
            int emailStart = startIndex + prefix.length();
            String remaining = description.substring(emailStart).trim();

            // Extract email using regex
            java.util.regex.Pattern pattern = java.util.regex.Pattern
                    .compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
            java.util.regex.Matcher matcher = pattern.matcher(remaining);

            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            log.debug("Error extracting email from description: {}", e.getMessage());
        }

        return null;
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
     * @param fromDate   Optional start date filter (YYYY-MM-DD)
     * @param toDate     Optional end date filter (YYYY-MM-DD)
     * @return Email statistics with incoming and outgoing counts
     */
    public com.example.backend.model.EmailStats getEmailStatsByStaff(String staffEmail, String fromDate,
            String toDate) {
        try {
            log.info("Fetching email statistics for staff: {} (FromDate: {}, ToDate: {})", staffEmail, fromDate,
                    toDate);

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

            // Use new method that checks email participants and excludes CC'd emails
            int[] emailCounts = countEmailsByParticipants(userId, staffEmail, fromDate, toDate);
            int incomingCount = emailCounts[0];
            int outgoingCount = emailCounts[1];

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
     * Count individual emails by direction (not conversations) with optional date
     * filtering.
     * - Incoming: Each email FROM external company TO staff (staff received)
     * - Outgoing: Each email FROM staff TO external company (staff sent)
     * Only counts emails with external COMPANY accounts (excludes personal emails
     * like Gmail, Yahoo, etc.)
     * 
     * @param userId    User's system ID
     * @param userEmail User's email address
     * @param fromDate  Optional start date filter (YYYY-MM-DD)
     * @param toDate    Optional end date filter (YYYY-MM-DD)
     * @return Array with [incomingCount, outgoingCount]
     */
    private int[] countEmailsByParticipants(String userId, String userEmail, String fromDate, String toDate) {
        try {
            String token = authService.getAccessToken();

            // Build filter with user ID and optional date range
            StringBuilder filter = new StringBuilder("_owninguser_value eq " + userId);

            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append(" and createdon ge ").append(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                filter.append(" and createdon le ").append(toDate);
            }

            // Fetch all emails for this user
            String uri = "/emails?$filter=" + filter.toString()
                    + "&$select=activityid,subject,createdon"
                    + "&$expand=email_activity_parties($select=participationtypemask,addressused)"
                    + "&$orderby=createdon asc"
                    + "&$top=500";

            log.debug("Fetching emails for individual email direction counting: {}", uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode emailsArray = root.get("value");

            // Count each email individually:
            // - incoming = each email FROM external TO staff (staff received from external)
            // - outgoing = each email FROM staff TO external (staff sent to external)
            int incomingCount = 0;
            int outgoingCount = 0;

            if (emailsArray != null && emailsArray.isArray()) {
                for (JsonNode email : emailsArray) {
                    String subject = email.has("subject") ? email.get("subject").asText() : "No Subject";
                    JsonNode parties = email.get("email_activity_parties");
                    if (parties == null || !parties.isArray()) {
                        continue;
                    }

                    boolean isStaffInTo = false;
                    boolean isStaffInFrom = false;
                    boolean hasExternalFrom = false;
                    boolean hasExternalTo = false;
                    boolean hasExternalParty = false;
                    java.util.List<String> externalAddresses = new java.util.ArrayList<>();

                    for (JsonNode party : parties) {
                        String address = party.has("addressused") ? party.get("addressused").asText().toLowerCase()
                                : "";
                        int participationType = party.has("participationtypemask")
                                ? party.get("participationtypemask").asInt()
                                : 0;

                        // Check if external COMPANY party (not @bintara.com.my and not personal email)
                        boolean isExternal = !address.isEmpty() && !address.contains("@bintara.com.my");
                        boolean isCompanyEmail = isExternal && !isPersonalEmailDomain(address);

                        if (isCompanyEmail) {
                            hasExternalParty = true;
                            externalAddresses.add(address);
                            if (participationType == 1) {
                                hasExternalFrom = true; // External company sent the email
                            } else if (participationType == 2) {
                                hasExternalTo = true; // External company received the email
                            }
                        }

                        // Check if this party is our staff user
                        if (address.equals(userEmail.toLowerCase())) {
                            if (participationType == 1) {
                                isStaffInFrom = true; // Staff sent the email
                            } else if (participationType == 2) {
                                isStaffInTo = true; // Staff received the email
                            }
                        }
                    }

                    // Only count emails with external parties
                    if (hasExternalParty) {
                        // Incoming = external FROM sends TO staff (staff received from external)
                        if (isStaffInTo && hasExternalFrom) {
                            incomingCount++;
                            log.debug("INCOMING email for {}: Subject='{}', External parties={}",
                                    userEmail, subject, externalAddresses);
                        }
                        // Outgoing = staff FROM sends TO external (staff sent to external)
                        else if (isStaffInFrom && hasExternalTo) {
                            outgoingCount++;
                            log.debug("OUTGOING email for {}: Subject='{}', External parties={}",
                                    userEmail, subject, externalAddresses);
                        }
                        // Check if email meets neither condition
                        else {
                            log.debug(
                                    "SKIPPED email for {}: Subject='{}', staffInFrom={}, staffInTo={}, extFrom={}, extTo={}",
                                    userEmail, subject, isStaffInFrom, isStaffInTo, hasExternalFrom, hasExternalTo);
                        }
                    }
                }
            }

            log.info(
                    "Counted INDIVIDUAL emails (COMPANY DOMAINS ONLY) for {}: Incoming (company→staff)={}, Outgoing (staff→company)={}, Total={}",
                    userEmail, incomingCount, outgoingCount, incomingCount + outgoingCount);
            return new int[] { incomingCount, outgoingCount };

        } catch (Exception e) {
            log.error("Error counting emails by participants for {}, falling back to direction code method: {}",
                    userEmail, e.getMessage(), e);
            // Fallback to old method if participant checking fails
            try {
                String incomingFilter = "_owninguser_value eq " + userId + " and directioncode eq false";
                int incomingCount = getEmailCount(incomingFilter);

                String outgoingFilter = "_owninguser_value eq " + userId + " and directioncode eq true";
                int outgoingCount = getEmailCount(outgoingFilter);

                return new int[] { incomingCount, outgoingCount };
            } catch (Exception fallbackError) {
                log.error("Fallback method also failed", fallbackError);
                return new int[] { 0, 0 };
            }
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
            log.error("Error calculating response time for {} accounts", accountIds != null ? accountIds.size() : 0, e);
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
                    + "&$select=activityid,createdon,subject,_regardingobjectid_value"
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
                    + "&$select=activityid,createdon,subject,_regardingobjectid_value"
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
                String regardingId = incoming.has("_regardingobjectid_value")
                        ? incoming.get("_regardingobjectid_value").asText()
                        : null;

                // Find matching outgoing email (by subject similarity or regarding object)
                for (JsonNode outgoing : outgoingEmails) {
                    String outgoingSubject = outgoing.has("subject") ? outgoing.get("subject").asText() : "";
                    String outgoingTime = outgoing.get("createdon").asText();
                    String outgoingRegardingId = outgoing.has("_regardingobjectid_value")
                            ? outgoing.get("_regardingobjectid_value").asText()
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
                            // Calculate working hours excluding weekends
                            long workingMinutes = calculateWorkingMinutes(incomingTime, outgoingTime);
                            if (workingMinutes > 0 && workingMinutes < 10080) { // Exclude responses > 1 week working
                                                                                // time (likely not related)
                                responseTimes.add((double) workingMinutes);
                                log.debug("Found response: {} working minutes for subject: {}", workingMinutes,
                                        incomingSubject);
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
                    + "&$select=activityid,createdon,subject,_regardingobjectid_value"
                    + "&$top=100";

            log.debug("Fetching incoming emails with URI: {}", incomingUri);

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
                    + "&$select=activityid,createdon,subject,_regardingobjectid_value"
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

            // Null safety: check before iteration (though we've already validated counts
            // above)
            if (incomingEmails != null && outgoingEmails != null) {
                for (JsonNode incoming : incomingEmails) {
                    String incomingSubject = incoming.has("subject") ? incoming.get("subject").asText() : "";
                    String incomingTime = incoming.get("createdon").asText();
                    String regardingId = incoming.has("regardingobjectid") ? incoming.get("regardingobjectid").asText()
                            : null;

                    // Find matching outgoing email (by subject similarity or regarding object)
                    for (JsonNode outgoing : outgoingEmails) {
                        String outgoingSubject = outgoing.has("subject") ? outgoing.get("subject").asText() : "";
                        String outgoingTime = outgoing.get("createdon").asText();
                        String outgoingRegardingId = outgoing.has("_regardingobjectid_value")
                                ? outgoing.get("_regardingobjectid_value").asText()
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
                            // Calculate response time in working hours (Mon-Thu 09:00-17:00)
                            try {
                                java.time.LocalDateTime incomingDateTime = java.time.LocalDateTime.parse(incomingTime,
                                        java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                                java.time.LocalDateTime outgoingDateTime = java.time.LocalDateTime.parse(outgoingTime,
                                        java.time.format.DateTimeFormatter.ISO_DATE_TIME);

                                // Only count if outgoing is after incoming (response, not proactive email)
                                if (outgoingDateTime.isAfter(incomingDateTime)) {
                                    // Calculate working hours (Mon-Thu 09:00-17:00)
                                    long workingMinutes = calculateWorkingMinutes(incomingTime, outgoingTime);
                                    if (workingMinutes > 0) {
                                        responseTimes.add((double) workingMinutes);
                                        break; // Found a match, move to next incoming email
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Error parsing timestamps: {} - {}", incomingTime, outgoingTime, e);
                            }
                        }
                    }
                }
            } // End null safety check

            return calculateResponseTimeStats(responseTimes, 1);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Error calculating response time for user: {} - Status: {}, Response: {}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString());
            return createEmptyResponseTimeStats();
        } catch (Exception e) {
            log.error("Error calculating response time for user: {}", userId, e);
            return createEmptyResponseTimeStats();
        }
    }

    /**
     * Calculate accurate response time for staff using email_activity_parties
     * Only counts relevant business emails (RE:, RFP, RFQ, FW:, TENDER in subject)
     * 
     * @param userId    System user ID
     * @param userEmail User's email address
     * @return Map with response time statistics
     */
    public java.util.Map<String, Object> calculateAccurateResponseTime(String userId, String userEmail) {
        try {
            log.info("Calculating accurate response time for user: {} ({})", userId, userEmail);
            String token = authService.getAccessToken();

            // Fetch emails with participants - filter for relevant subjects
            String uri = "/emails?$filter=_owninguser_value eq " + userId
                    + " and statecode eq 0"
                    + "&$select=activityid,subject,createdon"
                    + "&$expand=email_activity_parties($select=participationtypemask,addressused)"
                    + "&$orderby=createdon desc"
                    + "&$top=500";

            log.debug("Fetching emails for response time: {}", uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode emailsArray = root.get("value");

            if (emailsArray == null || emailsArray.size() == 0) {
                log.info("No emails found for response time calculation");
                return createEmptyResponseTimeStats();
            }

            // Separate incoming and outgoing emails by checking participants
            // Only include relevant business emails (RE:, RFP, RFQ, FW:, TENDER)
            java.util.List<EmailWithTime> incomingEmails = new java.util.ArrayList<>();
            java.util.List<EmailWithTime> outgoingEmails = new java.util.ArrayList<>();

            for (JsonNode email : emailsArray) {
                String subject = email.has("subject") ? email.get("subject").asText() : "";
                String createdOn = email.has("createdon") ? email.get("createdon").asText() : null;
                JsonNode parties = email.get("email_activity_parties");

                if (createdOn == null || parties == null || !parties.isArray()) {
                    continue;
                }

                // Check if subject is relevant (RE:, RFP, RFQ, FW:, TENDER)
                String subjectUpper = subject.toUpperCase();
                boolean isRelevant = subjectUpper.contains("RE:") ||
                        subjectUpper.contains("RFP") ||
                        subjectUpper.contains("RFQ") ||
                        subjectUpper.contains("FW:") ||
                        subjectUpper.contains("TENDER");

                if (!isRelevant) {
                    continue;
                }

                // Determine if incoming or outgoing based on participationTypeMask
                boolean isFromStaff = false;
                boolean isToStaff = false;
                boolean hasExternalParty = false;

                for (JsonNode party : parties) {
                    String address = party.has("addressused") ? party.get("addressused").asText().toLowerCase() : "";
                    int participationType = party.has("participationtypemask")
                            ? party.get("participationtypemask").asInt()
                            : 0;

                    // Check if party is external (not @bintara.com.my)
                    if (!address.contains("@bintara.com.my") && !address.isEmpty()) {
                        hasExternalParty = true;
                    }

                    // Check if our staff is involved
                    if (address.equals(userEmail.toLowerCase())) {
                        if (participationType == 1) { // From/Sender
                            isFromStaff = true;
                        } else if (participationType == 2) { // To/Recipient
                            isToStaff = true;
                        }
                    }
                }

                // Only count emails with external parties (client emails)
                if (!hasExternalParty) {
                    continue;
                }

                EmailWithTime emailData = new EmailWithTime();
                emailData.subject = subject;
                emailData.createdOn = createdOn;

                // Incoming: Staff is in TO, has external party
                if (isToStaff && hasExternalParty) {
                    incomingEmails.add(emailData);
                    log.debug("Incoming relevant email: {}", subject);
                }
                // Outgoing: Staff is in FROM, has external party
                else if (isFromStaff && hasExternalParty) {
                    outgoingEmails.add(emailData);
                    log.debug("Outgoing relevant email: {}", subject);
                }
            }

            log.info("Found {} relevant incoming, {} relevant outgoing emails for response time",
                    incomingEmails.size(), outgoingEmails.size());

            if (incomingEmails.isEmpty() || outgoingEmails.isEmpty()) {
                return createEmptyResponseTimeStats();
            }

            // Calculate response times by matching incoming with subsequent outgoing
            java.util.List<Double> responseTimes = new java.util.ArrayList<>();

            // Log first few subjects for debugging
            if (!incomingEmails.isEmpty()) {
                log.debug("Sample incoming normalized: {}", normalizeSubject(incomingEmails.get(0).subject));
            }
            if (!outgoingEmails.isEmpty()) {
                log.debug("Sample outgoing normalized: {}", normalizeSubject(outgoingEmails.get(0).subject));
            }

            for (EmailWithTime incoming : incomingEmails) {
                String normalizedIncomingSubject = normalizeSubject(incoming.subject);

                for (EmailWithTime outgoing : outgoingEmails) {
                    String normalizedOutgoingSubject = normalizeSubject(outgoing.subject);

                    // Match by normalized subject
                    if (normalizedIncomingSubject.equals(normalizedOutgoingSubject)) {
                        try {
                            java.time.Instant incomingTime = java.time.Instant.parse(incoming.createdOn);
                            java.time.Instant outgoingTime = java.time.Instant.parse(outgoing.createdOn);

                            // Only count if outgoing is after incoming (response)
                            if (outgoingTime.isAfter(incomingTime)) {
                                long workingMinutes = calculateWorkingMinutes(incoming.createdOn, outgoing.createdOn);
                                if (workingMinutes > 0 && workingMinutes < 10080) { // < 1 week
                                    responseTimes.add((double) workingMinutes);
                                    log.debug("Response time: {} minutes for subject: {}", workingMinutes,
                                            incoming.subject);
                                    break; // Found match, move to next incoming
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error parsing timestamps: {}", e.getMessage());
                        }
                    }
                }
            }

            return calculateResponseTimeStats(responseTimes, 1);

        } catch (Exception e) {
            log.error("Error calculating accurate response time for {}: {}", userEmail, e.getMessage(), e);
            return createEmptyResponseTimeStats();
        }
    }

    /**
     * Normalize email subject for matching (remove RE:, FW:, etc.)
     */
    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "";
        }
        return subject.trim()
                .replaceAll("(?i)^(RE:|FW:|FWD:)\\s*", "")
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    /**
     * Get overall email statistics for all Bintara staff
     * 
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     * @param toDate   Optional end date filter (YYYY-MM-DD)
     * @return Map with overall statistics
     */
    public java.util.Map<String, Object> getOverallEmailStats(String fromDate, String toDate) {
        try {
            log.info("Fetching overall email statistics for all staff. FromDate: {}, ToDate: {}", fromDate, toDate);

            String token = authService.getAccessToken();

            // Get all active staff
            String staffUri = "/systemusers?$filter=isdisabled eq false&$select=systemuserid,fullname,internalemailaddress&$top=200";
            String staffResponse = webClient.get()
                    .uri(staffUri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode staffRoot = objectMapper.readTree(staffResponse);
            JsonNode staffArray = staffRoot.get("value");

            int totalStaff = 0;
            int totalIncoming = 0;
            int totalOutgoing = 0;
            int staffWithEmails = 0;

            if (staffArray != null && staffArray.isArray()) {
                for (JsonNode staff : staffArray) {
                    String email = staff.has("internalemailaddress") ? staff.get("internalemailaddress").asText()
                            : null;

                    if (email != null && email.contains("@bintara.com.my")) {
                        String userId = staff.get("systemuserid").asText();
                        totalStaff++;

                        int[] emailCounts = countEmailsByParticipants(userId, email, fromDate, toDate);
                        int incoming = emailCounts[0];
                        int outgoing = emailCounts[1];

                        if (incoming > 0 || outgoing > 0) {
                            staffWithEmails++;
                        }

                        totalIncoming += incoming;
                        totalOutgoing += outgoing;
                    }
                }
            }

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalStaffMembers", totalStaff);
            stats.put("staffWithEmails", staffWithEmails);
            stats.put("totalIncomingEmails", totalIncoming);
            stats.put("totalOutgoingEmails", totalOutgoing);
            stats.put("totalEmails", totalIncoming + totalOutgoing);
            stats.put("averageIncomingPerStaff", totalStaff > 0 ? (double) totalIncoming / totalStaff : 0.0);
            stats.put("averageOutgoingPerStaff", totalStaff > 0 ? (double) totalOutgoing / totalStaff : 0.0);
            stats.put("fromDate", fromDate);
            stats.put("toDate", toDate);

            log.info("Overall stats: {} staff, {} incoming, {} outgoing, {} total emails",
                    totalStaff, totalIncoming, totalOutgoing, (totalIncoming + totalOutgoing));

            return stats;

        } catch (Exception e) {
            log.error("Error fetching overall email statistics", e);
            java.util.Map<String, Object> emptyStats = new java.util.HashMap<>();
            emptyStats.put("error", e.getMessage());
            return emptyStats;
        }
    }

    /**
     * Check if an email address is from a personal email provider
     * (not a company domain)
     */
    private boolean isPersonalEmailDomain(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        String lowerEmail = email.toLowerCase();

        // List of common personal email domains
        String[] personalDomains = {
                "@gmail.com", "@googlemail.com",
                "@yahoo.com", "@yahoo.co.uk", "@yahoo.co.in",
                "@hotmail.com", "@hotmail.co.uk", "@hotmail.fr",
                "@outlook.com", "@live.com", "@msn.com",
                "@icloud.com", "@me.com", "@mac.com",
                "@aol.com", "@protonmail.com", "@mail.com",
                "@zoho.com", "@yandex.com", "@gmx.com",
                "@163.com", "@qq.com", "@126.com"
        };

        for (String domain : personalDomains) {
            if (lowerEmail.contains(domain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper class to store email data for response time calculation
     */
    private static class EmailWithTime {
        String subject;
        String createdOn;
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

    /**
     * Calculate conversation thread statistics for a user
     * Groups emails by subject to identify unique conversation threads
     * 
     * @param userId   System user ID
     * @param fromDate Optional start date (YYYY-MM-DD)
     * @param toDate   Optional end date (YYYY-MM-DD)
     * @return Map with totalConversations and averageEmailsPerConversation
     */
    public java.util.Map<String, Object> calculateConversationStats(String userId, String fromDate, String toDate) {
        try {
            log.info("Calculating conversation statistics for user: {}", userId);
            String token = authService.getAccessToken();

            // Build date filter component
            String dateFilter = buildDateFilter(fromDate, toDate);

            // Fetch all emails (both incoming and outgoing) owned by this user
            String emailsUri = "/emails?$filter=_owninguser_value eq " + userId
                    + dateFilter
                    + "&$select=activityid,subject"
                    + "&$top=500";

            String emailsResponse = webClient.get()
                    .uri(emailsUri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode emailsRoot = objectMapper.readTree(emailsResponse);
            JsonNode emails = emailsRoot.get("value");

            if (emails == null || !emails.isArray() || emails.size() == 0) {
                log.info("No emails found for user: {}", userId);
                return createEmptyConversationStats();
            }

            // Group emails by normalized subject to identify unique conversations
            java.util.Set<String> uniqueConversations = new java.util.HashSet<>();
            int totalEmails = 0;

            for (JsonNode email : emails) {
                totalEmails++;
                String subject = email.has("subject") ? email.get("subject").asText() : "";

                // Normalize subject (remove Re:, Fw:, extra spaces)
                String normalizedSubject = normalizeSubject(subject);

                if (!normalizedSubject.isBlank()) {
                    uniqueConversations.add(normalizedSubject);
                }
            }

            int conversationCount = uniqueConversations.size();
            double averageEmailsPerConversation = conversationCount > 0
                    ? (double) totalEmails / conversationCount
                    : 0.0;

            log.info(
                    "Conversation stats for user {}: {} conversations, {} total emails, avg {:.2f} emails/conversation",
                    userId, conversationCount, totalEmails, averageEmailsPerConversation);

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalConversations", conversationCount);
            stats.put("averageEmailsPerConversation", averageEmailsPerConversation);
            return stats;

        } catch (Exception e) {
            log.error("Error calculating conversation stats for user: {}", userId, e);
            return createEmptyConversationStats();
        }
    }

    /**
     * Create empty conversation statistics
     */
    private java.util.Map<String, Object> createEmptyConversationStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalConversations", 0);
        stats.put("averageEmailsPerConversation", 0.0);
        return stats;
    }

    /**
     * Calculate working hours between two timestamps
     * Working hours: Monday-Thursday, 09:00-17:00
     * Excludes: Friday, Saturday, Sunday, and hours outside 09:00-17:00
     * 
     * @param startTime Start timestamp (ISO format)
     * @param endTime   End timestamp (ISO format)
     * @return Working minutes between the two timestamps
     */
    private long calculateWorkingMinutes(String startTime, String endTime) {
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime,
                    java.time.format.DateTimeFormatter.ISO_DATE_TIME);
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime,
                    java.time.format.DateTimeFormatter.ISO_DATE_TIME);

            // If end is before start, return 0
            if (end.isBefore(start)) {
                return 0;
            }

            long totalWorkingMinutes = 0;
            java.time.LocalDateTime current = start;

            // Working hours: 09:00-17:00 (8 hours)
            java.time.LocalTime workStart = java.time.LocalTime.of(9, 0);
            java.time.LocalTime workEnd = java.time.LocalTime.of(17, 0);

            // Process each day from start to end
            while (current.toLocalDate().isBefore(end.toLocalDate()) ||
                    current.toLocalDate().equals(end.toLocalDate())) {

                java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();

                // Skip weekends (Saturday=6, Sunday=7) and count only weekdays (Monday=1 to
                // Friday=5)
                // Note: Currently counting Mon-Thu only as per business rules
                boolean isWeekend = (dayOfWeek == java.time.DayOfWeek.SATURDAY
                        || dayOfWeek == java.time.DayOfWeek.SUNDAY);
                boolean isWorkingDay = (dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 4); // Mon-Thu

                if (!isWeekend && isWorkingDay) {
                    // Determine working period for this day
                    java.time.LocalDateTime dayStart = java.time.LocalDateTime.of(current.toLocalDate(), workStart);
                    java.time.LocalDateTime dayEnd = java.time.LocalDateTime.of(current.toLocalDate(), workEnd);

                    // Adjust start time if it's the first day
                    if (current.toLocalDate().equals(start.toLocalDate())) {
                        if (start.toLocalTime().isAfter(workEnd)) {
                            // Start is after work hours, skip this day
                            current = current.plusDays(1).with(java.time.LocalTime.MIN);
                            continue;
                        } else if (start.toLocalTime().isAfter(workStart)) {
                            dayStart = start;
                        }
                    }

                    // Adjust end time if it's the last day
                    if (current.toLocalDate().equals(end.toLocalDate())) {
                        if (end.toLocalTime().isBefore(workStart)) {
                            // End is before work hours, skip this day
                            break;
                        } else if (end.toLocalTime().isBefore(workEnd)) {
                            dayEnd = end;
                        }
                    }

                    // Calculate minutes for this working day
                    if (dayEnd.isAfter(dayStart)) {
                        long minutesThisDay = java.time.Duration.between(dayStart, dayEnd).toMinutes();
                        totalWorkingMinutes += minutesThisDay;
                    }
                }

                // Move to next day
                current = current.plusDays(1).with(java.time.LocalTime.MIN);

                // Safety check to prevent infinite loop
                if (current.toLocalDate().isAfter(end.toLocalDate().plusDays(365))) {
                    log.warn("Working hours calculation exceeded 365 days, breaking loop");
                    break;
                }
            }

            return totalWorkingMinutes;

        } catch (Exception e) {
            log.error("Error calculating working minutes between {} and {}", startTime, endTime, e);
            return 0;
        }
    }

    /**
     * Enrich activities with staff information and friendly field names
     * 
     * @param activities List of activities to enrich
     * @return List of enriched activities
     */
    private List<Activity> enrichActivitiesWithStaffInfo(List<Activity> activities) {
        try {
            String token = authService.getAccessToken();

            // Get unique owner IDs
            java.util.Set<String> ownerIds = activities.stream()
                    .map(Activity::getOwningUserId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toSet());

            if (ownerIds.isEmpty()) {
                log.debug("No owner IDs found in activities");
                return activities.stream()
                        .map(this::setFriendlyNames)
                        .collect(Collectors.toList());
            }

            // Build filter to fetch all staff info in one query
            String filter = ownerIds.stream()
                    .map(id -> "systemuserid eq " + id)
                    .collect(Collectors.joining(" or "));

            String uri = "/systemusers?$filter=" + filter
                    + "&$select=systemuserid,fullname,internalemailaddress,title";

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode valueArray = root.get("value");

            // Create a map of userId -> staff info
            java.util.Map<String, java.util.Map<String, String>> staffMap = new java.util.HashMap<>();
            if (valueArray != null && valueArray.isArray()) {
                for (JsonNode node : valueArray) {
                    String userId = node.has("systemuserid") ? node.get("systemuserid").asText() : null;
                    if (userId != null) {
                        java.util.Map<String, String> staffInfo = new java.util.HashMap<>();
                        staffInfo.put("name", node.has("fullname") ? node.get("fullname").asText() : "Unknown");
                        staffInfo.put("email",
                                node.has("internalemailaddress") ? node.get("internalemailaddress").asText() : "");
                        staffInfo.put("title", node.has("title") ? node.get("title").asText() : "");
                        staffMap.put(userId, staffInfo);
                    }
                }
            }

            // Enrich activities with staff info
            return activities.stream()
                    .map(activity -> {
                        // Set friendly names for activity type and direction
                        setFriendlyNames(activity);

                        // Add staff information
                        String ownerId = activity.getOwningUserId();
                        if (ownerId != null && staffMap.containsKey(ownerId)) {
                            java.util.Map<String, String> staffInfo = staffMap.get(ownerId);
                            activity.setStaffName(staffInfo.get("name"));
                            activity.setStaffEmail(staffInfo.get("email"));
                            activity.setStaffTitle(staffInfo.get("title"));
                        } else if (activity.getOwningUserName() != null) {
                            // Fallback to formatted value from D365
                            activity.setStaffName(activity.getOwningUserName());
                        } else {
                            activity.setStaffName("Unknown");
                        }

                        return activity;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error enriching activities with staff info", e);
            // Return activities with at least friendly names
            return activities.stream()
                    .map(this::setFriendlyNames)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Enrich email activities with email-specific details (from, to, cc, sender)
     * Fetches data from the /emails endpoint with expanded activity parties
     * 
     * @param activities List of activities to enrich
     * @return List of activities with email details added
     */
    private List<Activity> enrichEmailActivitiesWithDetails(List<Activity> activities) {
        log.info("Starting enrichEmailActivitiesWithDetails for {} activities", activities.size());
        try {
            // Filter only email activities
            List<Activity> emailActivities = activities.stream()
                    .filter(a -> "email".equalsIgnoreCase(a.getActivityTypeCode()))
                    .collect(Collectors.toList());

            log.info("Found {} email activities to enrich", emailActivities.size());

            if (emailActivities.isEmpty()) {
                log.info("No email activities to enrich, returning original list");
                return activities;
            }

            String token = authService.getAccessToken();

            // Build filter to fetch email details for all email activities in one query
            String activityIds = emailActivities.stream()
                    .map(Activity::getActivityId)
                    .filter(id -> id != null && !id.isEmpty())
                    .map(id -> "activityid eq '" + id + "'")
                    .collect(Collectors.joining(" or "));

            if (activityIds.isEmpty()) {
                return activities;
            }

            log.debug("Built filter for {} email activities: {}", emailActivities.size(), activityIds);

            // Query emails with expanded activity parties and regarding object
            String uri = "/emails?$filter=" + activityIds
                    + "&$select=activityid,sender,from,torecipients,ccrecipients,bccrecipients,directioncode"
                    + "&$expand=email_activity_parties($select=participationtypemask,addressused),"
                    + "regardingobjectid_account($select=name),"
                    + "regardingobjectid_contact($select=fullname)";

            log.info("Fetching email details for {} emails", emailActivities.size());

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode emailsArray = root.get("value");

            log.debug("Received {} email records from D365",
                    emailsArray != null && emailsArray.isArray() ? emailsArray.size() : 0);

            // Create a map of activityId -> email details
            java.util.Map<String, java.util.Map<String, Object>> emailDetailsMap = new java.util.HashMap<>();
            if (emailsArray != null && emailsArray.isArray()) {
                for (JsonNode emailNode : emailsArray) {
                    String activityId = emailNode.has("activityid") ? emailNode.get("activityid").asText() : null;
                    if (activityId != null) {
                        log.debug("Processing email activityId: {}", activityId);
                        java.util.Map<String, Object> details = new java.util.HashMap<>();

                        // Extract email addresses from activity parties
                        if (emailNode.has("email_activity_parties")) {
                            JsonNode parties = emailNode.get("email_activity_parties");
                            List<String> fromEmails = new ArrayList<>();
                            List<String> toEmails = new ArrayList<>();
                            List<String> ccEmails = new ArrayList<>();

                            for (JsonNode party : parties) {
                                String address = party.has("addressused") ? party.get("addressused").asText() : null;
                                int participationType = party.has("participationtypemask")
                                        ? party.get("participationtypemask").asInt()
                                        : 0;

                                if (address != null && !address.isEmpty()) {
                                    // 1 = Sender/From, 2 = To, 3 = CC, 4 = BCC
                                    if (participationType == 1) {
                                        fromEmails.add(address);
                                        // Use the first from email as sender if we don't have one yet
                                        if (!details.containsKey("sender") || details.get("sender") == null) {
                                            details.put("sender", address);
                                        }
                                    } else if (participationType == 2) {
                                        toEmails.add(address);
                                    } else if (participationType == 3) {
                                        ccEmails.add(address);
                                    }
                                }
                            }

                            details.put("fromEmail", String.join("; ", fromEmails));
                            details.put("toEmail", String.join("; ", toEmails));
                            details.put("ccEmail", String.join("; ", ccEmails));

                            log.debug("Email {} - Extracted: sender={}, from={}, to={}, cc={}",
                                    activityId, details.get("sender"),
                                    details.get("fromEmail"), details.get("toEmail"), details.get("ccEmail"));
                        }

                        // Extract regarding object (account or contact name)
                        if (emailNode.has("regardingobjectid_account")
                                && emailNode.get("regardingobjectid_account").has("name")) {
                            details.put("regardingObjectName",
                                    emailNode.get("regardingobjectid_account").get("name").asText());
                        } else if (emailNode.has("regardingobjectid_contact")
                                && emailNode.get("regardingobjectid_contact").has("fullname")) {
                            details.put("regardingObjectName",
                                    emailNode.get("regardingobjectid_contact").get("fullname").asText());
                        }

                        emailDetailsMap.put(activityId, details);
                    }
                }
            }

            // Enrich the original activities list with email details
            log.debug("Enriching {} activities with email details. emailDetailsMap has {} entries",
                    activities.size(), emailDetailsMap.size());

            return activities.stream()
                    .map(activity -> {
                        if ("email".equalsIgnoreCase(activity.getActivityTypeCode())
                                && emailDetailsMap.containsKey(activity.getActivityId())) {
                            java.util.Map<String, Object> details = emailDetailsMap.get(activity.getActivityId());
                            activity.setSender((String) details.get("sender"));
                            activity.setFromEmail((String) details.get("fromEmail"));
                            activity.setToEmail((String) details.get("toEmail"));
                            activity.setCcEmail((String) details.get("ccEmail"));
                            activity.setRegardingObjectName((String) details.get("regardingObjectName"));

                            log.debug("Enriched email activity {} with sender={}, from={}, to={}",
                                    activity.getActivityId(), activity.getSender(),
                                    activity.getFromEmail(), activity.getToEmail());
                        }
                        return activity;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error enriching email activities with details", e);
            // Return original activities if enrichment fails
            return activities;
        }
    }

    /**
     * Simplified email enrichment using conversation tracking and email entity
     * fields
     * This method directly queries the emails table to get sender/recipient info
     * 
     * @param activities List of activities to enrich
     * @return List of activities with email details added
     */
    private List<Activity> enrichEmailActivitiesSimple(List<Activity> activities) {
        log.info("Starting enrichEmailActivitiesSimple for {} activities", activities.size());

        try {
            // Filter to get only email activities
            List<Activity> emailActivities = activities.stream()
                    .filter(a -> "email".equalsIgnoreCase(a.getActivityTypeCode()))
                    .collect(Collectors.toList());

            log.info("Found {} email activities to enrich", emailActivities.size());

            if (emailActivities.isEmpty()) {
                log.info("No email activities to enrich, returning original list");
                return activities;
            }

            String token = authService.getAccessToken();

            // Process in batches of 10 to avoid URL length issues
            int batchSize = 10;
            for (int i = 0; i < emailActivities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, emailActivities.size());
                List<Activity> batch = emailActivities.subList(i, endIndex);

                log.info("Processing email batch {}/{}: emails {} to {}",
                        (i / batchSize) + 1,
                        (emailActivities.size() + batchSize - 1) / batchSize,
                        i + 1,
                        endIndex);

                // Build filter for this batch
                String activityIds = batch.stream()
                        .map(Activity::getActivityId)
                        .map(id -> "activityid eq '" + id + "'")
                        .collect(Collectors.joining(" or "));

                log.debug("Built filter for {} emails in batch", batch.size());

                // Query emails with sender and description fields
                String uri = "/emails?$filter=" + activityIds +
                        "&$select=activityid,sender,from,to,cc,description,conversationindex,conversationtrackingid";

                log.info("Fetching email details for {} emails", batch.size());

                String response = webClient.get()
                        .uri(uri)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(d365Config.getTimeout()))
                        .block();

                JsonNode root = objectMapper.readTree(response);
                JsonNode emailRecords = root.get("value");

                if (emailRecords != null && emailRecords.isArray()) {
                    log.debug("Received {} email records from D365", emailRecords.size());

                    // Create map of activityId -> email data
                    java.util.Map<String, JsonNode> emailMap = new java.util.HashMap<>();
                    for (JsonNode emailNode : emailRecords) {
                        String activityId = emailNode.has("activityid") ? emailNode.get("activityid").asText() : null;
                        if (activityId != null) {
                            emailMap.put(activityId, emailNode);
                        }
                    }

                    // Enrich activities with email data
                    for (Activity activity : batch) {
                        JsonNode emailData = emailMap.get(activity.getActivityId());
                        if (emailData != null) {
                            // Extract email fields directly
                            if (emailData.has("sender")) {
                                activity.setSender(emailData.get("sender").asText());
                            }
                            if (emailData.has("from")) {
                                activity.setFromEmail(emailData.get("from").asText());
                            }
                            if (emailData.has("to")) {
                                activity.setToEmail(emailData.get("to").asText());
                            }
                            if (emailData.has("cc")) {
                                activity.setCcEmail(emailData.get("cc").asText());
                            }

                            log.debug("Enriched email {} with sender={}, from={}, to={}, cc={}",
                                    activity.getActivityId(),
                                    activity.getSender(),
                                    activity.getFromEmail(),
                                    activity.getToEmail(),
                                    activity.getCcEmail());
                        } else {
                            log.debug("No email data found for activity {}", activity.getActivityId());
                        }
                    }
                } else {
                    log.warn("No email records returned from D365 for batch");
                }
            }

            log.info("Successfully enriched {} email activities", emailActivities.size());
            return activities;

        } catch (Exception e) {
            log.error("Error in enrichEmailActivitiesSimple", e);
            // Return original activities if enrichment fails
            return activities;
        }
    }

    /**
     * Set friendly names for activity type and direction
     * 
     * @param activity Activity to set friendly names for
     * @return Activity with friendly names set
     */
    private Activity setFriendlyNames(Activity activity) {
        // Set friendly activity type
        String typeCode = activity.getActivityTypeCode();
        if (typeCode != null) {
            switch (typeCode.toLowerCase()) {
                case "email":
                    activity.setActivityType("Email");
                    break;
                case "phonecall":
                    activity.setActivityType("Phone Call");
                    break;
                case "appointment":
                    activity.setActivityType("Meeting");
                    break;
                case "task":
                    activity.setActivityType("Task");
                    break;
                case "letter":
                    activity.setActivityType("Letter");
                    break;
                case "fax":
                    activity.setActivityType("Fax");
                    break;
                default:
                    activity.setActivityType(typeCode);
            }
        }

        // Set direction (incoming/outgoing)
        if (activity.getDirectionCode() != null) {
            activity.setDirection(activity.getDirectionCode() ? "outgoing" : "incoming");
        } else {
            // For non-email activities, assume outgoing
            activity.setDirection("outgoing");
        }

        return activity;
    }
}
