package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing an Activity (Email, Call, Meeting, Task) from Dynamics 365
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @JsonAlias("activityid")
    private String activityId;

    @JsonAlias("subject")
    private String subject;

    @JsonAlias("description")
    private String description;

    @JsonAlias("activitytypecode")
    private String activityTypeCode;

    @JsonAlias("directioncode")
    private Boolean directionCode; // true = outgoing/sent, false = incoming/received

    @JsonAlias("statecode")
    private Integer stateCode; // 0 = Open, 1 = Completed, 2 = Cancelled

    @JsonAlias("statuscode")
    private Integer statusCode;

    // Owner (Staff member who owns this activity)
    @JsonAlias("_owninguser_value")
    private String owningUserId;

    @JsonAlias("_owninguser_value@OData.Community.Display.V1.FormattedValue")
    private String owningUserName;

    // Regarding (Account/Client this activity is related to)
    @JsonAlias("_regardingobjectid_value")
    private String regardingObjectId;

    @JsonAlias("_regardingobjectid_value@OData.Community.Display.V1.FormattedValue")
    private String regardingObjectName;

    @JsonAlias("regardingobjecttypecode")
    private String regardingObjectTypeCode;

    // Sender (for emails)
    @JsonAlias("sender")
    private String sender;

    @JsonAlias("from")
    private String fromEmail;

    @JsonAlias("to")
    private String toEmail;

    @JsonAlias("cc")
    private String ccEmail;

    // Timestamps
    @JsonAlias("createdon")
    private String createdOn;

    @JsonAlias("modifiedon")
    private String modifiedOn;

    @JsonAlias("actualstart")
    private String actualStart;

    @JsonAlias("actualend")
    private String actualEnd;

    @JsonAlias("scheduledstart")
    private String scheduledStart;

    @JsonAlias("scheduledend")
    private String scheduledEnd;

    // Duration in minutes
    @JsonAlias("actualdurationminutes")
    private Integer actualDurationMinutes;

    @JsonAlias("scheduleddurationminutes")
    private Integer scheduledDurationMinutes;

    // Priority
    @JsonAlias("prioritycode")
    private Integer priorityCode;

    @JsonAlias("prioritycode@OData.Community.Display.V1.FormattedValue")
    private String priorityCodeFormatted;

    // Email Activity Parties (participants in the email: To, From, Cc, Bcc)
    @JsonAlias("email_activity_parties")
    private List<EmailActivityParty> emailActivityParties;

    // Additional fields for frontend (not from D365, enriched by backend)
    private String activityType; // Friendly name: "Email", "Phone Call", "Meeting", "Task"
    private String direction; // "incoming" or "outgoing"
    private String staffName; // Owner's full name
    private String staffEmail; // Owner's email
    private String staffTitle; // Owner's job title

    // OData metadata
    @JsonProperty("@odata.context")
    private String odataContext;

    @JsonProperty("@odata.etag")
    private String odataEtag;
}
