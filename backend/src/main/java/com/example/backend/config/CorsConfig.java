package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Global CORS Configuration for all API endpoints
 * This allows the Angular frontend to communicate with the backend
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

        @Value("${cors.allowed-origins:}")
        private String corsAllowedOrigins;

        private List<String> getAllowedOrigins() {
                List<String> origins = new ArrayList<>(Arrays.asList(
                                "http://localhost:*",
                                "http://127.0.0.1:*",
                                "https://localhost:*",
                                "https://127.0.0.1:*"));

                // Add production origins from environment variable
                if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
                        for (String origin : corsAllowedOrigins.split(",")) {
                                String trimmed = origin.trim();
                                if (!trimmed.isEmpty()) {
                                        origins.add(trimmed);
                                }
                        }
                }
                return origins;
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOriginPatterns(getAllowedOrigins().toArray(new String[0]))
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                                .allowedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allow origins from environment + localhost for development
                configuration.setAllowedOriginPatterns(getAllowedOrigins());

                // Allow all HTTP methods
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

                // Allow all headers
                configuration.setAllowedHeaders(Arrays.asList("*"));

                // Allow credentials (cookies, authorization headers, etc.)
                configuration.setAllowCredentials(true);

                // How long the response from a pre-flight request can be cached (1 hour)
                configuration.setMaxAge(3600L);

                // Expose headers that the browser can access
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With",
                                "Access-Control-Allow-Origin",
                                "Access-Control-Allow-Credentials"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        public CorsFilter corsFilter() {
                return new CorsFilter(corsConfigurationSource());
        }
}
