package com.vaultlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate issueDate;
    private LocalDate expiryDate;

    /** SAFE / WARNING / CRITICAL / EXPIRED */
    private String documentStatus;

    /** Calculated: days remaining until expiry (negative if already expired) */
    private Long daysUntilExpiry;

    private String filePath;
    private Boolean isActive;

    private String categoryName;
    private Long categoryId;

    private String ownerEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
