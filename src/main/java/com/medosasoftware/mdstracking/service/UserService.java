package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.model.*;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import com.medosasoftware.mdstracking.repository.CompanyUserRoleRepository;
import com.medosasoftware.mdstracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRoleRepository companyUserRoleRepository;

    // ✅ Kullanıcı oluşturma
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // ✅ Kullanıcıya firma rolü atama
    public CompanyUserRole assignRoleToUserInCompany(Long userId, Long companyId,
                                                     CompanyRole role, User assignedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Mevcut rolü kontrol et
        Optional<CompanyUserRole> existingRole =
                companyUserRoleRepository.findByUserAndCompany(user, company);

        if (existingRole.isPresent()) {
            // Rolü güncelle
            CompanyUserRole cur = existingRole.get();
            cur.setRole(role);
            cur.setAssignedBy(assignedBy);
            return companyUserRoleRepository.save(cur);
        } else {
            // Yeni rol oluştur
            CompanyUserRole newRole = new CompanyUserRole(user, company, role, assignedBy);
            return companyUserRoleRepository.save(newRole);
        }
    }

    // ✅ Kullanıcının firmadaki rolünü kaldırma
    public void removeUserFromCompany(Long userId, Long companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        companyUserRoleRepository.findByUserAndCompany(user, company)
                .ifPresent(companyUserRoleRepository::delete);
    }

    // ✅ Kullanıcının erişebildiği firmalar
    public List<Company> getUserAccessibleCompanies(User user) {
        if (user.isSuperAdmin()) {
            return companyRepository.findAll(); // Super admin tüm firmaları görür
        }
        return companyUserRoleRepository.findCompaniesByUser(user);
    }

    // ✅ Kullanıcının yönetebileceği firmalar
    public List<Company> getUserManageableCompanies(User user) {
        if (user.isSuperAdmin()) {
            return companyRepository.findAll();
        }

        return user.getCompanyRoles().stream()
                .filter(cur -> cur.getRole() == CompanyRole.COMPANY_ADMIN ||
                        cur.getRole() == CompanyRole.COMPANY_MANAGER)
                .map(CompanyUserRole::getCompany)
                .toList();
    }

    // ✅ Yetki kontrolleri
    public boolean canUserManageCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;
        return user.canManageUsersInCompany(company);
    }

    public boolean canUserViewCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;
        return user.getRoleInCompany(company) != null;
    }

    // ✅ Firma kullanıcılarını listeleme
    public List<User> getCompanyUsers(Long companyId) {
        return userRepository.findUsersByCompanyId(companyId);
    }

    // ✅ Super admin kontrolü
    public boolean isSuperAdmin(User user) {
        return user.isSuperAdmin();
    }

    // ✅ Email ile kullanıcı bulma
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ✅ ID ile kullanıcı bulma
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // ✅ Spring Security UserDetailsService implementation
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = findByEmail(username);
        if (!user.isPresent()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        User foundUser = user.get();
        List<GrantedAuthority> authorities = getAuthorities(foundUser);

        return new org.springframework.security.core.userdetails.User(
                foundUser.getEmail(),
                foundUser.getPassword(),
                foundUser.getIsActive(),
                true, true, true,
                authorities
        );
    }

    // ✅ Spring Security authorities oluşturma
    private List<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Global role
        if (user.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Company-specific roles
        user.getCompanyRoles().forEach(cur -> {
            String authority = "ROLE_" + cur.getRole().name() + "_COMPANY_" + cur.getCompany().getId();
            authorities.add(new SimpleGrantedAuthority(authority));
        });

        return authorities;
    }
}