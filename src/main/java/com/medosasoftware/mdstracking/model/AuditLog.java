package com.medosasoftware.mdstracking.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_entity_type", columnList = "entity_type"),
                @Index(name = "idx_timestamp", columnList = "timestamp")
        })
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ İşlemi gerçekleştiren kullanıcı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User performedBy;

    // ✅ İşlem adı
    @Column(length = 255, nullable = false)
    private String action;  // "CREATE", "UPDATE", "DELETE", "VIEW", vb.

    // ✅ Etkilenen entity tipi
    @Column(length = 100, nullable = false)
    private String entityType;  // "CustomsTransaction", "Company", "User", vb.

    // ✅ Etkilenen entity'nin ID'si
    @Column(name = "entity_id")
    private Long entityId;

    // ✅ İşlem tarihi
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // ✅ Değişiklik detayları (JSON formatında)
    @Column(columnDefinition = "LONGTEXT")
    private String changeDetails;  // {"before": {...}, "after": {...}}

    // ✅ İstemci IP adresi
    @Column(length = 50)
    private String ipAddress;

    // ✅ Sonuç: SUCCESS veya FAILURE
    @Column(length = 20)
    private String result = "SUCCESS";

    // ✅ Hata mesajı (varsa)
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public AuditLog() {
    }

    public AuditLog(User performedBy, String action, String entityType, Long entityId) {
        this.performedBy = performedBy;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.timestamp = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}