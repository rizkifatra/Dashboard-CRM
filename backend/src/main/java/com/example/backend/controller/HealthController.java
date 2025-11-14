package com.example.backend.controller;

import com.example.backend.config.AzureAdConfig;
import com.example.backend.config.D365Config;
import com.example.backend.model.ApiResponse;
import com.example.backend.service.D365AccountService;
import com.example.backend.service.D365AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check and system status controller
 */
@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthController {

    private final D365AuthService authService;
    private final D365AccountService accountService;
    private final D365Config d365Config;
    private final AzureAdConfig azureAdConfig;

    public HealthController(D365AuthService authService,
            D365AccountService accountService,
            D365Config d365Config,
            AzureAdConfig azureAdConfig) {
        this.authService = authService;
        this.accountService = accountService;
        this.d365Config = d365Config;
        this.azureAdConfig = azureAdConfig;
    }

    /**
     * Basic health check endpoint
     * 
     * @return Health status
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        log.info("Health check requested");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "D365 Dashboard API");
        health.put("timestamp", System.currentTimeMillis());

        // Check configuration
        boolean configured = authService.isConfigured();
        health.put("configured", configured);

        if (!configured) {
            health.put("warning", "Azure AD credentials not configured. Please update application.properties");
        }

        return ApiResponse.success("Application is running", health);
    }

    /**
     * Test connection to Dynamics 365
     * 
     * @return Connection test result
     */
    @GetMapping("/d365-connection")
    public ApiResponse<Map<String, Object>> testD365Connection() {
        log.info("D365 connection test requested");

        Map<String, Object> result = new HashMap<>();
        result.put("baseUrl", d365Config.getBaseUrl());
        result.put("timestamp", System.currentTimeMillis());

        try {
            // Check if configured
            if (!authService.isConfigured()) {
                result.put("status", "NOT_CONFIGURED");
                result.put("message", "Azure AD credentials not configured");
                return ApiResponse.error("Configuration required",
                        "Please configure Azure AD credentials in application.properties");
            }

            // Test authentication
            boolean authSuccess = authService.testAuthentication();
            result.put("authenticationStatus", authSuccess ? "SUCCESS" : "FAILED");

            if (!authSuccess) {
                return ApiResponse.error("Authentication failed", "Unable to obtain access token from Azure AD");
            }

            // Test D365 connection
            boolean connectionSuccess = accountService.testConnection();
            result.put("connectionStatus", connectionSuccess ? "SUCCESS" : "FAILED");

            if (connectionSuccess) {
                result.put("status", "CONNECTED");
                return ApiResponse.success("Successfully connected to Dynamics 365", result);
            } else {
                result.put("status", "CONNECTION_FAILED");
                return ApiResponse.error("Failed to connect to Dynamics 365", "Check D365 URL and permissions");
            }

        } catch (Exception e) {
            log.error("D365 connection test failed", e);
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            return ApiResponse.error("Connection test failed", e.getMessage());
        }
    }

    /**
     * Get configuration info (without sensitive data)
     * 
     * @return Configuration information
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        log.info("Configuration info requested");

        Map<String, Object> config = new HashMap<>();
        config.put("d365BaseUrl", d365Config.getBaseUrl());
        config.put("d365Scope", d365Config.getScope());
        config.put("azureTenantId", azureAdConfig.getTenantId());
        config.put("azureClientIdConfigured",
                azureAdConfig.getClientId() != null && !azureAdConfig.getClientId().equals("your-client-id"));
        config.put("azureClientSecretConfigured", azureAdConfig.getClientSecret() != null
                && !azureAdConfig.getClientSecret().equals("your-client-secret"));
        config.put("fullyConfigured", authService.isConfigured());

        return ApiResponse.success("Configuration information", config);
    }
}
