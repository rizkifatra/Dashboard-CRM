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

            // Fetch ALL emails in batches to avoid timeout (50 emails per batch)
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
                int daysOverdue = calculateDaysOverdue(email.getCreatedOn(), now);

                // Show all unreplied emails regardless of age
                if (daysOverdue >= 0) {
                    EmailReminder reminder = createReminder(email, daysOverdue);
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
     * Fetches 200 emails at a time until no more emails are returned
     * 
     * @return List of all emails
     */
    private List<Activity> fetchAllEmailsInBatches() {
        List<Activity> allEmails = new ArrayList<>();
        int batchSize = 200; // Increased batch size to fetch more emails per request
        int skip = 0;
        int totalFetched = 0;

        log.info("Starting batch fetch of all emails (batch size: {})", batchSize);

        while (true) {
            try {
                // Fetch next batch
                List<Activity> batch = activityService.getEmailsWithAddresses(batchSize, skip);

                if (batch == null || batch.isEmpty()) {
                    log.info("No more emails to fetch. Total fetched: {}", totalFetched);
                    break;
                }

                allEmails.addAll(batch);
                totalFetched += batch.size();
                skip += batchSize;

                log.info("Fetched batch: {} emails (total so far: {})", batch.size(), totalFetched);

                // If batch returned less than batchSize, we've reached the end
                if (batch.size() < batchSize) {
                    log.info("Last batch received. Total emails fetched: {}", totalFetched);
                    break;
                }

            } catch (Exception e) {
                log.error("Error fetching batch at skip={}", skip, e);
                // Continue with what we have so far
                break;
            }
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
    private EmailReminder createReminder(Activity email, int daysOverdue) {
        EmailReminder reminder = new EmailReminder();
        reminder.setActivityId(email.getActivityId());
        reminder.setSubject(email.getSubject() != null ? email.getSubject() : "No subject");
        reminder.setToEmail(email.getToEmail());
        reminder.setAccountId(email.getRegardingObjectId());
        reminder.setAccountName(email.getRegardingObjectName());
        reminder.setStaffName(email.getStaffName());
        reminder.setStaffEmail(email.getStaffEmail());
        reminder.setSentDate(email.getCreatedOn());
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
