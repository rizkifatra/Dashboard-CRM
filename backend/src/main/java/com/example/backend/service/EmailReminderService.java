package com.example.backend.service;

import com.example.backend.model.Activity;
import com.example.backend.model.EmailReminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${d365.api.internal-domains:@bintara.com.my}")
    private String internalDomainsConfig;

    @Value("${d365.api.internal-shared-mailboxes:}")
    private String internalSharedMailboxes;

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
        // Check cache first (2-minute cache to reduce D365 API calls)
        if (cachedReminders != null && cacheTimestamp != null) {
            long minutesSinceCache = ChronoUnit.MINUTES.between(cacheTimestamp, LocalDateTime.now());
            if (minutesSinceCache < CACHE_DURATION_MINUTES) {
                log.debug("Returning cached email reminders ({} items, cached {} min ago)",
                        cachedReminders.size(), minutesSinceCache);
                return cachedReminders;
            }
        }

        try {
            log.info("==============================================");
            log.info("★★★ STARTING EMAIL REMINDER FETCH ★★★");
            log.info("==============================================");
            log.info("Fetching fresh email follow-up reminders from D365");

            // Fetch ALL emails in batches to scan entire history
            List<Activity> allEmails = fetchAllEmailsInBatches();
            log.info("Found {} total emails across all batches", allEmails.size());

            // FLEXIBLE DETECTION: Use email addresses to detect outgoing/incoming
            // Outgoing: FROM internal (@bintara.com.my) TO external
            // Incoming: FROM external TO internal (@bintara.com.my)
            String[] internalDomains = internalDomainsConfig.split(",");
            log.info("Internal domains for detection: {}", Arrays.toString(internalDomains));

            // Filter outgoing: FROM Bintara staff TO external clients
            List<Activity> outgoingEmails = allEmails.stream()
                    .filter(email -> isOutgoingEmail(email, internalDomains))
                    .collect(Collectors.toList());

            log.info("Found {} outgoing emails (Bintara → External)", outgoingEmails.size());

            // Log state code distribution for debugging
            Map<Integer, Long> stateCodeCounts = outgoingEmails.stream()
                    .collect(Collectors.groupingBy(Activity::getStateCode, Collectors.counting()));
            log.info("Outgoing email state codes: {}", stateCodeCounts);

            // Log sample of outgoing emails for debugging
            if (!outgoingEmails.isEmpty()) {
                int sampleSize = Math.min(5, outgoingEmails.size());
                log.info("Sample outgoing emails (showing first {}):", sampleSize);
                for (int i = 0; i < sampleSize; i++) {
                    Activity email = outgoingEmails.get(i);
                    log.info("  - TO: {} | SUBJECT: '{}' | ACCOUNT: {} | STATE: {}",
                            email.getToEmail(),
                            email.getSubject(),
                            email.getRegardingObjectName() != null ? email.getRegardingObjectName() : "None",
                            email.getStateCode());
                }
            }

            List<Activity> incomingEmails = allEmails.stream()
                    .filter(email -> isIncomingEmail(email, internalDomains))
                    .collect(Collectors.toList());

            log.info("Found {} incoming emails (External → Bintara)", incomingEmails.size());

            // Log sample of incoming emails for debugging
            if (!incomingEmails.isEmpty()) {
                int sampleSize = Math.min(5, incomingEmails.size());
                log.info("Sample incoming emails (showing first {}):", sampleSize);
                for (int i = 0; i < sampleSize; i++) {
                    Activity email = incomingEmails.get(i);
                    log.info("  - FROM: {} | SUBJECT: '{}' | ACCOUNT: {}",
                            email.getFromEmail(),
                            email.getSubject(),
                            email.getRegardingObjectName() != null ? email.getRegardingObjectName() : "None");
                }
            }

            // Process each outgoing email to check if it needs follow-up
            // We'll check dates to ensure replies came AFTER the outgoing email
            List<EmailReminder> reminders = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            log.info("Processing with {} incoming emails available for reply matching", incomingEmails.size());

            int skippedNoRecipient = 0;
            int skippedHasReply = 0;
            int skippedCancelled = 0;
            int skippedTooRecent = 0;
            int skippedOtherType = 0;

            log.info("Processing {} outgoing emails to check for follow-ups needed...", outgoingEmails.size());

            for (Activity email : outgoingEmails) {
                // Skip if no recipient email
                if (email.getToEmail() == null || email.getToEmail().isEmpty()) {
                    skippedNoRecipient++;
                    log.debug("Skipped (no recipient): '{}'", email.getSubject());
                    continue;
                }

                // Skip "OTHER" type emails (not business-related)
                // Only include: RE:, FW:, RFQ, RFP, Quote emails
                if (isOtherTypeEmail(email.getSubject())) {
                    skippedOtherType++;
                    log.debug("Skipped (OTHER type): '{}'", email.getSubject());
                    continue;
                }

                // Skip if email is Cancelled (stateCode = 2) in CRM
                // Note: stateCode 1 (Completed) is normal for sent emails, so we don't skip
                // those
                // Only skip cancelled emails which indicate staff decided not to pursue
                if (email.getStateCode() != null && email.getStateCode() == 2) {
                    skippedCancelled++;
                    log.debug("Skipped (cancelled): '{}' to {}", email.getSubject(), email.getToEmail());
                    continue; // Email cancelled in CRM, no reminder needed
                }

                // Get sent date for this outgoing email (needed for both date comparison and
                // reminder)
                String sentDate = extractSentDateFromDescription(email);
                if (sentDate == null) {
                    sentDate = email.getSentOn() != null ? email.getSentOn()
                            : (email.getActualEnd() != null ? email.getActualEnd() : email.getCreatedOn());
                }

                if (sentDate == null) {
                    log.warn("No sent date available for email: '{}'", email.getSubject());
                    continue;
                }

                // Check if this email has been replied to (AFTER it was sent)
                if (hasReplyAfterDate(email, incomingEmails, sentDate)) {
                    skippedHasReply++;
                    log.info("Skipped (has reply): '{}' to {} [Account: {}]",
                            email.getSubject(),
                            email.getToEmail(),
                            email.getRegardingObjectName() != null ? email.getRegardingObjectName() : "None");
                    continue; // Email has reply, no reminder needed
                }

                int daysOverdue = calculateDaysOverdue(sentDate, now);

                log.debug("Email: '{}' to {} - {} days old (sentDate: {})",
                        email.getSubject(), email.getToEmail(), daysOverdue, sentDate);

                // Show emails that are 3+ days old to match follow-up thresholds
                // Urgency badges: Day 3 = "3 Days", Days 4-7 = "7 Days", Days 8+ = "14 Days"
                if (daysOverdue >= 3) {
                    EmailReminder reminder = createReminder(email, daysOverdue, sentDate);
                    reminders.add(reminder);
                    log.info("✓ Added reminder: '{}' to {} ({} days since sent) [Account: {}] - BADGE: {}",
                            email.getSubject(),
                            email.getToEmail(),
                            daysOverdue,
                            email.getRegardingObjectName() != null ? email.getRegardingObjectName() : "None",
                            reminder.getUrgencyBadge());
                } else {
                    skippedTooRecent++;
                    log.debug("Skipped (too recent - {} days): '{}' to {}", daysOverdue, email.getSubject(),
                            email.getToEmail());
                }
            }

            log.info("========================================");
            log.info("FILTERING SUMMARY:");
            log.info("  Total outgoing emails: {}", outgoingEmails.size());
            log.info("  Skipped (no recipient): {}", skippedNoRecipient);
            log.info("  Skipped (OTHER type - not business): {}", skippedOtherType);
            log.info("  Skipped (cancelled in CRM): {}", skippedCancelled);
            log.info("  Skipped (has reply): {}", skippedHasReply);
            log.info("  Skipped (too recent <3 days): {}", skippedTooRecent);
            log.info("  REMINDERS CREATED: {}", reminders.size());
            log.info("========================================");

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
     * Check if email is outgoing (FROM internal TO external)
     * Flexible detection using multiple strategies:
     * 1. Check direction field if available AND verify sender is internal
     * 2. Check if sender is internal and recipient is external
     * STRICT: Requires FROM to be Bintara staff - no assumptions based on recipient
     * only
     */
    private boolean isOutgoingEmail(Activity email, String[] internalDomains) {
        String fromEmail = email.getFromEmail();
        String toEmail = email.getToEmail();

        // STRICT: Must have both sender and recipient to verify direction
        if (fromEmail == null || fromEmail.isBlank() || toEmail == null || toEmail.isBlank()) {
            log.debug("Skipped (missing from/to): from={}, to={}", fromEmail, toEmail);
            return false;
        }

        boolean fromIsInternal = isInternalEmail(fromEmail, internalDomains);
        boolean toIsExternal = !isInternalEmail(toEmail, internalDomains);

        // STRICT: Only outgoing if FROM Bintara staff AND TO external client
        if (fromIsInternal && toIsExternal) {
            log.debug("Detected outgoing: {} -> {} (Bintara → External)", fromEmail, toEmail);
            return true;
        }

        // Also accept if direction is explicitly "outgoing" AND sender is internal
        if ("outgoing".equalsIgnoreCase(email.getDirection()) && fromIsInternal && toIsExternal) {
            log.debug("Detected outgoing by direction: {} -> {}", fromEmail, toEmail);
            return true;
        }

        return false;
    }

    /**
     * Check if email is incoming (FROM external TO internal)
     */
    private boolean isIncomingEmail(Activity email, String[] internalDomains) {
        // Strategy 1: Use direction field if available
        if ("incoming".equalsIgnoreCase(email.getDirection())) {
            return true;
        }

        // Strategy 2: Check sender and recipient
        // Incoming means: FROM external (client) TO internal (Bintara)
        String fromEmail = email.getFromEmail();
        String toEmail = email.getToEmail();

        if (fromEmail != null && toEmail != null) {
            boolean fromIsExternal = !isInternalEmail(fromEmail, internalDomains);
            boolean toIsInternal = isInternalEmail(toEmail, internalDomains);

            if (fromIsExternal && toIsInternal) {
                log.debug("Detected incoming: {} -> {}", fromEmail, toEmail);
                return true;
            }
        }

        // Strategy 3: If fromEmail is external, assume it's incoming
        if (fromEmail != null && !isInternalEmail(fromEmail, internalDomains)) {
            log.debug("Assuming incoming (from external): from={}", fromEmail);
            return true;
        }

        return false;
    }

    /**
     * Check if email is "OTHER" type (not business-related)
     * Business emails start with RE:, FW: or contain RFQ, RFP, Quote
     * OTHER emails (webinars, marketing, etc.) should be excluded from follow-ups
     */
    private boolean isOtherTypeEmail(String subject) {
        if (subject == null || subject.isBlank()) {
            return true; // No subject = OTHER
        }

        String upperSubject = subject.toUpperCase().trim();

        // Check for standard email prefixes (RE:, FW:)
        if (upperSubject.startsWith("RE:") || upperSubject.startsWith("RE ")) {
            return false; // This is a reply email - include it
        }
        if (upperSubject.startsWith("FW:") || upperSubject.startsWith("FWD:") || upperSubject.startsWith("FW ")) {
            return false; // This is a forwarded email - include it
        }

        // Check for business request types (can be anywhere in subject)
        if (upperSubject.contains("RFQ") || upperSubject.contains("REQUEST FOR QUOTE")) {
            return false; // Request for Quote - include it
        }
        if (upperSubject.contains("RFP") || upperSubject.contains("REQUEST FOR PROPOSAL")) {
            return false; // Request for Proposal - include it
        }
        if (upperSubject.contains("QUOTE") || upperSubject.contains("QUOTATION")) {
            return false; // Quote related - include it
        }

        // Everything else is "OTHER" type - exclude from follow-ups
        return true;
    }

    /**
     * Check if email address is from internal domain
     */
    private boolean isInternalEmail(String email, String[] internalDomains) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String lowerEmail = email.toLowerCase().trim();

        for (String domain : internalDomains) {
            String cleanDomain = domain.trim().toLowerCase();
            if (lowerEmail.endsWith(cleanDomain) || lowerEmail.contains(cleanDomain)) {
                return true;
            }
        }

        return false;
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
        log.info("▶▶▶ ENTERING fetchAllEmailsInBatches ◀◀◀");
        try {
            log.info("Fetching emails with addresses from D365");

            // Fetch 3000 emails to capture more historical data for follow-up detection
            // Increased to ensure we capture emails >= 7 days and >= 14 days old
            // Using lightweight API without description field (not needed for reminders)
            // With 2-min cache, we reduce D365 API calls significantly
            int batchSize = 3000;

            // Calculate cutoff date: go back 2 years to capture historical emails
            java.time.ZonedDateTime cutoffTime = java.time.ZonedDateTime.now().minusYears(2);
            String cutoffDate = cutoffTime.toString().substring(0, 19) + "Z";
            log.info("★ CALLING getEmailsWithAddressesLightweight: batchSize={}, cutoffDate={}", batchSize, cutoffDate);

            List<Activity> emails = activityService.getEmailsWithAddressesLightweight(batchSize, 0, cutoffDate);

            log.info("★ RETURNED: {} emails",
                    emails != null ? emails.size() : "NULL");

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
            log.error("✗✗✗ ERROR in fetchAllEmailsInBatches: {} ✗✗✗", e.getMessage());
            log.error("Full stack trace:", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if an outgoing email has been replied to AFTER it was sent
     * Uses multiple matching strategies with date checking:
     * 1. Email only (primary matching - catches replies even if account differs)
     * 2. Email + Account ID (secondary matching for more precision)
     * 3. Email + Subject (tertiary matching for thread detection)
     * 
     * CRITICAL: Only counts as a reply if the incoming email was received AFTER the
     * outgoing email was sent
     */
    private boolean hasReplyAfterDate(Activity outgoingEmail, List<Activity> incomingEmails, String outgoingSentDate) {
        List<String> outgoingKeys = getEmailKeys(outgoingEmail);
        LocalDateTime outgoingDate = parseDate(outgoingSentDate);

        log.debug("Checking reply for email '{}' to {} (sent: {}) with keys: {}",
                outgoingEmail.getSubject(), outgoingEmail.getToEmail(), outgoingSentDate, outgoingKeys);

        // Check each incoming email to see if it's a reply to this outgoing email
        for (Activity incomingEmail : incomingEmails) {
            if (incomingEmail.getFromEmail() == null || incomingEmail.getFromEmail().isEmpty()) {
                continue;
            }

            String emailAddr = incomingEmail.getFromEmail().toLowerCase().trim();

            // Build keys for this incoming email
            List<String> incomingKeys = new ArrayList<>();

            // Strategy 1: Email address only (most lenient)
            incomingKeys.add(emailAddr);

            // Strategy 2: Email + Account ID (if set)
            if (incomingEmail.getRegardingObjectId() != null) {
                incomingKeys.add(emailAddr + "|" + incomingEmail.getRegardingObjectId());
            }

            // Strategy 3: Email + Normalized Subject (for thread matching)
            if (incomingEmail.getSubject() != null && !incomingEmail.getSubject().isEmpty()) {
                String normalizedSubject = normalizeSubject(incomingEmail.getSubject());
                if (!normalizedSubject.isEmpty()) {
                    incomingKeys.add(emailAddr + "|subj:" + normalizedSubject);
                }
            }

            // Check if any incoming key matches any outgoing key
            boolean keysMatch = false;
            String matchedKey = null;
            for (String inKey : incomingKeys) {
                if (outgoingKeys.contains(inKey)) {
                    keysMatch = true;
                    matchedKey = inKey;
                    break;
                }
            }

            if (!keysMatch) {
                continue; // No key match, not a reply
            }

            // Keys match! Now check if the incoming email was received AFTER the outgoing
            // email was sent
            String incomingDateStr = incomingEmail.getSentOn() != null ? incomingEmail.getSentOn()
                    : (incomingEmail.getActualEnd() != null ? incomingEmail.getActualEnd()
                            : incomingEmail.getCreatedOn());

            if (incomingDateStr == null) {
                log.debug("Incoming email '{}' has no date, skipping", incomingEmail.getSubject());
                continue;
            }

            LocalDateTime incomingDate = parseDate(incomingDateStr);

            // Reply must be AFTER the outgoing email
            if (incomingDate.isAfter(outgoingDate)) {
                log.info("✓ Found reply match for '{}' using key: {} (outgoing: {}, incoming: {})",
                        outgoingEmail.getSubject(), matchedKey, outgoingSentDate, incomingDateStr);
                return true;
            } else {
                log.debug("✗ Key matched but date is before/equal: incoming '{}' ({}) is not after outgoing ({})",
                        incomingEmail.getSubject(), incomingDateStr, outgoingSentDate);
            }
        }

        log.debug("✗ No reply found for '{}' to {} (tried {} keys, checked {} incoming emails)",
                outgoingEmail.getSubject(), outgoingEmail.getToEmail(), outgoingKeys.size(), incomingEmails.size());
        return false;
    }

    /**
     * Get unique keys for email matching using multiple strategies
     * Checks email alone, email+account, and email+subject
     * Returns multiple keys to try for maximum match accuracy
     */
    private List<String> getEmailKeys(Activity email) {
        List<String> keys = new ArrayList<>();
        String emailAddr = email.getToEmail().toLowerCase().trim();

        // Strategy 1: Email address only (most lenient)
        keys.add(emailAddr);

        // Strategy 2: Email + Account ID (if set)
        if (email.getRegardingObjectId() != null) {
            keys.add(emailAddr + "|" + email.getRegardingObjectId());
        }

        // Strategy 3: Email + Normalized Subject (for thread matching)
        if (email.getSubject() != null && !email.getSubject().isEmpty()) {
            String normalizedSubject = normalizeSubject(email.getSubject());
            if (!normalizedSubject.isEmpty()) {
                keys.add(emailAddr + "|subj:" + normalizedSubject);
            }
        }

        return keys;
    }

    /**
     * Normalize email subject for thread matching
     * Removes RE:, FW:, FWD: prefixes and extra whitespace
     */
    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "";
        }

        return subject.trim()
                .replaceAll("(?i)^(RE:|FW:|FWD:)\\s*", "")
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();
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

        // Set urgency level based on days since sent
        // Follow-up milestones: 3, 7, and 14 days
        // 14+ days: critical (red) - needs immediate attention
        // 7-13 days: medium (orange) - overdue for follow-up
        // 3-6 days: low (yellow) - time to follow up
        if (daysOverdue >= 14) {
            // 14+ days without reply - critical urgency
            reminder.setUrgencyLevel("critical");
            reminder.setUrgencyBadge("14 Days");
        } else if (daysOverdue >= 7) {
            // 7-13 days without reply - medium urgency
            reminder.setUrgencyLevel("medium");
            reminder.setUrgencyBadge("7 Days");
        } else {
            // 3-6 days without reply - low urgency
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
