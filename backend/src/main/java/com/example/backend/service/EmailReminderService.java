package com.example.backend.service;

import com.example.backend.model.Activity;
import com.example.backend.model.EmailReminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailReminderService {

    private final D365ActivityService activityService;

    /**
     * Get all email reminders for emails that need follow-up
     * 
     * @return List of email reminders sorted by urgency (critical first)
     */
    public List<EmailReminder> getEmailReminders() {
        try {
            log.info("Fetching email follow-up reminders");

            // Fetch ALL emails in batches to scan entire history
            List<Activity> allEmails = fetchAllEmailsInBatches();
            log.info("Found {} total emails across all batches", allEmails.size());

            // Filter outgoing and incoming emails (no date limit - scan all history)
            List<Activity> outgoingEmails = allEmails.stream()
                    .filter(email -> "outgoing".equalsIgnoreCase(email.getDirection()))
                    .filter(email -> email.getStateCode() == 1) // Completed
                    .collect(Collectors.toList());

            log.info("Found {} outgoing emails", outgoingEmails.size());

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

            for (Activity email : outgoingEmails) {
                // Skip if no recipient email
                if (email.getToEmail() == null || email.getToEmail().isEmpty()) {
                    continue;
                }

                // Check if this email has been replied to
                String emailKey = getEmailKey(email);
                if (repliedEmailAddresses.contains(emailKey)) {
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

                // Show all unreplied emails (badges: 3 for 0-6 days, 7 for 7-13 days, 14 for
                // 14+ days)
                if (daysOverdue >= 0) {
                    EmailReminder reminder = createReminder(email, daysOverdue, sentDate);
                    reminders.add(reminder);
                }
            }

            // Sort by urgency: critical (14+) first, then medium (7-13), then low (3-6)
            reminders.sort((r1, r2) -> Integer.compare(r2.getDaysOverdue(), r1.getDaysOverdue()));

            log.info("Created {} email reminders", reminders.size());
            return reminders;

        } catch (Exception e) {
            log.error("Error fetching email reminders", e);
            throw new RuntimeException("Failed to fetch email reminders: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch all emails in batches to avoid timeout
     * Optimized batch size for reliability and performance
     * 
     * @return List of all emails
     */
    private List<Activity> fetchAllEmailsInBatches() {
        List<Activity> allEmails = new ArrayList<>();
        int batchSize = 100; // Balanced batch size for reliability
        int skip = 0;
        int totalFetched = 0;
        int maxBatches = 50; // Maximum 50 batches = 5000 emails total
        int batchCount = 0;

        log.info("Starting optimized batch fetch (batch size: {}, max batches: {})", batchSize, maxBatches);

        while (batchCount < maxBatches) {
            try {
                log.info("Fetching batch {} (skip: {})", batchCount + 1, skip);

                // Fetch next batch
                List<Activity> batch = activityService.getEmailsWithAddresses(batchSize, skip);

                if (batch == null || batch.isEmpty()) {
                    log.info("No more emails to fetch. Total fetched: {}", totalFetched);
                    break;
                }

                allEmails.addAll(batch);
                totalFetched += batch.size();
                skip += batchSize;
                batchCount++;

                log.info("✓ Batch {} complete: {} emails (total: {})", batchCount, batch.size(), totalFetched);

                // If batch returned less than batchSize, we've reached the end
                if (batch.size() < batchSize) {
                    log.info("✓ All emails fetched. Total: {} emails in {} batches", totalFetched, batchCount);
                    break;
                }

            } catch (Exception e) {
                log.error("✗ Error fetching batch {} at skip={}: {}", batchCount + 1, skip, e.getMessage(), e);
                log.warn("Continuing with {} emails already fetched", totalFetched);
                break;
            }
        }

        if (batchCount >= maxBatches) {
            log.warn("Reached maximum batch limit. Fetched {} emails. Consider increasing maxBatches if needed.",
                    totalFetched);
        }

        return allEmails;
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
