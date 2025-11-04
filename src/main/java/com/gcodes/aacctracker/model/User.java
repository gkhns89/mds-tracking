package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "global_role")
    private GlobalRole globalRole = GlobalRole.USER; // ✅ Varsayılan USER

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ Firma bazlı roller - One-to-Many ilişki
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CompanyUserRole> companyRoles = new ArrayList<>();

    public User() {
    }

    // ✅ Yardımcı metodlar
    public boolean isSuperAdmin() {
        return GlobalRole.SUPER_ADMIN.equals(this.globalRole);
    }

    // Belirli bir firmadaki rolü getir
    public CompanyRole getRoleInCompany(Company company) {
        return companyRoles.stream()
                .filter(cur -> cur.getCompany().getId().equals(company.getId()))
                .map(CompanyUserRole::getRole)
                .findFirst()
                .orElse(null);
    }

    // Kullanıcının erişebileceği firmaları getir
    public List<Company> getAccessibleCompanies() {
        return companyRoles.stream()
                .map(CompanyUserRole::getCompany)
                .toList();
    }

    // Belirli bir firmada admin mi?
    public boolean isCompanyAdmin(Company company) {
        return CompanyRole.COMPANY_ADMIN.equals(getRoleInCompany(company));
    }

    // Belirli bir firmada manager veya admin mi?
    public boolean canManageUsersInCompany(Company company) {
        CompanyRole role = getRoleInCompany(company);
        return CompanyRole.COMPANY_ADMIN.equals(role) ||
                CompanyRole.COMPANY_MANAGER.equals(role);
    }
}