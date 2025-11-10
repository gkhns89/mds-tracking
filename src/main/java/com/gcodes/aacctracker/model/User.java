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

    /**
     * Kullanıcı belirli bir şirkette ADMIN yetkisine sahip mi?
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Her zaman true
     * - BROKER_ADMIN: Kendi broker firması için true
     * - Diğerleri: false
     */
    public boolean isAdminOfCompany(Company targetCompany) {
        if (this.isSuperAdmin()) {
            return true;
        }

        if (this.isBrokerAdmin() && this.company != null && targetCompany != null) {
            // Broker ise, kendi firması mı kontrol et
            if (targetCompany.isBroker()) {
                return this.company.getId().equals(targetCompany.getId());
            }
            // Client ise, bu client'in parent broker'ı mı kontrol et
            if (targetCompany.isClient() && targetCompany.getParentBroker() != null) {
                return this.company.getId().equals(targetCompany.getParentBroker().getId());
            }
        }

        return false;
    }

    /**
     * Kullanıcı belirli bir şirketi yönetebilir mi?
     */
    public boolean canManageCompany(Company targetCompany) {
        return isAdminOfCompany(targetCompany);
    }

    /**
     * Kullanıcı belirli bir broker firmasında yetkili mi?
     */
    public boolean isAuthorizedForBroker(Company brokerCompany) {
        if (this.isSuperAdmin()) {
            return true;
        }

        if (this.isBrokerStaff() && this.company != null) {
            Company myBroker = this.getBrokerCompany();
            return myBroker != null && brokerCompany != null &&
                    myBroker.getId().equals(brokerCompany.getId());
        }

        return false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}