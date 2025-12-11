package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Model class representing a Dynamics 365 Opportunity entity
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Opportunity {

    @JsonProperty("opportunityid")
    private String opportunityId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("estimatedvalue")
    private BigDecimal estimatedValue;

    @JsonProperty("estimatedclosedate")
    private String estimatedCloseDate;

    @JsonProperty("actualvalue")
    private BigDecimal actualValue;

    @JsonProperty("actualclosedate")
    private String actualCloseDate;

    @JsonProperty("closeprobability")
    private Integer closeProbability;

    @JsonProperty("salesstage")
    private Integer salesStage;

    @JsonProperty("stepname")
    private String stepName;

    @JsonProperty("createdon")
    private String createdOn;

    @JsonProperty("modifiedon")
    private String modifiedOn;

    // State: 0 = Open, 1 = Won, 2 = Lost
    @JsonProperty("statecode")
    private Integer stateCode;

    // Status: 1 = In Progress, 2 = On Hold, 3 = Won, 4 = Cancelled, 5 = Out-Sold
    @JsonProperty("statuscode")
    private Integer statusCode;

    // Owner/Staff information
    @JsonProperty("_ownerid_value")
    private String ownerId;

    @JsonProperty("_createdby_value")
    private String createdById;

    @JsonProperty("_modifiedby_value")
    private String modifiedById;

    // Related Account (Customer)
    @JsonProperty("_customerid_value")
    private String customerId;

    @JsonProperty("_accountid_value")
    private String accountId;

    // OData metadata
    @JsonProperty("@odata.etag")
    private String etag;
}
