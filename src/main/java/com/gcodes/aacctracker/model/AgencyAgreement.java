package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agency_agreements",
        indexes = {
                @Index(name = "idx_broker_company", columnList = "broker_company_id"),
                @Index(name = "idx_client_company", columnList = "client_company_id"),
                @Index(name = "idx_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"broker_company_id", "client_company_id"},
                name = "uk_broker_client"))
@Getter
@Setter
public class AgencyAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⚠️ DİKKAT: Bunlar OK ama Company içinde collection'lar var
    // En iyisi DTO kullanmak!
    // ✅ Gümrük firması (Broker)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_company_id", nullable = false)
    private Company brokerCompany;

    // ✅ Müşteri firma (Client)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_company_id", nullable = false)
    private Company clientCompany;

    // ✅ Anlaşmayı kim oluşturdu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ✅ Anlaşma durumu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.ACTIVE;

    // ✅ Anlaşma tarihleri
    @Column(name = "start_date")
    private LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // ✅ Anlaşma referans numarası
    @Column(length = 100, unique = true)
    private String agreementNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(length = 500)
    private String notes;

    public AgencyAgreement() {
    }

    // ✅ Helper method: Anlaşma aktif mi?
    public boolean isActive() {
        return AgreementStatus.ACTIVE.equals(this.status);
    }

    // ✅ Helper method: Anlaşma süresi dolmuş mu?
    public boolean isExpired() {
        if (this.endDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.endDate);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}