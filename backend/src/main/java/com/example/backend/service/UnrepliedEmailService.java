package com.example.backend.service;

import com.example.backend.config.D365Config;
import com.example.backend.model.Activity;
import com.example.backend.model.D365Response;
import com.example.backend.model.EmailActivityParty;
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
                        .maxInMemorySize(100 * 1024 * 1024)) // 100MB buffer size for large email responses with
                                                             // email_activity_parties
                .build();
    }

    /**
     * Get unreplied emails for Bintara staff
     * An email is considered unreplied if:
     * 1. It was received by Bintara staff (incoming from external clients)
     * 2. Sent TO @bintara.com.my addresses
     * 3. FROM external (non-@bintara.com.my) addresses
     * 4. No outgoing reply from Bintara staff has been sent
     */
    public List<UnrepliedEmail> getUnrepliedEmails(Integer maxHoursOld) {
        try {
            log.info("=== Starting Unreplied Email Detection ===");
            log.info("Time window: {} hours (null = all time)", maxHoursOld);

            // Calculate cutoff time - if null, go back 1 year to get all emails
            ZonedDateTime cutoffTime = maxHoursOld != null
                    ? ZonedDateTime.now().minusHours(maxHoursOld)
                    : ZonedDateTime.now().minusYears(1); // Go back 1 year for "all time"
            log.info("Cutoff time: {} ({})", cutoffTime, maxHoursOld == null ? "ALL TIME" : maxHoursOld + " hours");

            // Get access token
            String token = authService.getAccessToken();

            // Fetch emails with more fields including from/to for direction detection
            String dateFilter = String.format("createdon ge %s", cutoffTime.toString().substring(0, 19) + "Z");
            String uri = "/emails?$select=activityid,subject,directioncode,createdon,modifiedon,description," +
                    "statecode,statuscode,sender&" +
                    "$expand=email_activity_parties($select=participationtypemask,addressused)&" +
                    "$filter=" + dateFilter + "&$top=500&$orderby=createdon desc";

            log.debug("Fetching emails from: {}", uri);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(60000))
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

            // Separate incoming and outgoing emails
            List<Activity> incomingEmails = new ArrayList<>();
            List<Activity> outgoingEmails = new ArrayList<>();

            for (Activity email : allEmails) {
                if (isIncomingEmail(email)) {
                    incomingEmails.add(email);
                } else {
                    outgoingEmails.add(email);
                }
            }

            log.info("Separated: {} incoming, {} outgoing emails", incomingEmails.size(), outgoingEmails.size());

            // Group outgoing emails by normalized subject for quick lookup
            Map<String, List<Activity>> outgoingBySubject = outgoingEmails.stream()
                    .filter(email -> email.getSubject() != null && !email.getSubject().trim().isEmpty())
                    .collect(Collectors.groupingBy(email -> normalizeSubject(email.getSubject())));

            List<UnrepliedEmail> unrepliedEmails = new ArrayList<>();
            int repliedCount = 0;
            int noSubjectCount = 0;

            // Check each incoming email for replies
            for (Activity incomingEmail : incomingEmails) {
                String subject = incomingEmail.getSubject();

                if (subject == null || subject.trim().isEmpty()) {
                    noSubjectCount++;
                    continue;
                }

                String normalizedSubject = normalizeSubject(subject);
                ZonedDateTime incomingTime = ZonedDateTime.parse(incomingEmail.getCreatedOn());

                // Check if there's an outgoing reply sent AFTER this incoming email
                List<Activity> possibleReplies = outgoingBySubject.getOrDefault(normalizedSubject, new ArrayList<>());

                boolean hasReply = possibleReplies.stream()
                        .anyMatch(reply -> {
                            try {
                                ZonedDateTime replyTime = ZonedDateTime.parse(reply.getCreatedOn());
                                // Reply must be sent AFTER the incoming email
                                return replyTime.isAfter(incomingTime);
                            } catch (Exception e) {
                                return false;
                            }
                        });

                if (!hasReply) {
                    log.info("✗ UNREPLIED: '{}' (from: {}, created: {})",
                            subject, incomingEmail.getSender(), incomingEmail.getCreatedOn());

                    // Calculate time-based metadata
                    long hoursUnreplied = calculateHoursUnreplied(incomingEmail.getCreatedOn());
                    String urgencyLevel = determineUrgencyLevel(hoursUnreplied);
                    String ageCategory = determineAgeCategory(hoursUnreplied);
                    boolean isOverdue = hoursUnreplied > 48;

                    // Extract Bintara recipient staff from email parties (participationTypeMask =
                    // 2)
                    String recipientStaffEmail = extractBintaraRecipient(incomingEmail);
                    String recipientStaffName = recipientStaffEmail != null ? extractNameFromEmail(recipientStaffEmail)
                            : null;

                    UnrepliedEmail unreplied = UnrepliedEmail.builder()
                            .activityId(incomingEmail.getActivityId())
                            .subject(subject)
                            .fromEmail(
                                    incomingEmail.getSender() != null ? incomingEmail.getSender() : "External Client")
                            .sender(incomingEmail.getSender() != null ? incomingEmail.getSender() : "External Client")
                            .description(incomingEmail.getDescription())
                            .assignedTo(recipientStaffEmail)
                            .assignedToName(recipientStaffName)
                            .createdOn(incomingEmail.getCreatedOn())
                            .modifiedOn(incomingEmail.getModifiedOn())
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
                    log.debug("✓ REPLIED: '{}' (reply sent after incoming)", subject);
                }
            }

            log.info("=== Detection Complete ===");
            log.info("Incoming: {}, Replied: {}, Unreplied: {}, No subject: {}",
                    incomingEmails.size(), repliedCount, unrepliedEmails.size(), noSubjectCount);

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
     * Determine if email is incoming (received from external clients)
     * Uses participationTypeMask from email_activity_parties:
     * - participationTypeMask = 1: From/Sender
     * - participationTypeMask = 2: To/Recipient
     * 
     * An email is INCOMING if:
     * 1. Has external company email in FROM (typeMask=1, NOT @bintara.com.my, NOT
     * personal email)
     * 2. Has @bintara.com.my in TO (typeMask=2)
     * 
     * Personal email domains (Gmail, Yahoo, Hotmail, etc.) are excluded.
     * 
     * FALLBACK: If emailActivityParties is missing, check fromEmail/toEmail fields
     */
    private boolean isIncomingEmail(Activity email) {
        // FALLBACK LOGIC: If emailActivityParties is missing, use fromEmail/toEmail
        // fields
        if (email.getEmailActivityParties() == null || email.getEmailActivityParties().isEmpty()) {
            log.warn("No email_activity_parties found for email '{}' ({}), using fallback fromEmail/toEmail detection",
                    email.getSubject(), email.getActivityId());

            String fromEmail = email.getFromEmail();
            String toEmail = email.getToEmail();

            // If we have fromEmail/toEmail data, use it
            if (fromEmail != null && toEmail != null) {
                // Incoming: FROM is external company (not Bintara, not personal) AND TO
                // contains @bintara.com.my
                boolean hasExternalCompanyFrom = fromEmail != null
                        && !fromEmail.toLowerCase().contains("@bintara.com.my")
                        && !isPersonalEmailDomain(fromEmail);
                boolean hasBintaraTo = toEmail != null && toEmail.toLowerCase().contains("@bintara.com.my");

                boolean isIncoming = hasExternalCompanyFrom && hasBintaraTo;

                if (isIncoming) {
                    log.info("✓ FALLBACK INCOMING: '{}' | FROM: {} (external company) | TO: {} (Bintara)",
                            email.getSubject() != null ? email.getSubject() : "No Subject",
                            fromEmail, toEmail);
                } else {
                    log.debug("✗ FALLBACK OUTGOING: '{}' | FROM: {} | TO: {} | Reason: {} {}",
                            email.getSubject() != null ? email.getSubject() : "No Subject",
                            fromEmail, toEmail,
                            !hasExternalCompanyFrom ? "FROM is Bintara/personal/null" : "",
                            !hasBintaraTo ? "TO is external or null" : "");
                }

                return isIncoming;
            }

            // No data at all - default to outgoing (don't show in unreplied list)
            log.debug("✗ FALLBACK OUTGOING: '{}' | No participant or direction data available",
                    email.getSubject() != null ? email.getSubject() : "No Subject");
            return false;
        }

        // Analyze all participants to determine email direction
        boolean hasBintaraInTO = false;
        boolean hasExternalCompanyInFROM = false;
        String bintaraRecipient = null;
        String externalSender = null;

        for (EmailActivityParty party : email.getEmailActivityParties()) {
            Integer participationType = party.getParticipationTypeMask();
            String address = party.getAddressUsed();

            if (address == null || address.isEmpty() || participationType == null) {
                continue;
            }

            String lowerAddress = address.toLowerCase();

            // Check for Bintara recipient (TO = participationType 2)
            if (participationType == 2 && lowerAddress.contains("@bintara.com.my")) {
                hasBintaraInTO = true;
                if (bintaraRecipient == null) {
                    bintaraRecipient = address;
                }
                log.debug("Found Bintara in TO: {} (participationType=2)", address);
            }

            // Check for external company sender (FROM = participationType 1)
            if (participationType == 1) {
                boolean isExternal = !lowerAddress.contains("@bintara.com.my");
                boolean isCompany = !isPersonalEmailDomain(address);

                if (isExternal && isCompany) {
                    hasExternalCompanyInFROM = true;
                    if (externalSender == null) {
                        externalSender = address;
                    }
                    log.debug("Found external company in FROM: {} (participationType=1)", address);
                }
            }
        }

        // Email is INCOMING only if it has BOTH:
        // 1. External company sender in FROM (participationType=1)
        // 2. Bintara recipient in TO (participationType=2)
        boolean isIncoming = hasBintaraInTO && hasExternalCompanyInFROM;

        // Enhanced logging with subject and classification reason
        String subject = email.getSubject() != null ? email.getSubject() : "No Subject";
        if (isIncoming) {
            log.info("✓ INCOMING: '{}' | FROM: {} → TO: {} | ActivityID: {}",
                    subject.length() > 50 ? subject.substring(0, 50) + "..." : subject,
                    externalSender != null ? externalSender : "unknown",
                    bintaraRecipient != null ? bintaraRecipient : "unknown",
                    email.getActivityId());
        } else {
            String reason = "";
            if (!hasBintaraInTO) {
                reason = "No @bintara in TO (participationType=2)";
            } else if (!hasExternalCompanyInFROM) {
                reason = "No external company in FROM (participationType=1) - may be Bintara sender or personal email";
            }
            log.debug("✗ OUTGOING/FILTERED: '{}' | ActivityID: {} | Reason: {}",
                    subject.length() > 50 ? subject.substring(0, 50) + "..." : subject,
                    email.getActivityId(),
                    reason);
        }

        return isIncoming;
    }

    /**
     * Check if an email address is from a personal email provider
     * (Gmail, Yahoo, Hotmail, etc.)
     */
    private boolean isPersonalEmailDomain(String email) {
        if (email == null) {
            return false;
        }

        String lowerEmail = email.toLowerCase();

        // Common personal email domains
        String[] personalDomains = {
                "@gmail.com", "@yahoo.com", "@hotmail.com", "@outlook.com",
                "@live.com", "@icloud.com", "@me.com", "@mac.com",
                "@aol.com", "@protonmail.com", "@mail.com", "@zoho.com",
                "@yandex.com", "@gmx.com", "@fastmail.com",
                "@qq.com", "@163.com", "@126.com", "@sina.com"
        };

        for (String domain : personalDomains) {
            if (lowerEmail.contains(domain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract the Bintara staff email who received this email
     * Looks for participationTypeMask = 2 (To/Recipient) with @bintara.com.my
     * FALLBACK: Uses toEmail field or staffEmail if emailActivityParties is missing
     */
    private String extractBintaraRecipient(Activity email) {
        // FALLBACK: Use toEmail or staffEmail if emailActivityParties is missing
        if (email.getEmailActivityParties() == null || email.getEmailActivityParties().isEmpty()) {
            // Try toEmail first
            String toEmail = email.getToEmail();
            if (toEmail != null && toEmail.toLowerCase().contains("@bintara.com.my")) {
                // Extract first @bintara.com.my address from comma-separated list
                String[] toAddresses = toEmail.split("[;,]");
                for (String addr : toAddresses) {
                    String trimmed = addr.trim().toLowerCase();
                    if (trimmed.contains("@bintara.com.my")) {
                        log.debug("Extracted Bintara recipient from toEmail fallback: {}", trimmed);
                        return trimmed;
                    }
                }
            }

            // If toEmail doesn't work, use staffEmail (owner) as last resort
            String staffEmail = email.getStaffEmail();
            if (staffEmail != null && staffEmail.toLowerCase().contains("@bintara.com.my")) {
                log.debug("Extracted Bintara recipient from staffEmail (owner) fallback: {}", staffEmail);
                return staffEmail;
            }

            return null;
        }

        return email.getEmailActivityParties().stream()
                .filter(party -> {
                    Integer typeMask = party.getParticipationTypeMask();
                    String address = party.getAddressUsed();
                    return typeMask != null && typeMask == 2 &&
                            address != null && address.toLowerCase().contains("@bintara.com.my");
                })
                .map(party -> party.getAddressUsed())
                .findFirst()
                .orElse(null);
    }

    /**
     * Extract a friendly name from email address
     * Converts "bunga@bintara.com.my" to "Bunga"
     * Converts "faiz.muhammad@bintara.com.my" to "Faiz Muhammad"
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        try {
            // Get the part before @
            String localPart = email.substring(0, email.indexOf("@"));

            // Replace dots and underscores with spaces
            String name = localPart.replace(".", " ").replace("_", " ");

            // Capitalize each word
            String[] words = name.split("\\s+");
            StringBuilder formattedName = new StringBuilder();

            for (String word : words) {
                if (!word.isEmpty()) {
                    if (formattedName.length() > 0) {
                        formattedName.append(" ");
                    }
                    formattedName.append(word.substring(0, 1).toUpperCase())
                            .append(word.substring(1).toLowerCase());
                }
            }

            return formattedName.toString();
        } catch (Exception e) {
            log.warn("Error extracting name from email: {}", email, e);
            return null;
        }
    }
}
