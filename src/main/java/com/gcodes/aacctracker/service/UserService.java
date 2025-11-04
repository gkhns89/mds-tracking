package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.dto.UserUpdateRequest;
import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CompanyRole;
import com.gcodes.aacctracker.model.CompanyUserRole;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.CompanyUserRoleRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    public User createUser(User user) {
        // Email uniqueness kontrolü
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }

        // Username uniqueness kontrolü
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }

        return userRepository.save(user);
    }

    // ✅ YENİ: Kullanıcı güncelleme metodu
    public User updateUser(Long userId, UserUpdateRequest request, User updatingUser) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Email güncellenecekse uniqueness kontrol et
        if (StringUtils.hasText(request.getEmail()) &&
                !request.getEmail().equals(userToUpdate.getEmail())) {
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new RuntimeException("Email already exists: " + request.getEmail());
            }
            userToUpdate.setEmail(request.getEmail());
        }

        // Username güncellenecekse uniqueness kontrol et
        if (StringUtils.hasText(request.getUsername()) &&
                !request.getUsername().equals(userToUpdate.getUsername())) {
            Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new RuntimeException("Username already exists: " + request.getUsername());
            }
            userToUpdate.setUsername(request.getUsername());
        }

        // Şifre güncellenecekse encode et
        if (StringUtils.hasText(request.getPassword())) {
            userToUpdate.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Aktiflik durumu - sadece SUPER_ADMIN değiştirebilir
        if (request.getIsActive() != null) {
            if (!updatingUser.isSuperAdmin()) {
                throw new RuntimeException("Only SUPER_ADMIN can change user active status");
            }
            userToUpdate.setIsActive(request.getIsActive());
        }

        return userRepository.save(userToUpdate);
    }

    // ✅ YENİ: Kullanıcı silme (soft delete)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Soft delete - aktiflik durumunu false yap
        user.setIsActive(false);
        userRepository.save(user);

        logger.info("User soft deleted: {} (ID: {})", user.getEmail(), userId);
    }

    // ✅ YENİ: Yetki bazlı kullanıcı listesi
    public List<User> getAllUsers(User currentUser) {
        if (currentUser.isSuperAdmin()) {
            // SUPER_ADMIN tüm kullanıcıları görebilir
            return userRepository.findAll();
        } else {
            // Normal kullanıcılar sadece kendi şirketlerindeki kullanıcıları görebilir
            List<Company> manageableCompanies = getUserManageableCompanies(currentUser);
            List<User> users = new ArrayList<>();

            for (Company company : manageableCompanies) {
                List<User> companyUsers = getCompanyUsers(company.getId());
                for (User user : companyUsers) {
                    if (!users.contains(user)) {
                        users.add(user);
                    }
                }
            }

            return users;
        }
    }

    // ✅ MEVCUT: Diğer metodlar (değişiklik yok)
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

        return switch (userRole) {
            case COMPANY_ADMIN -> true;
            case COMPANY_MANAGER -> roleToAssign == CompanyRole.COMPANY_USER;
            default -> false;
        };
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

    // ✅ YENİ: Kullanıcının başka bir kullanıcıyı düzenleyip düzenleyemeyeceğini kontrol et
    public boolean canUserEditUser(User currentUser, Long targetUserId) {
        // SUPER_ADMIN herkesi düzenleyebilir
        if (currentUser.isSuperAdmin()) {
            return true;
        }

        // Kullanıcı sadece kendisini düzenleyebilir
        if (currentUser.getId().equals(targetUserId)) {
            return true;
        }

        // Şirket yöneticileri kendi şirketlerindeki kullanıcıları düzenleyebilir
        Optional<User> targetUser = findById(targetUserId);
        if (targetUser.isPresent()) {
            List<Company> manageableCompanies = getUserManageableCompanies(currentUser);
            List<Company> targetUserCompanies = getUserAccessibleCompanies(targetUser.get());

            // Ortak şirket var mı kontrol et
            for (Company company : manageableCompanies) {
                if (targetUserCompanies.contains(company)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = findByEmail(username);
        if (user.isEmpty()) {
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