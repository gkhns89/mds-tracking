package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CompanyRole;
import com.gcodes.aacctracker.model.CompanyUserRole;
import com.gcodes.aacctracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyUserRoleRepository extends JpaRepository<CompanyUserRole, Long> {

    // Kullanıcının belirli firmadaki rolü
    Optional<CompanyUserRole> findByUserAndCompany(User user, Company company);

    // Kullanıcının tüm firma rolleri
    List<CompanyUserRole> findByUser(User user);

    // Firmanın tüm kullanıcı rolleri
    List<CompanyUserRole> findByCompany(Company company);

    // Firmadaki belirli roldeki kullanıcılar
    List<CompanyUserRole> findByCompanyAndRole(Company company, CompanyRole role);

    // Kullanıcının erişebildiği firmalar
    @Query("SELECT cur.company FROM CompanyUserRole cur WHERE cur.user = :user")
    List<Company> findCompaniesByUser(@Param("user") User user);

    // Firmada yönetici yetkisi olan kullanıcılar
    @Query("SELECT cur FROM CompanyUserRole cur WHERE cur.company = :company AND cur.role IN ('COMPANY_ADMIN', 'COMPANY_MANAGER')")
    List<CompanyUserRole> findManagersByCompany(@Param("company") Company company);

    // Kullanıcı-firma-rol kombinasyonu var mı?
    boolean existsByUserAndCompanyAndRole(User user, Company company, CompanyRole role);

    // Kullanıcının firma yetkisi var mı?
    boolean existsByUserAndCompany(User user, Company company);
}