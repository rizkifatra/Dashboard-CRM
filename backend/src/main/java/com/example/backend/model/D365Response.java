package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Generic wrapper for Dynamics 365 API responses
 * 
 * @param <T> The type of data contained in the response
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class D365Response<T> {

    @JsonProperty("@odata.context")
    private String odataContext;

    @JsonProperty("@odata.count")
    private Integer count;

    @JsonProperty("@odata.nextLink")
    private String nextLink;

    @JsonProperty("value")
    private List<T> value;
}
