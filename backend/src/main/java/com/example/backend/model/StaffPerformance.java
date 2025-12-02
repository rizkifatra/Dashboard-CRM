package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing staff email response performance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformance {

    private String staffId;
    private String staffName;
    private String email;

    // Email metrics
    private Integer totalEmailsSent;
    private Integer totalEmailsReceived;
    private Integer emailResponseCount;

    // Performance metrics
    private Double responseRate; // Percentage of emails responded to
    private Double averageResponseTimeHours;
    private Integer rank;

    // Additional metrics
    private Integer activeThreads;
    private Integer closedConversations;
    private Double customerSatisfactionScore;

    // Time period
    private String periodStart;
    private String periodEnd;
}
