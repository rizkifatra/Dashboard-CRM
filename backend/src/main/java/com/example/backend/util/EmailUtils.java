package com.example.backend.util;

/**
 * Email utility methods for common email operations
 * Centralizes email-related helper functions to avoid code duplication
 */
public class EmailUtils {

    private EmailUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Normalize email subject for matching
     * Removes RE:, FW:, FWD: prefixes and normalizes whitespace
     * 
     * @param subject The email subject to normalize
     * @return Normalized subject in lowercase with prefixes removed
     */
    public static String normalizeSubject(String subject) {
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
     * Check if an email address belongs to an internal domain
     * 
     * @param email           Email address to check
     * @param internalDomains Array of internal domains (e.g., "@bintara.com.my")
     * @return true if email is from internal domain
     */
    public static boolean isInternalEmail(String email, String[] internalDomains) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String lowerEmail = email.toLowerCase().trim();

        for (String domain : internalDomains) {
            if (lowerEmail.endsWith(domain.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract domain from email address
     * 
     * @param email Email address
     * @return Domain part (e.g., "@example.com") or empty string if invalid
     */
    public static String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }

        int atIndex = email.lastIndexOf('@');
        return email.substring(atIndex).toLowerCase();
    }

    /**
     * Check if email subject indicates a reply
     * 
     * @param subject Email subject
     * @return true if subject starts with RE:
     */
    public static boolean isReply(String subject) {
        if (subject == null || subject.isBlank()) {
            return false;
        }
        return subject.trim().toUpperCase().startsWith("RE:");
    }

    /**
     * Check if email subject indicates a forward
     * 
     * @param subject Email subject
     * @return true if subject starts with FW: or FWD:
     */
    public static boolean isForward(String subject) {
        if (subject == null || subject.isBlank()) {
            return false;
        }
        String upper = subject.trim().toUpperCase();
        return upper.startsWith("FW:") || upper.startsWith("FWD:");
    }

    /**
     * Get clean email subject without prefixes
     * 
     * @param subject Original subject
     * @return Subject with all RE:/FW:/FWD: prefixes removed
     */
    public static String getCleanSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "";
        }

        String cleaned = subject.trim();

        // Remove multiple prefixes (e.g., "RE: FW: RE: Original Subject")
        while (cleaned.matches("(?i)^(RE:|FW:|FWD:)\\s*.*")) {
            cleaned = cleaned.replaceAll("(?i)^(RE:|FW:|FWD:)\\s*", "").trim();
        }

        return cleaned;
    }
}
