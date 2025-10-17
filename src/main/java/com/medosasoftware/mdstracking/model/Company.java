package com.medosasoftware.mdstracking.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
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

    // ✅ YENİ: Şirket tipi (CUSTOMS_BROKER veya CLIENT)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "company_type")
    private CompanyType companyType = CompanyType.CLIENT;

    // ✅ YENİ: Eğer CLIENT ise, hangi BROKER ile anlaşmalı?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_broker_id")
    private Company parentBroker;

    // ✅ YENİ: Broker'ın müşteri listesi
    @OneToMany(mappedBy = "parentBroker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Company> clients = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ MEVCUT: Firma-kullanıcı rolleri
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CompanyUserRole> userRoles = new ArrayList<>();

    // ✅ YENİ: Firma ile ilgili işlemler (CustomsTransaction)
    @OneToMany(mappedBy = "brokerCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomsTransaction> brokerTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "clientCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomsTransaction> clientTransactions = new ArrayList<>();

    public Company() {
    }

    // ✅ YENİ: Helper methods
    public boolean isBroker() {
        return CompanyType.CUSTOMS_BROKER.equals(companyType);
    }

    public boolean isClient() {
        return CompanyType.CLIENT.equals(companyType);
    }

    // ✅ MEVCUT: Yardımcı metodlar
    public List<User> getUsers() {
        return userRoles.stream()
                .map(CompanyUserRole::getUser)
                .toList();
    }

    public List<User> getAdmins() {
        return userRoles.stream()
                .filter(cur -> CompanyRole.COMPANY_ADMIN.equals(cur.getRole()))
                .map(CompanyUserRole::getUser)
                .toList();
    }

    public List<User> getManagers() {
        return userRoles.stream()
                .filter(cur -> CompanyRole.COMPANY_MANAGER.equals(cur.getRole()) ||
                        CompanyRole.COMPANY_ADMIN.equals(cur.getRole()))
                .map(CompanyUserRole::getUser)
                .toList();
    }
}