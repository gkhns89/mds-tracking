package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    // ✅ Mevcut metodlar...
    Optional<Company> findByName(String name);

    Optional<Company> findByCompanyCode(String companyCode);

    // ✅ YENİ: Parent broker ile birlikte getir (N+1 önlenir)
    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.parentBroker WHERE c.id = :id")
    Optional<Company> findByIdWithParentBroker(@Param("id") Long id);

    // ✅ YENİ: Tüm ilişkileri getir
    @Query("SELECT DISTINCT c FROM Company c " +
            "LEFT JOIN FETCH c.parentBroker " +
            "LEFT JOIN FETCH c.users " +
            "WHERE c.id = :id")
    Optional<Company> findByIdWithDetails(@Param("id") Long id);

    // ✅ YENİ: Aktif şirketleri parent broker ile getir
    @Query("SELECT DISTINCT c FROM Company c " +
            "LEFT JOIN FETCH c.parentBroker " +
            "WHERE c.isActive = true " +
            "ORDER BY c.name")
    List<Company> findAllActiveWithParentBroker();

    // ✅ YENİ: Broker'ların client'larını tek sorguda getir
    @Query("SELECT DISTINCT c FROM Company c " +
            "LEFT JOIN FETCH c.clients " +
            "WHERE c.companyType = 'CUSTOMS_BROKER' AND c.isActive = true")
    List<Company> findAllBrokersWithClients();

    // ✅ YENİ: Belirli broker'ın client'larını getir
    @Query("SELECT c FROM Company c " +
            "WHERE c.parentBroker.id = :brokerId AND c.isActive = true")
    List<Company> findClientsByBrokerId(@Param("brokerId") Long brokerId);

    // ✅ YENİ: EntityGraph kullanarak
    @EntityGraph(attributePaths = {"parentBroker"})
    List<Company> findAllByIsActiveTrue();

    @EntityGraph(attributePaths = {"parentBroker", "users"})
    Optional<Company> findWithDetailsByIdAndIsActiveTrue(Long id);

    // Mevcut metodlar...
    List<Company> findByCompanyType(CompanyType companyType);

    List<Company> findByCompanyTypeAndIsActiveTrue(CompanyType companyType);

    long countByCompanyTypeAndIsActiveTrue(CompanyType companyType);

    List<Company> findByParentBroker(Company parentBroker);

    List<Company> findByParentBrokerAndIsActiveTrue(Company parentBroker);

    long countByParentBrokerIdAndIsActiveTrue(Long parentBrokerId);

    @Query("SELECT c FROM Company c WHERE c.parentBroker.id = :brokerId AND c.isActive = TRUE")
    List<Company> findActiveClientsByBrokerId(@Param("brokerId") Long brokerId);

    List<Company> findByIsActiveTrue();

    long countByIsActiveTrue();

    @Query("SELECT c FROM Company c WHERE c.companyType = 'CUSTOMS_BROKER' AND c.isActive = TRUE")
    List<Company> findAllActiveBrokers();

    @Query("SELECT c FROM Company c WHERE c.companyType = 'CUSTOMS_BROKER' " +
            "AND c.isActive = TRUE AND c.companyCode IS NOT NULL " +
            "ORDER BY c.name ASC")
    List<Company> findAllActiveBrokersWithCodes();

    @Query("SELECT COUNT(c) FROM Company c WHERE c.companyType = 'CLIENT' " +
            "AND c.parentBroker.id = :brokerId AND c.isActive = TRUE")
    long countActiveClientsByBrokerId(@Param("brokerId") Long brokerId);
}