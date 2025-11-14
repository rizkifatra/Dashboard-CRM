package com.example.backend.service;

import com.azure.core.credential.AccessToken;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.example.backend.config.AzureAdConfig;
import com.example.backend.config.D365Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Service for handling authentication with Azure AD and Dynamics 365
 */
@Service
@Slf4j
public class D365AuthService {

    private final AzureAdConfig azureAdConfig;
    private final D365Config d365Config;
    private ClientSecretCredential credential;
    private AccessToken currentToken;

    public D365AuthService(AzureAdConfig azureAdConfig, D365Config d365Config) {
        this.azureAdConfig = azureAdConfig;
        this.d365Config = d365Config;
        initializeCredential();
    }

    /**
     * Initialize Azure AD credential
     */
    private void initializeCredential() {
        try {
            this.credential = new ClientSecretCredentialBuilder()
                    .clientId(azureAdConfig.getClientId())
                    .clientSecret(azureAdConfig.getClientSecret())
                    .tenantId(azureAdConfig.getTenantId())
                    .build();

            log.info("Azure AD credential initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Azure AD credential", e);
            throw new RuntimeException("Failed to initialize authentication", e);
        }
    }

    /**
     * Get access token for Dynamics 365 API
     * Automatically refreshes token if expired
     * 
     * @return Valid access token
     */
    public String getAccessToken() {
        try {
            // Check if token exists and is still valid
            if (currentToken != null && currentToken.getExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(5))) {
                log.debug("Using cached access token");
                return currentToken.getToken();
            }

            // Get new token
            log.info("Requesting new access token from Azure AD");
            com.azure.core.credential.TokenRequestContext tokenRequestContext = new com.azure.core.credential.TokenRequestContext();
            tokenRequestContext.addScopes(d365Config.getScope());

            currentToken = credential.getToken(tokenRequestContext).block();

            if (currentToken != null) {
                log.info("Access token obtained successfully. Expires at: {}", currentToken.getExpiresAt());
                return currentToken.getToken();
            } else {
                throw new RuntimeException("Failed to obtain access token");
            }

        } catch (Exception e) {
            log.error("Error getting access token", e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if authentication is properly configured
     * 
     * @return true if configured, false otherwise
     */
    public boolean isConfigured() {
        return azureAdConfig.getClientId() != null
                && !azureAdConfig.getClientId().equals("your-client-id")
                && azureAdConfig.getClientSecret() != null
                && !azureAdConfig.getClientSecret().equals("your-client-secret")
                && azureAdConfig.getTenantId() != null
                && !azureAdConfig.getTenantId().equals("your-tenant-id");
    }

    /**
     * Test authentication by attempting to get a token
     * 
     * @return true if authentication successful
     */
    public boolean testAuthentication() {
        try {
            String token = getAccessToken();
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            log.error("Authentication test failed", e);
            return false;
        }
    }
}
