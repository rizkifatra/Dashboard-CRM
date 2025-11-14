package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for Microsoft Dynamics 365 integration
 */
@Configuration
@ConfigurationProperties(prefix = "d365.api")
@Data
public class D365Config {

    /**
     * Base URL for Dynamics 365 Web API
     * Example: https://yourorg.crm5.dynamics.com/api/data/v9.2
     */
    private String baseUrl;

    /**
     * OAuth scope for authentication
     * Example: https://yourorg.crm5.dynamics.com/.default
     */
    private String scope;

    /**
     * API timeout in milliseconds
     */
    private int timeout = 30000;
}
