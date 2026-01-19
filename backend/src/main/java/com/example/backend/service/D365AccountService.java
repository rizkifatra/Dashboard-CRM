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
import org.springframework.web.reactive.function.client.ExchangeStrategies;

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

        // Configure exchange strategies to allow larger buffer size (10MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(d365Config.getBaseUrl())
                .exchangeStrategies(strategies)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("OData-MaxVersion", "4.0")
                .defaultHeader("OData-Version", "4.0")
                .defaultHeader("Prefer", "return=representation")
                .build();
    }

    /**
     * Get all accounts from Dynamics 365
     * 
     * @param top     Maximum number of records to return (optional)
     * @param skip    Number of records to skip (optional)
     * @param search  Search term for filtering (optional)
     * @param ownerId Owner ID filter (optional)
     * @param status  Status filter (active/inactive) (optional)
     * @param select  Comma-separated list of fields to return (optional)
     * @return List of accounts
     */
    public List<Account> getAllAccounts(Integer top, Integer skip, String search, String ownerId, String status,
            String select) {
        try {
            log.info(
                    "Fetching accounts from Dynamics 365. Top: {}, Skip: {}, Search: {}, OwnerId: {}, Status: {}, Select: {}",
                    top, skip, search, ownerId, status, select);

            String token = authService.getAccessToken();

            // Build query parameters
            String uri = "/accounts";
            StringBuilder queryParams = new StringBuilder("?");

            // Note: D365 API doesn't support $skip parameter - it returns 400 Bad Request
            // We ignore the skip parameter and only use $top to load batches of data
            // This matches the behavior of the activities endpoint
            if (top != null && top > 0) {
                queryParams.append("$top=").append(top).append("&");
            }

            if (select != null && !select.isEmpty()) {
                queryParams.append("$select=").append(select).append("&");
            } else {
                // Default fields to select including owner, creator, and primary contact fields
                queryParams.append(
                        "$select=accountid,name,accountnumber,emailaddress1,emailaddress2,emailaddress3,telephone1,")
                        .append("websiteurl,address1_city,address1_country,revenue,")
                        .append("numberofemployees,createdon,modifiedon,")
                        .append("_ownerid_value,_createdby_value,_modifiedby_value,_primarycontactid_value&");
                // Note: Cannot expand ownerid as it's a polymorphic lookup to principal
                // (systemuser or team)
                // The owner name will need to be fetched separately or from the frontend staff
                // list
            }

            // Build filter conditions
            StringBuilder filterBuilder = new StringBuilder();

            // Search filter - search across multiple relevant fields
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = search.trim().replace("'", "''"); // Escape single quotes
                filterBuilder.append("(contains(name, '" + searchTerm + "')")
                        .append(" or contains(accountnumber, '" + searchTerm + "')")
                        .append(" or contains(emailaddress1, '" + searchTerm + "')")
                        .append(" or contains(emailaddress2, '" + searchTerm + "')")
                        .append(" or contains(emailaddress3, '" + searchTerm + "')")
                        .append(" or contains(telephone1, '" + searchTerm + "')")
                        .append(" or contains(websiteurl, '" + searchTerm + "')")
                        .append(" or contains(address1_city, '" + searchTerm + "')")
                        .append(" or contains(address1_country, '" + searchTerm + "'))");
            }

            // Owner filter
            if (ownerId != null && !ownerId.trim().isEmpty() && !"all".equals(ownerId)) {
                if (filterBuilder.length() > 0) {
                    filterBuilder.append(" and ");
                }
                filterBuilder.append("_ownerid_value eq '" + ownerId.trim() + "'");
            }

            // Status filter - Note: This is client-side filtering since D365 doesn't have a
            // simple status field
            // We'll apply this filter after receiving data from D365

            if (filterBuilder.length() > 0) {
                queryParams.append("$filter=").append(filterBuilder.toString()).append("&");
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
                    .uri("/accounts(" + accountId
                            + ")?$select=accountid,name,accountnumber,emailaddress1,emailaddress2,emailaddress3,telephone1,"
                            +
                            "websiteurl,address1_city,address1_country,revenue,numberofemployees,createdon,modifiedon,"
                            +
                            "_ownerid_value,_createdby_value,_modifiedby_value,_primarycontactid_value")
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

            log.info("Raw count response: {}", response);

            // Remove any quotes (regular and escaped), whitespace and non-digit characters
            String cleanResponse = response.trim()
                    .replace("\\\"", "")
                    .replace("\"", "")
                    .replaceAll("[^0-9]", "");

            log.info("Cleaned count response: {}", cleanResponse);
            int count = Integer.parseInt(cleanResponse);
            log.info("Total account count: {}", count);
            return count;

        } catch (Exception e) {
            log.error("Error fetching account count: {}", e.getMessage());
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

            uriBuilder.append(
                    "$select=accountid,name,accountnumber,emailaddress1,emailaddress2,emailaddress3,telephone1,")
                    .append("websiteurl,address1_city,address1_country,revenue,")
                    .append("numberofemployees,createdon,modifiedon,")
                    .append("_ownerid_value,_createdby_value,_modifiedby_value,_primarycontactid_value&");

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
     * Update an existing account in Dynamics 365
     * 
     * @param accountId The account ID (GUID)
     * @param account   The account object with updated values
     * @return Updated account
     */
    public Optional<Account> updateAccount(String accountId, Account account) {
        try {
            log.info("Updating account {} in Dynamics 365", accountId);
            String token = authService.getAccessToken();

            // Build the update payload - only include fields that should be updated
            // D365 uses PATCH method for updates
            StringBuilder payload = new StringBuilder("{");
            boolean needsComma = false;

            // Only include fields that are not null and can be updated
            if (account.getEmailAddress() != null) {
                payload.append("\"emailaddress1\":\"").append(escapeJson(account.getEmailAddress())).append("\"");
                needsComma = true;
            }
            if (account.getEmailAddress2() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"emailaddress2\":\"").append(escapeJson(account.getEmailAddress2())).append("\"");
                needsComma = true;
            }
            if (account.getEmailAddress3() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"emailaddress3\":\"").append(escapeJson(account.getEmailAddress3())).append("\"");
                needsComma = true;
            }
            if (account.getTelephone() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"telephone1\":\"").append(escapeJson(account.getTelephone())).append("\"");
                needsComma = true;
            }
            if (account.getWebsiteUrl() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"websiteurl\":\"").append(escapeJson(account.getWebsiteUrl())).append("\"");
                needsComma = true;
            }
            if (account.getCity() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"address1_city\":\"").append(escapeJson(account.getCity())).append("\"");
                needsComma = true;
            }
            if (account.getCountry() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"address1_country\":\"").append(escapeJson(account.getCountry())).append("\"");
                needsComma = true;
            }
            if (account.getRevenue() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"revenue\":").append(account.getRevenue());
                needsComma = true;
            }
            if (account.getNumberOfEmployees() != null) {
                if (needsComma)
                    payload.append(",");
                payload.append("\"numberofemployees\":").append(account.getNumberOfEmployees());
            }

            payload.append("}");

            log.info("Update payload: {}", payload.toString());

            // Send PATCH request to D365
            webClient.patch()
                    .uri("/accounts(" + accountId + ")")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(d365Config.getTimeout()))
                    .block();

            log.info("Account {} updated successfully", accountId);

            // Fetch and return the updated account
            return getAccountById(accountId);

        } catch (WebClientResponseException e) {
            log.error("Error updating account {}. Status: {}, Response: {}",
                    accountId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to update account: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating account {}", accountId, e);
            throw new RuntimeException("Failed to update account: " + e.getMessage(), e);
        }
    }

    /**
     * Escape special characters in JSON strings
     */
    private String escapeJson(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
