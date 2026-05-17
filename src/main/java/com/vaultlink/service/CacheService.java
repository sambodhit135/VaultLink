package com.vaultlink.service;

import java.util.Map;

public interface CacheService {

    void evictUserDocumentCache(String email);

    void evictDocumentCache(Long documentId);

    void evictDashboardCache(String email);

    void evictAllUserCaches(String email);

    Map<String, Object> getCacheStats();
}
