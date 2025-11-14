package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for Azure Active Directory authentication
 */
@Configuration
@ConfigurationProperties(prefix = "azure.ad")
@Data
public class AzureAdConfig {

    /**
     * Azure AD Tenant ID
     */
    private String tenantId;

    /**
     * Azure AD Client ID (Application ID)
     */
    private String clientId;

    /**
     * Azure AD Client Secret
     */
    private String clientSecret;

    /**
     * Azure AD Authority URL
     */
    private String authority;
}
