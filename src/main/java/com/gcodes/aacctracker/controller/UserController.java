package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.UserCreateRequest;
import com.gcodes.aacctracker.dto.UserUpdateRequest;
import com.gcodes.aacctracker.exception.LimitExceededException;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.service.LimitCheckService;
import com.gcodes.aacctracker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private LimitCheckService limitCheckService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==========================================
    // KULLANICI OLUŞTURMA
    // ==========================================

    /**
     * Kullanıcı oluştur
     * <p>
     * YETKİLER:
     * - SUPER_ADMIN: Gümrük firması için BROKER_ADMIN/BROKER_USER oluşturabilir
     * - BROKER_ADMIN: Kendi firması için BROKER_USER veya müşteri için CLIENT_USER oluşturabilir
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü
            if (!canUserCreateUser(currentUser, request.getGlobalRole(), request.getCompanyId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions to create this user type"));
            }

            // Company kontrolü
            Company company = null;
            if (request.getCompanyId() != null) {
                company = companyRepository.findById(request.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Company not found"));
            }

            // User objesi oluştur
            User newUser = new User();
            newUser.setEmail(request.getEmail());
            newUser.setUsername(request.getUsername());
            newUser.setPassword(passwordEncoder.encode(request.getPassword()));
            newUser.setGlobalRole(request.getGlobalRole());
            newUser.setCompany(company);
            newUser.setIsActive(true);

            User created = userService.createUser(newUser);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ User created successfully");
            response.put("userId", created.getId());
            response.put("email", created.getEmail());
            response.put("role", created.getGlobalRole());
            response.put("company", company != null ? company.getName() : null);

            return ResponseEntity.ok(response);

        } catch (LimitExceededException e) {
            logger.warn("Limit exceeded while creating user: {}", e.getMessage());
            return ResponseEntity.status(429) // Too Many Requests
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error creating user: " + e.getMessage()));
        }
    }

    // ==========================================
    // KULLANICI GÜNCELLEME VE SİLME
    // ==========================================

    /**
     * Kullanıcı güncelle
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Yetki kontrolü
            if (!userService.canUserEditUser(currentUser, id)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions to edit this user"));
            }

            User updatedUser = userService.updateUser(id, request, currentUser);
            logger.info("User updated: {} by {}", id, currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ User updated successfully",
                    "user", Map.of(
                            "id", updatedUser.getId(),
                            "email", updatedUser.getEmail(),
                            "username", updatedUser.getUsername(),
                            "isActive", updatedUser.getIsActive()
                    )
            ));
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error updating user: " + e.getMessage()));
        }
    }

    /**
     * Kullanıcı sil (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BROKER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Yetki kontrolü
            if (!currentUser.isSuperAdmin() && !userService.canUserEditUser(currentUser, id)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions to delete this user"));
            }

            // Kendini silmeye çalışıyor mu?
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ You cannot delete yourself"));
            }

            userService.deleteUser(id);
            logger.info("User deleted: {} by {}", id, currentUser.getEmail());

            return ResponseEntity.ok(Map.of("message", "✅ User deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error deleting user: " + e.getMessage()));
        }
    }

    // ==========================================
    // KULLANICI SORGULAMA
    // ==========================================

    /**
     * Tüm kullanıcıları listele (yetki bazlı)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            List<User> users = userService.getAllUsers(currentUser);

            return ResponseEntity.ok(Map.of(
                    "total", users.size(),
                    "users", users.stream().map(this::mapUserToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting all users", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * ID ile kullanıcı getir
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();

            // Yetki kontrolü - kullanıcı bu kullanıcıyı görebilir mi?
            if (!currentUser.isSuperAdmin() && !currentUser.getId().equals(id)) {
                if (currentUser.isBrokerAdmin()) {
                    Company userBroker = currentUser.getCompany();
                    Company targetBroker = user.getBrokerCompany();
                    if (userBroker == null || targetBroker == null ||
                            !userBroker.getId().equals(targetBroker.getId())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("error", "❌ Access denied"));
                    }
                } else {
                    return ResponseEntity.status(403)
                            .body(Map.of("error", "❌ Access denied"));
                }
            }

            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (Exception e) {
            logger.error("Error getting user", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Email ile kullanıcı getir
     */
    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> user = userService.findByEmail(email);
            if (user.isPresent()) {
                return ResponseEntity.ok(mapUserToResponse(user.get()));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting user by email", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Mevcut kullanıcının profilini getir
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            Map<String, Object> profile = mapUserToResponse(currentUser);

            // Ek bilgiler ekle
            if (currentUser.getCompany() != null) {
                Company company = currentUser.getCompany();
                profile.put("companyDetails", Map.of(
                        "id", company.getId(),
                        "name", company.getName(),
                        "type", company.getCompanyType(),
                        "isActive", company.getIsActive()
                ));

                // Broker ise limit bilgilerini ekle
                if (currentUser.isBrokerStaff() && company.isBroker()) {
                    try {
                        LimitCheckService.LimitInfo limits = limitCheckService.getLimitInfo(company.getId());
                        profile.put("limits", Map.of(
                                "maxBrokerUsers", limits.maxBrokerUsers,
                                "currentBrokerUsers", limits.currentBrokerUsers,
                                "remainingUserQuota", limits.getRemainingUserQuota(),
                                "maxClientCompanies", limits.maxClientCompanies,
                                "currentClientCompanies", limits.currentClientCompanies,
                                "remainingClientQuota", limits.getRemainingClientQuota(),
                                "daysUntilExpiry", limits.daysUntilExpiry
                        ));
                    } catch (Exception e) {
                        logger.warn("Could not fetch limits for broker: {}", company.getId());
                    }
                }
            }

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error getting user profile", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // ŞİRKET BAZLI SORGULAR
    // ==========================================

    /**
     * Erişilebilir firmaları getir
     */
    @GetMapping("/my-companies")
    public ResponseEntity<?> getMyAccessibleCompanies() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            List<Company> companies = userService.getUserAccessibleCompanies(currentUser);
            return ResponseEntity.ok(Map.of(
                    "total", companies.size(),
                    "companies", companies
            ));
        } catch (Exception e) {
            logger.error("Error getting accessible companies", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Yönetilebilir firmaları getir
     */
    @GetMapping("/manageable-companies")
    public ResponseEntity<?> getManageableCompanies() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            List<Company> companies = userService.getUserManageableCompanies(currentUser);
            return ResponseEntity.ok(Map.of(
                    "total", companies.size(),
                    "companies", companies
            ));
        } catch (Exception e) {
            logger.error("Error getting manageable companies", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Belirli firmadaki kullanıcıları getir
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getCompanyUsers(@PathVariable Long companyId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            if (!userService.canUserViewCompany(currentUser, company)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied to this company"));
            }

            List<User> users = userService.getCompanyUsers(companyId);
            return ResponseEntity.ok(Map.of(
                    "companyId", companyId,
                    "companyName", company.getName(),
                    "total", users.size(),
                    "users", users.stream().map(this::mapUserToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting company users", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // LİMİT KONTROLÜ
    // ==========================================

    /**
     * Gümrük firması için limit bilgilerini getir
     */
    @GetMapping("/limits")
    public ResponseEntity<?> getLimits(@RequestParam Long brokerCompanyId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            Company brokerCompany = companyRepository.findById(brokerCompanyId)
                    .orElseThrow(() -> new RuntimeException("Broker company not found"));

            // Yetki kontrolü
            if (!currentUser.isSuperAdmin() && !userService.canUserViewCompany(currentUser, brokerCompany)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            LimitCheckService.LimitInfo limits = limitCheckService.getLimitInfo(brokerCompanyId);

            Map<String, Object> response = new HashMap<>();
            response.put("brokerCompanyId", brokerCompanyId);
            response.put("brokerCompanyName", brokerCompany.getName());
            response.put("maxBrokerUsers", limits.maxBrokerUsers);
            response.put("currentBrokerUsers", limits.currentBrokerUsers);
            response.put("remainingUserQuota", limits.getRemainingUserQuota());
            response.put("canAddUser", limits.canAddUser());
            response.put("maxClientCompanies", limits.maxClientCompanies);
            response.put("currentClientCompanies", limits.currentClientCompanies);
            response.put("remainingClientQuota", limits.getRemainingClientQuota());
            response.put("canAddClient", limits.canAddClient());
            response.put("daysUntilExpiry", limits.daysUntilExpiry);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting limits", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // HELPER METODLARI
    // ==========================================

    /**
     * Kullanıcı bu tür bir kullanıcı oluşturabilir mi?
     */
    private boolean canUserCreateUser(User creator, GlobalRole targetRole, Long targetCompanyId) {
        if (creator.isSuperAdmin()) {
            return true;
        }

        if (creator.isBrokerAdmin()) {
            // BROKER_ADMIN kendi firması için BROKER_USER oluşturabilir
            if (targetRole == GlobalRole.BROKER_USER &&
                    creator.getCompany() != null &&
                    creator.getCompany().getId().equals(targetCompanyId)) {
                return true;
            }

            // BROKER_ADMIN müşteri firmaları için CLIENT_USER oluşturabilir
            if (targetRole == GlobalRole.CLIENT_USER) {
                Company targetCompany = companyRepository.findById(targetCompanyId).orElse(null);
                if (targetCompany != null && targetCompany.getParentBroker() != null) {
                    return creator.getCompany().getId().equals(targetCompany.getParentBroker().getId());
                }
            }
        }

        return false;
    }

    /**
     * User nesnesini response map'e çevir
     */
    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("globalRole", user.getGlobalRole());
        response.put("isActive", user.getIsActive());
        response.put("emailVerified", user.getEmailVerified());
        response.put("createdAt", user.getCreatedAt());

        if (user.getCompany() != null) {
            Company company = user.getCompany();
            response.put("company", Map.of(
                    "id", company.getId(),
                    "name", company.getName(),
                    "type", company.getCompanyType()
            ));
        } else {
            response.put("company", null);
        }

        return response;
    }
}