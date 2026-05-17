package com.vaultlink.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaultlink.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private String notificationType; // e.g. "90_DAYS", "30_DAYS", "7_DAYS", "EXPIRED"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String recipientEmail;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private Document document;
}
