package com.example.backend.service;

import com.example.backend.config.D365Config;
import com.example.backend.model.D365Response;
import com.example.backend.model.Opportunity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Service for interacting with Dynamics 365 Opportunity entity
 */
@Service
@Slf4j
public class D365OpportunityService {

    private final D365Config d365Config;
    private final D365AuthService authService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public D365OpportunityService(D365Config d365Config, D365AuthService authService) {
        this.d365Config = d365Config;
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl(d365Config.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .defaultHeader("OData-MaxVersion", "4.0")
                .defaultHeader("OData-Version", "4.0")
                .defaultHeader("Prefer", "return=representation")
                .build();
    }

    /**
     * Get count of active (open) opportunities
     * 
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     * @param toDate   Optional end date filter (YYYY-MM-DD)
     * @return Count of active opportunities
     */
    public int getActiveOpportunityCount(String fromDate, String toDate) {
        try {
            log.info("Fetching active opportunity count. FromDate: {}, ToDate: {}", fromDate, toDate);

            String token = authService.getAccessToken();

            // Build query - only count, no data needed
            StringBuilder uri = new StringBuilder("/opportunities?$count=true&$top=1");

            // Filter for open opportunities (statecode = 0)
            StringBuilder filter = new StringBuilder("$filter=statecode eq 0");

            // Add date filters if provided
            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append(" and createdon ge ").append(fromDate);
            }

            if (toDate != null && !toDate.isEmpty()) {
                filter.append(" and createdon le ").append(toDate);
            }

            uri.append("&").append(filter.toString());

            String response = webClient.get()
                    .uri(uri.toString())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response,
                    new TypeReference<D365Response<Opportunity>>() {
                    });

            int count = d365Response.getCount() != null ? d365Response.getCount() : 0;
            log.info("Active opportunity count: {}", count);
            return count;

        } catch (WebClientResponseException e) {
            log.error("Error fetching opportunity count. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return 0;
        } catch (Exception e) {
            log.error("Error fetching opportunity count", e);
            return 0;
        }
    }

    /**
     * Get all opportunities from Dynamics 365
     * 
     * @param top      Maximum number of records to return (optional)
     * @param select   Comma-separated list of fields to return (optional)
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     * @param toDate   Optional end date filter (YYYY-MM-DD)
     * @return List of opportunities
     */
    public List<Opportunity> getAllOpportunities(Integer top, String select, String fromDate, String toDate) {
        try {
            log.info("Fetching opportunities from Dynamics 365. Top: {}, Select: {}, FromDate: {}, ToDate: {}",
                    top, select, fromDate, toDate);

            String token = authService.getAccessToken();

            // Build query parameters
            StringBuilder uri = new StringBuilder("/opportunities?");

            if (top != null && top > 0) {
                uri.append("$top=").append(top).append("&");
            }

            if (select != null && !select.isEmpty()) {
                uri.append("$select=").append(select).append("&");
            } else {
                // Default fields to select
                uri.append("$select=opportunityid,name,description,estimatedvalue,estimatedclosedate,")
                        .append("actualvalue,actualclosedate,closeprobability,salesstage,stepname,")
                        .append("createdon,modifiedon,statecode,statuscode,")
                        .append("_ownerid_value,_createdby_value,_modifiedby_value,")
                        .append("_customerid_value,_accountid_value&");
            }

            // Add filters
            StringBuilder filter = new StringBuilder();

            if (fromDate != null && !fromDate.isEmpty()) {
                filter.append(filter.length() > 0 ? " and " : "")
                        .append("createdon ge ").append(fromDate);
            }

            if (toDate != null && !toDate.isEmpty()) {
                filter.append(filter.length() > 0 ? " and " : "")
                        .append("createdon le ").append(toDate);
            }

            if (filter.length() > 0) {
                uri.append("$filter=").append(filter.toString()).append("&");
            }

            // Add count
            uri.append("$count=true");

            String response = webClient.get()
                    .uri(uri.toString())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Opportunity> d365Response = objectMapper.readValue(
                    response,
                    new TypeReference<D365Response<Opportunity>>() {
                    });

            log.info("Successfully fetched {} opportunities", d365Response.getValue().size());
            return d365Response.getValue();

        } catch (WebClientResponseException e) {
            log.error("Error fetching opportunities. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch opportunities: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching opportunities", e);
            throw new RuntimeException("Failed to fetch opportunities: " + e.getMessage(), e);
        }
    }

    /**
     * Get a single opportunity by ID
     * 
     * @param opportunityId The ID of the opportunity
     * @return The opportunity if found
     */
    public Opportunity getOpportunityById(String opportunityId) {
        try {
            log.info("Fetching opportunity: {}", opportunityId);

            String token = authService.getAccessToken();

            String uri = String.format("/opportunities(%s)", opportunityId);

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            Opportunity opportunity = objectMapper.readValue(response, Opportunity.class);
            log.info("Successfully fetched opportunity: {}", opportunity.getName());
            return opportunity;

        } catch (WebClientResponseException e) {
            log.error("Error fetching opportunity. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch opportunity: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching opportunity", e);
            throw new RuntimeException("Failed to fetch opportunity: " + e.getMessage(), e);
        }
    }
}
