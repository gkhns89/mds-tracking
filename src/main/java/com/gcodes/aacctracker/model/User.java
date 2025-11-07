package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_company_role", columnList = "company_id, global_role"),
                @Index(name = "idx_email_active", columnList = "email, is_active")
        })
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
    private GlobalRole globalRole;

    // ✅ Kullanıcı hangi firmaya ait? (SUPER_ADMIN için NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public User() {
    }

    // ===== HELPER METODLARI =====

    public boolean isSuperAdmin() {
        return GlobalRole.SUPER_ADMIN.equals(this.globalRole);
    }

    public boolean isBrokerAdmin() {
        return GlobalRole.BROKER_ADMIN.equals(this.globalRole);
    }

    public boolean isBrokerUser() {
        return GlobalRole.BROKER_USER.equals(this.globalRole);
    }

    public boolean isClientUser() {
        return GlobalRole.CLIENT_USER.equals(this.globalRole);
    }

    /**
     * BROKER_ADMIN veya BROKER_USER mı?
     */
    public boolean isBrokerStaff() {
        return isBrokerAdmin() || isBrokerUser();
    }

    /**
     * Gümrük firmasını getir
     * - BROKER ise kendi firması
     * - CLIENT ise parent broker
     */
    public Company getBrokerCompany() {
        if (company == null) return null;

        if (company.isBroker()) {
            return company;
        } else {
            return company.getParentBroker();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}