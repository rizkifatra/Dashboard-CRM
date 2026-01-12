package com.example.backend.controller;

import com.example.backend.model.ApiResponse;
import com.example.backend.model.EmailReminder;
import com.example.backend.service.EmailReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for email follow-up reminders
 */
@RestController
@RequestMapping("/api/email-reminders")
@Slf4j
@RequiredArgsConstructor
public class EmailReminderController {

    private final EmailReminderService emailReminderService;

    /**
     * Get all email reminders that need follow-up
     * 
     * @return List of email reminders sorted by urgency
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailReminder>>> getEmailReminders() {
        try {
            log.info("GET /api/email-reminders - Fetching email follow-up reminders");

            List<EmailReminder> reminders = emailReminderService.getEmailReminders();

            log.info("Successfully fetched {} email reminders", reminders.size());
            return ResponseEntity.ok(ApiResponse.success(reminders));

        } catch (Exception e) {
            log.error("Error fetching email reminders", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to fetch email reminders: " + e.getMessage()));
        }
    }

    /**
     * Get count of reminders by urgency level
     * 
     * @return Map with counts by urgency (total, critical, medium, low)
     */
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getReminderCounts() {
        try {
            log.info("GET /api/email-reminders/counts - Fetching reminder counts");

            Map<String, Integer> counts = emailReminderService.getReminderCounts();

            log.info("Reminder counts: {}", counts);
            return ResponseEntity.ok(ApiResponse.success(counts));

        } catch (Exception e) {
            log.error("Error fetching reminder counts", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to fetch reminder counts: " + e.getMessage()));
        }
    }
}
