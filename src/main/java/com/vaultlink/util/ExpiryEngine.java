package com.vaultlink.util;

import com.vaultlink.entity.Document;
import com.vaultlink.enums.DocumentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ExpiryEngine — core utility component for document expiry
 * classification, alert scheduling, and urgency messaging.
 *
 * Injectable into any Spring-managed bean via constructor injection.
 */
@Slf4j
@Component
public class ExpiryEngine {

    // -------------------------------------------------------
    // Configurable thresholds (days)
    // -------------------------------------------------------

    public static final long CRITICAL_THRESHOLD    = 7;
    public static final long WARNING_THRESHOLD     = 30;
    public static final long EARLY_ALERT_THRESHOLD = 90;

    // -------------------------------------------------------
    // 1. calculateDaysUntilExpiry
    // -------------------------------------------------------

    /**
     * Calculates the number of days between today and {@code expiryDate}.
     * A negative return value means the document has already expired.
     *
     * @param expiryDate the document's expiry date
     * @return days remaining (negative if already expired)
     */
    public long calculateDaysUntilExpiry(LocalDate expiryDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    // -------------------------------------------------------
    // 2. classifyStatus
    // -------------------------------------------------------

    /**
     * Classifies a document's expiry status based on days remaining.
     *
     * <ul>
     *   <li>daysLeft &lt; 0  → {@link DocumentStatus#EXPIRED}</li>
     *   <li>daysLeft &le; 7  → {@link DocumentStatus#CRITICAL}</li>
     *   <li>daysLeft &le; 30 → {@link DocumentStatus#WARNING}</li>
     *   <li>else             → {@link DocumentStatus#SAFE}</li>
     * </ul>
     *
     * @param expiryDate the document's expiry date
     * @return the classified {@link DocumentStatus}
     */
    public DocumentStatus classifyStatus(LocalDate expiryDate) {
        long daysLeft = calculateDaysUntilExpiry(expiryDate);

        DocumentStatus status;
        if (daysLeft < 0) {
            status = DocumentStatus.EXPIRED;
        } else if (daysLeft <= CRITICAL_THRESHOLD) {
            status = DocumentStatus.CRITICAL;
        } else if (daysLeft <= WARNING_THRESHOLD) {
            status = DocumentStatus.WARNING;
        } else {
            status = DocumentStatus.SAFE;
        }

        log.debug("classifyStatus — expiryDate={}, daysLeft={}, status={}", expiryDate, daysLeft, status);
        return status;
    }

    // -------------------------------------------------------
    // 3. isExpiredToday
    // -------------------------------------------------------

    /**
     * Returns {@code true} if the document expires exactly today.
     *
     * @param expiryDate the document's expiry date
     * @return true if expiryDate equals today's date
     */
    public boolean isExpiredToday(LocalDate expiryDate) {
        return LocalDate.now().equals(expiryDate);
    }

    // -------------------------------------------------------
    // 4. shouldSendAlert
    // -------------------------------------------------------

    /**
     * Returns {@code true} if an alert email should be sent today.
     * Triggers at exactly 90, 30, 7, or 0 days before expiry.
     *
     * @param expiryDate the document's expiry date
     * @return true if today is an alert trigger day
     */
    public boolean shouldSendAlert(LocalDate expiryDate) {
        long daysLeft = calculateDaysUntilExpiry(expiryDate);
        boolean trigger = daysLeft == EARLY_ALERT_THRESHOLD   // 90 days
                || daysLeft == WARNING_THRESHOLD              // 30 days
                || daysLeft == CRITICAL_THRESHOLD             // 7 days
                || daysLeft == 0;                             // expiry day

        if (trigger) {
            log.info("shouldSendAlert — alert triggered for expiryDate={}, daysLeft={}", expiryDate, daysLeft);
        }
        return trigger;
    }

    // -------------------------------------------------------
    // 5. getAlertType
    // -------------------------------------------------------

    /**
     * Returns a string code identifying the alert category,
     * or {@code null} if today is not an alert day.
     *
     * @param expiryDate the document's expiry date
     * @return "90_DAYS", "30_DAYS", "7_DAYS", "EXPIRED", or null
     */
    public String getAlertType(LocalDate expiryDate) {
        long daysLeft = calculateDaysUntilExpiry(expiryDate);

        if (daysLeft == EARLY_ALERT_THRESHOLD) return "90_DAYS";
        if (daysLeft == WARNING_THRESHOLD)     return "30_DAYS";
        if (daysLeft == CRITICAL_THRESHOLD)    return "7_DAYS";
        if (daysLeft == 0)                     return "EXPIRED";
        return null;
    }

    // -------------------------------------------------------
    // 6. getUrgencyMessage
    // -------------------------------------------------------

    /**
     * Builds a human-readable urgency message for the document owner.
     *
     * <ul>
     *   <li>EXPIRED  → "Your document has expired today!"</li>
     *   <li>CRITICAL → "URGENT: Your document expires in X days!"</li>
     *   <li>WARNING  → "Your document expires in X days. Please renew soon."</li>
     *   <li>SAFE     → "Your document expires in X days."</li>
     * </ul>
     *
     * @param expiryDate the document's expiry date
     * @return urgency message string
     */
    public String getUrgencyMessage(LocalDate expiryDate) {
        long daysLeft = calculateDaysUntilExpiry(expiryDate);
        DocumentStatus status = classifyStatus(expiryDate);

        return switch (status) {
            case EXPIRED  -> "Your document has expired today!";
            case CRITICAL -> String.format("URGENT: Your document expires in %d days!", daysLeft);
            case WARNING  -> String.format("Your document expires in %d days. Please renew soon.", daysLeft);
            case SAFE     -> String.format("Your document expires in %d days.", daysLeft);
        };
    }

    // -------------------------------------------------------
    // 7. updateAllDocumentStatuses
    // -------------------------------------------------------

    /**
     * Recalculates and updates the {@link DocumentStatus} for every
     * document in the supplied list. Logs a message whenever a
     * document's status changes.
     *
     * @param documents list of documents to update
     * @return the same list with updated statuses (mutated in-place)
     */
    public List<Document> updateAllDocumentStatuses(List<Document> documents) {
        log.info("updateAllDocumentStatuses — processing {} document(s)", documents.size());

        for (Document document : documents) {
            DocumentStatus currentStatus = document.getDocumentStatus();
            long daysLeft = calculateDaysUntilExpiry(document.getExpiryDate());
            DocumentStatus newStatus = classifyStatus(document.getExpiryDate());

            log.debug("Document '{}' classified as {} ({} days left)", document.getTitle(), newStatus, daysLeft);

            if (currentStatus != newStatus) {
                log.info("Status changed for document '{}': {} → {}",
                        document.getTitle(), currentStatus, newStatus);
                document.setDocumentStatus(newStatus);
            }
        }

        return documents;
    }
}
