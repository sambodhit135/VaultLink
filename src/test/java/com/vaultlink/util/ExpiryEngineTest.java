package com.vaultlink.util;

import com.vaultlink.entity.Document;
import com.vaultlink.enums.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExpiryEngineTest {

    private ExpiryEngine expiryEngine;

    @BeforeEach
    void setUp() {
        expiryEngine = new ExpiryEngine();
    }

    @Nested
    @DisplayName("Calculate Days Until Expiry Tests")
    class CalculateDaysUntilExpiryTests {

        @Test
        @DisplayName("Should return 30 when expiry is 30 days in the future")
        void test_daysUntilExpiry_futureDate() {
            LocalDate expiryDate = LocalDate.now().plusDays(30);
            long result = expiryEngine.calculateDaysUntilExpiry(expiryDate);
            assertEquals(30, result);
        }

        @Test
        @DisplayName("Should return 0 when expiry is today")
        void test_daysUntilExpiry_today() {
            LocalDate expiryDate = LocalDate.now();
            long result = expiryEngine.calculateDaysUntilExpiry(expiryDate);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should return negative when expiry is in the past")
        void test_daysUntilExpiry_pastDate() {
            LocalDate expiryDate = LocalDate.now().minusDays(5);
            long result = expiryEngine.calculateDaysUntilExpiry(expiryDate);
            assertTrue(result < 0);
            assertEquals(-5, result);
        }

        @Test
        @DisplayName("Should return 365 when expiry is exactly one year away")
        void test_daysUntilExpiry_exactlyOneYear() {
            LocalDate expiryDate = LocalDate.now().plusDays(365);
            long result = expiryEngine.calculateDaysUntilExpiry(expiryDate);
            assertEquals(365, result);
        }
    }

    @Nested
    @DisplayName("Classify Status Tests")
    class ClassifyStatusTests {

        @Test
        @DisplayName("Should classify as SAFE when 60 days away")
        void test_classifyStatus_SAFE() {
            LocalDate expiryDate = LocalDate.now().plusDays(60);
            DocumentStatus result = expiryEngine.classifyStatus(expiryDate);
            assertEquals(DocumentStatus.SAFE, result);
        }

        @Test
        @DisplayName("Should classify as WARNING when 20 days away")
        void test_classifyStatus_WARNING() {
            LocalDate expiryDate = LocalDate.now().plusDays(20);
            DocumentStatus result = expiryEngine.classifyStatus(expiryDate);
            assertEquals(DocumentStatus.WARNING, result);
        }

        @Test
        @DisplayName("Should classify as CRITICAL when 5 days away")
        void test_classifyStatus_CRITICAL() {
            LocalDate expiryDate = LocalDate.now().plusDays(5);
            DocumentStatus result = expiryEngine.classifyStatus(expiryDate);
            assertEquals(DocumentStatus.CRITICAL, result);
        }

        @Test
        @DisplayName("Should classify as EXPIRED when in the past")
        void test_classifyStatus_EXPIRED() {
            LocalDate expiryDate = LocalDate.now().minusDays(1);
            DocumentStatus result = expiryEngine.classifyStatus(expiryDate);
            assertEquals(DocumentStatus.EXPIRED, result);
        }

        @Test
        @DisplayName("Should classify as WARNING exactly at 30 days threshold")
        void test_classifyStatus_exactlyWarningThreshold() {
            LocalDate expiryDate = LocalDate.now().plusDays(30);
            DocumentStatus result = expiryEngine.classifyStatus(expiryDate);
            assertEquals(DocumentStatus.WARNING, result);
        }

        @Test
        @DisplayName("Should classify as CRITICAL exactly at 7 days threshold")
        void test_classifyStatus_exactlyCriticalThreshold() {
            LocalDate expiryDate = LocalDate.now().plusDays(7);
            DocumentStatus result = expiryEngine.classifyStatus(expiryDate);
            assertEquals(DocumentStatus.CRITICAL, result);
        }
    }

    @Nested
    @DisplayName("Should Send Alert Tests")
    class ShouldSendAlertTests {

        @Test
        @DisplayName("Should trigger alert at exactly 90 days")
        void test_shouldSendAlert_at90Days() {
            LocalDate expiryDate = LocalDate.now().plusDays(90);
            boolean result = expiryEngine.shouldSendAlert(expiryDate);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should trigger alert at exactly 30 days")
        void test_shouldSendAlert_at30Days() {
            LocalDate expiryDate = LocalDate.now().plusDays(30);
            boolean result = expiryEngine.shouldSendAlert(expiryDate);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should trigger alert at exactly 7 days")
        void test_shouldSendAlert_at7Days() {
            LocalDate expiryDate = LocalDate.now().plusDays(7);
            boolean result = expiryEngine.shouldSendAlert(expiryDate);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should trigger alert exactly on expiry day")
        void test_shouldSendAlert_atExpiry() {
            LocalDate expiryDate = LocalDate.now();
            boolean result = expiryEngine.shouldSendAlert(expiryDate);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should not trigger alert on random day (e.g. 45 days)")
        void test_shouldSendAlert_randomDay() {
            LocalDate expiryDate = LocalDate.now().plusDays(45);
            boolean result = expiryEngine.shouldSendAlert(expiryDate);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Get Alert Type Tests")
    class GetAlertTypeTests {

        @Test
        @DisplayName("Should return 90_DAYS when 90 days left")
        void test_getAlertType_90Days() {
            LocalDate expiryDate = LocalDate.now().plusDays(90);
            String result = expiryEngine.getAlertType(expiryDate);
            assertEquals("90_DAYS", result);
        }

        @Test
        @DisplayName("Should return 30_DAYS when 30 days left")
        void test_getAlertType_30Days() {
            LocalDate expiryDate = LocalDate.now().plusDays(30);
            String result = expiryEngine.getAlertType(expiryDate);
            assertEquals("30_DAYS", result);
        }

        @Test
        @DisplayName("Should return 7_DAYS when 7 days left")
        void test_getAlertType_7Days() {
            LocalDate expiryDate = LocalDate.now().plusDays(7);
            String result = expiryEngine.getAlertType(expiryDate);
            assertEquals("7_DAYS", result);
        }

        @Test
        @DisplayName("Should return EXPIRED when 0 days left")
        void test_getAlertType_expired() {
            LocalDate expiryDate = LocalDate.now();
            String result = expiryEngine.getAlertType(expiryDate);
            assertEquals("EXPIRED", result);
        }

        @Test
        @DisplayName("Should return null for non-alert days")
        void test_getAlertType_other() {
            LocalDate expiryDate = LocalDate.now().plusDays(15);
            String result = expiryEngine.getAlertType(expiryDate);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Update All Document Statuses Tests")
    class UpdateAllDocumentStatusesTests {

        @Test
        @DisplayName("Should update list of documents with mixed expiry dates")
        void test_updateAllStatuses_mixedDates() {
            Document doc1 = new Document();
            doc1.setExpiryDate(LocalDate.now().plusDays(60));

            Document doc2 = new Document();
            doc2.setExpiryDate(LocalDate.now().plusDays(20));

            Document doc3 = new Document();
            doc3.setExpiryDate(LocalDate.now().plusDays(3));

            Document doc4 = new Document();
            doc4.setExpiryDate(LocalDate.now().minusDays(1));

            List<Document> documents = List.of(doc1, doc2, doc3, doc4);
            
            List<Document> result = expiryEngine.updateAllDocumentStatuses(documents);

            assertEquals(4, result.size());
            assertEquals(DocumentStatus.SAFE, doc1.getDocumentStatus());
            assertEquals(DocumentStatus.WARNING, doc2.getDocumentStatus());
            assertEquals(DocumentStatus.CRITICAL, doc3.getDocumentStatus());
            assertEquals(DocumentStatus.EXPIRED, doc4.getDocumentStatus());
        }

        @Test
        @DisplayName("Should handle empty document list safely")
        void test_updateAllStatuses_emptyList() {
            List<Document> documents = new ArrayList<>();
            List<Document> result = expiryEngine.updateAllDocumentStatuses(documents);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
