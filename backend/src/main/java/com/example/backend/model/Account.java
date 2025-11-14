package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Model class representing a Dynamics 365 Account entity
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {

    @JsonProperty("accountid")
    private String accountId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("accountnumber")
    private String accountNumber;

    @JsonProperty("emailaddress1")
    private String emailAddress;

    @JsonProperty("telephone1")
    private String telephone;

    @JsonProperty("websiteurl")
    private String websiteUrl;

    @JsonProperty("address1_city")
    private String city;

    @JsonProperty("address1_stateorprovince")
    private String state;

    @JsonProperty("address1_country")
    private String country;

    @JsonProperty("address1_postalcode")
    private String postalCode;

    @JsonProperty("address1_line1")
    private String addressLine1;

    @JsonProperty("revenue")
    private BigDecimal revenue;

    @JsonProperty("numberofemployees")
    private Integer numberOfEmployees;

    @JsonProperty("industrycode")
    private Integer industryCode;

    @JsonProperty("description")
    private String description;

    @JsonProperty("createdon")
    private String createdOn;

    @JsonProperty("modifiedon")
    private String modifiedOn;

    @JsonProperty("statecode")
    private Integer stateCode;

    @JsonProperty("statuscode")
    private Integer statusCode;

    // OData metadata
    @JsonProperty("@odata.etag")
    private String etag;
}
