package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.RegisterRequest;
import com.gcodes.aacctracker.model.GlobalRole;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.UserRepository;
import com.gcodes.aacctracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/setup")
@CrossOrigin(origins = "*")
public class SetupController {

    private static final Logger logger = LoggerFactory.getLogger(SetupController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * İlk super admin oluşturma
     * <p>
     * NOT: Bu endpoint sadece hiç SUPER_ADMIN yoksa çalışır.
     * İlk kurulum için kullanılır.
     */
    @PostMapping("/create-super-admin")
    public ResponseEntity<?> createInitialSuperAdmin(@RequestBody RegisterRequest request) {
        try {
            // Güvenlik kontrolü: Zaten super admin var mı?
            long adminCount = userRepository.countByGlobalRole(GlobalRole.SUPER_ADMIN);

            if (adminCount > 0) {
                logger.warn("Attempt to create SUPER_ADMIN when one already exists");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "❌ Super admin user already exists. This endpoint is only for initial setup.",
                                "existingAdmins", adminCount
                        ));
            }

            // İlk super admin kullanıcısını oluştur
            User adminUser = new User();
            adminUser.setEmail(request.getEmail());
            adminUser.setUsername(request.getUsername());
            adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
            adminUser.setGlobalRole(GlobalRole.SUPER_ADMIN);
            adminUser.setIsActive(true);
            adminUser.setCompany(null); // SUPER_ADMIN firma bağlantısı yok

            userService.createUser(adminUser);

            logger.info("Initial SUPER_ADMIN created: {}", request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Initial super admin user created successfully!",
                    "email", request.getEmail(),
                    "username", request.getUsername(),
                    "warning", "⚠️ Please change the password after first login!"
            ));
        } catch (Exception e) {
            logger.error("Error creating super admin", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error creating super admin: " + e.getMessage()));
        }
    }

    /**
     * Setup durumunu kontrol etme
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSetupStatus() {
        try {
            long superAdminCount = userRepository.countByGlobalRole(GlobalRole.SUPER_ADMIN);
            long totalUsers = userRepository.count();
            long brokerAdminCount = userRepository.countByGlobalRole(GlobalRole.BROKER_ADMIN);
            long brokerUserCount = userRepository.countByGlobalRole(GlobalRole.BROKER_USER);
            long clientUserCount = userRepository.countByGlobalRole(GlobalRole.CLIENT_USER);

            boolean setupComplete = superAdminCount > 0;

            return ResponseEntity.ok(Map.of(
                    "setupComplete", setupComplete,
                    "superAdminCount", superAdminCount,
                    "totalUsers", totalUsers,
                    "usersByRole", Map.of(
                            "SUPER_ADMIN", superAdminCount,
                            "BROKER_ADMIN", brokerAdminCount,
                            "BROKER_USER", brokerUserCount,
                            "CLIENT_USER", clientUserCount
                    ),
                    "message", setupComplete ?
                            "✅ Setup completed" :
                            "⚠️ Setup required - No super admin found"
            ));
        } catch (Exception e) {
            logger.error("Error getting setup status", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Sistem sağlık kontrolü
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActiveTrue();

            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "AACC Tracker API",
                    "version", "2.0.0",
                    "database", Map.of(
                            "totalUsers", totalUsers,
                            "activeUsers", activeUsers
                    ),
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "status", "DOWN",
                            "error", e.getMessage()
                    ));
        }
    }
}