package com.vaultlink.service.impl;

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
import com.vaultlink.service.CacheService;
import com.vaultlink.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CacheService cacheService;

    // -------------------------------------------------------
    // GET ALL DOCUMENTS
    // -------------------------------------------------------

    @Override
    @Cacheable(value = "documents", key = "#email")
    public List<DocumentResponse> getAllDocuments(String email) {
        log.debug("Fetching documents for user: {}", email);

        // Step 1: Resolve user
        User user = findUserByEmail(email);

        // Step 2 & 3 & 4: Fetch active docs, calculate status, map to response
        List<DocumentResponse> responses = documentRepository
                .findByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(this::mapToDocumentResponse)
                .collect(java.util.stream.Collectors.toList());

        log.debug("Found {} documents for user: {}", responses.size(), email);
        return responses;
    }

    // -------------------------------------------------------
    // GET DOCUMENT BY ID
    // -------------------------------------------------------

    @Override
    @Cacheable(value = "document", key = "#id + '-' + #email")
    public DocumentResponse getDocumentById(Long id, String email) {
        log.info("Fetching document id={} for user: {}", id, email);

        // Step 1: Find document
        Document document = findDocumentById(id);

        // Step 2 & 3: Verify ownership
        verifyOwnership(document, email);

        log.info("Returning document '{}' (id={}) for user: {}", document.getTitle(), id, email);
        return mapToDocumentResponse(document);
    }

    // -------------------------------------------------------
    // CREATE DOCUMENT
    // -------------------------------------------------------

    @Override
    @Transactional
    public DocumentResponse createDocument(DocumentRequest request, String email) {
        log.info("Creating document '{}' for user: {}", request.getTitle(), email);

        // Step 1: Resolve user
        User user = findUserByEmail(email);

        // Step 2: Resolve category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", request.getCategoryId());
                    return new RuntimeException("Category not found with id: " + request.getCategoryId());
                });

        // Step 3 & 4: Build entity with calculated status
        DocumentStatus status = calculateDocumentStatus(request.getExpiryDate());

        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .filePath(request.getFilePath())
                .documentStatus(status)
                .isActive(true)
                .user(user)
                .category(category)
                .build();

        // Step 5: Persist and return
        Document saved = documentRepository.save(document);
        log.info("Document created: {} for user: {}", saved.getTitle(), email);

        // Evict stale caches so next read reflects the new document
        cacheService.evictAllUserCaches(email);
        log.debug("Cache evicted after creating document for: {}", email);

        return mapToDocumentResponse(saved);
    }

    // -------------------------------------------------------
    // UPDATE DOCUMENT
    // -------------------------------------------------------

    @Override
    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentRequest request, String email) {
        log.info("Updating document id={} for user: {}", id, email);

        // Step 1: Find document
        Document document = findDocumentById(id);

        // Step 2: Verify ownership
        verifyOwnership(document, email);

        // Step 3: Resolve new category if changed
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", request.getCategoryId());
                    return new RuntimeException("Category not found with id: " + request.getCategoryId());
                });

        // Step 3: Update all fields from request
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setIssueDate(request.getIssueDate());
        document.setExpiryDate(request.getExpiryDate());
        document.setFilePath(request.getFilePath());
        document.setCategory(category);

        // Step 4: Recalculate status
        document.setDocumentStatus(calculateDocumentStatus(request.getExpiryDate()));

        // Step 5: Save and return
        Document updated = documentRepository.save(document);
        log.info("Document updated: {}", updated.getId());

        // Evict both the list cache and the individual document cache
        cacheService.evictAllUserCaches(email);
        cacheService.evictDocumentCache(id);
        log.debug("Cache evicted after updating document: {}", id);

        return mapToDocumentResponse(updated);
    }

    // -------------------------------------------------------
    // DELETE DOCUMENT (soft delete)
    // -------------------------------------------------------

    @Override
    @Transactional
    public ApiResponse deleteDocument(Long id, String email) {
        log.info("Soft-deleting document id={} for user: {}", id, email);

        // Step 1: Find document
        Document document = findDocumentById(id);

        // Step 2: Verify ownership
        verifyOwnership(document, email);

        // Step 3: Soft delete — set isActive = false
        document.setIsActive(false);
        documentRepository.save(document);

        log.info("Document deleted (soft): {}", id);

        // Evict both the list cache and the individual document cache
        cacheService.evictAllUserCaches(email);
        cacheService.evictDocumentCache(id);
        log.debug("Cache evicted after deleting document: {}", id);

        return ApiResponse.success("Document deleted successfully");
    }

    // -------------------------------------------------------
    // GET DOCUMENTS BY CATEGORY
    // -------------------------------------------------------

    @Override
    public List<DocumentResponse> getDocumentsByCategory(Long categoryId, String email) {
        log.info("Fetching documents for categoryId={}, user: {}", categoryId, email);

        // Step 1: Resolve user
        User user = findUserByEmail(email);

        // Step 2: Fetch active docs filtered by user + category
        List<DocumentResponse> responses = documentRepository
                .findByUserIdAndCategoryIdAndIsActiveTrue(user.getId(), categoryId)
                .stream()
                .map(this::mapToDocumentResponse)
                .collect(java.util.stream.Collectors.toList());

        log.info("Returning {} documents for categoryId={}, user: {}", responses.size(), categoryId, email);
        return responses;
    }

    // -------------------------------------------------------
    // GET DOCUMENTS BY STATUS
    // -------------------------------------------------------

    @Override
    public List<DocumentResponse> getDocumentsByStatus(String status, String email) {
        log.info("Fetching documents with status='{}' for user: {}", status, email);

        // Step 1: Resolve user
        User user = findUserByEmail(email);

        // Step 2: Convert status string to enum (throws IllegalArgumentException on invalid input)
        DocumentStatus documentStatus;
        try {
            documentStatus = DocumentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid document status provided: '{}'", status);
            throw new RuntimeException("Invalid document status: '" + status + "'. Valid values: SAFE, WARNING, CRITICAL, EXPIRED");
        }

        // Step 3 & 4: Fetch and map
        List<DocumentResponse> responses = documentRepository
                .findByUserIdAndDocumentStatusAndIsActiveTrue(user.getId(), documentStatus)
                .stream()
                .map(this::mapToDocumentResponse)
                .collect(java.util.stream.Collectors.toList());

        log.info("Returning {} documents with status='{}' for user: {}", responses.size(), status, email);
        return responses;
    }

    // -------------------------------------------------------
    // GET EXPIRING SOON
    // -------------------------------------------------------

    @Override
    public List<DocumentResponse> getExpiringSoon(String email, int days) {
        log.info("Fetching documents expiring within {} days for user: {}", days, email);

        // Step 1: Resolve user
        User user = findUserByEmail(email);

        // Step 2: Date range — today inclusive to today + days inclusive
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(days);

        // Step 3: Fetch, sort by expiryDate ascending, and map
        List<DocumentResponse> responses = documentRepository
                .findByUserIdAndExpiryDateBetweenAndIsActiveTrue(user.getId(), today, until)
                .stream()
                .sorted(Comparator.comparing(Document::getExpiryDate))
                .map(this::mapToDocumentResponse)
                .collect(java.util.stream.Collectors.toList());

        log.info("Returning {} documents expiring within {} days for user: {}", responses.size(), days, email);
        return responses;
    }

    // -------------------------------------------------------
    // GET EXPIRY DASHBOARD
    // -------------------------------------------------------

    @Override
    @Cacheable(value = "expiryDashboard", key = "#email")
    public ExpiryDashboardResponse getExpiryDashboard(String email) {
        log.info("Building expiry dashboard for user: {}", email);

        // Step 1: Resolve user
        User user = findUserByEmail(email);

        // Step 2: Get all active documents
        List<Document> allActive = documentRepository.findByUserIdAndIsActiveTrue(user.getId());

        // Step 3: Count each status group using the live status from the entity
        List<DocumentResponse> mappedAll = allActive.stream()
                .map(this::mapToDocumentResponse)
                .collect(java.util.stream.Collectors.toList());

        int criticalCount = (int) mappedAll.stream()
                .filter(d -> DocumentStatus.CRITICAL.name().equals(d.getDocumentStatus()))
                .count();

        int warningCount = (int) mappedAll.stream()
                .filter(d -> DocumentStatus.WARNING.name().equals(d.getDocumentStatus()))
                .count();

        int safeCount = (int) mappedAll.stream()
                .filter(d -> DocumentStatus.SAFE.name().equals(d.getDocumentStatus()))
                .count();

        int expiredCount = (int) mappedAll.stream()
                .filter(d -> DocumentStatus.EXPIRED.name().equals(d.getDocumentStatus()))
                .count();

        // Step 4: Critical document list
        List<DocumentResponse> criticalDocuments = mappedAll.stream()
                .filter(d -> DocumentStatus.CRITICAL.name().equals(d.getDocumentStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Step 5: Warning document list
        List<DocumentResponse> warningDocuments = mappedAll.stream()
                .filter(d -> DocumentStatus.WARNING.name().equals(d.getDocumentStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Step 6: Count expiring today
        int expiringTodayCount = documentRepository
                .findByUserIdAndExpiryDateAndIsActiveTrue(user.getId(), LocalDate.now())
                .size();

        log.info("Dashboard built — total={}, critical={}, warning={}, safe={}, expired={}, today={}",
                allActive.size(), criticalCount, warningCount, safeCount, expiredCount, expiringTodayCount);

        // Step 7: Build and return
        return ExpiryDashboardResponse.builder()
                .totalDocuments(allActive.size())
                .criticalCount(criticalCount)
                .warningCount(warningCount)
                .safeCount(safeCount)
                .expiredCount(expiredCount)
                .expiringTodayCount(expiringTodayCount)
                .criticalDocuments(criticalDocuments)
                .warningDocuments(warningDocuments)
                .build();
    }

    // -------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------

    /**
     * Calculates the DocumentStatus based on days remaining until expiry.
     * <ul>
     *   <li>daysLeft &lt; 0  → EXPIRED</li>
     *   <li>daysLeft &le; 7  → CRITICAL</li>
     *   <li>daysLeft &le; 30 → WARNING</li>
     *   <li>else             → SAFE</li>
     * </ul>
     */
    private DocumentStatus calculateDocumentStatus(LocalDate expiryDate) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (daysLeft < 0)  return DocumentStatus.EXPIRED;
        if (daysLeft <= 7) return DocumentStatus.CRITICAL;
        if (daysLeft <= 30) return DocumentStatus.WARNING;
        return DocumentStatus.SAFE;
    }

    /**
     * Maps a {@link Document} entity to a {@link DocumentResponse} DTO,
     * including recalculated daysUntilExpiry and documentStatus.
     */
    private DocumentResponse mapToDocumentResponse(Document document) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), document.getExpiryDate());
        DocumentStatus liveStatus = calculateDocumentStatus(document.getExpiryDate());

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .issueDate(document.getIssueDate())
                .expiryDate(document.getExpiryDate())
                .documentStatus(liveStatus.name())
                .daysUntilExpiry(daysUntilExpiry)
                .filePath(document.getFilePath())
                .isActive(document.getIsActive())
                .categoryId(document.getCategory() != null ? document.getCategory().getId() : null)
                .categoryName(document.getCategory() != null ? document.getCategory().getName() : null)
                .ownerEmail(document.getUser().getEmail())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * Finds a user by email or throws RuntimeException.
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new RuntimeException("User not found: " + email);
                });
    }

    /**
     * Finds a document by id or throws RuntimeException.
     */
    private Document findDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Document not found with id: {}", id);
                    return new RuntimeException("Document not found with id: " + id);
                });
    }

    /**
     * Verifies that the given email matches the document's owner.
     * Throws RuntimeException("Access denied") if not the owner.
     */
    private void verifyOwnership(Document document, String email) {
        if (!document.getUser().getEmail().equals(email)) {
            log.warn("Access denied — user '{}' attempted to access document id={} owned by '{}'",
                    email, document.getId(), document.getUser().getEmail());
            throw new RuntimeException("Access denied: you do not have permission to access this document");
        }
    }
}
