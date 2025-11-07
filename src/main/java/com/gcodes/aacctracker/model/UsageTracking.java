package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usage_tracking")
@Getter
@Setter
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_company_id", nullable = false, unique = true)
    private Company brokerCompany;

    @Column(name = "current_broker_users")
    private Integer currentBrokerUsers = 0;

    @Column(name = "current_client_companies")
    private Integer currentClientCompanies = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public UsageTracking() {
    }

    // ===== HELPER METODLARI =====

    public void incrementBrokerUsers() {
        this.currentBrokerUsers++;
        this.lastUpdated = LocalDateTime.now();
    }

    public void decrementBrokerUsers() {
        if (this.currentBrokerUsers > 0) {
            this.currentBrokerUsers--;
        }
        this.lastUpdated = LocalDateTime.now();
    }

    public void incrementClientCompanies() {
        this.currentClientCompanies++;
        this.lastUpdated = LocalDateTime.now();
    }

    public void decrementClientCompanies() {
        if (this.currentClientCompanies > 0) {
            this.currentClientCompanies--;
        }
        this.lastUpdated = LocalDateTime.now();
    }
}