package com.vaultlink.controller;

import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    // -------------------------------------------------------
    // HELPER — resolve authenticated user's email from JWT
    // -------------------------------------------------------

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    // -------------------------------------------------------
    // 1. GET /api/cache/stats
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Returns Redis connectivity status and total cached key count.
     * Useful for health checks and debugging.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        String email = getCurrentUserEmail();
        log.info("GET /api/cache/stats — cache stats requested by: {}", email);
        Map<String, Object> stats = cacheService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    // -------------------------------------------------------
    // 2. DELETE /api/cache/clear/documents
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Evicts the "documents" cache entry for the current user.
     * Forces the next GET /api/documents call to hit the database.
     */
    @DeleteMapping("/clear/documents")
    public ResponseEntity<ApiResponse> clearDocumentCache() {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/cache/clear/documents — document cache cleared for: {}", email);
        cacheService.evictUserDocumentCache(email);
        return ResponseEntity.ok(ApiResponse.success("Document cache cleared"));
    }

    // -------------------------------------------------------
    // 3. DELETE /api/cache/clear/dashboard
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Evicts the "expiryDashboard" cache entry for the current user.
     * Forces the next GET /api/documents/expiry/summary to recalculate.
     */
    @DeleteMapping("/clear/dashboard")
    public ResponseEntity<ApiResponse> clearDashboardCache() {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/cache/clear/dashboard — dashboard cache cleared for: {}", email);
        cacheService.evictDashboardCache(email);
        return ResponseEntity.ok(ApiResponse.success("Dashboard cache cleared"));
    }

    // -------------------------------------------------------
    // 4. DELETE /api/cache/clear/all
    //    Protected — JWT required
    // -------------------------------------------------------

    /**
     * Evicts both the documents list and the expiry dashboard cache
     * for the current user in a single call.
     */
    @DeleteMapping("/clear/all")
    public ResponseEntity<ApiResponse> clearAllCaches() {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/cache/clear/all — all caches cleared for: {}", email);
        cacheService.evictAllUserCaches(email);
        return ResponseEntity.ok(ApiResponse.success("All caches cleared"));
    }
}
