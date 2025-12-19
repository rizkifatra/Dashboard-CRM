package com.example.backend.service;

import com.example.backend.config.D365Config;
import com.example.backend.model.Account;
import com.example.backend.model.D365Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service for interacting with Dynamics 365 Account entity
 */
@Service
@Slf4j
public class D365AccountService {

    private final D365Config d365Config;
    private final D365AuthService authService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public D365AccountService(D365Config d365Config, D365AuthService authService) {
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
     * Get all accounts from Dynamics 365
     * 
     * @param top    Maximum number of records to return (optional)
     * @param select Comma-separated list of fields to return (optional)
     * @return List of accounts
     */
    public List<Account> getAllAccounts(Integer top, String select) {
        try {
            log.info("Fetching accounts from Dynamics 365. Top: {}, Select: {}", top, select);

            String token = authService.getAccessToken();

            // Build query parameters
            String uri = "/accounts";
            StringBuilder queryParams = new StringBuilder("?");

            if (top != null && top > 0) {
                queryParams.append("$top=").append(top).append("&");
            }

            if (select != null && !select.isEmpty()) {
                queryParams.append("$select=").append(select).append("&");
            } else {
                // Default fields to select including owner and creator fields
                queryParams.append("$select=accountid,name,accountnumber,emailaddress1,telephone1,")
                        .append("websiteurl,address1_city,address1_country,revenue,")
                        .append("numberofemployees,createdon,modifiedon,")
                        .append("_ownerid_value,_createdby_value,_modifiedby_value&");
            }

            // Sort by creation date (newest first)
            queryParams.append("$orderby=createdon desc&");

            // Add count
            queryParams.append("$count=true");

            uri += queryParams.toString();

            String response = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Account> d365Response = objectMapper.readValue(
                    response,
                    new TypeReference<D365Response<Account>>() {
                    });

            log.info("Successfully fetched {} accounts", d365Response.getValue().size());
            return d365Response.getValue();

        } catch (WebClientResponseException e) {
            log.error("Error fetching accounts. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch accounts: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching accounts", e);
            throw new RuntimeException("Failed to fetch accounts: " + e.getMessage(), e);
        }
    }

    /**
     * Get account by ID
     * 
     * @param accountId Account ID (GUID)
     * @return Account if found
     */
    public Optional<Account> getAccountById(String accountId) {
        try {
            log.info("Fetching account with ID: {}", accountId);

            String token = authService.getAccessToken();

            String response = webClient.get()
                    .uri("/accounts(" + accountId + ")?$select=accountid,name,accountnumber,emailaddress1,telephone1," +
                            "websiteurl,address1_city,address1_country,revenue,numberofemployees,createdon,modifiedon,"
                            +
                            "_ownerid_value,_createdby_value,_modifiedby_value")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            Account account = objectMapper.readValue(response, Account.class);
            log.info("Successfully fetched account: {}", account.getName());
            return Optional.of(account);

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Account not found with ID: {}", accountId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching account by ID", e);
            throw new RuntimeException("Failed to fetch account: " + e.getMessage(), e);
        }
    }

    /**
     * Get count of all accounts
     * 
     * @return Total count of accounts
     */
    public int getAccountCount() {
        try {
            log.info("Fetching account count from Dynamics 365");

            String token = authService.getAccessToken();

            String response = webClient.get()
                    .uri("/accounts/$count")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            // Remove any quotes, whitespace and parse
            String cleanResponse = response.trim().replace("\"", "");
            int count = Integer.parseInt(cleanResponse);
            log.info("Total account count: {}", count);
            return count;

        } catch (Exception e) {
            log.error("Error fetching account count. Response was: {}", e);
            throw new RuntimeException("Failed to fetch account count: " + e.getMessage(), e);
        }
    }

    /**
     * Search accounts with OData filtering (efficient server-side filtering)
     * 
     * @param name    Account name to search (contains)
     * @param city    City to filter
     * @param country Country to filter
     * @param top     Maximum records to return
     * @return List of matching accounts
     */
    public List<Account> searchAccounts(String name, String city, String country, Integer top) {
        try {
            log.info("Searching accounts - Name: {}, City: {}, Country: {}, Top: {}", name, city, country, top);

            String token = authService.getAccessToken();

            // Build OData filter
            StringBuilder filterBuilder = new StringBuilder();

            if (name != null && !name.isBlank()) {
                filterBuilder.append("contains(name,'").append(name.trim()).append("')");
            }

            if (city != null && !city.isBlank()) {
                if (filterBuilder.length() > 0) {
                    filterBuilder.append(" and ");
                }
                filterBuilder.append("address1_city eq '").append(city.trim()).append("'");
            }

            if (country != null && !country.isBlank()) {
                if (filterBuilder.length() > 0) {
                    filterBuilder.append(" and ");
                }
                filterBuilder.append("address1_country eq '").append(country.trim()).append("'");
            }

            // Build URI with OData query
            StringBuilder uriBuilder = new StringBuilder("/accounts?");

            if (filterBuilder.length() > 0) {
                uriBuilder.append("$filter=").append(filterBuilder.toString()).append("&");
            }

            uriBuilder.append("$select=accountid,name,accountnumber,emailaddress1,telephone1,")
                    .append("websiteurl,address1_city,address1_country,revenue,")
                    .append("numberofemployees,createdon,modifiedon,")
                    .append("_ownerid_value,_createdby_value,_modifiedby_value&");

            if (top != null && top > 0) {
                uriBuilder.append("$top=").append(top).append("&");
            }

            uriBuilder.append("$count=true");

            String response = webClient.get()
                    .uri(uriBuilder.toString())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            D365Response<Account> d365Response = objectMapper.readValue(
                    response,
                    new TypeReference<D365Response<Account>>() {
                    });

            log.info("Search found {} accounts", d365Response.getValue().size());
            return d365Response.getValue();

        } catch (WebClientResponseException e) {
            log.error("Error searching accounts. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to search accounts: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error searching accounts", e);
            throw new RuntimeException("Failed to search accounts: " + e.getMessage(), e);
        }
    }

    /**
     * Test connection to Dynamics 365
     * 
     * @return true if connection successful
     */
    public boolean testConnection() {
        try {
            log.info("Testing connection to Dynamics 365");
            String token = authService.getAccessToken();

            webClient.get()
                    .uri("/accounts?$top=1")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            log.info("Connection test successful");
            return true;
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return false;
        }
    }
}
