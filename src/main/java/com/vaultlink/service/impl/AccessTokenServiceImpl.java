package com.vaultlink.service.impl;

import com.vaultlink.dto.request.ShareDocumentRequest;
import com.vaultlink.dto.response.AccessTokenResponse;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.DocumentResponse;
import com.vaultlink.entity.AccessToken;
import com.vaultlink.entity.Document;
import com.vaultlink.repository.AccessTokenRepository;
import com.vaultlink.repository.DocumentRepository;
import com.vaultlink.service.AccessTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessTokenServiceImpl implements AccessTokenService {

    private final AccessTokenRepository accessTokenRepository;
    private final DocumentRepository documentRepository;

    // -------------------------------------------------------
    // SHARE DOCUMENT
    // -------------------------------------------------------

    @Override
    @Transactional
    public AccessTokenResponse shareDocument(Long documentId, ShareDocumentRequest request, String ownerEmail) {
        log.info("Sharing document id={} by user: {} with: {}", documentId, ownerEmail, request.getSharedWithEmail());

        // Step 1: Find document
        Document document = findDocumentById(documentId);

        // Step 2: Verify ownership
        verifyOwnership(document, ownerEmail);

        // Step 3: Generate a unique UUID token
        String tokenValue = UUID.randomUUID().toString();

        // Step 4: Calculate expiry timestamp
        int hours = request.getExpiresInHours() != null ? request.getExpiresInHours() : 24;
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(hours);

        // Step 5: Build and persist the AccessToken entity
        AccessToken accessToken = AccessToken.builder()
                .token(tokenValue)
                .sharedWithEmail(request.getSharedWithEmail())
                .expiresAt(expiresAt)
                .isUsed(false)
                .document(document)
                .build();

        AccessToken saved = accessTokenRepository.save(accessToken);
        log.info("Access token created — id={}, expires at: {}", saved.getId(), expiresAt);

        // Step 6: Return AccessTokenResponse
        return mapToAccessTokenResponse(saved);
    }

    // -------------------------------------------------------
    // GET SHARED DOCUMENT (public — no auth)
    // -------------------------------------------------------

    @Override
    @Transactional
    public DocumentResponse getSharedDocument(String token) {
        log.info("Accessing shared document via token: {}", token);

        // Step 1: Find AccessToken by token string
        AccessToken accessToken = accessTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid access token: {}", token);
                    return new RuntimeException("Invalid token");
                });

        // Step 3: Check if token has already been used
        if (Boolean.TRUE.equals(accessToken.getIsUsed())) {
            log.warn("Token already used: {}", token);
            throw new RuntimeException("Token already used");
        }

        // Step 4: Check if token has expired
        if (accessToken.getExpiresAt() != null && accessToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Token expired: {} — expired at: {}", token, accessToken.getExpiresAt());
            throw new RuntimeException("Token expired");
        }

        // Step 5: Mark token as used and persist
        accessToken.setIsUsed(true);
        accessTokenRepository.save(accessToken);
        log.info("Token consumed — id={}, document id={}", accessToken.getId(), accessToken.getDocument().getId());

        // Step 6: Map and return the associated document
        return mapToDocumentResponse(accessToken.getDocument());
    }

    // -------------------------------------------------------
    // GET SHARE HISTORY
    // -------------------------------------------------------

    @Override
    public List<AccessTokenResponse> getShareHistory(Long documentId, String ownerEmail) {
        log.info("Fetching share history for document id={}, owner: {}", documentId, ownerEmail);

        // Step 1: Find document
        Document document = findDocumentById(documentId);

        // Step 2: Verify ownership
        verifyOwnership(document, ownerEmail);

        // Step 3 & 4: Fetch all tokens for this document and map
        List<AccessTokenResponse> history = accessTokenRepository
                .findByDocumentId(documentId)
                .stream()
                .map(this::mapToAccessTokenResponse)
                .toList();

        log.info("Returning {} share record(s) for document id={}", history.size(), documentId);
        return history;
    }

    // -------------------------------------------------------
    // REVOKE TOKEN
    // -------------------------------------------------------

    @Override
    @Transactional
    public ApiResponse revokeToken(Long tokenId, String ownerEmail) {
        log.info("Revoking token id={} by user: {}", tokenId, ownerEmail);

        // Step 1: Find AccessToken by id
        AccessToken accessToken = accessTokenRepository.findById(tokenId)
                .orElseThrow(() -> {
                    log.warn("Access token not found with id: {}", tokenId);
                    return new RuntimeException("Access token not found with id: " + tokenId);
                });

        // Step 2: Verify the requesting user owns the document the token belongs to
        verifyOwnership(accessToken.getDocument(), ownerEmail);

        // Step 3: Revoke — set isUsed = true
        accessToken.setIsUsed(true);
        accessTokenRepository.save(accessToken);

        log.info("Token revoked successfully — id={}", tokenId);

        // Step 4: Return success response
        return ApiResponse.success("Access token revoked successfully");
    }

    // -------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------

    /**
     * Finds a Document by id or throws RuntimeException.
     */
    private Document findDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("Document not found with id: {}", documentId);
                    return new RuntimeException("Document not found with id: " + documentId);
                });
    }

    /**
     * Verifies that the given email matches the document owner's email.
     * Throws RuntimeException("Access denied") if verification fails.
     */
    private void verifyOwnership(Document document, String email) {
        if (!document.getUser().getEmail().equals(email)) {
            log.warn("Access denied — user '{}' does not own document id={} (owner: '{}')",
                    email, document.getId(), document.getUser().getEmail());
            throw new RuntimeException("Access denied: you do not own this document");
        }
    }

    /**
     * Maps an {@link AccessToken} entity to {@link AccessTokenResponse}.
     */
    private AccessTokenResponse mapToAccessTokenResponse(AccessToken accessToken) {
        return AccessTokenResponse.builder()
                .id(accessToken.getId())
                .token(accessToken.getToken())
                .sharedWithEmail(accessToken.getSharedWithEmail())
                .expiresAt(accessToken.getExpiresAt())
                .isUsed(accessToken.getIsUsed())
                .documentTitle(accessToken.getDocument().getTitle())
                .build();
    }

    /**
     * Maps a {@link Document} entity to {@link DocumentResponse},
     * including a freshly calculated daysUntilExpiry and documentStatus.
     */
    private DocumentResponse mapToDocumentResponse(Document document) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), document.getExpiryDate());

        String liveStatus;
        if (daysUntilExpiry < 0)       liveStatus = "EXPIRED";
        else if (daysUntilExpiry <= 7)  liveStatus = "CRITICAL";
        else if (daysUntilExpiry <= 30) liveStatus = "WARNING";
        else                            liveStatus = "SAFE";

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .issueDate(document.getIssueDate())
                .expiryDate(document.getExpiryDate())
                .documentStatus(liveStatus)
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
}
