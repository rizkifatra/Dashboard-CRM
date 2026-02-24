package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom OAuth2 User Service to handle Microsoft Azure AD user info
 */
@Configuration
public class OAuth2UserServiceConfig {

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            // Load user from delegate (this gets the userInfo)
            OidcUser oidcUser;
            try {
                oidcUser = delegate.loadUser(userRequest);
            } catch (Exception e) {
                // If userInfo endpoint fails, create user from ID token only
                OidcIdToken idToken = userRequest.getIdToken();
                return new DefaultOidcUser(
                        Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                        idToken);
            }

            // Get the ID token
            OidcIdToken idToken = userRequest.getIdToken();

            // Build custom userInfo that matches the ID token's sub claim
            Map<String, Object> claims = new HashMap<>(idToken.getClaims());

            // Add additional info from the userInfo if available
            if (oidcUser.getUserInfo() != null) {
                Map<String, Object> userInfoClaims = oidcUser.getUserInfo().getClaims();
                // Only add claims that don't conflict with ID token
                userInfoClaims.forEach((key, value) -> {
                    if (!"sub".equals(key) && !claims.containsKey(key)) {
                        claims.put(key, value);
                    }
                });
            }

            // Create userInfo from the combined claims with sub from ID token
            OidcUserInfo userInfo = new OidcUserInfo(claims);

            // Create a new OidcUser with ID token's sub claim
            return new DefaultOidcUser(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    idToken,
                    userInfo);
        };
    }
}
