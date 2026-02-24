package com.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to store user OAuth2 access tokens for D365 API access
 * In production, this should use Redis or database
 */
@Service
@Slf4j
public class TokenStorageService {

    // In-memory storage: userId -> D365 access token
    // For production, use Redis or database
    private final Map<String, String> userTokens = new ConcurrentHashMap<>();

    /**
     * Store D365 access token for a user
     */
    public void storeD365Token(String userId, String accessToken) {
        log.info("Storing D365 access token for user: {}", userId);
        userTokens.put(userId, accessToken);
    }

    /**
     * Retrieve D365 access token for a user
     */
    public String getD365Token(String userId) {
        log.debug("Retrieving D365 access token for user: {}", userId);
        return userTokens.get(userId);
    }

    /**
     * Remove D365 access token for a user (on logout)
     */
    public void removeD365Token(String userId) {
        log.info("Removing D365 access token for user: {}", userId);
        userTokens.remove(userId);
    }

    /**
     * Check if user has a stored D365 token
     */
    public boolean hasD365Token(String userId) {
        return userTokens.containsKey(userId);
    }
}
