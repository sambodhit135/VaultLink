package com.vaultlink.controller;

import com.vaultlink.dto.request.DocumentRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.DocumentResponse;
import com.vaultlink.dto.response.ExpiryDashboardResponse;
import com.vaultlink.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // -------------------------------------------------------
    // HELPER — resolve authenticated user's email from JWT
    // -------------------------------------------------------

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    // -------------------------------------------------------
    // 1. GET /api/documents
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns all active documents belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        String email = getCurrentUserEmail();
        log.info("GET /api/documents — fetching all documents for user: {}", email);
        List<DocumentResponse> response = documentService.getAllDocuments(email);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 2. GET /api/documents/{id}
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns a single document by id, enforcing ownership check.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        log.info("GET /api/documents/{} — fetching document id: {}", id, id);
        DocumentResponse response = documentService.getDocumentById(id, getCurrentUserEmail());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 3. POST /api/documents
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Creates a new document for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        String email = getCurrentUserEmail();
        log.info("POST /api/documents — creating document for user: {}", email);
        DocumentResponse response = documentService.createDocument(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------
    // 4. PUT /api/documents/{id}
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Updates an existing document. Only the owner can update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request
    ) {
        log.info("PUT /api/documents/{} — updating document id: {}", id, id);
        DocumentResponse response = documentService.updateDocument(id, request, getCurrentUserEmail());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 5. DELETE /api/documents/{id}
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Soft-deletes a document (sets isActive = false). Only the owner can delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteDocument(@PathVariable Long id) {
        log.info("DELETE /api/documents/{} — deleting document id: {}", id, id);
        ApiResponse response = documentService.deleteDocument(id, getCurrentUserEmail());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 6. GET /api/documents/category/{categoryId}
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns all active documents in the given category for the authenticated user.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByCategory(@PathVariable Long categoryId) {
        log.info("GET /api/documents/category/{} — fetching documents by category: {}", categoryId, categoryId);
        List<DocumentResponse> response = documentService.getDocumentsByCategory(categoryId, getCurrentUserEmail());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 7. GET /api/documents/status/{status}
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns active documents filtered by DocumentStatus (SAFE / WARNING / CRITICAL / EXPIRED).
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByStatus(@PathVariable String status) {
        log.info("GET /api/documents/status/{} — fetching documents by status: {}", status, status);
        List<DocumentResponse> response = documentService.getDocumentsByStatus(status, getCurrentUserEmail());
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 8. GET /api/documents/expiring-soon?days=7
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns documents expiring within the next {@code days} days (default 7), sorted by expiry date.
     */
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<DocumentResponse>> getExpiringSoon(
            @RequestParam(defaultValue = "7") int days
    ) {
        String email = getCurrentUserEmail();
        log.info("GET /api/documents/expiring-soon?days={} — fetching documents expiring in {} days for user: {}",
                days, days, email);
        List<DocumentResponse> response = documentService.getExpiringSoon(email, days);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 9. GET /api/documents/expiry/summary
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns an aggregated expiry dashboard (counts + critical/warning document lists).
     */
    @GetMapping("/expiry/summary")
    public ResponseEntity<ExpiryDashboardResponse> getExpiryDashboard() {
        String email = getCurrentUserEmail();
        log.info("GET /api/documents/expiry/summary — fetching expiry dashboard for user: {}", email);
        ExpiryDashboardResponse response = documentService.getExpiryDashboard(email);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // 10. GET /api/documents/search?keyword=passport
    //     Protected — JWT required
    // -------------------------------------------------------

    /**
     * Searches documents by title keyword (case-insensitive, in-memory filter).
     */
    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> searchDocuments(@RequestParam String keyword) {
        String email = getCurrentUserEmail();
        log.info("GET /api/documents/search?keyword={} — searching documents with keyword: '{}'", keyword, keyword);

        List<DocumentResponse> filtered = documentService.getAllDocuments(email)
                .stream()
                .filter(doc -> doc.getTitle() != null
                        && doc.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();

        log.info("Search returned {} result(s) for keyword: '{}'", filtered.size(), keyword);
        return ResponseEntity.ok(filtered);
    }
}
