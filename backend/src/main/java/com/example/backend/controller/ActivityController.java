package com.example.backend.controller;

import com.example.backend.model.Activity;
import com.example.backend.model.ApiResponse;
import com.example.backend.service.D365ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Activity operations (Emails, Calls, Meetings, Tasks)
 */
@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
// Removed @CrossOrigin - using global CORS configuration in WebConfig.java
public class ActivityController {

    private final D365ActivityService activityService;

    /**
     * Get all activities
     * 
     * @param top    Optional limit for results (default: 50)
     * @param filter Optional OData filter
     * @return List of activities wrapped in ApiResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities(
            @RequestParam(required = false, defaultValue = "50") Integer top,
            @RequestParam(required = false) String filter) {
        try {
            log.info("GET /api/activities - top: {}, filter: {}", top, filter);

            List<Activity> activities = activityService.getAllActivities(top, filter);

            return ResponseEntity.ok(ApiResponse.<List<Activity>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " activities")
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving activities", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Activity>>builder()
                            .success(false)
                            .message("Failed to retrieve activities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get activity by ID
     * 
     * @param id The activity ID
     * @return Activity wrapped in ApiResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Activity>> getActivityById(@PathVariable String id) {
        try {
            log.info("GET /api/activities/{}", id);

            Optional<Activity> activity = activityService.getActivityById(id);

            if (activity.isPresent()) {
                return ResponseEntity.ok(ApiResponse.<Activity>builder()
                        .success(true)
                        .message("Successfully retrieved activity")
                        .data(activity.get())
                        .build());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error retrieving activity by ID", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Activity>builder()
                            .success(false)
                            .message("Failed to retrieve activity: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get activities by staff email
     * 
     * @param email Staff member's email
     * @param top   Optional limit for results (default: 50)
     * @return List of activities for the staff member
     */
    @GetMapping("/staff/{email}")
    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStaff(
            @PathVariable String email,
            @RequestParam(required = false, defaultValue = "50") Integer top) {
        try {
            log.info("GET /api/activities/staff/{} - top: {}", email, top);

            List<Activity> activities = activityService.getActivitiesByStaffEmail(email, top);

            return ResponseEntity.ok(ApiResponse.<List<Activity>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " activities for staff: " + email)
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving activities by staff", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Activity>>builder()
                            .success(false)
                            .message("Failed to retrieve activities by staff: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get activities by account (client)
     * 
     * @param accountId Account ID
     * @param top       Optional limit for results (default: 50)
     * @return List of activities for the account
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByAccount(
            @PathVariable String accountId,
            @RequestParam(required = false, defaultValue = "50") Integer top) {
        try {
            log.info("GET /api/activities/account/{} - top: {}", accountId, top);

            List<Activity> activities = activityService.getActivitiesByAccount(accountId, top);

            return ResponseEntity.ok(ApiResponse.<List<Activity>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " activities for account")
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving activities by account", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Activity>>builder()
                            .success(false)
                            .message("Failed to retrieve activities by account: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get email activities only
     * 
     * @param top Optional limit for results (default: 50)
     * @return List of email activities
     */
    @GetMapping("/emails")
    public ResponseEntity<ApiResponse<List<Activity>>> getEmailActivities(
            @RequestParam(required = false, defaultValue = "50") Integer top) {
        try {
            log.info("GET /api/activities/emails - top: {}", top);

            List<Activity> activities = activityService.getEmailActivities(top);

            return ResponseEntity.ok(ApiResponse.<List<Activity>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " email activities")
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving email activities", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Activity>>builder()
                            .success(false)
                            .message("Failed to retrieve email activities: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get email details for specific activity IDs
     * POST endpoint to accept a list of activity IDs and return email details
     * 
     * @param activityIds List of activity IDs to get email details for
     * @return Map of activityId -> email details (from, to, cc, sender)
     */
    @PostMapping("/emails/details")
    public ResponseEntity<ApiResponse<java.util.Map<String, java.util.Map<String, String>>>> getEmailDetails(
            @RequestBody List<String> activityIds) {
        try {
            log.info("POST /api/activities/emails/details - {} activity IDs",
                    activityIds != null ? activityIds.size() : 0);

            java.util.Map<String, java.util.Map<String, String>> emailDetails = activityService
                    .getEmailDetailsByIds(activityIds);

            return ResponseEntity.ok(ApiResponse.<java.util.Map<String, java.util.Map<String, String>>>builder()
                    .success(true)
                    .message("Successfully retrieved email details for " + emailDetails.size() + " activities")
                    .data(emailDetails)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving email details", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<java.util.Map<String, java.util.Map<String, String>>>builder()
                            .success(false)
                            .message("Failed to retrieve email details: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get sent emails by staff
     * 
     * @param email Staff member's email
     * @param top   Optional limit for results (default: 50)
     * @return List of sent email activities
     */
    @GetMapping("/emails/sent/{email}")
    public ResponseEntity<ApiResponse<List<Activity>>> getSentEmailsByStaff(
            @PathVariable String email,
            @RequestParam(required = false, defaultValue = "50") Integer top) {
        try {
            log.info("GET /api/activities/emails/sent/{} - top: {}", email, top);

            List<Activity> activities = activityService.getSentEmailsByStaff(email, top);

            return ResponseEntity.ok(ApiResponse.<List<Activity>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " sent emails for: " + email)
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving sent emails", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Activity>>builder()
                            .success(false)
                            .message("Failed to retrieve sent emails: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get received emails by staff
     * 
     * @param email Staff member's email
     * @param top   Optional limit for results (default: 50)
     * @return List of received email activities
     */
    @GetMapping("/emails/received/{email}")
    public ResponseEntity<ApiResponse<List<Activity>>> getReceivedEmailsByStaff(
            @PathVariable String email,
            @RequestParam(required = false, defaultValue = "50") Integer top) {
        try {
            log.info("GET /api/activities/emails/received/{} - top: {}", email, top);

            List<Activity> activities = activityService.getReceivedEmailsByStaff(email, top);

            return ResponseEntity.ok(ApiResponse.<List<Activity>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " received emails for: " + email)
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving received emails", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Activity>>builder()
                            .success(false)
                            .message("Failed to retrieve received emails: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get activity count
     * 
     * @param filter Optional OData filter
     * @return Count of activities
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getActivityCount(
            @RequestParam(required = false) String filter) {
        try {
            log.info("GET /api/activities/count - filter: {}", filter);

            int count = activityService.getActivityCount(filter);

            return ResponseEntity.ok(ApiResponse.<Integer>builder()
                    .success(true)
                    .message("Successfully retrieved activity count")
                    .data(count)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving activity count", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Integer>builder()
                            .success(false)
                            .message("Failed to retrieve activity count: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get email statistics (incoming/outgoing counts) for a staff member
     * 
     * @param email Staff member's email
     * @return Email statistics with counts
     */
    @GetMapping("/emails/stats/{email}")
    public ResponseEntity<ApiResponse<com.example.backend.model.EmailStats>> getEmailStats(
            @PathVariable String email) {
        try {
            log.info("GET /api/activities/emails/stats/{}", email);

            com.example.backend.model.EmailStats stats = activityService.getEmailStatsByStaff(email);

            return ResponseEntity.ok(ApiResponse.<com.example.backend.model.EmailStats>builder()
                    .success(true)
                    .message("Successfully retrieved email statistics for: " + email)
                    .data(stats)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving email statistics", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<com.example.backend.model.EmailStats>builder()
                            .success(false)
                            .message("Failed to retrieve email statistics: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get recent activities across all staff for dashboard timeline
     * 
     * @param top      Optional limit for results (default: 20)
     * @param fromDate Optional start date (YYYY-MM-DD)
     * @param toDate   Optional end date (YYYY-MM-DD)
     * @return List of recent activities with staff information
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> getRecentActivities(
            @RequestParam(required = false, defaultValue = "20") Integer top,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            log.info("GET /api/activities/recent - top: {}, fromDate: {}, toDate: {}", top, fromDate, toDate);

            List<java.util.Map<String, Object>> activities = activityService.getRecentActivities(top, fromDate, toDate);

            return ResponseEntity.ok(ApiResponse.<List<java.util.Map<String, Object>>>builder()
                    .success(true)
                    .message("Successfully retrieved " + activities.size() + " recent activities")
                    .data(activities)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving recent activities", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<java.util.Map<String, Object>>>builder()
                            .success(false)
                            .message("Failed to retrieve recent activities: " + e.getMessage())
                            .build());
        }
    }
}
