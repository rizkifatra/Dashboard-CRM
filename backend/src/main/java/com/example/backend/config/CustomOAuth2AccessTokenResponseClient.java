package com.example.backend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

/**
 * Custom OAuth2 Access Token Response Client for Azure AD
 * Azure AD v2.0 requires the scope parameter in the token request
 */
public class CustomOAuth2AccessTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

    private RestTemplate restTemplate;

    private Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> requestEntityConverter = new CustomOAuth2AuthorizationCodeGrantRequestEntityConverter();

    public CustomOAuth2AccessTokenResponseClient() {
        RestTemplate restTemplate = new RestTemplate(Arrays.asList(
                new FormHttpMessageConverter(),
                new OAuth2AccessTokenResponseHttpMessageConverter()));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.restTemplate = restTemplate;
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(
            OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        Assert.notNull(authorizationCodeGrantRequest, "authorizationCodeGrantRequest cannot be null");

        RequestEntity<?> request = this.requestEntityConverter.convert(authorizationCodeGrantRequest);

        ResponseEntity<OAuth2AccessTokenResponse> response;
        try {
            response = this.restTemplate.exchange(request, OAuth2AccessTokenResponse.class);
        } catch (RestClientException ex) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: "
                            + ex.getMessage(),
                    null);
            throw new OAuth2AuthorizationException(oauth2Error, ex);
        }

        OAuth2AccessTokenResponse tokenResponse = response.getBody();

        return tokenResponse;
    }

    /**
     * Custom converter that adds scope parameter to token request for Azure AD
     */
    private static class CustomOAuth2AuthorizationCodeGrantRequestEntityConverter
            implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {

        @Override
        public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

            parameters.add("grant_type", "authorization_code");
            parameters.add("code",
                    authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());

            String redirectUri = authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationRequest()
                    .getRedirectUri();
            if (StringUtils.hasText(redirectUri)) {
                parameters.add("redirect_uri", redirectUri);
            }

            // Add client credentials
            parameters.add("client_id", authorizationCodeGrantRequest.getClientRegistration().getClientId());
            parameters.add("client_secret", authorizationCodeGrantRequest.getClientRegistration().getClientSecret());

            // CRITICAL: Add scopes to token request (required by Azure AD v2.0)
            if (!CollectionUtils.isEmpty(authorizationCodeGrantRequest.getClientRegistration().getScopes())) {
                parameters.add("scope", StringUtils.collectionToDelimitedString(
                        authorizationCodeGrantRequest.getClientRegistration().getScopes(), " "));
            }

            String tokenUri = authorizationCodeGrantRequest.getClientRegistration().getProviderDetails().getTokenUri();

            return RequestEntity
                    .post(tokenUri)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(parameters);
        }
    }
}
