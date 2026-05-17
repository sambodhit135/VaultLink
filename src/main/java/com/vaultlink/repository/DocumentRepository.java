package com.vaultlink.repository;

import com.vaultlink.entity.Document;
import com.vaultlink.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserId(Long userId);

    List<Document> findByUserIdAndIsActiveTrue(Long userId);

    List<Document> findByExpiryDateBefore(LocalDate date);

    List<Document> findByUserIdAndDocumentStatus(Long userId, DocumentStatus status);

    List<Document> findByUserIdAndDocumentStatusAndIsActiveTrue(Long userId, DocumentStatus status);

    List<Document> findByExpiryDateBetween(LocalDate start, LocalDate end);

    List<Document> findByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);

    List<Document> findByUserIdAndExpiryDateBetweenAndIsActiveTrue(Long userId, LocalDate start, LocalDate end);

    List<Document> findByUserIdAndExpiryDateAndIsActiveTrue(Long userId, LocalDate date);

    Long countByUserIdAndDocumentStatus(Long userId, DocumentStatus status);

    Long countByCategoryId(Long categoryId);

    List<Document> findByIsActiveTrue();

    List<Document> findByIsActiveTrueAndExpiryDateGreaterThanEqual(LocalDate date);
}
