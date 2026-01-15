package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonAlias("opportunityid")
    private String opportunityId;

    @JsonAlias("name")
    private String name;

    @JsonAlias("description")
    private String description;

    @JsonAlias("estimatedvalue")
    private BigDecimal estimatedValue;

    @JsonAlias("estimatedclosedate")
    private String estimatedCloseDate;

    @JsonAlias("actualvalue")
    private BigDecimal actualValue;

    @JsonAlias("actualclosedate")
    private String actualCloseDate;

    @JsonAlias("budgetamount")
    private BigDecimal budgetAmount;

    @JsonAlias("closeprobability")
    private Integer closeProbability;

    @JsonAlias("salesstage")
    private Integer salesStage;

    @JsonAlias("stepname")
    private String stepName;

    @JsonAlias("createdon")
    private String createdOn;

    @JsonAlias("modifiedon")
    private String modifiedOn;

    // State: 0 = Open, 1 = Won, 2 = Lost
    @JsonAlias("statecode")
    private Integer stateCode;

    // Status: 1 = In Progress, 2 = On Hold, 3 = Won, 4 = Cancelled, 5 = Out-Sold
    @JsonAlias("statuscode")
    private Integer statusCode;

    // Owner/Staff information
    @JsonAlias("_ownerid_value")
    private String ownerId;

    @JsonAlias("_ownerid_value@OData.Community.Display.V1.FormattedValue")
    private String ownerName;

    // Expanded owner entity (from $expand=ownerid_systemuser) - using type-specific
    // expansion
    @JsonProperty("ownerid_systemuser")
    @JsonIgnoreProperties(ignoreUnknown = true)
    private OwnerDetails ownerDetails;

    @JsonAlias("_createdby_value")
    private String createdById;

    @JsonAlias("_modifiedby_value")
    private String modifiedById;

    // Related Account (Customer) - can be account or contact
    @JsonAlias("_customerid_value")
    private String customerId;

    @JsonAlias("_parentaccountid_value")
    private String accountId;

    // Expanded customer account entity (from $expand=customerid_account)
    @JsonProperty("customerid_account")
    @JsonIgnoreProperties(ignoreUnknown = true)
    private AccountDetails customerAccount;

    // Expanded customer contact entity (from $expand=customerid_contact)
    @JsonProperty("customerid_contact")
    @JsonIgnoreProperties(ignoreUnknown = true)
    private ContactDetails customerContact;

    // Expanded parent account entity (from $expand=parentaccountid)
    @JsonProperty("parentaccountid")
    @JsonIgnoreProperties(ignoreUnknown = true)
    private AccountDetails parentAccount;

    // OData metadata
    @JsonAlias({ "@odata.etag", "odata.etag" })
    private String etag;

    // Nested classes for expanded entities
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OwnerDetails {
        @JsonAlias("fullname")
        private String fullname;

        @JsonAlias("systemuserid")
        private String systemuserid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountDetails {
        @JsonAlias("name")
        private String name;

        @JsonAlias("accountid")
        private String accountid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactDetails {
        @JsonAlias("fullname")
        private String fullname;

        @JsonAlias("contactid")
        private String contactid;
    }
}
