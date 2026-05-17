package com.vaultlink.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaultlink.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus documentStatus = DocumentStatus.SAFE;

    private String filePath;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<NotificationLog> notificationLogs;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AccessToken> accessTokens;
}
