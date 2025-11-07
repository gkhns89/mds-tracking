package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.BrokerSubscription;
import com.gcodes.aacctracker.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BrokerSubscriptionRepository extends JpaRepository<BrokerSubscription, Long> {

    // ===== AKTİF ABONELİK =====

    @Query("SELECT bs FROM BrokerSubscription bs " +
            "WHERE bs.brokerCompany.id = :brokerCompanyId " +
            "AND bs.isActive = TRUE " +
            "AND (bs.endDate IS NULL OR bs.endDate > :now) " +
            "ORDER BY bs.createdAt DESC")
    Optional<BrokerSubscription> findActiveBrokerSubscription(
            @Param("brokerCompanyId") Long brokerCompanyId,
            @Param("now") LocalDateTime now
    );

    default Optional<BrokerSubscription> findActiveBrokerSubscription(Long brokerCompanyId) {
        return findActiveBrokerSubscription(brokerCompanyId, LocalDateTime.now());
    }

    // ===== BROKER ABONELİKLERİ =====

    List<BrokerSubscription> findByBrokerCompany(Company brokerCompany);

    List<BrokerSubscription> findByBrokerCompanyAndIsActiveTrue(Company brokerCompany);

    // ===== SÜRE DOLMAK ÜZERE OLANLAR =====

    @Query("SELECT bs FROM BrokerSubscription bs " +
            "WHERE bs.isActive = TRUE " +
            "AND bs.endDate IS NOT NULL " +
            "AND bs.endDate BETWEEN :now AND :endDate")
    List<BrokerSubscription> findSubscriptionsExpiringBetween(
            @Param("now") LocalDateTime now,
            @Param("endDate") LocalDateTime endDate
    );

    default List<BrokerSubscription> findSubscriptionsExpiringInDays(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(days);
        return findSubscriptionsExpiringBetween(now, endDate);
    }

    // ===== SÜRESİ DOLMUŞ ABONELİKLER =====

    @Query("SELECT bs FROM BrokerSubscription bs " +
            "WHERE bs.isActive = TRUE " +
            "AND bs.endDate IS NOT NULL " +
            "AND bs.endDate < :now")
    List<BrokerSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    default List<BrokerSubscription> findExpiredSubscriptions() {
        return findExpiredSubscriptions(LocalDateTime.now());
    }

    // ===== İSTATİSTİKLER =====

    long countByIsActiveTrue();

    @Query("SELECT COUNT(bs) FROM BrokerSubscription bs " +
            "WHERE bs.subscriptionPlan.id = :planId AND bs.isActive = TRUE")
    long countActiveSubscriptionsByPlanId(@Param("planId") Long planId);
}