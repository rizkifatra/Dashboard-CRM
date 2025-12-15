package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonProperty("systemUserId")
    @JsonAlias("systemuserid")
    private String systemUserId;

    @JsonProperty("fullName")
    @JsonAlias("fullname")
    private String fullName;

    @JsonProperty("email")
    @JsonAlias("internalemailaddress")
    private String email;

    @JsonProperty("domainName")
    @JsonAlias("domainname")
    private String domainName;

    @JsonProperty("title")
    @JsonAlias("title")
    private String title;

    @JsonProperty("mobilePhone")
    @JsonAlias("mobilephone")
    private String mobilePhone;

    @JsonProperty("telephone")
    @JsonAlias("address1_telephone1")
    private String telephone;

    @JsonProperty("createdOn")
    @JsonAlias("createdon")
    private String createdOn;

    @JsonProperty("modifiedOn")
    @JsonAlias("modifiedon")
    private String modifiedOn;

    // Email statistics for KPI calculation
    @JsonProperty("incomingEmailCount")
    private Integer incomingEmailCount;

    @JsonProperty("outgoingEmailCount")
    private Integer outgoingEmailCount;

    @JsonProperty("totalEmailCount")
    private Integer totalEmailCount;

    // Conversation thread statistics
    @JsonProperty("totalConversations")
    private Integer totalConversations; // Number of unique email threads

    @JsonProperty("averageEmailsPerConversation")
    private Double averageEmailsPerConversation; // Avg emails per thread

    // Email responsiveness metrics (in minutes, calculated during working hours
    // Mon-Thu 09:00-17:00)
    @JsonProperty("averageResponseTimeMinutes")
    private Double averageResponseTimeMinutes;

    @JsonProperty("fastestResponseTimeMinutes")
    private Double fastestResponseTimeMinutes;

    @JsonProperty("slowestResponseTimeMinutes")
    private Double slowestResponseTimeMinutes;

    @JsonProperty("respondedEmailCount")
    private Integer respondedEmailCount; // Number of emails that received responses

    // OData metadata
    @JsonProperty("@odata.context")
    private String odataContext;

    @JsonProperty("@odata.etag")
    private String odataEtag;
}
