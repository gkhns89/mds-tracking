package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "broker_subscriptions",
        indexes = {
                @Index(name = "idx_broker_active", columnList = "broker_company_id, is_active")
        })
@Getter
@Setter
public class BrokerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_company_id", nullable = false)
    private Company brokerCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ Özel anlaşmalar için override değerleri
    @Column(name = "custom_max_broker_users")
    private Integer customMaxBrokerUsers;

    @Column(name = "custom_max_client_companies")
    private Integer customMaxClientCompanies;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private User createdByAdmin;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public BrokerSubscription() {
    }

    // ===== HELPER METODLARI =====

    /**
     * Geçerli maksimum kullanıcı sayısı
     * (Özel limit varsa onu, yoksa plan limitini döner)
     */
    public int getEffectiveMaxBrokerUsers() {
        return customMaxBrokerUsers != null ? customMaxBrokerUsers :
                (subscriptionPlan != null ? subscriptionPlan.getMaxBrokerUsers() : 0);
    }

    /**
     * Geçerli maksimum müşteri firma sayısı
     */
    public int getEffectiveMaxClientCompanies() {
        return customMaxClientCompanies != null ? customMaxClientCompanies :
                (subscriptionPlan != null ? subscriptionPlan.getMaxClientCompanies() : 0);
    }

    /**
     * Abonelik süresi dolmuş mu?
     */
    public boolean isExpired() {
        return endDate != null && LocalDateTime.now().isAfter(endDate);
    }

    /**
     * Abonelik bitimine kaç gün kaldı?
     */
    public long getDaysUntilExpiry() {
        if (endDate == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), endDate);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}