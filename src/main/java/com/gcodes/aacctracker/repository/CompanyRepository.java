package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    // ===== TEMEL SORGULAR =====

    Optional<Company> findByName(String name);

    // ✅ YENİ: Firma koduna göre bul
    Optional<Company> findByCompanyCode(String companyCode);

    // ===== TİP BAZLI SORGULAR =====

    List<Company> findByCompanyType(CompanyType companyType);

    List<Company> findByCompanyTypeAndIsActiveTrue(CompanyType companyType);

    long countByCompanyTypeAndIsActiveTrue(CompanyType companyType);

    // ===== PARENT BROKER SORGU LARI =====

    List<Company> findByParentBroker(Company parentBroker);

    List<Company> findByParentBrokerAndIsActiveTrue(Company parentBroker);

    long countByParentBrokerIdAndIsActiveTrue(Long parentBrokerId);

    @Query("SELECT c FROM Company c WHERE c.parentBroker.id = :brokerId AND c.isActive = TRUE")
    List<Company> findActiveClientsByBrokerId(@Param("brokerId") Long brokerId);

    // ===== AKTİFLİK SORGU LARI =====

    List<Company> findByIsActiveTrue();

    long countByIsActiveTrue();

    // ===== ÖZEL SORGULAR =====

    @Query("SELECT c FROM Company c WHERE c.companyType = 'CUSTOMS_BROKER' AND c.isActive = TRUE")
    List<Company> findAllActiveBrokers();

    // ✅ YENİ: Firma kodu olan aktif broker'lar (kayıt ekranı için)
    @Query("SELECT c FROM Company c WHERE c.companyType = 'CUSTOMS_BROKER' " +
            "AND c.isActive = TRUE AND c.companyCode IS NOT NULL " +
            "ORDER BY c.name ASC")
    List<Company> findAllActiveBrokersWithCodes();

    @Query("SELECT COUNT(c) FROM Company c WHERE c.companyType = 'CLIENT' " +
            "AND c.parentBroker.id = :brokerId AND c.isActive = TRUE")
    long countActiveClientsByBrokerId(@Param("brokerId") Long brokerId);
}