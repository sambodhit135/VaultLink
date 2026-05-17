package com.vaultlink.repository;

import com.vaultlink.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByDocumentId(Long documentId);

    List<NotificationLog> findByRecipientEmail(String email);

    List<NotificationLog> findByRecipientEmailOrderBySentAtDesc(String email);

    boolean existsByDocumentIdAndNotificationType(Long documentId, String notificationType);
}
