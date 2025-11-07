package com.gcodes.aacctracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_broker_users", nullable = false)
    private Integer maxBrokerUsers = 5;

    @Column(name = "max_client_companies", nullable = false)
    private Integer maxClientCompanies = 20;

    @Column(name = "monthly_price", precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "JSON")
    private String features;  // JSON array: ["Excel Import", "Email Notifications"]

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public SubscriptionPlan() {
    }

    public SubscriptionPlan(String name, Integer maxBrokerUsers, Integer maxClientCompanies,
                            BigDecimal monthlyPrice, BigDecimal yearlyPrice) {
        this.name = name;
        this.maxBrokerUsers = maxBrokerUsers;
        this.maxClientCompanies = maxClientCompanies;
        this.monthlyPrice = monthlyPrice;
        this.yearlyPrice = yearlyPrice;
    }
}