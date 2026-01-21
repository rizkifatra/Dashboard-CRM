package com.example.backend.service;

import com.example.backend.model.Activity;
import com.example.backend.model.EmailReminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing email follow-up reminders
 * Optimized with caching to reduce D365 API calls
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailReminderService {

    private final D365ActivityService activityService;

    // Cache for performance optimization
    private List<EmailReminder> cachedReminders = null;
    private LocalDateTime cacheTimestamp = null;
    private static final int CACHE_DURATION_MINUTES = 2; // Cache for 2 minutes

    /**
     * Get all email reminders for emails that need follow-up
     * Cached for 2 minutes to improve performance
     * 
     * @return List of email reminders sorted by urgency (critical first)
     */
    public List<EmailReminder> getEmailReminders() {
        // Check cache first
        if (cachedReminders != null && cacheTimestamp != null) {
            long minutesSinceCache = ChronoUnit.MINUTES.between(cacheTimestamp, LocalDateTime.now());
            if (minutesSinceCache < CACHE_DURATION_MINUTES) {
                log.info("Returning cached email reminders ({} items, cached {} min ago)",
                        cachedReminders.size(), minutesSinceCache);
                return cachedReminders;
            }
        }

        try {
            log.info("Fetching fresh email follow-up reminders from D365");

            // Fetch ALL emails in batches to scan entire history
            List<Activity> allEmails = fetchAllEmailsInBatches();
            log.info("Found {} total emails across all batches", allEmails.size());

            // Filter outgoing and incoming emails (no date limit - scan all history)
            List<Activity> outgoingEmails = allEmails.stream()
                    .filter(email -> "outgoing".equalsIgnoreCase(email.getDirection()))
                    // Remove stateCode filter - include all sent emails regardless of completion
                    // status
                    .collect(Collectors.toList());

            log.info("Found {} outgoing emails (all states)", outgoingEmails.size());

            // Log state code distribution for debugging
            Map<Integer, Long> stateCodeCounts = outgoingEmails.stream()
                    .collect(Collectors.groupingBy(Activity::getStateCode, Collectors.counting()));
            log.info("Outgoing email state codes: {}", stateCodeCounts);

            List<Activity> incomingEmails = allEmails.stream()
                    .filter(email -> "incoming".equalsIgnoreCase(email.getDirection()))
                    .collect(Collectors.toList());

            log.info("Found {} incoming emails", incomingEmails.size());

            // Build map of replied emails (by email address and account)
            Set<String> repliedEmailAddresses = buildRepliedEmailSet(incomingEmails);
            log.info("Found {} unique replied email addresses", repliedEmailAddresses.size());

            // Process each outgoing email to check if it needs follow-up
            List<EmailReminder> reminders = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            int skippedNoRecipient = 0;
            int skippedHasReply = 0;
            int skippedTooRecent = 0;
            int skippedCancelled = 0;

            for (Activity email : outgoingEmails) {
                // Skip if no recipient email
                if (email.getToEmail() == null || email.getToEmail().isEmpty()) {
                    skippedNoRecipient++;
                    continue;
                }

                // Skip if email is Cancelled (stateCode = 2) in CRM
                // Note: stateCode 1 (Completed) is normal for sent emails, so we don't skip
                // those
                // Only skip cancelled emails which indicate staff decided not to pursue
                if (email.getStateCode() != null && email.getStateCode() == 2) {
                    skippedCancelled++;
                    continue; // Email cancelled in CRM, no reminder needed
                }

                // Check if this email has been replied to
                String emailKey = getEmailKey(email);
                if (repliedEmailAddresses.contains(emailKey)) {
                    skippedHasReply++;
                    continue; // Email has reply, no reminder needed
                }

                // Calculate days since sent
                // Try to parse actual sent date from email body first, then fallback to D365
                // fields
                String sentDate = extractSentDateFromDescription(email);
                if (sentDate == null) {
                    sentDate = email.getSentOn() != null ? email.getSentOn()
                            : (email.getActualEnd() != null ? email.getActualEnd() : email.getCreatedOn());
                }
                int daysOverdue = calculateDaysOverdue(sentDate, now);

                // Show only emails that are 3+ days old (badges: 3 for 3-6 days, 7 for 7-13
                // days, 14 for 14+ days)
                if (daysOverdue >= 3) {
                    EmailReminder reminder = createReminder(email, daysOverdue, sentDate);
                    reminders.add(reminder);
                } else {
                    skippedTooRecent++;
                }
            }

            log.info(
                    "Filtering summary: {} skipped (no recipient), {} skipped (cancelled in CRM), {} skipped (has reply), {} skipped (too recent <3 days)",
                    skippedNoRecipient, skippedCancelled, skippedHasReply, skippedTooRecent);

            // Sort by urgency: critical (14+) first, then medium (7-13), then low (3-6)
            reminders.sort((r1, r2) -> Integer.compare(r2.getDaysOverdue(), r1.getDaysOverdue()));

            log.info("Created {} email reminders", reminders.size());

            // Cache the results
            cachedReminders = reminders;
            cacheTimestamp = LocalDateTime.now();
            log.info("Cached {} reminders for {} minutes", reminders.size(), CACHE_DURATION_MINUTES);

            return reminders;

        } catch (Exception e) {
            log.error("Error fetching email reminders", e);
            throw new RuntimeException("Failed to fetch email reminders: " + e.getMessage(), e);
        }
    }

    /**
     * Clear cache manually (useful for forcing refresh)
     */
    public void clearCache() {
        cachedReminders = null;
        cacheTimestamp = null;
        log.info("Email reminders cache cleared");
    }

    /**
     * Fetch all emails in batches to avoid timeout
     * Optimized batch size for reliability and performance
     * 
     * Note: D365 doesn't support $skip with $expand, so we fetch a single large
     * batch
     * and deduplicate by activityId to prevent duplicates
     * 
     * @return List of all emails (deduplicated)
     */
    private List<Activity> fetchAllEmailsInBatches() {
        try {
            log.info("Fetching emails with addresses from D365");

            // Fetch 600 emails - optimized balance between coverage and performance
            // Description field needed to extract actual sent date from email body
            // WebClient buffer is 50MB - 600 prevents buffer overflow
            // With 2-min cache, we reduce D365 API calls significantly
            int batchSize = 600;
            List<Activity> emails = activityService.getEmailsWithAddresses(batchSize, 0);

            if (emails == null || emails.isEmpty()) {
                log.info("No emails found");
                return new ArrayList<>();
            }

            // Deduplicate by activityId (just in case)
            Map<String, Activity> uniqueEmails = new LinkedHashMap<>();
            for (Activity email : emails) {
                if (email.getActivityId() != null) {
                    uniqueEmails.put(email.getActivityId(), email);
                }
            }

            List<Activity> deduplicatedEmails = new ArrayList<>(uniqueEmails.values());
            log.info("✓ Fetched {} emails ({} after deduplication)", emails.size(), deduplicatedEmails.size());

            return deduplicatedEmails;

        } catch (Exception e) {
            log.error("✗ Error fetching emails: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Build set of email addresses that have replied
     */
    private Set<String> buildRepliedEmailSet(List<Activity> incomingEmails) {
        Set<String> repliedEmails = new HashSet<>();

        for (Activity email : incomingEmails) {
            if (email.getFromEmail() != null && !email.getFromEmail().isEmpty()) {
                String key = email.getFromEmail().toLowerCase().trim();
                if (email.getRegardingObjectId() != null) {
                    key = key + "|" + email.getRegardingObjectId();
                }
                repliedEmails.add(key);
            }
        }

        return repliedEmails;
    }

    /**
     * Get unique key for email (recipient email + account ID)
     */
    private String getEmailKey(Activity email) {
        String key = email.getToEmail().toLowerCase().trim();
        if (email.getRegardingObjectId() != null) {
            key = key + "|" + email.getRegardingObjectId();
        }
        return key;
    }

    /**
     * Calculate days overdue since email was sent
     */
    private int calculateDaysOverdue(String sentDateString, LocalDateTime now) {
        try {
            LocalDateTime sentDate = parseDate(sentDateString);
            return (int) ChronoUnit.DAYS.between(sentDate, now);
        } catch (Exception e) {
            log.error("Error calculating days overdue", e);
            return 0;
        }
    }

    /**
     * Parse ISO 8601 date string to LocalDateTime
     */
    private LocalDateTime parseDate(String dateString) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
            return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.error("Error parsing date: {}", dateString, e);
            return LocalDateTime.now();
        }
    }

    /**
     * Create EmailReminder from Activity
     */
    private EmailReminder createReminder(Activity email, int daysOverdue, String sentDate) {
        EmailReminder reminder = new EmailReminder();
        reminder.setActivityId(email.getActivityId());
        reminder.setSubject(email.getSubject() != null ? email.getSubject() : "No subject");
        reminder.setToEmail(email.getToEmail());
        reminder.setAccountId(email.getRegardingObjectId());
        reminder.setAccountName(email.getRegardingObjectName());
        reminder.setStaffName(email.getStaffName());
        reminder.setStaffEmail(email.getStaffEmail());
        reminder.setSentDate(sentDate);
        reminder.setDaysOverdue(daysOverdue);

        // Set urgency level based on days overdue
        // Critical: 14+ days, Medium: 7-13 days, Low: 3-6 days
        if (daysOverdue >= 14) {
            reminder.setUrgencyLevel("critical");
            reminder.setUrgencyBadge("14+ Days");
        } else if (daysOverdue >= 7) {
            reminder.setUrgencyLevel("medium");
            reminder.setUrgencyBadge("7 Days");
        } else {
            reminder.setUrgencyLevel("low");
            reminder.setUrgencyBadge("3 Days");
        }

        return reminder;
    }

    /**
     * Extract actual sent date from email description/body
     * Parses patterns like "Sent: Tuesday, December 30, 2025 2:50 PM"
     * 
     * @param email Email activity containing description
     * @return ISO 8601 formatted date string or null if not found
     */
    private String extractSentDateFromDescription(Activity email) {
        if (email.getDescription() == null || email.getDescription().isEmpty()) {
            return null;
        }

        try {
            // Strip HTML tags from description since D365 stores emails as HTML
            // This converts "<b>Sent:</b>&nbsp;Tuesday, December 30, 2025 2:50 PM"
            // to "Sent: Tuesday, December 30, 2025 2:50 PM"
            String plainText = email.getDescription()
                    .replaceAll("<[^>]*>", " ") // Remove all HTML tags
                    .replaceAll("&nbsp;", " ") // Replace HTML spaces
                    .replaceAll("&amp;", "&") // Replace HTML ampersands
                    .replaceAll("\\s+", " ") // Normalize whitespace
                    .trim();

            log.debug("Searching for sent date in email: {}", email.getSubject());
            log.debug("Plain text length: {} chars", plainText.length());

            // Pattern 1: "Sent: Tuesday, December 30, 2025 2:50 PM" or "Sent: Monday,
            // December 30, 2025, 2:50 PM"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "Sent:\\s+(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s+" +
                            "([A-Za-z]+)\\s+(\\d{1,2}),\\s+(\\d{4})[,\\s]+(\\d{1,2}):(\\d{2})\\s+(AM|PM)",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

            java.util.regex.Matcher matcher = pattern.matcher(plainText);

            if (matcher.find()) {
                log.info("MATCH FOUND in email: {}", email.getSubject());
                log.info("Matched text: {}", matcher.group(0));

                String month = matcher.group(1);
                String day = matcher.group(2);
                String year = matcher.group(3);
                String hour = matcher.group(4);
                String minute = matcher.group(5);
                String ampm = matcher.group(6);

                // Convert month name to number
                Map<String, String> monthMap = Map.ofEntries(
                        Map.entry("january", "01"), Map.entry("february", "02"), Map.entry("march", "03"),
                        Map.entry("april", "04"), Map.entry("may", "05"), Map.entry("june", "06"),
                        Map.entry("july", "07"), Map.entry("august", "08"), Map.entry("september", "09"),
                        Map.entry("october", "10"), Map.entry("november", "11"), Map.entry("december", "12"));

                String monthNum = monthMap.get(month.toLowerCase());
                if (monthNum == null) {
                    log.warn("Unknown month name: {}", month);
                    return null;
                }

                // Convert 12-hour to 24-hour format
                int hourInt = Integer.parseInt(hour);
                if ("PM".equalsIgnoreCase(ampm) && hourInt != 12) {
                    hourInt += 12;
                } else if ("AM".equalsIgnoreCase(ampm) && hourInt == 12) {
                    hourInt = 0;
                }

                // Build ISO 8601 format: 2025-12-30T14:50:00Z
                String isoDate = String.format("%s-%s-%02dT%02d:%s:00Z",
                        year, monthNum, Integer.parseInt(day), hourInt, minute);

                log.info("Successfully extracted sent date: {} -> {}", matcher.group(0), isoDate);
                return isoDate;
            } else {
                log.warn("No regex match found in email: {}", email.getSubject());
            }
        } catch (Exception e) {
            log.error("Failed to parse sent date from email description: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Get count of reminders by urgency level
     */
    public Map<String, Integer> getReminderCounts() {
        List<EmailReminder> reminders = getEmailReminders();

        Map<String, Integer> counts = new HashMap<>();
        counts.put("total", reminders.size());
        counts.put("critical", (int) reminders.stream().filter(r -> "critical".equals(r.getUrgencyLevel())).count());
        counts.put("medium", (int) reminders.stream().filter(r -> "medium".equals(r.getUrgencyLevel())).count());
        counts.put("low", (int) reminders.stream().filter(r -> "low".equals(r.getUrgencyLevel())).count());

        return counts;
    }
}
