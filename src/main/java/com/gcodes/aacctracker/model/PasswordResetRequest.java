package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_requests",
        indexes = {
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_approver_pending", columnList = "approver_id, status")
        })
@Getter
@Setter
public class PasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResetRequestStatus status = ResetRequestStatus.PENDING;

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public PasswordResetRequest() {
    }

    // ===== HELPER METODLARI =====

    public boolean isPending() {
        return ResetRequestStatus.PENDING.equals(status);
    }

    public boolean isApproved() {
        return ResetRequestStatus.APPROVED.equals(status);
    }

    public boolean isCompleted() {
        return ResetRequestStatus.COMPLETED.equals(status);
    }

    /**
     * İsteği onayla ve token oluştur
     */
    public void approve(User approver, String token) {
        this.status = ResetRequestStatus.APPROVED;
        this.approver = approver;
        this.resetToken = token;
        this.tokenExpiresAt = LocalDateTime.now().plusHours(24);
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * İsteği reddet
     */
    public void reject(User approver, String reason) {
        this.status = ResetRequestStatus.REJECTED;
        this.approver = approver;
        this.adminNotes = reason;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Şifre sıfırlama tamamlandı
     */
    public void complete() {
        this.status = ResetRequestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}