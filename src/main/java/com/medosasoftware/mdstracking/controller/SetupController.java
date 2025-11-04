package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.dto.RegisterRequest;
import com.medosasoftware.mdstracking.model.GlobalRole;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.UserRepository;
import com.medosasoftware.mdstracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ İlk super admin oluşturma
    @PostMapping("/create-super-admin")
    public ResponseEntity<?> createInitialSuperAdmin(@RequestBody RegisterRequest request) {
        // 🛡️ Güvenlik kontrolü: Zaten super admin var mı?
        long adminCount = userRepository.countByGlobalRole(GlobalRole.SUPER_ADMIN);

        if (adminCount > 0) {
            return ResponseEntity.badRequest()
                    .body("❌ Super admin user already exists. This endpoint is only for initial setup.");
        }

        try {
            // 🎯 İlk super admin kullanıcısını oluştur
            User adminUser = new User();
            adminUser.setEmail(request.getEmail());
            adminUser.setUsername(request.getUsername());
            adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
            adminUser.setGlobalRole(GlobalRole.SUPER_ADMIN);

            userService.createUser(adminUser);

            return ResponseEntity.ok("✅ Initial super admin user created successfully! Email: " + request.getEmail());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error creating super admin: " + e.getMessage());
        }
    }

    // ✅ Setup durumunu kontrol etme
    @GetMapping("/status")
    public ResponseEntity<?> getSetupStatus() {
        long superAdminCount = userRepository.countByGlobalRole(GlobalRole.SUPER_ADMIN);
        long totalUsers = userRepository.count();

        return ResponseEntity.ok(new SetupStatus(
                superAdminCount > 0,
                superAdminCount,
                totalUsers,
                superAdminCount > 0 ? "Setup completed" : "Setup required - No super admin found"
        ));
    }

    // ✅ DTO class
    public static class SetupStatus {
        public boolean setupComplete;
        public long superAdminCount;
        public long totalUsers;
        public String message;

        public SetupStatus(boolean setupComplete, long superAdminCount, long totalUsers, String message) {
            this.setupComplete = setupComplete;
            this.superAdminCount = superAdminCount;
            this.totalUsers = totalUsers;
            this.message = message;
        }
    }
}