package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.model.BrokerSubscription;
import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.SubscriptionPlan;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.BrokerSubscriptionRepository;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.SubscriptionPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private BrokerSubscriptionRepository subscriptionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    /**
     * Abonelik planı oluştur (SUPER_ADMIN)
     */
    public SubscriptionPlan createPlan(SubscriptionPlan plan, User createdBy) {
        if (!createdBy.isSuperAdmin()) {
            throw new RuntimeException("Only SUPER_ADMIN can create subscription plans");
        }

        SubscriptionPlan saved = planRepository.save(plan);
        logger.info("Subscription plan created: {} by {}", saved.getName(), createdBy.getEmail());

        return saved;
    }

    /**
     * Gümrük firması için abonelik oluştur
     */
    public BrokerSubscription createBrokerSubscription(
            Long brokerCompanyId,
            Long planId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            User createdBy
    ) {
        if (!createdBy.isSuperAdmin()) {
            throw new RuntimeException("Only SUPER_ADMIN can create subscriptions");
        }

        Company brokerCompany = companyRepository.findById(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        if (!brokerCompany.isBroker()) {
            throw new RuntimeException("Company must be a CUSTOMS_BROKER");
        }

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        BrokerSubscription subscription = new BrokerSubscription();
        subscription.setBrokerCompany(brokerCompany);
        subscription.setSubscriptionPlan(plan);
        subscription.setStartDate(startDate != null ? startDate : LocalDateTime.now());
        subscription.setEndDate(endDate);
        subscription.setIsActive(true);
        subscription.setCreatedByAdmin(createdBy);

        BrokerSubscription saved = subscriptionRepository.save(subscription);
        logger.info("Broker subscription created for: {} - Plan: {} by {}",
                brokerCompany.getName(), plan.getName(), createdBy.getEmail());

        return saved;
    }

    /**
     * Abonelik güncelle
     */
    public BrokerSubscription updateSubscription(
            Long subscriptionId,
            Long newPlanId,
            LocalDateTime newEndDate,
            Integer customMaxUsers,
            Integer customMaxClients,
            User updatedBy
    ) {
        if (!updatedBy.isSuperAdmin()) {
            throw new RuntimeException("Only SUPER_ADMIN can update subscriptions");
        }

        BrokerSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (newPlanId != null) {
            SubscriptionPlan newPlan = planRepository.findById(newPlanId)
                    .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
            subscription.setSubscriptionPlan(newPlan);
        }

        if (newEndDate != null) {
            subscription.setEndDate(newEndDate);
        }

        subscription.setCustomMaxBrokerUsers(customMaxUsers);
        subscription.setCustomMaxClientCompanies(customMaxClients);

        BrokerSubscription saved = subscriptionRepository.save(subscription);
        logger.info("Broker subscription updated: {} by {}", subscriptionId, updatedBy.getEmail());

        return saved;
    }

    /**
     * Aboneliği iptal et
     */
    public void cancelSubscription(Long subscriptionId, User cancelledBy) {
        if (!cancelledBy.isSuperAdmin()) {
            throw new RuntimeException("Only SUPER_ADMIN can cancel subscriptions");
        }

        BrokerSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setIsActive(false);
        subscription.setEndDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);
        logger.info("Broker subscription cancelled: {} by {}", subscriptionId, cancelledBy.getEmail());
    }

    /**
     * Tüm planları getir
     */
    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    /**
     * Aktif planları getir
     */
    public List<SubscriptionPlan> getActivePlans() {
        return planRepository.findByIsActiveTrue();
    }

    /**
     * Broker'ın aboneliğini getir
     */
    public BrokerSubscription getBrokerSubscription(Long brokerCompanyId) {
        return subscriptionRepository.findActiveBrokerSubscription(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));
    }

    /**
     * Süresi dolmak üzere olan abonelikleri getir
     */
    public List<BrokerSubscription> getSubscriptionsExpiringInDays(int days) {
        return subscriptionRepository.findSubscriptionsExpiringInDays(days);
    }

    /**
     * Süresi dolmuş abonelikleri getir
     */
    public List<BrokerSubscription> getExpiredSubscriptions() {
        return subscriptionRepository.findExpiredSubscriptions();
    }
}