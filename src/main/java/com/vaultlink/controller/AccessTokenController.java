package com.vaultlink.controller;

import com.vaultlink.dto.request.ShareDocumentRequest;
import com.vaultlink.dto.response.AccessTokenResponse;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.DocumentResponse;
import com.vaultlink.service.AccessTokenService;
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
public class AccessTokenController {

    private final AccessTokenService accessTokenService;

    // -------------------------------------------------------
    // HELPER — resolve authenticated user's email from JWT
    // -------------------------------------------------------

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    // -------------------------------------------------------
    // POST /api/documents/{id}/share
    // Protected — JWT required
    // -------------------------------------------------------

    /**
     * Generates a one-time share token for the given document.
     * Only the document owner can share it.
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<AccessTokenResponse> shareDocument(
            @PathVariable Long id,
            @Valid @RequestBody ShareDocumentRequest request
    ) {
        String email = getCurrentUserEmail();
        log.info("POST /api/documents/{}/share — sharing document id={} by user: {}", id, id, email);
        AccessTokenResponse response = accessTokenService.shareDocument(id, request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------
    // GET /api/documents/shared/{token}
    // PUBLIC — no authentication required
    // (permitted via PUBLIC_URLS → /api/documents/shared/**)
    // -------------------------------------------------------

    /**
     * Consumes a share token and returns the associated document.
     * Token is single-use and time-limited — valid JWT is NOT required.
     */
    @GetMapping("/shared/{token}")
    public ResponseEntity<DocumentResponse> getSharedDocument(@PathVariable String token) {
        log.info("GET /api/documents/shared/{} — accessing shared document via token", token);
        DocumentResponse response = accessTokenService.getSharedDocument(token);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // GET /api/documents/{id}/share-history
    // Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns all share tokens ever generated for a document.
     * Only the document owner can view the share history.
     */
    @GetMapping("/{id}/share-history")
    public ResponseEntity<List<AccessTokenResponse>> getShareHistory(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        log.info("GET /api/documents/{}/share-history — fetching share history for document id={}, user: {}",
                id, id, email);
        List<AccessTokenResponse> response = accessTokenService.getShareHistory(id, email);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // DELETE /api/documents/share/{tokenId}/revoke
    // Protected — JWT required
    // -------------------------------------------------------

    /**
     * Revokes a share token by marking it as used.
     * Only the document owner can revoke its tokens.
     */
    @DeleteMapping("/share/{tokenId}/revoke")
    public ResponseEntity<ApiResponse> revokeToken(@PathVariable Long tokenId) {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/documents/share/{}/revoke — revoking token id={} by user: {}",
                tokenId, tokenId, email);
        ApiResponse response = accessTokenService.revokeToken(tokenId, email);
        return ResponseEntity.ok(response);
    }
}
