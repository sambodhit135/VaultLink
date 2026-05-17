package com.vaultlink.service;

import com.vaultlink.entity.Document;
import com.vaultlink.entity.NotificationLog;
import com.vaultlink.entity.User;

import java.util.List;

public interface NotificationService {

    void sendExpiryAlert(Document document, String alertType);

    void sendWelcomeEmail(User user);

    List<NotificationLog> getNotificationHistory(String email);

    List<NotificationLog> getNotificationsByDocument(Long documentId, String email);
}
