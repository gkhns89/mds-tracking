package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.model.CompanyRole;
import com.medosasoftware.mdstracking.model.CompanyUserRole;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    // ✅ Kullanıcı oluşturma (Sadece SUPER_ADMIN)
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.ok("✅ User created successfully: " + createdUser.getEmail());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error creating user: " + e.getMessage());
        }
    }

    // ✅ Kullanıcıya firma rolü atama
    @PostMapping("/{userId}/assign-role")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Long userId,
            @RequestParam Long companyId,
            @RequestParam CompanyRole role) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            CompanyUserRole assignedRole = userService.assignRoleToUserInCompany(
                    userId, companyId, role, currentUser);

            return ResponseEntity.ok("✅ Role " + role + " assigned to user in company successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error assigning role: " + e.getMessage());
        }
    }

    // ✅ Kullanıcıyı firmadan çıkarma
    @DeleteMapping("/{userId}/remove-from-company/{companyId}")
    public ResponseEntity<?> removeUserFromCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {

        try {
            userService.removeUserFromCompany(userId, companyId);
            return ResponseEntity.ok("✅ User removed from company successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error removing user: " + e.getMessage());
        }
    }

    // ✅ Email ile kullanıcı getirme
    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    // ✅ ID ile kullanıcı getirme
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    // ✅ Giriş yapan kullanıcının erişebildiği firmalar
    @GetMapping("/my-companies")
    public ResponseEntity<List<Company>> getMyAccessibleCompanies() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<Company> companies = userService.getUserAccessibleCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    // ✅ Giriş yapan kullanıcının yönetebileceği firmalar
    @GetMapping("/manageable-companies")
    public ResponseEntity<List<Company>> getManageableCompanies() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<Company> companies = userService.getUserManageableCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    // ✅ Belirli firmadaki kullanıcıları listeleme
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<User>> getCompanyUsers(@PathVariable Long companyId) {
        try {
            List<User> users = userService.getCompanyUsers(companyId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Kullanıcı profili
    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        return ResponseEntity.ok(currentUser);
    }
}