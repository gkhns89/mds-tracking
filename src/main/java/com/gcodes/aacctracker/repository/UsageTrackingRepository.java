package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {

    Optional<UsageTracking> findByBrokerCompany(Company brokerCompany);

    Optional<UsageTracking> findByBrokerCompanyId(Long brokerCompanyId);

    @Query("SELECT ut FROM UsageTracking ut " +
            "WHERE ut.brokerCompany.id = :brokerCompanyId")
    Optional<UsageTracking> findByBrokerCompanyIdWithLock(@Param("brokerCompanyId") Long brokerCompanyId);

    boolean existsByBrokerCompanyId(Long brokerCompanyId);
}