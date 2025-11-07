package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.GlobalRole;
import com.gcodes.aacctracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ===== TEMEL SORGULAR =====

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

    // ===== ROL BAZLI SORGULAR =====

    long countByGlobalRole(GlobalRole globalRole);

    List<User> findByGlobalRole(GlobalRole globalRole);

    Optional<User> findFirstByGlobalRole(GlobalRole globalRole);

    // ===== AKTİFLİK SORGU LARI =====

    List<User> findByIsActiveTrue();

    long countByIsActiveTrue();

    // ===== ŞİRKET BAZLI SORGULAR =====

    List<User> findByCompany(Company company);

    List<User> findByCompanyAndIsActiveTrue(Company company);

    long countByCompanyIdAndIsActiveTrue(Long companyId);

    long countByCompanyIdAndGlobalRoleInAndIsActiveTrue(Long companyId, List<GlobalRole> roles);

    List<User> findByCompanyAndGlobalRoleInAndIsActiveTrue(Company company, List<GlobalRole> roles);

    Optional<User> findByCompanyAndGlobalRoleAndIsActiveTrue(Company company, GlobalRole role);

    Optional<User> findByCompanyAndGlobalRole(Company company, GlobalRole role);

    // ===== BROKER FİRMASI KULLANICILARI =====

    @Query("SELECT u FROM User u WHERE u.company.id = :brokerCompanyId " +
            "AND u.globalRole IN ('BROKER_ADMIN', 'BROKER_USER') " +
            "AND u.isActive = TRUE")
    List<User> findBrokerStaffByBrokerCompanyId(@Param("brokerCompanyId") Long brokerCompanyId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :brokerCompanyId " +
            "AND u.globalRole IN ('BROKER_ADMIN', 'BROKER_USER') " +
            "AND u.isActive = TRUE")
    long countBrokerStaffByBrokerCompanyId(@Param("brokerCompanyId") Long brokerCompanyId);

    // ===== MÜŞTERİ FİRMASI KULLANICILARI =====

    @Query("SELECT u FROM User u WHERE u.company.parentBroker.id = :brokerCompanyId " +
            "AND u.globalRole = 'CLIENT_USER' " +
            "AND u.isActive = TRUE")
    List<User> findClientUsersByBrokerCompanyId(@Param("brokerCompanyId") Long brokerCompanyId);
}