package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.OAuth2AuthenticationSuccessHandler;
import com.example.backend.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration with OAuth2 and JWT
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final CorsConfigurationSource corsConfigurationSource;
        private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
        private final ClientRegistrationRepository clientRegistrationRepository;

        @Value("${app.frontend-url:http://localhost:4200}")
        private String frontendUrl;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
                        OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                        CorsConfigurationSource corsConfigurationSource,
                        RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                        ClientRegistrationRepository clientRegistrationRepository) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.oidcUserService = oidcUserService;
                this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
                this.corsConfigurationSource = corsConfigurationSource;
                this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
                this.clientRegistrationRepository = clientRegistrationRepository;
        }

        @Bean
        public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
                return new CustomOAuth2AccessTokenResponseClient();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Enable CORS with
                                                                                                 // configuration source
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(restAuthenticationEntryPoint))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/health/**").permitAll()
                                                .requestMatchers("/login/**").permitAll()
                                                .requestMatchers("/oauth2/**").permitAll()
                                                // Protected endpoints - require authentication
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authorization -> authorization
                                                                .authorizationRequestResolver(
                                                                                customAuthorizationRequestResolver()))
                                                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                                                .accessTokenResponseClient(accessTokenResponseClient()))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .oidcUserService(oidcUserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureUrl(frontendUrl + "/login?error=true"))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * Custom OAuth2 Authorization Request Resolver that adds prompt=select_account
         * to force Microsoft to show account selection screen on every login
         */
        private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
                DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository, "/oauth2/authorization");

                resolver.setAuthorizationRequestCustomizer(authorizationRequest -> authorizationRequest
                                .additionalParameters(params -> params.put("prompt", "select_account")));

                return resolver;
        }
}
