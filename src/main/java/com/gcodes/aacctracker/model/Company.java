package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies",
        indexes = {
                @Index(name = "idx_parent_broker", columnList = "parent_broker_id"),
                @Index(name = "idx_type_active", columnList = "company_type, is_active"),
                @Index(name = "idx_company_code", columnList = "company_code")
        })
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    // ✅ YENİ: Firma kodu (Müşterilerin kayıt olurken kullanacağı)
    @Column(unique = true, length = 20, name = "company_code")
    private String companyCode; // Örn: "GMK001", "GMK002"

    // ✅ YENİ: Genel açıklama (Müşterilere gösterilecek)
    @Column(length = 500, name = "public_description")
    private String publicDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "company_type")
    private CompanyType companyType = CompanyType.CLIENT;

    // ✅ CLIENT ise, hangi gümrük firmasına ait?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_broker_id")
    private Company parentBroker;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ Broker'ın müşteri listesi
    @OneToMany(mappedBy = "parentBroker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Company> clients = new ArrayList<>();

    // ✅ Firmaya ait kullanıcılar
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    // ✅ Firma ile ilgili işlemler
    @OneToMany(mappedBy = "brokerCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomsTransaction> brokerTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "clientCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomsTransaction> clientTransactions = new ArrayList<>();

    public Company() {
    }

    // ===== HELPER METODLARI =====

    public boolean isBroker() {
        return CompanyType.CUSTOMS_BROKER.equals(companyType);
    }

    public boolean isClient() {
        return CompanyType.CLIENT.equals(companyType);
    }

    /**
     * Gümrük firmasını getir
     * - BROKER ise kendisi
     * - CLIENT ise parent broker
     */
    public Company getBrokerCompany() {
        return isBroker() ? this : parentBroker;
    }

    /**
     * Firma kodu oluştur (otomatik)
     */
    public void generateCompanyCode() {
        if (this.companyCode == null && this.isBroker() && this.id != null) {
            // Örnek: GMK001, GMK002, GMK003
            String prefix = "GMK";
            this.companyCode = prefix + String.format("%03d", this.id);
        }
    }
}