package com.example.backend.service;

import com.example.backend.config.D365Config;
import com.example.backend.model.Activity;
import com.example.backend.model.D365Response;
import com.example.backend.model.UnrepliedEmail;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnrepliedEmailService {

    private final WebClient webClient;
    private final D365AuthService authService;
    private final ObjectMapper objectMapper;

    public UnrepliedEmailService(WebClient.Builder webClientBuilder,
            D365AuthService authService,
            D365Config d365Config,
            ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(d365Config.getBaseUrl())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024)) // 50MB buffer size for large email responses
                .build();
    }

    /**
     * Get unreplied emails for Bintara staff
     * An email is considered unreplied if:
     * 1. It was received by Bintara staff (incoming, directionCode=false)
     * 2. No outgoing email with matching subject was sent after receiving it
     */
    public List<UnrepliedEmail> getUnrepliedEmails(Integer maxHoursOld) {
        try {
            log.info("=== Starting Unreplied Email Detection ===");
            log.info("Time window: {} hours", maxHoursOld);

            // Calculate cutoff time first
            ZonedDateTime cutoffTime = maxHoursOld != null
                    ? ZonedDateTime.now().minusHours(maxHoursOld)
                    : ZonedDateTime.now().minusDays(30);
            log.info("Cutoff time: {}", cutoffTime);

            // Get access token
            String token = authService.getAccessToken();

            // Fetch emails from /emails endpoint with date filter
            // Format: createdon ge 2025-12-15T00:00:00Z
            String dateFilter = String.format("createdon ge %s", cutoffTime.toString().substring(0, 19) + "Z");
            String uri = "/emails?$select=activityid,subject,directioncode,createdon,modifiedon,description," +
                    "statecode,statuscode&$filter=" + dateFilter + "&$top=1000";

            log.debug("Fetching emails from: {}", uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(60000)) // Increase timeout to 60 seconds
                    .block();

            D365Response<Activity> d365Response = objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructParametricType(D365Response.class, Activity.class));

            List<Activity> allEmails = d365Response.getValue();
            log.info("Retrieved {} total emails from D365 (filtered by date >= {})", allEmails.size(), cutoffTime);

            if (allEmails.isEmpty()) {
                log.warn("No emails found in D365 within the specified time window");
                return new ArrayList<>();
            }

            // Treat all emails as potential candidates
            // We'll filter by checking for replies based on subject matching
            List<Activity> allEmailsList = new ArrayList<>(allEmails);

            log.info("Processing {} total emails for unreplied detection", allEmailsList.size());

            // Create a set of normalized subjects from ALL emails for matching
            Set<String> allSubjects = allEmailsList.stream()
                    .map(Activity::getSubject)
                    .filter(Objects::nonNull)
                    .map(this::normalizeSubject)
                    .collect(Collectors.toSet());

            log.info("Normalized {} unique subjects for matching", allSubjects.size());

            // Find unreplied emails - emails with unique subjects that don't have a
            // matching reply
            // Group by normalized subject and find those with only one occurrence
            Map<String, List<Activity>> emailsBySubject = allEmailsList.stream()
                    .filter(email -> email.getSubject() != null && !email.getSubject().trim().isEmpty())
                    .collect(Collectors.groupingBy(email -> normalizeSubject(email.getSubject())));

            List<UnrepliedEmail> unrepliedEmails = new ArrayList<>();
            int noSubjectCount = 0;
            int repliedCount = 0;

            for (Activity email : allEmailsList) {
                String subject = email.getSubject();
                if (subject == null || subject.trim().isEmpty()) {
                    noSubjectCount++;
                    continue;
                }

                String normalizedSubject = normalizeSubject(subject);
                List<Activity> matchingEmails = emailsBySubject.get(normalizedSubject);

                // An email is unreplied if:
                // 1. It's the only email with this subject (no replies)
                // 2. OR it doesn't start with RE:/FW: and there's no reply to it
                boolean hasReply = false;
                boolean isOriginalEmail = !subject.toLowerCase().trim().startsWith("re:") &&
                        !subject.toLowerCase().trim().startsWith("fw:") &&
                        !subject.toLowerCase().trim().startsWith("fwd:");

                if (matchingEmails != null && matchingEmails.size() > 1 && isOriginalEmail) {
                    // Check if there's any email that starts with RE: or FW: (indicating a reply)
                    hasReply = matchingEmails.stream()
                            .anyMatch(e -> e.getSubject() != null && !e.getActivityId().equals(email.getActivityId()) &&
                                    (e.getSubject().toLowerCase().trim().startsWith("re:") ||
                                            e.getSubject().toLowerCase().trim().startsWith("fw:") ||
                                            e.getSubject().toLowerCase().trim().startsWith("fwd:")));
                }

                // Only show original emails (not RE:/FW:) that don't have replies
                if (isOriginalEmail && !hasReply) {
                    log.info("✗ UNREPLIED: '{}' (created: {})",
                            subject, email.getCreatedOn());

                    // Calculate time-based metadata
                    long hoursUnreplied = calculateHoursUnreplied(email.getCreatedOn());
                    String urgencyLevel = determineUrgencyLevel(hoursUnreplied);
                    String ageCategory = determineAgeCategory(hoursUnreplied);
                    boolean isOverdue = hoursUnreplied > 48;

                    UnrepliedEmail unreplied = UnrepliedEmail.builder()
                            .activityId(email.getActivityId())
                            .subject(subject)
                            .fromEmail("Staff Member")
                            .sender("Staff Member")
                            .description(email.getDescription())
                            .assignedTo(null)
                            .assignedToName("Staff Member")
                            .createdOn(email.getCreatedOn())
                            .modifiedOn(email.getModifiedOn())
                            .hoursUnreplied(hoursUnreplied)
                            .urgencyLevel(urgencyLevel)
                            .ageCategory(ageCategory)
                            .isOverdue(isOverdue)
                            .regardingObjectId(null)
                            .regardingObjectName(null)
                            .regardingObjectTypeCode(null)
                            .build();

                    unrepliedEmails.add(unreplied);
                } else {
                    repliedCount++;
                    log.debug("✓ REPLIED: '{}'", subject);
                }
            }

            log.info("=== Detection Complete ===");
            log.info("Total emails: {}, Replied: {}, Unreplied: {}, No subject: {}",
                    allEmailsList.size(), repliedCount, unrepliedEmails.size(), noSubjectCount);

            return unrepliedEmails;

        } catch (Exception e) {
            log.error("Error fetching unreplied emails", e);
            return new ArrayList<>();
        }
    }

    /**
     * Normalize subject for matching
     * Removes RE:, FW:, FWD: prefixes and normalizes whitespace
     */
    private String normalizeSubject(String subject) {
        if (subject == null)
            return "";

        // Remove RE:, FW:, FWD: prefixes (case insensitive)
        String normalized = subject.trim()
                .replaceAll("(?i)^(re:|fw:|fwd:)\\s*", "")
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();

        return normalized;
    }

    /**
     * Calculate hours since the email was created
     */
    private long calculateHoursUnreplied(String createdOn) {
        try {
            ZonedDateTime created = ZonedDateTime.parse(createdOn);
            ZonedDateTime now = ZonedDateTime.now();
            return Duration.between(created, now).toHours();
        } catch (Exception e) {
            log.error("Error parsing createdOn date: {}", createdOn, e);
            return 0;
        }
    }

    /**
     * Determine urgency level based on hours unreplied
     * - low: < 24 hours
     * - medium: 24-48 hours
     * - high: 48-72 hours
     * - critical: > 72 hours
     */
    private String determineUrgencyLevel(long hours) {
        if (hours < 24) {
            return "low";
        } else if (hours < 48) {
            return "medium";
        } else if (hours < 72) {
            return "high";
        } else {
            return "critical";
        }
    }

    /**
     * Determine age category for display
     */
    private String determineAgeCategory(long hours) {
        if (hours < 24) {
            return "< 24h";
        } else if (hours < 48) {
            return "24-48h";
        } else if (hours < 72) {
            return "48-72h";
        } else {
            return "> 72h";
        }
    }

    /**
     * Determine if email is incoming based on recipient
     * Email is considered incoming if sent TO a @bintara.com.my address
     */
    private boolean isEmailIncoming(Activity email) {
        String toEmail = email.getToEmail();
        String fromEmail = email.getFromEmail();

        // Check if TO field contains @bintara.com.my
        if (toEmail != null && toEmail.toLowerCase().contains("@bintara.com.my")) {
            return true;
        }

        // If FROM field does NOT contain @bintara.com.my, it's likely incoming
        if (fromEmail != null && !fromEmail.toLowerCase().contains("@bintara.com.my")) {
            return true;
        }

        return false;
    }
}
