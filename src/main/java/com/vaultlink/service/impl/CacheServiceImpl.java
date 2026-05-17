package com.vaultlink.service.impl;

import com.vaultlink.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    // -------------------------------------------------------
    // Cache key prefix convention (mirrors Spring's default):
    //   <cacheName>::<key>
    // -------------------------------------------------------

    private static final String DOCUMENTS_PREFIX  = "documents::";
    private static final String DOCUMENT_PREFIX   = "document::";
    private static final String DASHBOARD_PREFIX  = "expiryDashboard::";

    // -------------------------------------------------------
    // evictUserDocumentCache
    // -------------------------------------------------------

    /**
     * Removes the "documents" cache entry keyed by the user's email.
     * Call this whenever a document is created, updated, or deleted.
     */
    @Override
    public void evictUserDocumentCache(String email) {
        String key = DOCUMENTS_PREFIX + email;
        try {
            redisTemplate.delete(key);
            log.info("Evicted documents cache for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to evict documents cache for user: {} — {}", email, e.getMessage());
        }
    }

    // -------------------------------------------------------
    // evictDocumentCache
    // -------------------------------------------------------

    /**
     * Removes the "document" cache entry keyed by the document id.
     * Call this when a single document is updated or deleted.
     */
    @Override
    public void evictDocumentCache(Long documentId) {
        String key = DOCUMENT_PREFIX + documentId;
        try {
            redisTemplate.delete(key);
            log.info("Evicted document cache for id: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to evict document cache for id: {} — {}", documentId, e.getMessage());
        }
    }

    // -------------------------------------------------------
    // evictDashboardCache
    // -------------------------------------------------------

    /**
     * Removes the "expiryDashboard" cache entry keyed by the user's email.
     * Call this whenever any document belonging to the user changes.
     */
    @Override
    public void evictDashboardCache(String email) {
        String key = DASHBOARD_PREFIX + email;
        try {
            redisTemplate.delete(key);
            log.info("Evicted dashboard cache for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to evict dashboard cache for user: {} — {}", email, e.getMessage());
        }
    }

    // -------------------------------------------------------
    // evictAllUserCaches
    // -------------------------------------------------------

    /**
     * Convenience method that evicts both the documents list cache
     * and the expiry dashboard cache for the given user in one call.
     * Use after any write operation (create / update / delete).
     */
    @Override
    public void evictAllUserCaches(String email) {
        evictUserDocumentCache(email);
        evictDashboardCache(email);
        log.info("Evicted all caches for user: {}", email);
    }

    // -------------------------------------------------------
    // getCacheStats
    // -------------------------------------------------------

    /**
     * Probes Redis connectivity and returns a status map containing:
     * <ul>
     *   <li>{@code redisStatus} — "UP" or "DOWN"</li>
     *   <li>{@code cachedKeys}  — total number of keys in Redis</li>
     *   <li>{@code timestamp}   — time the stats were collected</li>
     * </ul>
     */
    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", LocalDateTime.now());

        try {
            // Ping Redis to verify connectivity
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            boolean isUp = "PONG".equalsIgnoreCase(pong);
            stats.put("redisStatus", isUp ? "UP" : "DOWN");

            // Count all keys currently stored in Redis
            Set<String> keys = redisTemplate.keys("*");
            long keyCount = (keys != null) ? keys.size() : 0L;
            stats.put("cachedKeys", keyCount);

            log.info("getCacheStats — Redis={}, keys={}", stats.get("redisStatus"), keyCount);

        } catch (Exception e) {
            log.error("getCacheStats — Redis appears to be DOWN: {}", e.getMessage());
            stats.put("redisStatus", "DOWN");
            stats.put("cachedKeys", 0L);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
