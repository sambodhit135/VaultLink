package com.vaultlink.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * EmailTemplateService — builds HTML email content for all
 * VaultLink notification types (expiry alerts + welcome).
 */
@Slf4j
@Service
public class EmailTemplateService {

    // -------------------------------------------------------
    // Color constants per alert severity
    // -------------------------------------------------------

    private static final String COLOR_EARLY   = "#2196F3";  // Blue  — 90 days
    private static final String COLOR_WARNING = "#FF9800";  // Orange — 30 days
    private static final String COLOR_URGENT  = "#F44336";  // Red    — 7 days
    private static final String COLOR_EXPIRED = "#B71C1C";  // Dark Red — expired
    private static final String COLOR_WELCOME = "#1565C0";  // Deep Blue — welcome

    // -------------------------------------------------------
    // 1. buildExpiryAlertEmail
    // -------------------------------------------------------

    /**
     * Builds a styled HTML expiry alert email based on {@code alertType}.
     *
     * @param firstName     recipient's first name
     * @param documentTitle title of the expiring document
     * @param expiryDate    document's expiry date
     * @param alertType     one of: "90_DAYS", "30_DAYS", "7_DAYS", "EXPIRED"
     * @param daysLeft      pre-calculated days remaining (negative if expired)
     * @return HTML email body as a String
     */
    public String buildExpiryAlertEmail(
            String firstName,
            String documentTitle,
            LocalDate expiryDate,
            String alertType,
            long daysLeft
    ) {
        log.debug("buildExpiryAlertEmail — alertType={}, document='{}', daysLeft={}", alertType, documentTitle, daysLeft);

        String color   = resolveColor(alertType);
        String message = resolveMessage(alertType, documentTitle, expiryDate);

        return buildAlertHtml(color, firstName, message, documentTitle, expiryDate, daysLeft);
    }

    // -------------------------------------------------------
    // 2. buildWelcomeEmail
    // -------------------------------------------------------

    /**
     * Builds a welcome email for newly registered users.
     *
     * @param firstName recipient's first name
     * @param email     recipient's email address
     * @return HTML email body as a String
     */
    public String buildWelcomeEmail(String firstName, String email) {
        log.debug("buildWelcomeEmail — firstName={}, email={}", firstName, email);

        return """
                <html>
                  <body style="font-family: Arial; padding: 20px;">
                    <div style="max-width:600px; margin:auto; border:2px solid %s; border-radius:8px; padding:20px;">
                      <h2 style="color:%s">VaultLink Alert</h2>
                      <p>Dear %s,</p>
                      <p>Welcome to VaultLink! Your account has been created successfully.</p>
                      <div style="background:%s; color:white; padding:10px; border-radius:5px; margin:15px 0;">
                        <strong>Email:</strong> %s<br/>
                      </div>
                      <p>Please log in to VaultLink to take action.</p>
                      <p style="color:gray; font-size:12px;">
                        This is an automated message from VaultLink.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(COLOR_WELCOME, COLOR_WELCOME, firstName, COLOR_WELCOME, email);
    }

    // -------------------------------------------------------
    // 3. getEmailSubject
    // -------------------------------------------------------

    /**
     * Returns the email subject line for a given alert type.
     *
     * @param alertType     one of: "90_DAYS", "30_DAYS", "7_DAYS", "EXPIRED"
     * @param documentTitle title of the document
     * @return subject string, or a generic fallback for unknown types
     */
    public String getEmailSubject(String alertType, String documentTitle) {
        return switch (alertType) {
            case "90_DAYS" -> "[VaultLink] Early Reminder: " + documentTitle + " expires in 90 days";
            case "30_DAYS" -> "[VaultLink] Warning: "        + documentTitle + " expires in 30 days";
            case "7_DAYS"  -> "[VaultLink] URGENT: "         + documentTitle + " expires in 7 days!";
            case "EXPIRED" -> "[VaultLink] EXPIRED: "        + documentTitle + " has expired today";
            default -> {
                log.warn("getEmailSubject — unknown alertType: '{}'", alertType);
                yield "[VaultLink] Document Alert: " + documentTitle;
            }
        };
    }

    // -------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------

    /**
     * Resolves the hex color for a given alertType.
     */
    private String resolveColor(String alertType) {
        return switch (alertType) {
            case "90_DAYS" -> COLOR_EARLY;
            case "30_DAYS" -> COLOR_WARNING;
            case "7_DAYS"  -> COLOR_URGENT;
            case "EXPIRED" -> COLOR_EXPIRED;
            default        -> COLOR_EARLY;
        };
    }

    /**
     * Resolves the body message for a given alertType.
     */
    private String resolveMessage(String alertType, String documentTitle, LocalDate expiryDate) {
        return switch (alertType) {
            case "90_DAYS" -> String.format(
                    "This is an early reminder that your document '%s' will expire in 90 days on %s.",
                    documentTitle, expiryDate);
            case "30_DAYS" -> String.format(
                    "Your document '%s' will expire in 30 days on %s. Please start the renewal process.",
                    documentTitle, expiryDate);
            case "7_DAYS"  -> String.format(
                    "URGENT: Your document '%s' expires in just 7 days on %s. Immediate action required!",
                    documentTitle, expiryDate);
            case "EXPIRED" -> String.format(
                    "Your document '%s' has expired today on %s.",
                    documentTitle, expiryDate);
            default -> {
                log.warn("resolveMessage — unknown alertType: '{}'", alertType);
                yield String.format("Your document '%s' requires your attention (expiry: %s).",
                        documentTitle, expiryDate);
            }
        };
    }

    /**
     * Assembles the full HTML email body using the resolved color and message.
     */
    private String buildAlertHtml(
            String color,
            String firstName,
            String message,
            String documentTitle,
            LocalDate expiryDate,
            long daysLeft
    ) {
        return """
                <html>
                  <body style="font-family: Arial; padding: 20px;">
                    <div style="max-width:600px; margin:auto; border:2px solid %s; border-radius:8px; padding:20px;">
                      <h2 style="color:%s">VaultLink Alert</h2>
                      <p>Dear %s,</p>
                      <p>%s</p>
                      <div style="background:%s; color:white; padding:10px; border-radius:5px; margin:15px 0;">
                        <strong>Document:</strong> %s<br/>
                        <strong>Expiry Date:</strong> %s<br/>
                        <strong>Days Remaining:</strong> %d
                      </div>
                      <p>Please log in to VaultLink to take action.</p>
                      <p style="color:gray; font-size:12px;">
                        This is an automated message from VaultLink.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(color, color, firstName, message, color, documentTitle, expiryDate, daysLeft);
    }
}
