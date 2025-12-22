package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing an unreplied incoming email with reminder metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnrepliedEmail {

    private String activityId;
    private String subject;
    private String fromEmail;
    private String sender;
    private String description;
    private String assignedTo; // Staff email
    private String assignedToName; // Staff name
    private String createdOn;
    private String modifiedOn;

    // Reminder metadata
    private Long hoursUnreplied; // How many hours since received
    private String urgencyLevel; // "low", "medium", "high", "critical"
    private String ageCategory; // "< 24h", "24-48h", "48-72h", "> 72h"
    private Boolean isOverdue; // Over 48 hours

    // Related account info
    private String regardingObjectId;
    private String regardingObjectName;
    private String regardingObjectTypeCode;
}
