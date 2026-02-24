package com.example.backend.controller;

import com.example.backend.model.ApiResponse;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller for handling Microsoft OAuth2 login
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    /**
     * Get user info after Microsoft OAuth2 login and return JWT token
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUser(
            @AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("User not authenticated", null));
        }

        try {
            // Generate JWT token
            String token = tokenProvider.generateToken(authentication);

            // Get user attributes
            String email = principal.getAttribute("mail");
            if (email == null || email.isEmpty()) {
                email = principal.getAttribute("userPrincipalName");
            }
            String name = principal.getAttribute("displayName");
            String givenName = principal.getAttribute("givenName");
            String surname = principal.getAttribute("surname");

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", email);
            userInfo.put("name", name);
            userInfo.put("givenName", givenName);
            userInfo.put("surname", surname);
            userInfo.put("token", token);

            log.info("User logged in successfully: {}", email);

            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            log.error("Error getting user info", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error retrieving user information", null));
        }
    }

    /**
     * Validate JWT token
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Invalid token format", null));
            }

            String token = authHeader.substring(7);
            boolean isValid = tokenProvider.validateToken(token);

            if (isValid) {
                String email = tokenProvider.getEmailFromToken(token);
                String name = tokenProvider.getNameFromToken(token);

                Map<String, Object> data = new HashMap<>();
                data.put("valid", true);
                data.put("email", email);
                data.put("name", name);

                return ResponseEntity.ok(ApiResponse.success(data));
            } else {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Invalid or expired token", null));
            }
        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Token validation failed", null));
        }
    }

    /**
     * Logout endpoint (client-side will clear token)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
