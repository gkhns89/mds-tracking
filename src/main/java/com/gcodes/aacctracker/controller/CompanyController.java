package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.BrokerCompanyCreateRequest;
import com.gcodes.aacctracker.dto.ClientCompanyCreateRequest;
import com.gcodes.aacctracker.dto.CompanyUpdateRequest;
import com.gcodes.aacctracker.exception.LimitExceededException;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.service.CompanyService;
import com.gcodes.aacctracker.service.LimitCheckService;
import com.gcodes.aacctracker.service.SubscriptionService;
import com.gcodes.aacctracker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*")
public class CompanyController {

    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private LimitCheckService limitCheckService;

    // ==========================================
    // GÜMRÜK FİRMASI OLUŞTURMA (SUPER_ADMIN)
    // ==========================================

    /**
     * Gümrük firması oluştur (sadece SUPER_ADMIN)
     */
    @PostMapping("/broker")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createBrokerCompany(@Valid @RequestBody BrokerCompanyCreateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Only SUPER_ADMIN can create broker companies"));
            }

            // Company objesi oluştur
            Company company = new Company();
            company.setName(request.getName());
            company.setDescription(request.getDescription());
            company.setCompanyType(CompanyType.CUSTOMS_BROKER);
            company.setIsActive(true);

            // BrokerSubscription objesi oluştur
            BrokerSubscription subscription = new BrokerSubscription();
            subscription.setSubscriptionPlan(
                    subscriptionService.getAllPlans().stream()
                            .filter(p -> p.getId().equals(request.getSubscriptionPlanId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Subscription plan not found"))
            );
            subscription.setStartDate(request.getStartDate());
            subscription.setEndDate(request.getEndDate());
            subscription.setCustomMaxBrokerUsers(request.getCustomMaxBrokerUsers());
            subscription.setCustomMaxClientCompanies(request.getCustomMaxClientCompanies());
            subscription.setNotes(request.getNotes());
            subscription.setIsActive(true);

            Company savedCompany = companyService.createBrokerCompany(company, subscription, currentUser);

            logger.info("Broker company created: {} by {}", savedCompany.getName(), currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Broker company created successfully",
                    "companyId", savedCompany.getId(),
                    "companyName", savedCompany.getName()
            ));

        } catch (Exception e) {
            logger.error("Error creating broker company", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error creating broker company: " + e.getMessage()));
        }
    }

    // ==========================================
    // MÜŞTERİ FİRMASI OLUŞTURMA (BROKER_ADMIN)
    // ==========================================

    /**
     * Müşteri firması oluştur (BROKER_ADMIN)
     */
    @PostMapping("/client")
    public ResponseEntity<?> createClientCompany(@Valid @RequestBody ClientCompanyCreateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Company objesi oluştur
            Company clientCompany = new Company();
            clientCompany.setName(request.getName());
            clientCompany.setDescription(request.getDescription());
            clientCompany.setIsActive(true);

            Company savedClient = companyService.createClientCompany(
                    clientCompany,
                    request.getParentBrokerId(),
                    currentUser
            );

            logger.info("Client company created: {} by {}", savedClient.getName(), currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Client company created successfully",
                    "companyId", savedClient.getId(),
                    "companyName", savedClient.getName(),
                    "parentBroker", savedClient.getParentBroker().getName()
            ));

        } catch (LimitExceededException e) {
            logger.warn("Limit exceeded while creating client company: {}", e.getMessage());
            return ResponseEntity.status(429)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating client company", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error creating client company: " + e.getMessage()));
        }
    }

    // ==========================================
    // FİRMA GÜNCELLEME VE SİLME
    // ==========================================

    /**
     * Firma güncelle
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Company updated = companyService.updateCompany(
                    id,
                    request.getName(),
                    request.getDescription(),
                    currentUser
            );

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Company updated successfully",
                    "company", Map.of(
                            "id", updated.getId(),
                            "name", updated.getName(),
                            "description", updated.getDescription()
                    )
            ));
        } catch (Exception e) {
            logger.error("Error updating company", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error updating company: " + e.getMessage()));
        }
    }

    /**
     * Firma sil (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            companyService.deleteCompany(id, currentUser);

            return ResponseEntity.ok(Map.of("message", "✅ Company deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting company", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error deleting company: " + e.getMessage()));
        }
    }

    /**
     * Firma durumunu değiştir (aktif/pasif)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleCompanyStatus(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Only SUPER_ADMIN can change company status"));
            }

            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Company company = companyOpt.get();
            company.setIsActive(!company.getIsActive());
            companyRepository.save(company);

            String status = company.getIsActive() ? "activated" : "deactivated";
            logger.info("Company {}: {} by {}", status, company.getName(), currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Company " + status + " successfully",
                    "isActive", company.getIsActive()
            ));

        } catch (Exception e) {
            logger.error("Error changing company status", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error changing company status: " + e.getMessage()));
        }
    }

    // ==========================================
    // FİRMA SORGULAMA
    // ==========================================

    /**
     * Tüm firmaları getir (yetki bazlı)
     */
    @GetMapping
    public ResponseEntity<?> getAllCompanies() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Company> companies = companyService.getAllCompanies(currentUser);

            return ResponseEntity.ok(Map.of(
                    "total", companies.size(),
                    "companies", companies.stream().map(this::mapCompanyToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting all companies", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * ID ile firma getir
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Company> companyOpt = companyRepository.findById(id);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Company company = companyOpt.get();

            if (!userService.canUserViewCompany(currentUser, company)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied to this company"));
            }

            return ResponseEntity.ok(mapCompanyToResponse(company));
        } catch (Exception e) {
            logger.error("Error getting company", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Gümrük firmasının müşteri firmalarını getir
     */
    @GetMapping("/{brokerId}/clients")
    public ResponseEntity<?> getClientCompanies(@PathVariable Long brokerId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Company> clients = companyService.getClientCompanies(brokerId, currentUser);

            Company broker = companyRepository.findById(brokerId).orElse(null);

            return ResponseEntity.ok(Map.of(
                    "brokerId", brokerId,
                    "brokerName", broker != null ? broker.getName() : "Unknown",
                    "total", clients.size(),
                    "clients", clients.stream().map(this::mapCompanyToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting client companies", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Erişilebilir firmalar
     */
    @GetMapping("/my-companies")
    public ResponseEntity<?> getMyCompanies() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Company> companies = userService.getUserAccessibleCompanies(currentUser);
            return ResponseEntity.ok(Map.of(
                    "total", companies.size(),
                    "companies", companies.stream().map(this::mapCompanyToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting my companies", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Yönetilebilir firmalar
     */
    @GetMapping("/manageable")
    public ResponseEntity<?> getManageableCompanies() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Company> companies = userService.getUserManageableCompanies(currentUser);
            return ResponseEntity.ok(Map.of(
                    "total", companies.size(),
                    "companies", companies.stream().map(this::mapCompanyToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting manageable companies", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Tüm gümrük firmalarını getir (SUPER_ADMIN)
     */
    @GetMapping("/brokers")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllBrokers() {
        try {
            List<Company> brokers = companyRepository.findAllActiveBrokers();
            return ResponseEntity.ok(Map.of(
                    "total", brokers.size(),
                    "brokers", brokers.stream().map(this::mapCompanyToResponse).toList()
            ));
        } catch (Exception e) {
            logger.error("Error getting all brokers", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // FİRMA İSTATİSTİKLERİ
    // ==========================================

    /**
     * Firma istatistiklerini getir
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getCompanyStats(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Company company = companyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            if (!userService.canUserViewCompany(currentUser, company)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("companyId", id);
            stats.put("companyName", company.getName());
            stats.put("companyType", company.getCompanyType());

            if (company.isBroker()) {
                // Gümrük firması istatistikleri
                long userCount = userService.getCompanyUserCount(id);
                long clientCount = companyRepository.countActiveClientsByBrokerId(id);

                stats.put("totalUsers", userCount);
                stats.put("totalClients", clientCount);

                // Limit bilgileri ekle
                try {
                    LimitCheckService.LimitInfo limits = limitCheckService.getLimitInfo(id);
                    stats.put("limits", Map.of(
                            "maxBrokerUsers", limits.maxBrokerUsers,
                            "currentBrokerUsers", limits.currentBrokerUsers,
                            "remainingUserQuota", limits.getRemainingUserQuota(),
                            "maxClientCompanies", limits.maxClientCompanies,
                            "currentClientCompanies", limits.currentClientCompanies,
                            "remainingClientQuota", limits.getRemainingClientQuota(),
                            "daysUntilExpiry", limits.daysUntilExpiry
                    ));
                } catch (Exception e) {
                    logger.warn("Could not fetch limits for broker: {}", id);
                }
            } else {
                // Müşteri firması istatistikleri
                long userCount = userService.getCompanyUserCount(id);
                stats.put("totalUsers", userCount);
                stats.put("maxUsers", 1); // Her zaman 1
                stats.put("parentBroker", company.getParentBroker() != null ?
                        Map.of(
                                "id", company.getParentBroker().getId(),
                                "name", company.getParentBroker().getName()
                        ) : null
                );
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting company stats", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // HELPER METODLARI
    // ==========================================

    /**
     * Company nesnesini response map'e çevir
     */
    private Map<String, Object> mapCompanyToResponse(Company company) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", company.getId());
        response.put("name", company.getName());
        response.put("description", company.getDescription());
        response.put("companyType", company.getCompanyType());
        response.put("isActive", company.getIsActive());
        response.put("createdAt", company.getCreatedAt());

        if (company.getParentBroker() != null) {
            response.put("parentBroker", Map.of(
                    "id", company.getParentBroker().getId(),
                    "name", company.getParentBroker().getName()
            ));
        } else {
            response.put("parentBroker", null);
        }

        // Gümrük firması ise müşteri sayısını ekle
        if (company.isBroker()) {
            long clientCount = companyRepository.countActiveClientsByBrokerId(company.getId());
            response.put("clientCount", clientCount);
        }

        return response;
    }
}