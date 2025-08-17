package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.dto.UserUpdateRequest;
import com.medosasoftware.mdstracking.model.*;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import com.medosasoftware.mdstracking.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
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
    private PasswordEncoder passwordEncoder;

    // ✅ Kullanıcı oluşturma (mevcut kod)
    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!currentUser.isSuperAdmin()) {
            return ResponseEntity.status(403).body("❌ Only SUPER_ADMIN can create users");
        }

        user.setGlobalRole(GlobalRole.USER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            User createdUser = userService.createUser(user);
            logger.info("User created successfully: {} by admin: {}", createdUser.getEmail(), currentUser.getEmail());
            return ResponseEntity.ok("✅ User created successfully: " + createdUser.getEmail());
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.badRequest().body("❌ Error creating user: " + e.getMessage());
        }
    }

    // ✅ YENİ: Kullanıcı düzenleme
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Yetki kontrolü
            if (!canUserEditUser(currentUser, id)) {
                return ResponseEntity.status(403).body("❌ Insufficient permissions to edit this user");
            }

            User updatedUser = userService.updateUser(id, request, currentUser);
            logger.info("User updated: {} by {}", id, currentUser.getEmail());

            return ResponseEntity.ok("✅ User updated successfully");
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return ResponseEntity.badRequest().body("❌ Error updating user: " + e.getMessage());
        }
    }

    // ✅ YENİ: Kullanıcı silme (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Sadece SUPER_ADMIN kullanıcı silebilir
            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.status(403).body("❌ Only SUPER_ADMIN can delete users");
            }

            // Kendini silmeye çalışıyor mu?
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest().body("❌ You cannot delete yourself");
            }

            userService.deleteUser(id);
            logger.info("User deleted: {} by {}", id, currentUser.getEmail());

            return ResponseEntity.ok("✅ User deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity.badRequest().body("❌ Error deleting user: " + e.getMessage());
        }
    }

    // ✅ YENİ: Tüm kullanıcıları listeleme (yetki bazlı)
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<User> users = userService.getAllUsers(currentUser);
        return ResponseEntity.ok(users);
    }

    // ✅ Yetki kontrol metodu
    private boolean canUserEditUser(User currentUser, Long targetUserId) {
        // SUPER_ADMIN herkeyi edit edebilir
        if (currentUser.isSuperAdmin()) {
            return true;
        }

        // Kullanıcı sadece kendisini edit edebilir
        return currentUser.getId().equals(targetUserId);
    }

    @PostMapping("/{userId}/assign-role")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Long userId,
            @RequestParam Long companyId,
            @RequestParam CompanyRole role) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            if (!userService.canUserAssignRoleInCompany(currentUser, companyId, role)) {
                return ResponseEntity.status(403)
                        .body("❌ Insufficient permissions to assign this role in this company");
            }

            CompanyUserRole assignedRole = userService.assignRoleToUserInCompany(
                    userId, companyId, role, currentUser);

            logger.info("Role {} assigned to user {} in company {} by {}",
                    role, userId, companyId, currentUser.getEmail());

            return ResponseEntity.ok("✅ Role " + role + " assigned to user in company successfully");
        } catch (Exception e) {
            logger.error("Error assigning role", e);
            return ResponseEntity.badRequest().body("❌ Error assigning role: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/remove-from-company/{companyId}")
    public ResponseEntity<?> removeUserFromCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            if (!currentUser.isSuperAdmin() && !currentUser.canManageUsersInCompany(company)) {
                return ResponseEntity.status(403).body("❌ Insufficient permissions to remove user from this company");
            }

            userService.removeUserFromCompany(userId, companyId);
            logger.info("User {} removed from company {} by {}", userId, companyId, currentUser.getEmail());

            return ResponseEntity.ok("✅ User removed from company successfully");
        } catch (Exception e) {
            logger.error("Error removing user from company", e);
            return ResponseEntity.badRequest().body("❌ Error removing user: " + e.getMessage());
        }
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/my-companies")
    public ResponseEntity<List<Company>> getMyAccessibleCompanies() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<Company> companies = userService.getUserAccessibleCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/manageable-companies")
    public ResponseEntity<List<Company>> getManageableCompanies() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<Company> companies = userService.getUserManageableCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<User>> getCompanyUsers(@PathVariable Long companyId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            if (!userService.canUserViewCompany(currentUser, company)) {
                return ResponseEntity.status(403).build();
            }

            List<User> users = userService.getCompanyUsers(companyId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error getting company users", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        return ResponseEntity.ok(currentUser);
    }
}