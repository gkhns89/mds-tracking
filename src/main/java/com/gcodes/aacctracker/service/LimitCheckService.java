package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.exception.SubscriptionExpiredException;
import com.gcodes.aacctracker.exception.SubscriptionNotFoundException;
import com.gcodes.aacctracker.model.BrokerSubscription;
import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.UsageTracking;
import com.gcodes.aacctracker.repository.BrokerSubscriptionRepository;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.UsageTrackingRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LimitCheckService {

    private static final Logger logger = LoggerFactory.getLogger(LimitCheckService.class);

    @Autowired
    private BrokerSubscriptionRepository subscriptionRepository;

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    /**
     * Gümrük firması yeni kullanıcı ekleyebilir mi?
     */
    public boolean canAddBrokerUser(Long brokerCompanyId) {
        try {
            BrokerSubscription subscription = getActiveSubscription(brokerCompanyId);
            UsageTracking usage = getOrCreateUsageTracking(brokerCompanyId);
            int maxUsers = subscription.getEffectiveMaxBrokerUsers();

            return usage.getCurrentBrokerUsers() < maxUsers;
        } catch (Exception e) {
            logger.error("Error checking broker user limit for company: {}", brokerCompanyId, e);
            return false;
        }
    }

    /**
     * Gümrük firması yeni müşteri firma ekleyebilir mi?
     */
    public boolean canAddClientCompany(Long brokerCompanyId) {
        try {
            BrokerSubscription subscription = getActiveSubscription(brokerCompanyId);
            UsageTracking usage = getOrCreateUsageTracking(brokerCompanyId);
            int maxClients = subscription.getEffectiveMaxClientCompanies();

            return usage.getCurrentClientCompanies() < maxClients;
        } catch (Exception e) {
            logger.error("Error checking client company limit for broker: {}", brokerCompanyId, e);
            return false;
        }
    }

    /**
     * Kalan kullanıcı kotasını getir
     */
    public int getRemainingUserQuota(Long brokerCompanyId) {
        try {
            BrokerSubscription subscription = getActiveSubscription(brokerCompanyId);
            UsageTracking usage = getOrCreateUsageTracking(brokerCompanyId);
            int maxUsers = subscription.getEffectiveMaxBrokerUsers();

            return Math.max(0, maxUsers - usage.getCurrentBrokerUsers());
        } catch (Exception e) {
            logger.error("Error getting remaining user quota for broker: {}", brokerCompanyId, e);
            return 0;
        }
    }

    /**
     * Kalan müşteri firma kotasını getir
     */
    public int getRemainingClientQuota(Long brokerCompanyId) {
        try {
            BrokerSubscription subscription = getActiveSubscription(brokerCompanyId);
            UsageTracking usage = getOrCreateUsageTracking(brokerCompanyId);
            int maxClients = subscription.getEffectiveMaxClientCompanies();

            return Math.max(0, maxClients - usage.getCurrentClientCompanies());
        } catch (Exception e) {
            logger.error("Error getting remaining client quota for broker: {}", brokerCompanyId, e);
            return 0;
        }
    }

    /**
     * Limit bilgilerini getir
     */
    public LimitInfo getLimitInfo(Long brokerCompanyId) {
        BrokerSubscription subscription = getActiveSubscription(brokerCompanyId);
        UsageTracking usage = getOrCreateUsageTracking(brokerCompanyId);

        return new LimitInfo(
                subscription.getEffectiveMaxBrokerUsers(),
                usage.getCurrentBrokerUsers(),
                subscription.getEffectiveMaxClientCompanies(),
                usage.getCurrentClientCompanies(),
                subscription.getDaysUntilExpiry()
        );
    }

    /**
     * Aktif aboneliği getir
     */
    private BrokerSubscription getActiveSubscription(Long brokerCompanyId) {
        return subscriptionRepository.findActiveBrokerSubscription(brokerCompanyId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "No active subscription found for broker company: " + brokerCompanyId
                ));
    }

    /**
     * UsageTracking kaydını getir veya oluştur
     */
    private UsageTracking getOrCreateUsageTracking(Long brokerCompanyId) {
        return usageTrackingRepository.findByBrokerCompanyId(brokerCompanyId)
                .orElseGet(() -> {
                    Company broker = companyRepository.findById(brokerCompanyId)
                            .orElseThrow(() -> new RuntimeException("Broker company not found: " + brokerCompanyId));

                    UsageTracking newTracking = new UsageTracking();
                    newTracking.setBrokerCompany(broker);
                    newTracking.setCurrentBrokerUsers(0);
                    newTracking.setCurrentClientCompanies(0);

                    return usageTrackingRepository.save(newTracking);
                });
    }

    /**
     * Kullanım istatistiklerini manuel güncelle
     * (Trigger çalışmazsa veya senkronizasyon gerekirse)
     */
    public void refreshUsageTracking(Long brokerCompanyId) {
        UsageTracking usage = getOrCreateUsageTracking(brokerCompanyId);

        // Aktif broker kullanıcı sayısını say
        long brokerUserCount = userRepository.countBrokerStaffByBrokerCompanyId(brokerCompanyId);

        // Aktif müşteri firma sayısını say
        long clientCount = companyRepository.countByParentBrokerIdAndIsActiveTrue(brokerCompanyId);

        usage.setCurrentBrokerUsers((int) brokerUserCount);
        usage.setCurrentClientCompanies((int) clientCount);

        usageTrackingRepository.save(usage);

        logger.info("Usage tracking refreshed for broker: {} - Users: {}, Clients: {}",
                brokerCompanyId, brokerUserCount, clientCount);
    }

    // ===== INNER CLASS =====

    public static class LimitInfo {
        public final int maxBrokerUsers;
        public final int currentBrokerUsers;
        public final int maxClientCompanies;
        public final int currentClientCompanies;
        public final long daysUntilExpiry;

        public LimitInfo(int maxBrokerUsers, int currentBrokerUsers,
                         int maxClientCompanies, int currentClientCompanies,
                         long daysUntilExpiry) {
            this.maxBrokerUsers = maxBrokerUsers;
            this.currentBrokerUsers = currentBrokerUsers;
            this.maxClientCompanies = maxClientCompanies;
            this.currentClientCompanies = currentClientCompanies;
            this.daysUntilExpiry = daysUntilExpiry;
        }

        public int getRemainingUserQuota() {
            return Math.max(0, maxBrokerUsers - currentBrokerUsers);
        }

        public int getRemainingClientQuota() {
            return Math.max(0, maxClientCompanies - currentClientCompanies);
        }

        public boolean canAddUser() {
            return getRemainingUserQuota() > 0;
        }

        public boolean canAddClient() {
            return getRemainingClientQuota() > 0;
        }
    }
}