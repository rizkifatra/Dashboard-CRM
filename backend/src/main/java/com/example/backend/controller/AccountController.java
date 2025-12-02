package com.example.backend.controller;

import com.example.backend.model.Account;
import com.example.backend.model.ApiResponse;
import com.example.backend.service.D365AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Account operations
 */
@RestController
@RequestMapping("/api/accounts")
@Slf4j
public class AccountController {

    private final D365AccountService accountService;

    public AccountController(D365AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Get all accounts with pagination
     * 
     * @param top    Maximum number of records to return (default: 50, max: 1000)
     * @param skip   Number of records to skip for pagination (default: 0)
     * @param select Comma-separated list of fields to return
     * @return List of accounts
     */
    @GetMapping
    public ApiResponse<List<Account>> getAllAccounts(
            @RequestParam(required = false, defaultValue = "50") Integer top,
            @RequestParam(required = false, defaultValue = "0") Integer skip,
            @RequestParam(required = false) String select) {

        log.info("GET /api/accounts - Top: {}, Skip: {}, Select: {}", top, skip, select);

        try {
            // Validate input
            if (top != null && top > 1000) {
                return ApiResponse.error("Top parameter cannot exceed 1000");
            }
            if (skip != null && skip < 0) {
                return ApiResponse.error("Skip parameter cannot be negative");
            }

            List<Account> accounts = accountService.getAllAccounts(top, select);

            String message = String.format("Successfully retrieved %d accounts", accounts.size());
            return ApiResponse.success(message, accounts);

        } catch (Exception e) {
            log.error("Error fetching accounts", e);
            return ApiResponse.error("Failed to fetch accounts", e.getMessage());
        }
    }

    /**
     * Get account by ID
     * 
     * @param id Account ID (GUID format required)
     * @return Account details
     */
    @GetMapping("/{id}")
    public ApiResponse<Account> getAccountById(@PathVariable String id) {
        log.info("GET /api/accounts/{} - Fetching account by ID", id);

        try {
            // Validate GUID format (basic check)
            if (id == null || id.isBlank()) {
                return ApiResponse.error("Account ID is required");
            }
            if (!id.matches("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$")) {
                return ApiResponse.error("Invalid account ID format. Expected GUID format.");
            }

            Optional<Account> account = accountService.getAccountById(id);

            if (account.isPresent()) {
                return ApiResponse.success("Account found", account.get());
            } else {
                return ApiResponse.error("Account not found with ID: " + id);
            }

        } catch (Exception e) {
            log.error("Error fetching account by ID: {}", id, e);
            return ApiResponse.error("Failed to fetch account", e.getMessage());
        }
    }

    /**
     * Get total count of accounts
     * 
     * @return Account count
     */
    @GetMapping("/count")
    public ApiResponse<Map<String, Integer>> getAccountCount() {
        log.info("GET /api/accounts/count - Fetching account count");

        try {
            int count = accountService.getAccountCount();

            Map<String, Integer> result = new HashMap<>();
            result.put("count", count);

            return ApiResponse.success("Account count retrieved", result);

        } catch (Exception e) {
            log.error("Error fetching account count", e);
            return ApiResponse.error("Failed to fetch account count", e.getMessage());
        }
    }

    /**
     * Get accounts with custom filtering using OData
     * Example: /api/accounts/search?city=Seattle&top=10
     * Example: /api/accounts/search?name=Contoso&country=Malaysia
     * 
     * @param name    Filter by account name (contains)
     * @param city    Filter by city
     * @param country Filter by country
     * @param top     Maximum records to return (max 1000)
     * @return Filtered accounts
     */
    @GetMapping("/search")
    public ApiResponse<List<Account>> searchAccounts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "50") Integer top) {

        log.info("GET /api/accounts/search - Name: {}, City: {}, Country: {}, Top: {}", name, city, country, top);

        try {
            // Validate input
            if (top != null && top > 1000) {
                return ApiResponse.error("Top parameter cannot exceed 1000");
            }

            // Validate at least one search parameter is provided
            if ((name == null || name.isBlank()) &&
                    (city == null || city.isBlank()) &&
                    (country == null || country.isBlank())) {
                return ApiResponse.error("At least one search parameter (name, city, or country) is required");
            }

            // Build OData filter
            List<Account> accounts = accountService.searchAccounts(name, city, country, top);

            String message = String.format("Found %d accounts matching criteria", accounts.size());
            return ApiResponse.success(message, accounts);

        } catch (Exception e) {
            log.error("Error searching accounts", e);
            return ApiResponse.error("Failed to search accounts", e.getMessage());
        }
    }
}
