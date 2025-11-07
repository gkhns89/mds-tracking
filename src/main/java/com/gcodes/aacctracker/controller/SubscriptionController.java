package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.SubscriptionPlanCreateRequest;
import com.gcodes.aacctracker.dto.BrokerSubscriptionUpdateRequest;
import com.gcodes.aacctracker.model.BrokerSubscription;
import com.gcodes.aacctracker.model.SubscriptionPlan;
import com.gcodes.aacctracker.model.User;
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

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    // ==========================================
    // ABONELİK PLANLARI (SUPER_ADMIN)
    // ==========================================

    /**
     * Abonelik planı oluştur (SUPER_ADMIN)
     */
    @PostMapping("/plans")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createPlan(@Valid @RequestBody SubscriptionPlanCreateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SubscriptionPlan plan = new SubscriptionPlan();
            plan.setName(request.getName());
            plan.setDescription(request.getDescription());
            plan.setMaxBrokerUsers(request.getMaxBrokerUsers());
            plan.setMaxClientCompanies(request.getMaxClientCompanies());
            plan.setMonthlyPrice(request.getMonthlyPrice());
            plan.setYearlyPrice(request.getYearlyPrice());
            plan.setFeatures(request.getFeatures());
            plan.setIsActive(true);

            SubscriptionPlan created = subscriptionService.createPlan(plan, currentUser);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Subscription plan created successfully",
                    "planId", created.getId(),
                    "planName", created.getName()
            ));
        } catch (Exception e) {
            logger.error("Error creating subscription plan", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Tüm abonelik planlarını getir
     */
    @GetMapping("/plans")
    public ResponseEntity<?> getAllPlans() {
        try {
            List<SubscriptionPlan> plans = subscriptionService.getAllPlans();
            return ResponseEntity.ok(Map.of(
                    "total", plans.size(),
                    "plans", plans
            ));
        } catch (Exception e) {
            logger.error("Error getting plans", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Aktif planları getir
     */
    @GetMapping("/plans/active")
    public ResponseEntity<?> getActivePlans() {
        try {
            List<SubscriptionPlan> plans = subscriptionService.getActivePlans();
            return ResponseEntity.ok(Map.of(
                    "total", plans.size(),
                    "plans", plans
            ));
        } catch (Exception e) {
            logger.error("Error getting active plans", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Plan detayı getir
     */
    @GetMapping("/plans/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable Long id) {
        try {
            List<SubscriptionPlan> plans = subscriptionService.getAllPlans();
            SubscriptionPlan plan = plans.stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Plan not found"));

            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            logger.error("Error getting plan", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // BROKER ABONELİKLERİ
    // ==========================================

    /**
     * Gümrük firmasının aboneliğini getir
     */
    @GetMapping("/broker/{brokerId}")
    public ResponseEntity<?> getBrokerSubscription(@PathVariable Long brokerId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü
            if (!currentUser.isSuperAdmin() &&
                    (currentUser.getCompany() == null ||
                            !currentUser.getCompany().getId().equals(brokerId))) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            BrokerSubscription subscription = subscriptionService.getBrokerSubscription(brokerId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", subscription.getId());
            response.put("brokerId", subscription.getBrokerCompany().getId());
            response.put("brokerName", subscription.getBrokerCompany().getName());
            response.put("startDate", subscription.getStartDate());
            response.put("endDate", subscription.getEndDate());
            response.put("isActive", subscription.getIsActive());
            response.put("daysUntilExpiry", subscription.getDaysUntilExpiry());

            if (subscription.getSubscriptionPlan() != null) {
                response.put("plan", Map.of(
                        "id", subscription.getSubscriptionPlan().getId(),
                        "name", subscription.getSubscriptionPlan().getName(),
                        "maxBrokerUsers", subscription.getEffectiveMaxBrokerUsers(),
                        "maxClientCompanies", subscription.getEffectiveMaxClientCompanies()
                ));
            }

            response.put("customMaxBrokerUsers", subscription.getCustomMaxBrokerUsers());
            response.put("customMaxClientCompanies", subscription.getCustomMaxClientCompanies());
            response.put("notes", subscription.getNotes());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting broker subscription", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Abonelik güncelle (SUPER_ADMIN)
     */
    @PutMapping("/broker/{brokerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateBrokerSubscription(
            @PathVariable Long brokerId,
            @Valid @RequestBody BrokerSubscriptionUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            BrokerSubscription subscription = subscriptionService.getBrokerSubscription(brokerId);

            BrokerSubscription updated = subscriptionService.updateSubscription(
                    subscription.getId(),
                    request.getNewPlanId(),
                    request.getNewEndDate(),
                    request.getCustomMaxBrokerUsers(),
                    request.getCustomMaxClientCompanies(),
                    currentUser
            );

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Subscription updated successfully",
                    "subscriptionId", updated.getId()
            ));

        } catch (Exception e) {
            logger.error("Error updating subscription", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Aboneliği iptal et (SUPER_ADMIN)
     */
    @DeleteMapping("/broker/{brokerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long brokerId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            BrokerSubscription subscription = subscriptionService.getBrokerSubscription(brokerId);
            subscriptionService.cancelSubscription(subscription.getId(), currentUser);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Subscription cancelled successfully"
            ));

        } catch (Exception e) {
            logger.error("Error cancelling subscription", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // ABONELİK UYARILARI
    // ==========================================

    /**
     * Süresi dolmak üzere olan abonelikleri getir (SUPER_ADMIN)
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getExpiringSubscriptions(@RequestParam(defaultValue = "30") int days) {
        try {
            List<BrokerSubscription> subscriptions = subscriptionService.getSubscriptionsExpiringInDays(days);

            return ResponseEntity.ok(Map.of(
                    "total", subscriptions.size(),
                    "daysThreshold", days,
                    "subscriptions", subscriptions.stream().map(this::mapSubscriptionToResponse).toList()
            ));

        } catch (Exception e) {
            logger.error("Error getting expiring subscriptions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Süresi dolmuş abonelikleri getir (SUPER_ADMIN)
     */
    @GetMapping("/expired")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getExpiredSubscriptions() {
        try {
            List<BrokerSubscription> subscriptions = subscriptionService.getExpiredSubscriptions();

            return ResponseEntity.ok(Map.of(
                    "total", subscriptions.size(),
                    "subscriptions", subscriptions.stream().map(this::mapSubscriptionToResponse).toList()
            ));

        } catch (Exception e) {
            logger.error("Error getting expired subscriptions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // HELPER METODLARI
    // ==========================================

    private Map<String, Object> mapSubscriptionToResponse(BrokerSubscription subscription) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", subscription.getId());
        response.put("brokerCompany", Map.of(
                "id", subscription.getBrokerCompany().getId(),
                "name", subscription.getBrokerCompany().getName()
        ));
        response.put("startDate", subscription.getStartDate());
        response.put("endDate", subscription.getEndDate());
        response.put("isActive", subscription.getIsActive());
        response.put("daysUntilExpiry", subscription.getDaysUntilExpiry());
        response.put("isExpired", subscription.isExpired());

        if (subscription.getSubscriptionPlan() != null) {
            response.put("plan", subscription.getSubscriptionPlan().getName());
        }

        return response;
    }
}