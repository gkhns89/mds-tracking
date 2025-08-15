package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.model.*;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import com.medosasoftware.mdstracking.repository.CompanyUserRoleRepository;
import com.medosasoftware.mdstracking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRoleRepository companyUserRoleRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public CompanyUserRole assignRoleToUserInCompany(Long userId, Long companyId,
                                                     CompanyRole role, User assignedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        Optional<CompanyUserRole> existingRole =
                companyUserRoleRepository.findByUserAndCompany(user, company);

        if (existingRole.isPresent()) {
            CompanyUserRole cur = existingRole.get();
            cur.setRole(role);
            cur.setAssignedBy(assignedBy);
            return companyUserRoleRepository.save(cur);
        } else {
            CompanyUserRole newRole = new CompanyUserRole(user, company, role, assignedBy);
            return companyUserRoleRepository.save(newRole);
        }
    }

    public void removeUserFromCompany(Long userId, Long companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        companyUserRoleRepository.findByUserAndCompany(user, company)
                .ifPresent(companyUserRoleRepository::delete);
    }

    public List<Company> getUserAccessibleCompanies(User user) {
        if (user.isSuperAdmin()) {
            return companyRepository.findAll();
        }
        return companyUserRoleRepository.findCompaniesByUser(user);
    }

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

    public boolean canUserAssignRoleInCompany(User user, Long companyId, CompanyRole roleToAssign) {
        if (user.isSuperAdmin()) return true;

        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) return false;

        CompanyRole userRole = user.getRoleInCompany(company);
        if (userRole == null) return false;

        switch (userRole) {
            case COMPANY_ADMIN:
                return true;
            case COMPANY_MANAGER:
                return roleToAssign == CompanyRole.COMPANY_USER;
            default:
                return false;
        }
    }

    public boolean canUserManageCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;
        return user.canManageUsersInCompany(company);
    }

    public boolean canUserViewCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;
        return user.getRoleInCompany(company) != null;
    }

    public List<User> getCompanyUsers(Long companyId) {
        return userRepository.findUsersByCompanyId(companyId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

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

    private List<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        user.getCompanyRoles().forEach(cur -> {
            String authority = "ROLE_" + cur.getRole().name() + "_COMPANY_" + cur.getCompany().getId();
            authorities.add(new SimpleGrantedAuthority(authority));
        });

        return authorities;
    }
}