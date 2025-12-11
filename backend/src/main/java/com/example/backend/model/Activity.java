package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing an Activity (Email, Call, Meeting, Task) from Dynamics 365
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @JsonProperty("activityid")
    private String activityId;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("description")
    private String description;

    @JsonProperty("activitytypecode")
    private String activityTypeCode;

    @JsonProperty("directioncode")
    private Boolean directionCode; // true = outgoing/sent, false = incoming/received

    @JsonProperty("statecode")
    private Integer stateCode; // 0 = Open, 1 = Completed, 2 = Cancelled

    @JsonProperty("statuscode")
    private Integer statusCode;

    // Owner (Staff member who owns this activity)
    @JsonProperty("_owninguser_value")
    private String owningUserId;

    @JsonProperty("_owninguser_value@OData.Community.Display.V1.FormattedValue")
    private String owningUserName;

    // Regarding (Account/Client this activity is related to)
    @JsonProperty("_regardingobjectid_value")
    private String regardingObjectId;

    @JsonProperty("_regardingobjectid_value@OData.Community.Display.V1.FormattedValue")
    private String regardingObjectName;

    @JsonProperty("regardingobjecttypecode")
    private String regardingObjectTypeCode;

    // Sender (for emails)
    @JsonProperty("sender")
    private String sender;

    @JsonProperty("from")
    private String fromEmail;

    @JsonProperty("to")
    private String toEmail;

    @JsonProperty("cc")
    private String ccEmail;

    // Timestamps
    @JsonProperty("createdon")
    private String createdOn;

    @JsonProperty("modifiedon")
    private String modifiedOn;

    @JsonProperty("actualstart")
    private String actualStart;

    @JsonProperty("actualend")
    private String actualEnd;

    @JsonProperty("scheduledstart")
    private String scheduledStart;

    @JsonProperty("scheduledend")
    private String scheduledEnd;

    // Duration in minutes
    @JsonProperty("actualdurationminutes")
    private Integer actualDurationMinutes;

    @JsonProperty("scheduleddurationminutes")
    private Integer scheduledDurationMinutes;

    // Priority
    @JsonProperty("prioritycode")
    private Integer priorityCode;

    @JsonProperty("prioritycode@OData.Community.Display.V1.FormattedValue")
    private String priorityCodeFormatted;

    // OData metadata
    @JsonProperty("@odata.context")
    private String odataContext;

    @JsonProperty("@odata.etag")
    private String odataEtag;
}
