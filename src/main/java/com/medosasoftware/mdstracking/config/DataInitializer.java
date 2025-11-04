package com.medosasoftware.mdstracking.config;

import com.medosasoftware.mdstracking.model.GlobalRole;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired

    private PasswordEncoder passwordEncoder;

    // Environment variables veya application.properties'den al
    @Value("${app.admin.email:admin@mdstracking.com}")
    private String adminEmail;

    @Value("${app.admin.password:AdminPassword123!}")
    private String adminPassword;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @PostConstruct
    public void checkEnv() {
        System.out.println("🧩 Loaded adminEmail from env: " + adminEmail);
    }

    @Bean
    public ApplicationRunner initializeDefaultAdmin() {
        return args -> {
            // Hiç super admin kullanıcı var mı kontrol et
            long adminCount = userRepository.countByGlobalRole(GlobalRole.SUPER_ADMIN);

            if (adminCount == 0) {
                System.out.println("🚀 No super admin user found, creating default super admin...");

                // Varsayılan super admin kullanıcısı oluştur
                User adminUser = new User();
                adminUser.setEmail(adminEmail);
                adminUser.setUsername(adminUsername);
                adminUser.setPassword(passwordEncoder.encode(adminPassword));
                adminUser.setGlobalRole(GlobalRole.SUPER_ADMIN); // ✅ SUPER_ADMIN

                userRepository.save(adminUser);

                System.out.println("✅ Default super admin user created:");
                System.out.println("   📧 Email: " + adminEmail);
                System.out.println("   👤 Username: " + adminUsername);
                System.out.println("   🔑 Password: " + adminPassword);
                System.out.println("⚠️  SECURITY WARNING: Please change the default password after first login!");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                System.out.println("✅ Super admin user(s) already exist. Total count: " + adminCount);
            }
        };
    }

}