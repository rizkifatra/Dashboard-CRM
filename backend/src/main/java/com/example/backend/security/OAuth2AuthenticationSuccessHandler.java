package com.example.backend.security;

import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.TokenStorageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Custom OAuth2 authentication success handler
 * Generates JWT token and redirects to frontend with token
 */
@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final TokenStorageService tokenStorageService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider,
            TokenStorageService tokenStorageService,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.tokenProvider = tokenProvider;
        this.tokenStorageService = tokenStorageService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        try {
            // Generate JWT token
            String token = tokenProvider.generateToken(authentication);

            // Get user info
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = getUserEmail(oAuth2User);
            String name = oAuth2User.getAttribute("name");

            log.info("User {} logged in successfully, generating JWT token", email);

            // Extract and store OAuth2 access token for D365 API access
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

                // Get the authorized client which contains the access token
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken.getName());

                if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                    String d365AccessToken = authorizedClient.getAccessToken().getTokenValue();

                    // Store the D365 access token for this user
                    tokenStorageService.storeD365Token(email, d365AccessToken);

                    log.info("Stored D365 access token for user: {}", email);
                } else {
                    log.warn("No OAuth2 access token found for user: {}", email);
                }
            }

            // Redirect to frontend with token in URL
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                    .queryParam("token", token)
                    .queryParam("email", email)
                    .queryParam("name", name)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Error during OAuth2 authentication success handling", e);
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=true");
        }
    }

    private String getUserEmail(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("mail");
        if (email == null || email.isEmpty()) {
            email = oAuth2User.getAttribute("userPrincipalName");
        }
        if (email == null || email.isEmpty()) {
            email = oAuth2User.getAttribute("email");
        }
        if (email == null || email.isEmpty()) {
            email = oAuth2User.getAttribute("preferred_username");
        }

        log.info("Extracted user email: {}", email);
        return email;
    }
}
