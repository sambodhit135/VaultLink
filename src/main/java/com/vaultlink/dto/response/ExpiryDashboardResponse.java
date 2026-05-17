package com.vaultlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiryDashboardResponse {

    private Integer totalDocuments;

    /** Expiring within 7 days */
    private Integer criticalCount;

    /** Expiring within 8–30 days */
    private Integer warningCount;

    /** Expiring after 30 days */
    private Integer safeCount;

    /** Already expired */
    private Integer expiredCount;

    /** Expiring today */
    private Integer expiringTodayCount;

    private List<DocumentResponse> criticalDocuments;
    private List<DocumentResponse> warningDocuments;
}
