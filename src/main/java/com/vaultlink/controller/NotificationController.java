package com.vaultlink.controller;

import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.entity.NotificationLog;
import com.vaultlink.entity.User;
import com.vaultlink.repository.UserRepository;
import com.vaultlink.scheduler.DocumentExpiryScheduler;
import com.vaultlink.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService        notificationService;
    private final DocumentExpiryScheduler    documentExpiryScheduler;
    private final UserRepository             userRepository;

    // -------------------------------------------------------
    // HELPER — resolve authenticated user's email from JWT
    // -------------------------------------------------------

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    // -------------------------------------------------------
    // 1. GET /api/notifications
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns all notification logs for the currently authenticated user,
     * sorted newest-first.
     */
    @GetMapping
    public ResponseEntity<List<NotificationLog>> getNotificationHistory() {
        String email = getCurrentUserEmail();
        log.info("GET /api/notifications — fetching notification history for: {}", email);
        List<NotificationLog> logs = notificationService.getNotificationHistory(email);
        return ResponseEntity.ok(logs);
    }

    // -------------------------------------------------------
    // 2. GET /api/notifications/document/{documentId}
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns all notification logs for a specific document.
     * Only the document owner can view its notification history.
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<NotificationLog>> getNotificationsByDocument(
            @PathVariable Long documentId
    ) {
        String email = getCurrentUserEmail();
        log.info("GET /api/notifications/document/{} — fetching notifications for document: {}",
                documentId, documentId);
        List<NotificationLog> logs = notificationService.getNotificationsByDocument(documentId, email);
        return ResponseEntity.ok(logs);
    }

    // -------------------------------------------------------
    // 3. POST /api/notifications/trigger-check
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Manually triggers the same logic as the daily expiry scheduler.
     * Useful for testing without waiting for the 08:00 AM cron job.
     * Returns a summary of what was processed.
     */
    @PostMapping("/trigger-check")
    public ResponseEntity<ApiResponse> triggerManualCheck() {
        String email = getCurrentUserEmail();
        log.info("POST /api/notifications/trigger-check — manual expiry check triggered by: {}", email);
        String summary = documentExpiryScheduler.triggerManualCheck();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // -------------------------------------------------------
    // 4. POST /api/notifications/test-email?email=...
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Sends a test welcome email to the specified address.
     * The target user must exist in the database.
     * Useful for verifying SMTP configuration is working.
     */
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse> sendTestEmail(@RequestParam String email) {
        log.info("POST /api/notifications/test-email — sending test email to: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("test-email — user not found: {}", email);
                    return new RuntimeException("User not found: " + email);
                });

        notificationService.sendWelcomeEmail(user);
        log.info("Test email dispatched to: {}", email);

        return ResponseEntity.ok(ApiResponse.success("Test email sent to: " + email));
    }
}
