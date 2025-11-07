package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import com.gcodes.aacctracker.service.LimitCheckService;
import com.gcodes.aacctracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private LimitCheckService limitCheckService;

    /**
     * Dashboard istatistikleri (rol bazlı)
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> stats = new HashMap<>();
            stats.put("userRole", currentUser.getGlobalRole());

            if (currentUser.isSuperAdmin()) {
                // SUPER_ADMIN istatistikleri
                stats.put("totalUsers", userRepository.count());
                stats.put("activeUsers", userRepository.countByIsActiveTrue());
                stats.put("totalCompanies", companyRepository.count());
                stats.put("activeCompanies", companyRepository.countByIsActiveTrue());
                stats.put("totalBrokers", companyRepository.countByCompanyTypeAndIsActiveTrue(
                        com.gcodes.aacctracker.model.CompanyType.CUSTOMS_BROKER
                ));
                stats.put("totalClients", companyRepository.countByCompanyTypeAndIsActiveTrue(
                        com.gcodes.aacctracker.model.CompanyType.CLIENT
                ));
            } else if (currentUser.isBrokerStaff()) {
                // BROKER_ADMIN/BROKER_USER istatistikleri
                Company brokerCompany = currentUser.getBrokerCompany();

                if (brokerCompany != null) {
                    stats.put("companyName", brokerCompany.getName());
                    stats.put("companyType", brokerCompany.getCompanyType());

                    long userCount = userService.getCompanyUserCount(brokerCompany.getId());
                    long clientCount = companyRepository.countActiveClientsByBrokerId(brokerCompany.getId());

                    stats.put("totalUsers", userCount);
                    stats.put("totalClients", clientCount);

                    // Limit bilgileri
                    try {
                        LimitCheckService.LimitInfo limits = limitCheckService.getLimitInfo(brokerCompany.getId());
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
                        logger.warn("Could not fetch limits for broker: {}", brokerCompany.getId());
                    }
                }
            } else if (currentUser.isClientUser()) {
                // CLIENT_USER istatistikleri
                Company clientCompany = currentUser.getCompany();

                if (clientCompany != null) {
                    stats.put("companyName", clientCompany.getName());
                    stats.put("companyType", clientCompany.getCompanyType());

                    if (clientCompany.getParentBroker() != null) {
                        stats.put("parentBroker", Map.of(
                                "id", clientCompany.getParentBroker().getId(),
                                "name", clientCompany.getParentBroker().getName()
                        ));
                    }
                }
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting dashboard stats", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Menü öğelerini getir (rol bazlı)
     */
    @GetMapping("/menu-items")
    public ResponseEntity<?> getMenuItems() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Boolean> menuItems = new HashMap<>();

            // Herkesin erişebileceği menüler
            menuItems.put("dashboard", true);
            menuItems.put("profile", true);

            // Rol bazlı menüler
            if (currentUser.isSuperAdmin()) {
                menuItems.put("allUsers", true);
                menuItems.put("allCompanies", true);
                menuItems.put("brokerManagement", true);
                menuItems.put("subscriptionManagement", true);
                menuItems.put("systemSettings", true);
            } else if (currentUser.isBrokerAdmin()) {
                menuItems.put("companyUsers", true);
                menuItems.put("clientManagement", true);
                menuItems.put("transactionManagement", true);
                menuItems.put("reports", true);
            } else if (currentUser.isBrokerUser()) {
                menuItems.put("transactionManagement", true);
                menuItems.put("reports", true);
            } else if (currentUser.isClientUser()) {
                menuItems.put("viewTransactions", true);
                menuItems.put("reports", true);
            }

            return ResponseEntity.ok(Map.of(
                    "role", currentUser.getGlobalRole(),
                    "menuItems", menuItems
            ));

        } catch (Exception e) {
            logger.error("Error getting menu items", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }
}