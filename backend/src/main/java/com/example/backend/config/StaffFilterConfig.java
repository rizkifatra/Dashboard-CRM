package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for filtering specific staff members to track in the CRM system
 * Based on job titles instead of individual names
 */
@Configuration
public class StaffFilterConfig {

    /**
     * List of job titles to include in the CRM dashboard and reports
     * Staff members with these titles will be tracked for performance metrics
     */
    public static final List<String> TRACKED_JOB_TITLES = Arrays.asList(
            // Sales Team
            "Business Development Executive",
            "Inside Sales Representative (ISR)",

            // Bid & Proposal Team
            "Bid & Proposal Assistant",
            "Bid & Proposal Team Lead",

            // Sales Admin
            "Sales Admin",

            // Sales Coordinator
            "Sales Coordinator (Operation and Reporting)");

    /**
     * Check if a staff member should be tracked based on their job title
     * 
     * @param title The job title of the staff member
     * @return true if the staff member should be tracked
     */
    public static boolean shouldTrackStaff(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }

        // Case-insensitive comparison, also check for partial matches
        String normalizedTitle = title.trim().toLowerCase();
        return TRACKED_JOB_TITLES.stream()
                .anyMatch(trackedTitle -> {
                    String normalizedTrackedTitle = trackedTitle.toLowerCase();
                    // Exact match or contains match
                    return normalizedTitle.equals(normalizedTrackedTitle) ||
                            normalizedTitle.contains(normalizedTrackedTitle) ||
                            normalizedTrackedTitle.contains(normalizedTitle);
                });
    }

    /**
     * Get the list of tracked job titles
     * 
     * @return List of job titles to track
     */
    public static List<String> getTrackedJobTitles() {
        return TRACKED_JOB_TITLES;
    }
}
