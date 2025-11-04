package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customs_transactions",
        indexes = {
                @Index(name = "idx_file_no", columnList = "file_no", unique = true),
                @Index(name = "idx_broker_company", columnList = "broker_company_id"),
                @Index(name = "idx_client_company", columnList = "client_company_id"),
                @Index(name = "idx_created_by", columnList = "created_by_user_id")
        })
@Getter
@Setter
public class CustomsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ İşlemi giren gümrük firması
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_company_id", nullable = false)
    private Company brokerCompany;

    // ✅ İşlemin ait olduğu alıcı firma
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_company_id", nullable = false)
    private Company clientCompany;

    // ✅ İşlemi kimin girdisi?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    // ===== EXCEL SUTUNLARI (16 alan) =====

    @Column(unique = true, nullable = false, length = 100)
    private String fileNo;  // Dosya No

    @Column(length = 255)
    private String recipientName;  // Alıcı (Firm Name)

    @Column(length = 255)
    private String customsWarehouse;  // Gümrük Antrepo

    @Column(length = 50)
    private String gate;  // Kapı (Gate/Port)

    @Column(precision = 12, scale = 2)
    private BigDecimal weight;  // Kilo

    @Column(precision = 15, scale = 2)
    private BigDecimal tax;  // Vergi

    @Column(length = 255)
    private String senderName;  // Gönderici

    @Column(name = "warehouse_arrival_date")
    private LocalDate warehouseArrivalDate;  // Antrepo & Varış Tarihi

    @Column(name = "registration_date")
    private LocalDate registrationDate;  // Tescil Tarihi

    @Column(length = 100)
    private String declarationNumber;  // Beyanname No

    @Column(name = "line_closure_date")
    private LocalDate lineClosureDate;  // Hat Kapanma Tarihi

    @Column(name = "import_processing_time")
    private Integer importProcessingTime;  // İthalat İşlem Süresi (gün)

    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate;  // Çekilme Tarihi

    @Column(length = 500)
    private String description;  // Açıklama

    @Column(name = "total_processing_time")
    private Integer totalProcessingTime;  // Toplam İşlem Süresi (gün)

    @Column(name = "delay_reason", length = 500)
    private String delayReason;  // Gecikme Nedeni

    // ===== METADATA =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    public CustomsTransaction() {
    }

    // ✅ Helper method: İşlem süresini otomatik hesapla
    public void calculateProcessingTime() {
        if (this.registrationDate != null && this.withdrawalDate != null) {
            this.totalProcessingTime = (int) java.time.temporal.ChronoUnit.DAYS
                    .between(this.registrationDate, this.withdrawalDate);
        }
    }

    // ✅ Helper method: Gecikme var mı kontrol et
    public boolean hasDelay() {
        return this.delayReason != null && !this.delayReason.isEmpty();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.calculateProcessingTime();
    }
}