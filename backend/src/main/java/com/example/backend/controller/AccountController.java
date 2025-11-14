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
     * Get all accounts
     * 
     * @param top    Maximum number of records to return (default: 50)
     * @param select Comma-separated list of fields to return
     * @return List of accounts
     */
    @GetMapping
    public ApiResponse<List<Account>> getAllAccounts(
            @RequestParam(required = false, defaultValue = "50") Integer top,
            @RequestParam(required = false) String select) {

        log.info("GET /api/accounts - Fetching accounts. Top: {}, Select: {}", top, select);

        try {
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
     * @param id Account ID (GUID)
     * @return Account details
     */
    @GetMapping("/{id}")
    public ApiResponse<Account> getAccountById(@PathVariable String id) {
        log.info("GET /api/accounts/{} - Fetching account by ID", id);

        try {
            Optional<Account> account = accountService.getAccountById(id);

            if (account.isPresent()) {
                return ApiResponse.success("Account found", account.get());
            } else {
                return ApiResponse.error("Account not found with ID: " + id);
            }

        } catch (Exception e) {
            log.error("Error fetching account by ID", e);
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
     * Get accounts with custom filtering
     * Example: /api/accounts/search?city=Seattle&top=10
     * 
     * @param city    Filter by city
     * @param country Filter by country
     * @param top     Maximum records to return
     * @return Filtered accounts
     */
    @GetMapping("/search")
    public ApiResponse<List<Account>> searchAccounts(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "50") Integer top) {

        log.info("GET /api/accounts/search - City: {}, Country: {}, Top: {}", city, country, top);

        try {
            // For now, get all and filter in memory (can be optimized with OData filters)
            List<Account> accounts = accountService.getAllAccounts(top, null);

            // Simple filtering
            if (city != null && !city.isEmpty()) {
                accounts = accounts.stream()
                        .filter(a -> a.getCity() != null && a.getCity().equalsIgnoreCase(city))
                        .toList();
            }

            if (country != null && !country.isEmpty()) {
                accounts = accounts.stream()
                        .filter(a -> a.getCountry() != null && a.getCountry().equalsIgnoreCase(country))
                        .toList();
            }

            String message = String.format("Found %d accounts matching criteria", accounts.size());
            return ApiResponse.success(message, accounts);

        } catch (Exception e) {
            log.error("Error searching accounts", e);
            return ApiResponse.error("Failed to search accounts", e.getMessage());
        }
    }
}
