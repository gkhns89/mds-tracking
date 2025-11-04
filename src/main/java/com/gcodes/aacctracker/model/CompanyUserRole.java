package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_user_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_id"}))
@Getter
@Setter
public class CompanyUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRole role;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy; // Kim atadı bu rolü

    public CompanyUserRole() {
    }

    public CompanyUserRole(User user, Company company, CompanyRole role, User assignedBy) {
        this.user = user;
        this.company = company;
        this.role = role;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
    }
}
