package com.vaultlink.service;

import com.vaultlink.dto.request.DocumentRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.DocumentResponse;
import com.vaultlink.dto.response.ExpiryDashboardResponse;
import com.vaultlink.entity.Category;
import com.vaultlink.entity.Document;
import com.vaultlink.entity.User;
import com.vaultlink.enums.DocumentStatus;
import com.vaultlink.repository.CategoryRepository;
import com.vaultlink.repository.DocumentRepository;
import com.vaultlink.repository.UserRepository;
import com.vaultlink.service.impl.DocumentServiceImpl;
import com.vaultlink.util.ExpiryEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CacheService cacheService;



    @InjectMocks
    private DocumentServiceImpl documentService;

    private User testUser;
    private Category testCategory;
    private Document testDocument;
    private final String userEmail = "test@vaultlink.com";

    protected User createTestUser() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@vaultlink.com");
        user.setPassword("hashedpassword123");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    protected Document createTestDocument(User user, LocalDate expiryDate) {
        Document doc = new Document();
        doc.setTitle("Test Document");
        doc.setDescription("Test Description");
        doc.setIssueDate(LocalDate.now().minusYears(1));
        doc.setExpiryDate(expiryDate);
        doc.setIsActive(true);
        doc.setUser(user);
        doc.setCreatedAt(LocalDateTime.now());
        return doc;
    }

    protected Category createTestCategory() {
        Category cat = new Category();
        cat.setName("Test Category");
        cat.setDescription("Test Category Description");
        cat.setCreatedAt(LocalDateTime.now());
        return cat;
    }

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testUser.setId(1L);

        testCategory = createTestCategory();
        testCategory.setId(1L);
        testCategory.setName("Identity");

        testDocument = createTestDocument(testUser, LocalDate.now().plusDays(60));
        testDocument.setId(1L);
        testDocument.setTitle("Test Passport");
        testDocument.setCategory(testCategory);
        testDocument.setDocumentStatus(DocumentStatus.SAFE);
    }

    @Nested
    @DisplayName("GetAllDocuments Tests")
    class GetAllDocumentsTests {

        @Test
        @DisplayName("Should return list of documents when successful")
        void test_getAllDocuments_success() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
            when(documentRepository.findByUserIdAndIsActiveTrue(any())).thenReturn(List.of(testDocument));

            List<DocumentResponse> result = documentService.getAllDocuments(userEmail);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Test Passport", result.get(0).getTitle());
            verify(documentRepository, times(1)).findByUserIdAndIsActiveTrue(any());
        }

        @Test
        @DisplayName("Should throw exception when user is not found")
        void test_getAllDocuments_userNotFound() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> documentService.getAllDocuments(userEmail));
        }

        @Test
        @DisplayName("Should return empty list when user has no documents")
        void test_getAllDocuments_emptyList() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
            when(documentRepository.findByUserIdAndIsActiveTrue(any())).thenReturn(Collections.emptyList());

            List<DocumentResponse> result = documentService.getAllDocuments(userEmail);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("CreateDocument Tests")
    class CreateDocumentTests {

        private DocumentRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new DocumentRequest();
            request.setTitle("New Document");
            request.setCategoryId(1L);
            request.setExpiryDate(LocalDate.now().plusDays(30));
        }

        @Test
        @DisplayName("Should successfully create a document")
        void test_createDocument_success() {
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);


            DocumentResponse response = documentService.createDocument(request, userEmail);

            assertNotNull(response);
            assertEquals(testDocument.getTitle(), response.getTitle());
            verify(documentRepository, times(1)).save(any(Document.class));
            verify(cacheService, times(1)).evictAllUserCaches(userEmail);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void test_createDocument_categoryNotFound() {
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> documentService.createDocument(request, userEmail));
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should calculate document status correctly as CRITICAL")
        void test_createDocument_statusCalculatedCorrectly() {
            request.setExpiryDate(LocalDate.now().plusDays(5));
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            
            Document critDoc = createTestDocument(testUser, request.getExpiryDate());
            critDoc.setDocumentStatus(DocumentStatus.CRITICAL);
            when(documentRepository.save(any(Document.class))).thenReturn(critDoc);


            DocumentResponse response = documentService.createDocument(request, userEmail);

            assertNotNull(response);
            assertEquals("CRITICAL", response.getDocumentStatus());
        }
    }

    @Nested
    @DisplayName("UpdateDocument Tests")
    class UpdateDocumentTests {

        private DocumentRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new DocumentRequest();
            request.setTitle("Updated Title");
            request.setCategoryId(1L);
            request.setExpiryDate(LocalDate.now().plusDays(30));
        }

        @Test
        @DisplayName("Should successfully update a document")
        void test_updateDocument_success() {
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

            DocumentResponse response = documentService.updateDocument(1L, request, userEmail);

            assertNotNull(response);
            verify(documentRepository, times(1)).save(any(Document.class));
            verify(cacheService, times(1)).evictAllUserCaches(userEmail);
        }

        @Test
        @DisplayName("Should throw exception when user attempts to update another user's document")
        void test_updateDocument_accessDenied() {
            User otherUser = createTestUser();
            otherUser.setId(2L);
            otherUser.setEmail("other@vaultlink.com");
            testDocument.setUser(otherUser);

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            assertThrows(RuntimeException.class, () -> documentService.updateDocument(1L, request, userEmail));
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void test_updateDocument_documentNotFound() {
            when(documentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> documentService.updateDocument(1L, request, userEmail));
        }
    }

    @Nested
    @DisplayName("DeleteDocument Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should logically delete document successfully")
        void test_deleteDocument_success() {
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            ApiResponse response = documentService.deleteDocument(1L, userEmail);

            assertNotNull(response);
            assertTrue(response.getSuccess());
            verify(documentRepository, times(1)).save(testDocument);
            assertFalse(testDocument.getIsActive());
            verify(cacheService, times(1)).evictAllUserCaches(userEmail);
        }

        @Test
        @DisplayName("Should throw exception when deleting another user's document")
        void test_deleteDocument_accessDenied() {
            User otherUser = createTestUser();
            otherUser.setId(2L);
            otherUser.setEmail("other@vaultlink.com");
            testDocument.setUser(otherUser);

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            assertThrows(RuntimeException.class, () -> documentService.deleteDocument(1L, userEmail));
        }
    }

    @Nested
    @DisplayName("GetExpiryDashboard Tests")
    class GetExpiryDashboardTests {

        @Test
        @DisplayName("Should calculate document counts correctly")
        void test_getExpiryDashboard_correctCounts() {
            Document docCrit1 = createTestDocument(testUser, LocalDate.now().plusDays(2));
            docCrit1.setDocumentStatus(DocumentStatus.CRITICAL);

            Document docCrit2 = createTestDocument(testUser, LocalDate.now().plusDays(3));
            docCrit2.setDocumentStatus(DocumentStatus.CRITICAL);

            Document docWarn = createTestDocument(testUser, LocalDate.now().plusDays(20));
            docWarn.setDocumentStatus(DocumentStatus.WARNING);

            Document docSafe = createTestDocument(testUser, LocalDate.now().plusDays(60));
            docSafe.setDocumentStatus(DocumentStatus.SAFE);

            Document docExp = createTestDocument(testUser, LocalDate.now().minusDays(1));
            docExp.setDocumentStatus(DocumentStatus.EXPIRED);

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(documentRepository.findByUserIdAndIsActiveTrue(any()))
                    .thenReturn(List.of(docCrit1, docCrit2, docWarn, docSafe, docExp));

            ExpiryDashboardResponse response = documentService.getExpiryDashboard(userEmail);

            assertNotNull(response);
            assertEquals(2, response.getCriticalCount());
            assertEquals(1, response.getWarningCount());
            assertEquals(1, response.getSafeCount());
            assertEquals(1, response.getExpiredCount());
            assertEquals(5, response.getTotalDocuments());
        }

        @Test
        @DisplayName("Should handle empty vault safely")
        void test_getExpiryDashboard_emptyVault() {
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(documentRepository.findByUserIdAndIsActiveTrue(any()))
                    .thenReturn(Collections.emptyList());

            ExpiryDashboardResponse response = documentService.getExpiryDashboard(userEmail);

            assertNotNull(response);
            assertEquals(0, response.getCriticalCount());
            assertEquals(0, response.getWarningCount());
            assertEquals(0, response.getSafeCount());
            assertEquals(0, response.getExpiredCount());
            assertEquals(0, response.getTotalDocuments());
        }
    }
}
