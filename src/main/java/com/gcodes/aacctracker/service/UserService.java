package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.dto.UserUpdateRequest;
import com.gcodes.aacctracker.exception.LimitExceededException;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import com.gcodes.aacctracker.repository.UsageTrackingRepository;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private LimitCheckService limitCheckService;

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    // ==========================================
    // KULLANICI OLUŞTURMA VE GÜNCELLEME
    // ==========================================

    /**
     * Kullanıcı oluştur (limit kontrolü ile)
     * <p>
     * KURALLAR:
     * - Email ve username benzersiz olmalı
     * - BROKER_ADMIN/BROKER_USER ise abonelik limiti kontrolü yapılır
     * - CLIENT_USER ise, müşteri firması başına sadece 1 kullanıcı olabilir
     * - UsageTracking otomatik güncellenir
     */
    // ✅ Write işlemi - Transactional
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public User createUser(User user) {
        logger.info("Creating user: {}", user.getEmail());

        try {
            // Email uniqueness kontrolü
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + user.getEmail());
            }

            // Username uniqueness kontrolü
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new RuntimeException("Username already exists: " + user.getUsername());
            }

            // ✅ BROKER_ADMIN veya BROKER_USER ise limit kontrolü
            if (user.isBrokerStaff() && user.getCompany() != null && user.getIsActive()) {
                Company company = user.getCompany();
                Company brokerCompany = company.getBrokerCompany();

                if (brokerCompany == null) {
                    throw new RuntimeException("Cannot determine broker company for user");
                }

                // Limit kontrolü yap
                if (!limitCheckService.canAddBrokerUser(brokerCompany.getId())) {
                    int remaining = limitCheckService.getRemainingUserQuota(brokerCompany.getId());
                    throw new LimitExceededException(
                            "User limit exceeded. Remaining quota: " + remaining
                    );
                }
            }

            // ✅ CLIENT_USER ise, müşteri firmasına zaten kullanıcı var mı kontrol et
            if (user.isClientUser() && user.getCompany() != null) {
                Company clientCompany = user.getCompany();

                long existingUsers = userRepository.countByCompanyIdAndIsActiveTrue(clientCompany.getId());
                if (existingUsers > 0) {
                    throw new RuntimeException(
                            "This client company already has a user. Only one user per client company is allowed."
                    );
                }
            }

            // ✅ Kullanıcıyı kaydet
            User savedUser = userRepository.save(user);

            // ✅ UsageTracking'i güncelle (aynı transaction içinde)
            if (savedUser.isBrokerStaff() && savedUser.getCompany() != null) {
                updateUsageTrackingAfterUserAdd(savedUser);
            }

            logger.info("User created successfully: {} - Role: {}",
                    savedUser.getEmail(), savedUser.getGlobalRole());

            return savedUser;

        } catch (LimitExceededException e) {
            logger.warn("User creation failed - Limit exceeded: {}", e.getMessage());
            throw e; // Re-throw to rollback transaction
        } catch (Exception e) {
            logger.error("User creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    // ✅ Write işlemi - Transactional
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public User updateUser(Long userId, UserUpdateRequest request, User updatingUser) {
        logger.info("Updating user: {}", userId);

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        try {
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
                logger.info("Password updated for user: {}", userToUpdate.getEmail());
            }

            // Aktiflik durumu - sadece SUPER_ADMIN veya BROKER_ADMIN değiştirebilir
            if (request.getIsActive() != null) {
                if (!updatingUser.isSuperAdmin() && !updatingUser.isBrokerAdmin()) {
                    throw new RuntimeException("Only SUPER_ADMIN or BROKER_ADMIN can change user active status");
                }

                boolean wasActive = userToUpdate.getIsActive();
                boolean willBeActive = request.getIsActive();

                // Kullanıcı devre dışı bırakılıyorsa
                if (wasActive && !willBeActive) {
                    userToUpdate.setIsActive(false);
                    updateUsageTrackingAfterUserRemove(userToUpdate);
                    logger.info("User deactivated: {}", userToUpdate.getEmail());
                }
                // Kullanıcı aktifleştiriliyorsa
                else if (!wasActive && willBeActive) {
                    // Aktifleştirmeden önce limit kontrolü yap
                    if (userToUpdate.isBrokerStaff() && userToUpdate.getCompany() != null) {
                        Company brokerCompany = userToUpdate.getBrokerCompany();
                        if (brokerCompany != null && !limitCheckService.canAddBrokerUser(brokerCompany.getId())) {
                            throw new LimitExceededException("Cannot reactivate user: User limit exceeded");
                        }
                    }

                    userToUpdate.setIsActive(true);
                    updateUsageTrackingAfterUserAdd(userToUpdate);
                    logger.info("User activated: {}", userToUpdate.getEmail());
                }
            }

            User updated = userRepository.save(userToUpdate);
            logger.info("User updated successfully: {}", updated.getEmail());

            return updated;

        } catch (LimitExceededException e) {
            logger.warn("User update failed - Limit exceeded: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("User update failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    // ✅ Write işlemi - Transactional
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public void deleteUser(Long userId) {
        logger.info("Deleting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        try {
            // Soft delete
            user.setIsActive(false);
            userRepository.save(user);

            // UsageTracking'i güncelle (aynı transaction içinde)
            updateUsageTrackingAfterUserRemove(user);

            logger.info("User soft deleted successfully: {} (ID: {})", user.getEmail(), userId);

        } catch (Exception e) {
            logger.error("User deletion failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    // ✅ Write işlemi - Transactional
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public User activateUser(Long userId, User approvingUser) {
        logger.info("Activating user: {} by {}", userId, approvingUser.getEmail());

        User userToActivate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Zaten aktif mi?
            if (userToActivate.getIsActive()) {
                throw new RuntimeException("User is already active");
            }

            // Yetki kontrolü
            if (!canUserApproveUser(approvingUser, userToActivate)) {
                throw new RuntimeException("You don't have permission to approve this user");
            }

            // CLIENT_USER için müşteri firmasında başka aktif kullanıcı var mı kontrol et
            if (userToActivate.isClientUser() && userToActivate.getCompany() != null) {
                Company clientCompany = userToActivate.getCompany();
                long existingUsers = userRepository.countByCompanyIdAndIsActiveTrue(clientCompany.getId());

                if (existingUsers > 0) {
                    throw new RuntimeException(
                            "This client company already has an active user. " +
                                    "Only one user per client company is allowed."
                    );
                }
            }

            // BROKER_USER için limit kontrolü
            if (userToActivate.isBrokerUser() && userToActivate.getCompany() != null) {
                Company brokerCompany = userToActivate.getCompany();

                if (!limitCheckService.canAddBrokerUser(brokerCompany.getId())) {
                    int remaining = limitCheckService.getRemainingUserQuota(brokerCompany.getId());
                    throw new LimitExceededException(
                            "User limit exceeded. Remaining quota: " + remaining
                    );
                }
            }

            // Kullanıcıyı aktifleştir
            userToActivate.setIsActive(true);
            User activated = userRepository.save(userToActivate);

            // UsageTracking güncelle (aynı transaction içinde)
            if (activated.isBrokerStaff()) {
                updateUsageTrackingAfterUserAdd(activated);
            }

            logger.info("User activated successfully: {} by {}",
                    activated.getEmail(), approvingUser.getEmail());

            return activated;

        } catch (LimitExceededException e) {
            logger.warn("User activation failed - Limit exceeded: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("User activation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to activate user: " + e.getMessage(), e);
        }
    }

    // ✅ Write işlemi - Transactional
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public void rejectUser(Long userId, String reason, User rejectingUser) {
        logger.info("Rejecting user: {} by {} - Reason: {}",
                userId, rejectingUser.getEmail(), reason);

        User userToReject = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Sadece pasif kullanıcılar reddedilebilir
            if (userToReject.getIsActive()) {
                throw new RuntimeException("Can only reject inactive users");
            }

            // Yetki kontrolü
            if (!canUserApproveUser(rejectingUser, userToReject)) {
                throw new RuntimeException("You don't have permission to reject this user");
            }

            // Kullanıcıyı sil (hard delete - henüz sisteme girmedi)
            userRepository.delete(userToReject);

            logger.info("User rejected and deleted successfully: {} by {}",
                    userToReject.getEmail(), rejectingUser.getEmail());

        } catch (Exception e) {
            logger.error("User rejection failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to reject user: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // KULLANICI SORGULAMA
    // ==========================================

    /**
     * Tüm kullanıcıları getir (yetki bazlı)
     * <p>
     * - SUPER_ADMIN: Tüm kullanıcıları görebilir
     * - BROKER_ADMIN: Kendi firmasındaki + müşteri firmalarındaki kullanıcıları görebilir
     * - BROKER_USER: Sadece kendisini görebilir
     * - CLIENT_USER: Sadece kendisini görebilir
     */
    public List<User> getAllUsers(User currentUser) {
        if (currentUser.isSuperAdmin()) {
            // SUPER_ADMIN tüm kullanıcıları görebilir
            return userRepository.findAll();
        } else if (currentUser.isBrokerAdmin()) {
            // BROKER_ADMIN kendi firmasındaki tüm kullanıcıları + müşteri kullanıcılarını görebilir
            Company brokerCompany = currentUser.getCompany();

            List<User> allUsers = new ArrayList<>();

            // 1. Kendi firmasındaki broker kullanıcıları
            allUsers.addAll(userRepository.findByCompanyAndIsActiveTrue(brokerCompany));

            // 2. Müşteri firmalarının kullanıcıları
            List<Company> clientCompanies = companyRepository.findByParentBrokerAndIsActiveTrue(brokerCompany);
            for (Company client : clientCompanies) {
                allUsers.addAll(userRepository.findByCompanyAndIsActiveTrue(client));
            }

            return allUsers;
        } else {
            // BROKER_USER ve CLIENT_USER sadece kendilerini görebilir
            return List.of(currentUser);
        }
    }

    /**
     * Email ile kullanıcı bul
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailWithCompany(email); // ✅ Optimized
    }

    /**
     * ID ile kullanıcı bul
     */
    public Optional<User> findById(Long id) {
        return userRepository.findByIdWithCompanyDetails(id); // ✅ Optimized
    }

    /**
     * Username ile kullanıcı bul
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Gümrük firmasının kullanıcıları
     * (Sadece BROKER_ADMIN ve BROKER_USER)
     */
    public List<User> getBrokerUsers(Long brokerCompanyId) {
        Company broker = companyRepository.findById(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return userRepository.findByCompanyAndGlobalRoleInAndIsActiveTrue(
                broker,
                List.of(GlobalRole.BROKER_ADMIN, GlobalRole.BROKER_USER)
        );
    }

    /**
     * Müşteri firmasının kullanıcısı (tek kullanıcı)
     */
    public Optional<User> getClientUser(Long clientCompanyId) {
        Company client = companyRepository.findById(clientCompanyId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return userRepository.findByCompanyAndGlobalRoleAndIsActiveTrue(
                client,
                GlobalRole.CLIENT_USER
        );
    }

    /**
     * Belirli şirketteki tüm kullanıcılar
     */
    public List<User> getCompanyUsers(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        return userRepository.findByCompanyAndIsActiveTrue(company);
    }

    // ==========================================
    // YETKİ KONTROL METODLARI
    // ==========================================

    /**
     * Kullanıcı başka bir kullanıcıyı düzenleyebilir mi?
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Herkesi düzenleyebilir
     * - Kullanıcı: Kendisini düzenleyebilir
     * - BROKER_ADMIN: Kendi broker firmasındaki kullanıcıları düzenleyebilir
     */
    public boolean canUserEditUser(User currentUser, Long targetUserId) {
        // SUPER_ADMIN herkesi düzenleyebilir
        if (currentUser.isSuperAdmin()) {
            return true;
        }

        // Kullanıcı sadece kendisini düzenleyebilir
        if (currentUser.getId().equals(targetUserId)) {
            return true;
        }

        // BROKER_ADMIN kendi firmasındaki kullanıcıları düzenleyebilir
        if (currentUser.isBrokerAdmin()) {
            Optional<User> targetUser = findById(targetUserId);
            if (targetUser.isPresent()) {
                User target = targetUser.get();
                Company currentBroker = currentUser.getCompany();
                Company targetBroker = target.getBrokerCompany();

                return currentBroker != null && targetBroker != null &&
                        currentBroker.getId().equals(targetBroker.getId());
            }
        }

        return false;
    }

    /**
     * Kullanıcı belirli bir firmayı görebilir mi?
     */
    public boolean canUserViewCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;

        if (user.isBrokerStaff()) {
            Company userBroker = user.getBrokerCompany();
            Company targetBroker = company.getBrokerCompany();
            return userBroker != null && targetBroker != null &&
                    userBroker.getId().equals(targetBroker.getId());
        }

        if (user.isClientUser()) {
            return user.getCompany() != null &&
                    user.getCompany().getId().equals(company.getId());
        }

        return false;
    }

    /**
     * Kullanıcının erişebileceği firmalar
     */
    public List<Company> getUserAccessibleCompanies(User user) {
        if (user.isSuperAdmin()) {
            return companyRepository.findAll();
        }

        List<Company> companies = new ArrayList<>();

        if (user.getCompany() != null) {
            companies.add(user.getCompany());

            // BROKER_ADMIN veya BROKER_USER ise, müşteri firmalarını da ekle
            if (user.isBrokerStaff() && user.getCompany().isBroker()) {
                companies.addAll(
                        companyRepository.findByParentBrokerAndIsActiveTrue(user.getCompany())
                );
            }
        }

        return companies;
    }

    /**
     * Kullanıcının yönetebileceği firmalar
     * (Sadece SUPER_ADMIN ve BROKER_ADMIN)
     */
    public List<Company> getUserManageableCompanies(User user) {
        if (user.isSuperAdmin()) {
            return companyRepository.findAll();
        }

        if (user.isBrokerAdmin() && user.getCompany() != null) {
            List<Company> companies = new ArrayList<>();
            companies.add(user.getCompany());

            if (user.getCompany().isBroker()) {
                companies.addAll(
                        companyRepository.findByParentBrokerAndIsActiveTrue(user.getCompany())
                );
            }

            return companies;
        }

        return new ArrayList<>();
    }

    // ==========================================
    // USAGE TRACKING GÜNCELLEMELERI
    // ==========================================

    /**
     * UsageTracking güncelle - kullanıcı ekleme
     */
    private void updateUsageTrackingAfterUserAdd(User user) {
        if (user.getCompany() == null) return;

        Company brokerCompany = user.getBrokerCompany();
        if (brokerCompany == null) return;

        usageTrackingRepository.findByBrokerCompanyId(brokerCompany.getId())
                .ifPresent(tracking -> {
                    if (user.isBrokerStaff()) {
                        tracking.incrementBrokerUsers();
                        usageTrackingRepository.save(tracking);
                        logger.debug("Usage tracking updated - Users incremented for company: {}",
                                brokerCompany.getId());
                    }
                });
    }

    /**
     * UsageTracking güncelle - kullanıcı silme
     */
    private void updateUsageTrackingAfterUserRemove(User user) {
        if (user.getCompany() == null) return;

        Company brokerCompany = user.getBrokerCompany();
        if (brokerCompany == null) return;

        usageTrackingRepository.findByBrokerCompanyId(brokerCompany.getId())
                .ifPresent(tracking -> {
                    if (user.isBrokerStaff()) {
                        tracking.decrementBrokerUsers();
                        usageTrackingRepository.save(tracking);
                        logger.debug("Usage tracking updated - Users decremented for company: {}",
                                brokerCompany.getId());
                    }
                });
    }

    // ==========================================
    // SPRING SECURITY - UserDetailsService
    // ==========================================

    /**
     * Spring Security için kullanıcı yükleme
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmailWithCompany(username); // ✅ Optimized

        if (user.isEmpty()) {
            logger.warn("User not found with email: {}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        User foundUser = user.get();

        if (!foundUser.getIsActive()) {
            logger.warn("Inactive user attempted login: {}", username);
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        List<GrantedAuthority> authorities = getAuthorities(foundUser);

        logger.debug("User loaded for authentication: {} - Authorities: {}",
                foundUser.getEmail(), authorities);

        return new org.springframework.security.core.userdetails.User(
                foundUser.getEmail(),
                foundUser.getPassword(),
                foundUser.getIsActive(),
                true,
                true,
                true,
                authorities
        );
    }

    /**
     * Kullanıcının yetkilerini (authorities) oluştur
     */
    private List<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getGlobalRole().name()));

        if (user.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }

        if (user.getCompany() != null) {
            String companyRole = "ROLE_" + user.getGlobalRole().name() +
                    "_COMPANY_" + user.getCompany().getId();
            authorities.add(new SimpleGrantedAuthority(companyRole));
        }

        return authorities;
    }

    // ==========================================
    // İSTATİSTİK VE YARDIMCI METODLAR
    // ==========================================

    /**
     * Aktif kullanıcı sayısı
     */
    public long getActiveUserCount() {
        return userRepository.countByIsActiveTrue();
    }

    /**
     * Belirli role sahip kullanıcı sayısı
     */
    public long getUserCountByRole(GlobalRole role) {
        return userRepository.countByGlobalRole(role);
    }

    /**
     * Belirli şirketteki kullanıcı sayısı
     */
    public long getCompanyUserCount(Long companyId) {
        return userRepository.countByCompanyIdAndIsActiveTrue(companyId);
    }

    /**
     * Email ile kullanıcı var mı kontrolü
     */
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Username ile kullanıcı var mı kontrolü
     */
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    // ==========================================
// PASİF KULLANICI YÖNETİMİ
// ==========================================

    /**
     * Bekleyen (pasif) kullanıcıları getir
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Tüm pasif kullanıcıları görebilir
     * - BROKER_ADMIN: Kendi broker firmasının müşterilerindeki pasif kullanıcıları görebilir
     */
    public List<User> getPendingUsers(User requestingUser) {
        if (requestingUser.isSuperAdmin()) {
            // SUPER_ADMIN tüm pasif kullanıcıları görebilir
            return userRepository.findByIsActiveFalse();
        }

        if (requestingUser.isBrokerAdmin()) {
            // BROKER_ADMIN kendi müşteri firmalarındaki pasif kullanıcıları görebilir
            Company brokerCompany = requestingUser.getCompany();

            List<User> pendingUsers = new ArrayList<>();

            // Müşteri firmalarını bul
            List<Company> clientCompanies = companyRepository
                    .findByParentBrokerAndIsActiveTrue(brokerCompany);

            // Her müşteri firmasının pasif kullanıcılarını ekle
            for (Company client : clientCompanies) {
                List<User> clientPendingUsers = userRepository
                        .findByCompanyAndIsActiveFalse(client);
                pendingUsers.addAll(clientPendingUsers);
            }

            logger.info("Found {} pending users for broker: {}",
                    pendingUsers.size(), brokerCompany.getName());

            return pendingUsers;
        }

        // Diğerleri pasif kullanıcıları göremez
        return List.of();
    }

    /**
     * Kullanıcı başka bir kullanıcıyı onaylayabilir mi?
     */
    private boolean canUserApproveUser(User approver, User target) {
        // SUPER_ADMIN herkesi onaylayabilir
        if (approver.isSuperAdmin()) {
            return true;
        }

        // BROKER_ADMIN kendi müşteri firmalarındaki kullanıcıları onaylayabilir
        if (approver.isBrokerAdmin() && target.isClientUser()) {
            Company approverBroker = approver.getCompany();
            Company targetClient = target.getCompany();

            if (targetClient != null && targetClient.getParentBroker() != null) {
                return approverBroker.getId().equals(targetClient.getParentBroker().getId());
            }
        }

        return false;
    }
}