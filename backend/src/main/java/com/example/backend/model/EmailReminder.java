package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Email Reminder model for tracking emails that need follow-up
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailReminder {

    @JsonProperty("activityId")
    private String activityId;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("toEmail")
    private String toEmail;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("staffName")
    private String staffName;

    @JsonProperty("staffEmail")
    private String staffEmail;

    @JsonProperty("sentDate")
    private String sentDate;

    @JsonProperty("daysOverdue")
    private int daysOverdue;

    @JsonProperty("urgencyLevel")
    private String urgencyLevel; // "low", "medium", "critical"

    @JsonProperty("urgencyBadge")
    private String urgencyBadge; // "3 Days", "7 Days", "14+ Days"
}
