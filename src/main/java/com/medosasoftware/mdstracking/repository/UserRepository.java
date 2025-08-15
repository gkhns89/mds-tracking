package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.GlobalRole;
import com.medosasoftware.mdstracking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // ✅ Global role'e göre kullanıcı sayısını döndür
    long countByGlobalRole(GlobalRole globalRole);

    // ✅ Global role'e göre kullanıcıları getir
    List<User> findByGlobalRole(GlobalRole globalRole);

    // ✅ Aktif kullanıcıları getir
    List<User> findByIsActiveTrue();

    // ✅ Belirli firmadaki kullanıcıları getir
    @Query("SELECT DISTINCT cur.user FROM CompanyUserRole cur WHERE cur.company.id = :companyId")
    List<User> findUsersByCompanyId(@Param("companyId") Long companyId);

    // ✅ Username'e göre kullanıcı bul
    Optional<User> findByUsername(String username);

    // ✅ Email veya username'e göre kullanıcı bul
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);
}