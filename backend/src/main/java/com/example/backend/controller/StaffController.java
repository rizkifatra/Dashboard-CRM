package com.example.backend.controller;

import com.example.backend.model.ApiResponse;
import com.example.backend.model.Staff;
import com.example.backend.service.D365StaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for staff/system user operations
 */
@Slf4j
@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class StaffController {

    private final D365StaffService staffService;

    public StaffController(D365StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * Get all staff members (unfiltered - includes all staff regardless of job
     * title)
     * Use this endpoint for Staff Management page to show all staff
     * 
     * @param top               Optional limit for number of results (default: 100)
     * @param select            Optional comma-separated list of fields to select
     * @param includeEmailStats Optional flag to include email statistics (default:
     *                          false)
     * @param fromDate          Optional start date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @param toDate            Optional end date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @return List of all staff members
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaffUnfiltered(
            @RequestParam(required = false, defaultValue = "100") Integer top,
            @RequestParam(required = false) String select,
            @RequestParam(required = false, defaultValue = "false") Boolean includeEmailStats,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        log.info(
                "GET /api/staff/all (unfiltered) - Top: {}, Select: {}, IncludeEmailStats: {}, FromDate: {}, ToDate: {}",
                top, select, includeEmailStats, fromDate, toDate);

        try {
            List<Staff> staff = staffService.getAllStaffUnfiltered(top, select, includeEmailStats, fromDate, toDate);

            ApiResponse<List<Staff>> response = ApiResponse.<List<Staff>>builder()
                    .success(true)
                    .message("Successfully retrieved " + staff.size() + " staff members (unfiltered)")
                    .data(staff)
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving all staff (unfiltered)", e);

            ApiResponse<List<Staff>> response = ApiResponse.<List<Staff>>builder()
                    .success(false)
                    .message("Failed to retrieve staff: " + e.getMessage())
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get tracked staff members (filtered by configured job titles)
     * Use this endpoint for Dashboard/Ranking page to show only tracked roles
     * 
     * @param top               Optional limit for number of results (default: 100)
     * @param select            Optional comma-separated list of fields to select
     * @param includeEmailStats Optional flag to include email statistics (default:
     *                          false)
     * @param fromDate          Optional start date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @param toDate            Optional end date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @return List of tracked staff members only
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaff(
            @RequestParam(required = false, defaultValue = "100") Integer top,
            @RequestParam(required = false) String select,
            @RequestParam(required = false, defaultValue = "false") Boolean includeEmailStats,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        log.info("GET /api/staff (filtered) - Top: {}, Select: {}, IncludeEmailStats: {}, FromDate: {}, ToDate: {}",
                top, select, includeEmailStats, fromDate, toDate);

        try {
            List<Staff> staff = staffService.getAllStaff(top, select, includeEmailStats, fromDate, toDate);

            ApiResponse<List<Staff>> response = ApiResponse.<List<Staff>>builder()
                    .success(true)
                    .message("Successfully retrieved " + staff.size() + " tracked staff members")
                    .data(staff)
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving tracked staff", e);

            ApiResponse<List<Staff>> response = ApiResponse.<List<Staff>>builder()
                    .success(false)
                    .message("Failed to retrieve staff: " + e.getMessage())
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get a specific staff member by ID
     * 
     * @param id                The system user ID
     * @param includeEmailStats Optional flag to include email statistics (default:
     *                          false)
     * @param fromDate          Optional start date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @param toDate            Optional end date for email statistics filter (ISO
     *                          format: YYYY-MM-DD)
     * @return Staff member details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> getStaffById(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") Boolean includeEmailStats,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        log.info("GET /api/staff/{} - IncludeEmailStats: {}, FromDate: {}, ToDate: {}",
                id, includeEmailStats, fromDate, toDate);

        try {
            Optional<Staff> staff = staffService.getStaffById(id, includeEmailStats, fromDate, toDate);

            if (staff.isPresent()) {
                ApiResponse<Staff> response = ApiResponse.<Staff>builder()
                        .success(true)
                        .message("Staff member found")
                        .data(staff.get())
                        .build();

                return ResponseEntity.ok(response);
            } else {
                ApiResponse<Staff> response = ApiResponse.<Staff>builder()
                        .success(false)
                        .message("Staff member not found with ID: " + id)
                        .build();

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error retrieving staff by ID", e);

            ApiResponse<Staff> response = ApiResponse.<Staff>builder()
                    .success(false)
                    .message("Failed to retrieve staff: " + e.getMessage())
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Search for staff by name or email (accepts searchTerm parameter for unified
     * search)
     * This is the UNFILTERED search - shows all staff regardless of job title
     * 
     * @param searchTerm Combined search term for name or email (partial match)
     * @param name       Optional name to search for (partial match)
     * @param email      Optional email to search for (partial match)
     * @param top        Optional limit for number of results (default: 50)
     * @return List of matching staff members
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Staff>>> searchStaff(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false, defaultValue = "50") Integer top) {

        log.info("GET /api/staff/search (unfiltered) - SearchTerm: {}, Name: {}, Email: {}, Top: {}",
                searchTerm, name, email, top);

        try {
            // If searchTerm is provided, use it for both name and email search
            String searchName = (searchTerm != null && !searchTerm.isBlank()) ? searchTerm : name;
            String searchEmail = (searchTerm != null && !searchTerm.isBlank()) ? searchTerm : email;

            List<Staff> staff = staffService.searchStaffUnfiltered(searchName, searchEmail, top);

            ApiResponse<List<Staff>> response = ApiResponse.<List<Staff>>builder()
                    .success(true)
                    .message("Found " + staff.size() + " staff members (unfiltered)")
                    .data(staff)
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching staff (unfiltered)", e);

            ApiResponse<List<Staff>> response = ApiResponse.<List<Staff>>builder()
                    .success(false)
                    .message("Failed to search staff: " + e.getMessage())
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get count of active staff members
     * 
     * @return Count of active staff
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getStaffCount() {
        log.info("GET /api/staff/count");

        try {
            int count = staffService.getStaffCount();

            ApiResponse<Integer> response = ApiResponse.<Integer>builder()
                    .success(true)
                    .message("Active staff count retrieved successfully")
                    .data(count)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving staff count", e);

            ApiResponse<Integer> response = ApiResponse.<Integer>builder()
                    .success(false)
                    .message("Failed to retrieve staff count: " + e.getMessage())
                    .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
