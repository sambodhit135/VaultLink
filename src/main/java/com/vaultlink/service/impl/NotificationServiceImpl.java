package com.vaultlink.service.impl;

import com.vaultlink.entity.Document;
import com.vaultlink.entity.NotificationLog;
import com.vaultlink.entity.User;
import com.vaultlink.enums.NotificationStatus;
import com.vaultlink.repository.DocumentRepository;
import com.vaultlink.repository.NotificationLogRepository;
import com.vaultlink.repository.UserRepository;
import com.vaultlink.service.NotificationService;
import com.vaultlink.util.EmailTemplateService;
import com.vaultlink.util.ExpiryEngine;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender          javaMailSender;
    private final EmailTemplateService    emailTemplateService;
    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository          userRepository;
    private final DocumentRepository      documentRepository;
    private final ExpiryEngine            expiryEngine;

    private static final String FROM_ADDRESS = "noreply@vaultlink.com";

    // -------------------------------------------------------
    // SEND EXPIRY ALERT
    // -------------------------------------------------------

    /**
     * Sends an HTML expiry alert email to the document owner.
     * On success → saves a SENT NotificationLog.
     * On failure → saves a FAILED NotificationLog, does NOT rethrow
     * (scheduler must continue processing other documents).
     */
    @Override
    public void sendExpiryAlert(Document document, String alertType) {
        // Step 1: Resolve owner details
        String recipientEmail = document.getUser().getEmail();
        String firstName      = document.getUser().getFirstName();

        log.info("Sending {} alert for document: {}", alertType, document.getTitle());

        // Step 2: Calculate days remaining
        long daysLeft = expiryEngine.calculateDaysUntilExpiry(document.getExpiryDate());

        // Step 3: Build HTML content
        String htmlContent = emailTemplateService.buildExpiryAlertEmail(
                firstName,
                document.getTitle(),
                document.getExpiryDate(),
                alertType,
                daysLeft
        );

        // Step 4: Build subject
        String subject = emailTemplateService.getEmailSubject(alertType, document.getTitle());

        // Step 5–7: Compose and send MimeMessage
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);   // true = HTML
            helper.setFrom(FROM_ADDRESS);

            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", recipientEmail);

            // Step 8: Persist SENT notification log
            String urgencyMessage = expiryEngine.getUrgencyMessage(document.getExpiryDate());
            saveNotificationLog(document, recipientEmail, alertType, urgencyMessage, NotificationStatus.SENT);

        } catch (Exception e) {
            // Step 9: Log and persist FAILED log — do NOT rethrow
            log.error("Failed to send email to: {}", recipientEmail, e);
            saveNotificationLog(document, recipientEmail, alertType,
                    "Email delivery failed: " + e.getMessage(), NotificationStatus.FAILED);
        }
    }

    // -------------------------------------------------------
    // SEND WELCOME EMAIL
    // -------------------------------------------------------

    /**
     * Sends a welcome email to a newly registered user.
     * Failures are logged but not rethrown — registration must succeed
     * even if the mail server is temporarily unavailable.
     */
    @Override
    public void sendWelcomeEmail(User user) {
        log.info("sendWelcomeEmail — recipient={}", user.getEmail());

        // Step 1: Build welcome HTML
        String htmlContent = emailTemplateService.buildWelcomeEmail(
                user.getFirstName(),
                user.getEmail()
        );

        // Step 2: Send
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("[VaultLink] Welcome to VaultLink!");
            helper.setText(htmlContent, true);
            helper.setFrom(FROM_ADDRESS);

            javaMailSender.send(mimeMessage);
            log.info("Welcome email sent successfully — recipient={}", user.getEmail());

        } catch (Exception e) {
            // Step 3: Log failure — no NotificationLog for welcome emails
            log.error("Failed to send welcome email — recipient={}, error: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // GET NOTIFICATION HISTORY
    // -------------------------------------------------------

    /**
     * Returns all notification logs for the given user's email,
     * sorted newest-first.
     */
    @Override
    public List<NotificationLog> getNotificationHistory(String email) {
        log.info("getNotificationHistory — email={}", email);

        // Step 1: Verify user exists
        userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("getNotificationHistory — user not found: {}", email);
                    return new RuntimeException("User not found: " + email);
                });

        // Step 2 & 3: Fetch sorted logs
        List<NotificationLog> logs = notificationLogRepository
                .findByRecipientEmailOrderBySentAtDesc(email);

        log.info("Returning {} notification log(s) for email={}", logs.size(), email);
        return logs;
    }

    // -------------------------------------------------------
    // GET NOTIFICATIONS BY DOCUMENT
    // -------------------------------------------------------

    /**
     * Returns all notification logs for a specific document,
     * verifying that the requesting user owns that document.
     */
    @Override
    public List<NotificationLog> getNotificationsByDocument(Long documentId, String email) {
        log.info("getNotificationsByDocument — documentId={}, email={}", documentId, email);

        // Step 1: Find document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("getNotificationsByDocument — document not found: id={}", documentId);
                    return new RuntimeException("Document not found with id: " + documentId);
                });

        // Step 2: Verify ownership
        if (!document.getUser().getEmail().equals(email)) {
            log.warn("getNotificationsByDocument — access denied: user='{}' does not own documentId={}",
                    email, documentId);
            throw new RuntimeException("Access denied: you do not own this document");
        }

        // Step 3: Return logs for this document
        List<NotificationLog> logs = notificationLogRepository.findByDocumentId(documentId);
        log.info("Returning {} notification log(s) for documentId={}", logs.size(), documentId);
        return logs;
    }

    // -------------------------------------------------------
    // PRIVATE HELPER
    // -------------------------------------------------------

    /**
     * Creates and persists a {@link NotificationLog} entry.
     */
    private void saveNotificationLog(
            Document document,
            String recipientEmail,
            String notificationType,
            String message,
            NotificationStatus status
    ) {
        try {
            NotificationLog log = NotificationLog.builder()
                    .document(document)
                    .recipientEmail(recipientEmail)
                    .notificationType(notificationType)
                    .message(message)
                    .status(status)
                    .build();

            notificationLogRepository.save(log);
            NotificationServiceImpl.log.info("Notification logged: {} - {}", notificationType, status);

        } catch (Exception e) {
            NotificationServiceImpl.log.error("Failed to save NotificationLog — documentId={}, error: {}",
                    document.getId(), e.getMessage(), e);
        }
    }
}
