package com.vaultlink.scheduler;

import com.vaultlink.entity.Document;
import com.vaultlink.enums.DocumentStatus;
import com.vaultlink.repository.DocumentRepository;
import com.vaultlink.repository.NotificationLogRepository;
import com.vaultlink.service.NotificationService;
import com.vaultlink.util.ExpiryEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentExpiryScheduler {

    private final DocumentRepository       documentRepository;
    private final NotificationService      notificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final ExpiryEngine             expiryEngine;

    // -------------------------------------------------------
    // 1. DAILY EXPIRY CHECK — runs every day at 08:00 AM
    // -------------------------------------------------------

    /**
     * Scans all active documents and sends alert emails for
     * documents that hit a threshold milestone today (90/30/7/0 days).
     * Deduplication prevents duplicate alerts per document per type.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyExpiryCheck() {
        log.info("=== Daily Expiry Check Started: {} ===", LocalDateTime.now());

        try {
            // (b) today's date
            LocalDate today = LocalDate.now();

            // (c) Fetch all active documents with expiryDate >= today
            //     PLUS already-expired active ones for EXPIRED alert
            //     We use two queries and merge them to cover both cases.
            List<Document> upcomingDocs = documentRepository
                    .findByIsActiveTrueAndExpiryDateGreaterThanEqual(today);

            List<Document> expiredToday = documentRepository
                    .findByIsActiveTrue()
                    .stream()
                    .filter(d -> d.getExpiryDate().equals(today))
                    .toList();

            // Merge — upcoming already contains today, so just use upcoming
            List<Document> documentsToCheck = upcomingDocs;

            log.info("Processing {} active documents", documentsToCheck.size());

            int processed  = 0;
            int alertsSent = 0;

            // (d) For each document — check & send alert
            for (Document document : documentsToCheck) {
                processed++;

                // (i) Should we send any alert today?
                if (!expiryEngine.shouldSendAlert(document.getExpiryDate())) {
                    continue;
                }

                // (ii) Which alert type?
                String alertType = expiryEngine.getAlertType(document.getExpiryDate());
                if (alertType == null) {
                    continue;
                }

                // (iii) Deduplication — already sent this alertType for this doc?
                boolean alreadySent = notificationLogRepository
                        .existsByDocumentIdAndNotificationType(document.getId(), alertType);

                if (alreadySent) {
                    log.debug("Skipping duplicate alert — documentId={}, alertType={}", document.getId(), alertType);
                    continue;
                }

                // (iv) Send the alert (internally saves NotificationLog SENT/FAILED)
                notificationService.sendExpiryAlert(document, alertType);
                alertsSent++;

                // (v) Log success
                log.info("Alert sent: {} → {}", document.getTitle(), alertType);
            }

            // (e) Bulk status refresh
            int statusUpdates = refreshDocumentStatuses();

            // (f) Summary log
            log.info("=== Daily Check Complete. Processed: {}, Alerts: {} ===", processed, alertsSent);

        } catch (Exception e) {
            // (g) Global safety net — scheduler thread must not die
            log.error("Daily expiry check failed with unexpected error: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // 2. MIDNIGHT STATUS REFRESH — runs every day at 00:00
    // -------------------------------------------------------

    /**
     * Recalculates and persists the {@link DocumentStatus} for every
     * active document. Runs at midnight so the DB always reflects
     * the correct status at the start of each day.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runStatusUpdateOnly() {
        log.info("=== Starting midnight status refresh at {} ===", LocalDateTime.now());

        try {
            int updated = refreshDocumentStatuses();
            log.info("=== Midnight status refresh complete — {} document(s) updated ===", updated);
        } catch (Exception e) {
            log.error("Midnight status refresh failed: {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // 3. MANUAL TRIGGER — for testing / admin use
    // -------------------------------------------------------

    /**
     * Executes the same logic as {@link #runDailyExpiryCheck()} on demand.
     * Not annotated with {@code @Scheduled} — call directly from a test
     * endpoint or admin controller.
     *
     * @return a summary string of what was processed
     */
    public String triggerManualCheck() {
        log.info("=== Manual expiry check triggered at {} ===", LocalDateTime.now());

        int processed  = 0;
        int alertsSent = 0;
        int statusUpdates = 0;

        try {
            LocalDate today = LocalDate.now();

            List<Document> documentsToCheck = documentRepository
                    .findByIsActiveTrueAndExpiryDateGreaterThanEqual(today);

            for (Document document : documentsToCheck) {
                processed++;

                if (!expiryEngine.shouldSendAlert(document.getExpiryDate())) {
                    continue;
                }

                String alertType = expiryEngine.getAlertType(document.getExpiryDate());
                if (alertType == null) {
                    continue;
                }

                boolean alreadySent = notificationLogRepository
                        .existsByDocumentIdAndNotificationType(document.getId(), alertType);

                if (alreadySent) {
                    log.debug("Manual check — skipping duplicate: documentId={}, alertType={}",
                            document.getId(), alertType);
                    continue;
                }

                notificationService.sendExpiryAlert(document, alertType);
                alertsSent++;
                log.info("Manual alert sent — document='{}', alertType={}", document.getTitle(), alertType);
            }

            statusUpdates = refreshDocumentStatuses();

        } catch (Exception e) {
            log.error("Manual check failed: {}", e.getMessage(), e);
            return "Manual check FAILED: " + e.getMessage();
        }

        String summary = String.format(
                "Manual check complete — processed: %d document(s), alerts sent: %d, statuses updated: %d",
                processed, alertsSent, statusUpdates);

        log.info("=== {} ===", summary);
        return summary;
    }

    // -------------------------------------------------------
    // PRIVATE HELPER
    // -------------------------------------------------------

    /**
     * Recalculates {@link DocumentStatus} for all active documents.
     * Persists the document only if the status actually changed.
     *
     * @return the number of documents whose status was updated
     */
    private int refreshDocumentStatuses() {
        List<Document> allActive = documentRepository.findByIsActiveTrue();
        int updated = 0;

        for (Document document : allActive) {
            DocumentStatus currentStatus = document.getDocumentStatus();
            DocumentStatus newStatus     = expiryEngine.classifyStatus(document.getExpiryDate());

            if (currentStatus != newStatus) {
                log.info("Status changed — documentId={}, title='{}', {} → {}",
                        document.getId(), document.getTitle(), currentStatus, newStatus);
                document.setDocumentStatus(newStatus);
                documentRepository.save(document);
                updated++;
            }
        }

        log.debug("refreshDocumentStatuses — checked: {}, updated: {}", allActive.size(), updated);
        return updated;
    }
}
