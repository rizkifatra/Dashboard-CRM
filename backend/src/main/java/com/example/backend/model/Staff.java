package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing staff/system user from Dynamics 365
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

    @JsonProperty("systemuserid")
    private String systemUserId;

    @JsonProperty("fullname")
    private String fullName;

    @JsonProperty("internalemailaddress")
    private String email;

    @JsonProperty("domainname")
    private String domainName;

    @JsonProperty("title")
    private String title;

    @JsonProperty("mobilephone")
    private String mobilePhone;

    @JsonProperty("address1_telephone1")
    private String telephone;

    @JsonProperty("createdon")
    private String createdOn;

    @JsonProperty("modifiedon")
    private String modifiedOn;

    // Email statistics for KPI calculation
    private Integer incomingEmailCount;
    private Integer outgoingEmailCount;
    private Integer totalEmailCount;

    // Email responsiveness metrics (in minutes)
    private Double averageResponseTimeMinutes;
    private Double fastestResponseTimeMinutes;
    private Double slowestResponseTimeMinutes;
    private Integer respondedEmailCount; // Number of emails that received responses

    // OData metadata
    @JsonProperty("@odata.context")
    private String odataContext;

    @JsonProperty("@odata.etag")
    private String odataEtag;
}
