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

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ Firma-kullanıcı rolleri - One-to-Many ilişki
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CompanyUserRole> userRoles = new ArrayList<>();

    public Company() {
    }

    // ✅ Yardımcı metodlar
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