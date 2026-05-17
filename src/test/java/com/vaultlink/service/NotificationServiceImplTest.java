package com.vaultlink.service;

import com.vaultlink.entity.Document;
import com.vaultlink.entity.NotificationLog;
import com.vaultlink.entity.User;
import com.vaultlink.enums.NotificationStatus;
import com.vaultlink.repository.NotificationLogRepository;
import com.vaultlink.repository.UserRepository;
import com.vaultlink.service.impl.NotificationServiceImpl;
import com.vaultlink.util.EmailTemplateService;
import com.vaultlink.util.ExpiryEngine;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpiryEngine expiryEngine;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Document testDocument;
    private MimeMessage mockMimeMessage;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@test.com");
        testUser.setFirstName("John");

        testDocument = new Document();
        testDocument.setId(10L);
        testDocument.setTitle("Test Passport");
        testDocument.setExpiryDate(LocalDate.now().plusDays(7));
        testDocument.setUser(testUser);

        // We use lenient to allow mockMimeMessage to be used in multiple tests
        // Some tests might not invoke the email sending part
        mockMimeMessage = mock(MimeMessage.class);
        lenient().when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Nested
    @DisplayName("SendExpiryAlert Tests")
    class SendExpiryAlertTests {

        @Test
        @DisplayName("Should successfully send expiry alert and save SENT log")
        void test_sendExpiryAlert_success() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(7L);
            when(expiryEngine.getUrgencyMessage(any())).thenReturn("Expires in 7 days");

            notificationService.sendExpiryAlert(testDocument, "7_DAYS");

            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
            verify(notificationLogRepository, times(1)).save(any(NotificationLog.class));
        }

        @Test
        @DisplayName("Should log successful status after sending email")
        void test_sendExpiryAlert_logsSuccessStatus() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(7L);
            when(expiryEngine.getUrgencyMessage(any())).thenReturn("Expires in 7 days");

            notificationService.sendExpiryAlert(testDocument, "7_DAYS");

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());

            NotificationLog savedLog = captor.getValue();
            assertNotNull(savedLog);
            assertEquals(NotificationStatus.SENT, savedLog.getStatus());
        }

        @Test
        @DisplayName("Should log failure without throwing exception on MailException")
        void test_sendExpiryAlert_mailException_logsFailure() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(7L);
            
            // Note: MailException is abstract, so we mock or use an anonymous subclass
            doThrow(new MailException("SMTP error") {}).when(javaMailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> notificationService.sendExpiryAlert(testDocument, "7_DAYS"));

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());

            NotificationLog savedLog = captor.getValue();
            assertNotNull(savedLog);
            assertEquals(NotificationStatus.FAILED, savedLog.getStatus());
        }

        @Test
        @DisplayName("Should process all alert types successfully")
        void test_sendExpiryAlert_allAlertTypes() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(10L);
            when(expiryEngine.getUrgencyMessage(any())).thenReturn("Generic Urgency");

            String[] alertTypes = {"90_DAYS", "30_DAYS", "7_DAYS", "EXPIRED"};

            for (String type : alertTypes) {
                assertDoesNotThrow(() -> notificationService.sendExpiryAlert(testDocument, type));
            }

            verify(emailTemplateService, times(4)).buildExpiryAlertEmail(any(), any(), any(), any(), anyLong());
            verify(javaMailSender, times(4)).send(any(MimeMessage.class));
            verify(notificationLogRepository, times(4)).save(any(NotificationLog.class));
        }
    }

    @Nested
    @DisplayName("GetNotificationHistory Tests")
    class GetNotificationHistoryTests {

        @Test
        @DisplayName("Should successfully retrieve notification history")
        void test_getNotificationHistory_success() {
            NotificationLog log1 = new NotificationLog();
            NotificationLog log2 = new NotificationLog();
            NotificationLog log3 = new NotificationLog();

            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(notificationLogRepository.findByRecipientEmailOrderBySentAtDesc(testUser.getEmail()))
                    .thenReturn(List.of(log1, log2, log3));

            List<NotificationLog> result = notificationService.getNotificationHistory(testUser.getEmail());

            assertNotNull(result);
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("Should return empty list when no notifications exist")
        void test_getNotificationHistory_emptyList() {
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(notificationLogRepository.findByRecipientEmailOrderBySentAtDesc(testUser.getEmail()))
                    .thenReturn(Collections.emptyList());

            List<NotificationLog> result = notificationService.getNotificationHistory(testUser.getEmail());

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception if user is not found")
        void test_getNotificationHistory_userNotFound() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> notificationService.getNotificationHistory("unknown@test.com"));
            verify(notificationLogRepository, never()).findByRecipientEmailOrderBySentAtDesc(anyString());
        }
    }

    @Nested
    @DisplayName("ExpiryEngine Integration Tests")
    class ExpiryEngineIntegrationTests {

        @Test
        @DisplayName("Should pass correct alert type to email template")
        void test_correctAlertTypePassedToTemplate() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(-1L);
            when(expiryEngine.getUrgencyMessage(any())).thenReturn("Document has expired");

            notificationService.sendExpiryAlert(testDocument, "EXPIRED");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(emailTemplateService, times(1)).buildExpiryAlertEmail(
                    any(), any(), any(), captor.capture(), anyLong()
            );

            assertEquals("EXPIRED", captor.getValue());
        }

        @Test
        @DisplayName("Should save NotificationLog with correct Document association")
        void test_notificationLogSavedWithCorrectDocumentId() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(7L);
            when(expiryEngine.getUrgencyMessage(any())).thenReturn("Expires in 7 days");

            notificationService.sendExpiryAlert(testDocument, "7_DAYS");

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());

            NotificationLog savedLog = captor.getValue();
            assertNotNull(savedLog);
            assertNotNull(savedLog.getDocument());
            assertEquals(testDocument.getId(), savedLog.getDocument().getId());
        }

        @Test
        @DisplayName("Should save NotificationLog with correct recipient email")
        void test_notificationLogSavedWithCorrectEmail() {
            when(emailTemplateService.buildExpiryAlertEmail(any(), any(), any(), any(), anyLong()))
                    .thenReturn("<html>test email</html>");
            when(emailTemplateService.getEmailSubject(any(), any()))
                    .thenReturn("Test Subject");
            when(expiryEngine.calculateDaysUntilExpiry(any())).thenReturn(7L);
            when(expiryEngine.getUrgencyMessage(any())).thenReturn("Expires in 7 days");

            notificationService.sendExpiryAlert(testDocument, "7_DAYS");

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());

            NotificationLog savedLog = captor.getValue();
            assertNotNull(savedLog);
            assertEquals(testUser.getEmail(), savedLog.getRecipientEmail());
        }
    }
}
