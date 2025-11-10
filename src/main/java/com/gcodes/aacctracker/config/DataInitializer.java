package com.gcodes.aacctracker.config;

import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BrokerSubscriptionRepository brokerSubscriptionRepository;

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    // Environment variables veya application.properties'den al
    @Value("${APP_EMAIL}")
    private String adminEmail;

    @Value("${APP_PASSWORD}")
    private String adminPassword;

    @Value("${APP_USERNAME}")
    private String adminUsername;

    @PostConstruct
    public void checkEnv() {
        logger.info("🔧 Environment variables loaded:");
        logger.info("   📧 Admin Email: {}", adminEmail);
        logger.info("   👤 Admin Username: {}", adminUsername);
    }

    @Bean
    public ApplicationRunner initializeData() {
        return args -> {
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("🚀 AACC TRACKER - DATA INITIALIZATION STARTED");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 1. Süper admin kontrolü ve oluşturma
            initializeSuperAdmin();

            // 2. Varsayılan abonelik planlarını oluştur
            initializeSubscriptionPlans();

            // 3. Demo verilerini oluştur (opsiyonel - geliştirme için)
            if (shouldCreateDemoData()) {
                createDemoData();
            }

            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("✅ DATA INITIALIZATION COMPLETED");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        };
    }

    // ==========================================
    // SÜPER ADMIN OLUŞTURMA
    // ==========================================

    private void initializeSuperAdmin() {
        logger.info("👤 Checking for SUPER_ADMIN users...");

        long adminCount = userRepository.countByGlobalRole(GlobalRole.SUPER_ADMIN);

        if (adminCount == 0) {
            logger.info("🚀 No SUPER_ADMIN found. Creating default super admin...");

            User adminUser = new User();
            adminUser.setEmail(adminEmail);
            adminUser.setUsername(adminUsername);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setGlobalRole(GlobalRole.SUPER_ADMIN);
            adminUser.setIsActive(true);
            adminUser.setEmailVerified(true);
            adminUser.setCompany(null); // SUPER_ADMIN firma bağlantısı yok

            userRepository.save(adminUser);

            logger.info("✅ Default SUPER_ADMIN created successfully:");
            logger.info("   📧 Email: {}", adminEmail);
            logger.info("   👤 Username: {}", adminUsername);
            logger.info("   🔑 Password: {}", adminPassword);
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.warn("⚠️  SECURITY WARNING: Please change the default password after first login!");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } else {
            logger.info("✅ SUPER_ADMIN user(s) already exist. Total count: {}", adminCount);
        }
    }

    // ==========================================
    // ABONELİK PLANLARI OLUŞTURMA
    // ==========================================

    private void initializeSubscriptionPlans() {
        logger.info("📦 Checking subscription plans...");

        long planCount = subscriptionPlanRepository.count();

        if (planCount == 0) {
            logger.info("🚀 No subscription plans found. Creating default plans...");

            // Starter Plan
            SubscriptionPlan starter = new SubscriptionPlan();
            starter.setName("Starter");
            starter.setDescription("Küçük gümrük müşavirlikleri için ideal başlangıç paketi");
            starter.setMaxBrokerUsers(3);
            starter.setMaxClientCompanies(10);
            starter.setMonthlyPrice(new BigDecimal("500.00"));
            starter.setYearlyPrice(new BigDecimal("5000.00"));
            starter.setIsActive(true);
            starter.setFeatures("[\"Temel Özellikler\",\"Email Destek\",\"Aylık Raporlar\"]");
            subscriptionPlanRepository.save(starter);

            // Professional Plan
            SubscriptionPlan professional = new SubscriptionPlan();
            professional.setName("Professional");
            professional.setDescription("Orta ölçekli gümrük müşavirlikleri için gelişmiş özellikler");
            professional.setMaxBrokerUsers(10);
            professional.setMaxClientCompanies(50);
            professional.setMonthlyPrice(new BigDecimal("1500.00"));
            professional.setYearlyPrice(new BigDecimal("15000.00"));
            professional.setIsActive(true);
            professional.setFeatures("[\"Gelişmiş Özellikler\",\"Excel Import/Export\",\"Öncelikli Destek\",\"Haftalık Raporlar\"]");
            subscriptionPlanRepository.save(professional);

            // Enterprise Plan
            SubscriptionPlan enterprise = new SubscriptionPlan();
            enterprise.setName("Enterprise");
            enterprise.setDescription("Büyük gümrük müşavirlikleri için kurumsal çözüm");
            enterprise.setMaxBrokerUsers(50);
            enterprise.setMaxClientCompanies(200);
            enterprise.setMonthlyPrice(new BigDecimal("5000.00"));
            enterprise.setYearlyPrice(new BigDecimal("50000.00"));
            enterprise.setIsActive(true);
            enterprise.setFeatures("[\"Tüm Özellikler\",\"API Erişimi\",\"Özel Entegrasyonlar\",\"7/24 Destek\",\"Günlük Raporlar\",\"Özel Eğitim\"]");
            subscriptionPlanRepository.save(enterprise);

            logger.info("✅ Default subscription plans created:");
            logger.info("   📦 Starter: 3 users, 10 clients - ₺500/month");
            logger.info("   📦 Professional: 10 users, 50 clients - ₺1,500/month");
            logger.info("   📦 Enterprise: 50 users, 200 clients - ₺5,000/month");
        } else {
            logger.info("✅ Subscription plans already exist. Total count: {}", planCount);
        }
    }

    // ==========================================
    // DEMO VERİLERİ OLUŞTURMA (Opsiyonel)
    // ==========================================

    private boolean shouldCreateDemoData() {
        // Sadece development ortamında ve hiç broker yoksa demo data oluştur
        String profile = System.getProperty("spring.profiles.active", "local");
        long brokerCount = companyRepository.countByCompanyTypeAndIsActiveTrue(CompanyType.CUSTOMS_BROKER);

        return "local".equals(profile) && brokerCount == 0;
    }

    private void createDemoData() {
        logger.info("🎭 Creating demo data for development...");

        try {
            // 1. Demo Gümrük Firması Oluştur
            Company demoBoker = createDemoBrokerCompany();

            // 2. Demo Broker Admin Kullanıcısı Oluştur
            User demoBrokerAdmin = createDemoBrokerAdmin(demoBoker);

            // 3. Demo Broker User Oluştur
            User demoBrokerUser = createDemoBrokerUser(demoBoker);

            // 4. Demo Müşteri Firmaları Oluştur
            Company demoClient1 = createDemoClientCompany(demoBoker, "ABC İthalat A.Ş.");
            Company demoClient2 = createDemoClientCompany(demoBoker, "XYZ Dış Ticaret Ltd.");

            // 5. Demo Client User Oluştur
            User demoClientUser1 = createDemoClientUser(demoClient1);
            User demoClientUser2 = createDemoClientUser(demoClient2);

            logger.info("✅ Demo data created successfully!");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("🎭 DEMO ACCOUNTS:");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("   👔 Broker Admin:");
            logger.info("      📧 Email: broker.admin@demo.com");
            logger.info("      🔑 Password: Demo1234!");
            logger.info("");
            logger.info("   👨‍💼 Broker User:");
            logger.info("      📧 Email: broker.user@demo.com");
            logger.info("      🔑 Password: Demo1234!");
            logger.info("");
            logger.info("   👤 Client User 1:");
            logger.info("      📧 Email: client1@demo.com");
            logger.info("      🔑 Password: Demo1234!");
            logger.info("");
            logger.info("   👤 Client User 2:");
            logger.info("      📧 Email: client2@demo.com");
            logger.info("      🔑 Password: Demo1234!");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            logger.error("❌ Error creating demo data", e);
        }
    }

    private Company createDemoBrokerCompany() {
        // Demo broker firması oluştur
        Company broker = new Company();
        broker.setName("Demo Gümrük Müşavirliği A.Ş.");
        broker.setDescription("Demo ve test amaçlı gümrük müşavirliği");
        broker.setCompanyType(CompanyType.CUSTOMS_BROKER);
        broker.setIsActive(true);
        // ✅ YENİ: Public açıklama ekle
        broker.setPublicDescription("Demo gümrük müşavirliği - Test ve geliştirme amaçlı");
        Company savedBroker = companyRepository.save(broker);

        // ✅ YENİ: Firma kodunu oluştur
        savedBroker.generateCompanyCode();
        savedBroker = companyRepository.save(savedBroker);

        // Professional planı al
        SubscriptionPlan professionalPlan = subscriptionPlanRepository.findByName("Professional")
                .orElseThrow(() -> new RuntimeException("Professional plan not found"));

        // Broker subscription oluştur
        BrokerSubscription subscription = new BrokerSubscription();
        subscription.setBrokerCompany(savedBroker);
        subscription.setSubscriptionPlan(professionalPlan);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusYears(1));
        subscription.setIsActive(true);
        subscription.setNotes("Demo subscription - automatically created");
        brokerSubscriptionRepository.save(subscription);

        // Usage tracking oluştur
        UsageTracking tracking = new UsageTracking();
        tracking.setBrokerCompany(savedBroker);
        tracking.setCurrentBrokerUsers(0);
        tracking.setCurrentClientCompanies(0);
        usageTrackingRepository.save(tracking);

        logger.info("   ✅ Demo broker company created: {} (Code: {})",
                savedBroker.getName(), savedBroker.getCompanyCode());
        return savedBroker;
    }

    private User createDemoBrokerAdmin(Company broker) {
        User admin = new User();
        admin.setEmail("broker.admin@demo.com");
        admin.setUsername("demo_broker_admin");
        admin.setPassword(passwordEncoder.encode("Demo1234!"));
        admin.setGlobalRole(GlobalRole.BROKER_ADMIN);
        admin.setCompany(broker);
        admin.setIsActive(true);
        admin.setEmailVerified(true);
        User saved = userRepository.save(admin);

        // Usage tracking güncelle
        updateUsageTracking(broker, 1, 0);

        logger.info("   ✅ Demo broker admin created: {}", saved.getEmail());
        return saved;
    }

    private User createDemoBrokerUser(Company broker) {
        User user = new User();
        user.setEmail("broker.user@demo.com");
        user.setUsername("demo_broker_user");
        user.setPassword(passwordEncoder.encode("Demo1234!"));
        user.setGlobalRole(GlobalRole.BROKER_USER);
        user.setCompany(broker);
        user.setIsActive(true);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);

        // Usage tracking güncelle
        updateUsageTracking(broker, 1, 0);

        logger.info("   ✅ Demo broker user created: {}", saved.getEmail());
        return saved;
    }

    private Company createDemoClientCompany(Company broker, String name) {
        Company client = new Company();
        client.setName(name);
        client.setDescription("Demo müşteri firması");
        client.setCompanyType(CompanyType.CLIENT);
        client.setParentBroker(broker);
        client.setIsActive(true);
        Company saved = companyRepository.save(client);

        // Usage tracking güncelle
        updateUsageTracking(broker, 0, 1);

        logger.info("   ✅ Demo client company created: {}", saved.getName());
        return saved;
    }

    private User createDemoClientUser(Company client) {
        String email = client.getName().toLowerCase()
                .replace("ş", "s")
                .replace("ı", "i")
                .replace("ç", "c")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ö", "o")
                .replaceAll("[^a-z0-9]", "")
                .substring(0, Math.min(10, client.getName().length())) + "@demo.com";

        User user = new User();
        user.setEmail(email);
        user.setUsername("demo_client_" + client.getId());
        user.setPassword(passwordEncoder.encode("Demo1234!"));
        user.setGlobalRole(GlobalRole.CLIENT_USER);
        user.setCompany(client);
        user.setIsActive(true);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);

        logger.info("   ✅ Demo client user created: {}", saved.getEmail());
        return saved;
    }

    private void updateUsageTracking(Company broker, int userIncrement, int clientIncrement) {
        usageTrackingRepository.findByBrokerCompanyId(broker.getId())
                .ifPresent(tracking -> {
                    if (userIncrement > 0) {
                        tracking.setCurrentBrokerUsers(tracking.getCurrentBrokerUsers() + userIncrement);
                    }
                    if (clientIncrement > 0) {
                        tracking.setCurrentClientCompanies(tracking.getCurrentClientCompanies() + clientIncrement);
                    }
                    usageTrackingRepository.save(tracking);
                });
    }
}