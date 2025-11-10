package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.AgencyAgreementCreateRequest;
import com.gcodes.aacctracker.dto.AgencyAgreementUpdateRequest;
import com.gcodes.aacctracker.model.AgencyAgreement;
import com.gcodes.aacctracker.model.AgreementStatus;
import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.service.AgencyAgreementService;
import com.gcodes.aacctracker.service.AuditLogService;
import com.gcodes.aacctracker.service.UserService;
import com.gcodes.aacctracker.repository.CompanyRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agreements")
@CrossOrigin(origins = "*")
public class AgencyAgreementController {

    private static final Logger logger = LoggerFactory.getLogger(AgencyAgreementController.class);

    @Autowired
    private AgencyAgreementService agreementService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CompanyRepository companyRepository;

    // ✅ Anlaşma oluşturma
    @PostMapping
    public ResponseEntity<?> createAgreement(@Valid @RequestBody AgencyAgreementCreateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü - Sadece SUPER_ADMIN veya Broker ADMIN
            Company broker = companyRepository.findById(request.getBrokerCompanyId())
                    .orElseThrow(() -> new RuntimeException("Broker company not found"));

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Only SUPER_ADMIN or Broker ADMIN can create agreements"));
            }

            AgencyAgreement agreement = agreementService.createAgreement(
                    request.getBrokerCompanyId(),
                    request.getClientCompanyId(),
                    currentUser
            );

            // Audit log
            auditLogService.logAction(currentUser, "CREATE_AGREEMENT",
                    "AgencyAgreement", agreement.getId());

            logger.info("Agreement created: {} by {}", agreement.getAgreementNumber(), currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Agreement created successfully",
                    "agreementId", agreement.getId(),
                    "agreementNumber", agreement.getAgreementNumber(),
                    "agreement", agreement
            ));

        } catch (Exception e) {
            logger.error("Error creating agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error creating agreement: " + e.getMessage()));
        }
    }

    // ✅ Anlaşmayı güncelleme
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgreement(
            @PathVariable Long id,
            @Valid @RequestBody AgencyAgreementUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AgencyAgreement agreement = agreementService.getAgreementById(id);
            Company broker = agreement.getBrokerCompany();

            // Yetki kontrolü
            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions to update this agreement"));
            }

            // Durumu güncelle
            if (request.getStatus() != null) {
                agreement = switch (request.getStatus()) {
                    case SUSPENDED -> agreementService.suspendAgreement(id, request.getNotes());
                    case TERMINATED -> agreementService.terminateAgreement(id, request.getNotes());
                    case ACTIVE -> agreementService.reactivateAgreement(id);
                };
            }

            // Audit log
            auditLogService.logAction(currentUser, "UPDATE_AGREEMENT",
                    "AgencyAgreement", id);

            logger.info("Agreement updated: {} by {}", id, currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Agreement updated successfully",
                    "agreement", agreement
            ));

        } catch (Exception e) {
            logger.error("Error updating agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error updating agreement: " + e.getMessage()));
        }
    }

    // ✅ Anlaşmayı duraklatma
    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspendAgreement(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AgencyAgreement agreement = agreementService.getAgreementById(id);
            Company broker = agreement.getBrokerCompany();

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions"));
            }

            AgencyAgreement suspended = agreementService.suspendAgreement(id, reason);

            auditLogService.logAction(currentUser, "SUSPEND_AGREEMENT",
                    "AgencyAgreement", id);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Agreement suspended successfully",
                    "agreement", suspended
            ));

        } catch (Exception e) {
            logger.error("Error suspending agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Anlaşmayı sonlandırma
    @PostMapping("/{id}/terminate")
    public ResponseEntity<?> terminateAgreement(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AgencyAgreement agreement = agreementService.getAgreementById(id);
            Company broker = agreement.getBrokerCompany();

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions"));
            }

            AgencyAgreement terminated = agreementService.terminateAgreement(id, reason);

            auditLogService.logAction(currentUser, "TERMINATE_AGREEMENT",
                    "AgencyAgreement", id);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Agreement terminated successfully",
                    "agreement", terminated
            ));

        } catch (Exception e) {
            logger.error("Error terminating agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Anlaşmayı reaktivleştirme
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivateAgreement(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AgencyAgreement agreement = agreementService.getAgreementById(id);
            Company broker = agreement.getBrokerCompany();

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions"));
            }

            AgencyAgreement reactivated = agreementService.reactivateAgreement(id);

            auditLogService.logAction(currentUser, "REACTIVATE_AGREEMENT",
                    "AgencyAgreement", id);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Agreement reactivated successfully",
                    "agreement", reactivated
            ));

        } catch (Exception e) {
            logger.error("Error reactivating agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Tek anlaşma getir
    @GetMapping("/{id}")
    public ResponseEntity<?> getAgreement(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AgencyAgreement agreement = agreementService.getAgreementById(id);

            // Yetki kontrolü
            if (!currentUser.isSuperAdmin() &&
                    !currentUser.isAdminOfCompany(agreement.getBrokerCompany()) &&
                    !currentUser.isAdminOfCompany(agreement.getClientCompany())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied to this agreement"));
            }

            return ResponseEntity.ok(agreement);

        } catch (Exception e) {
            logger.error("Error getting agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Anlaşma numarasına göre getir
    @GetMapping("/by-number/{agreementNumber}")
    public ResponseEntity<?> getAgreementByNumber(@PathVariable String agreementNumber) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            AgencyAgreement agreement = agreementService.getAgreementByNumber(agreementNumber)
                    .orElseThrow(() -> new RuntimeException("Agreement not found"));

            // Yetki kontrolü
            if (!currentUser.isSuperAdmin() &&
                    !currentUser.isAdminOfCompany(agreement.getBrokerCompany()) &&
                    !currentUser.isAdminOfCompany(agreement.getClientCompany())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied to this agreement"));
            }

            return ResponseEntity.ok(agreement);

        } catch (Exception e) {
            logger.error("Error getting agreement by number", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Broker'ın tüm anlaşmaları
    @GetMapping("/broker/{brokerId}")
    public ResponseEntity<?> getBrokerAgreements(
            @PathVariable Long brokerId,
            @RequestParam(required = false) AgreementStatus status) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Company broker = companyRepository.findById(brokerId)
                    .orElseThrow(() -> new RuntimeException("Broker company not found"));

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            List<AgencyAgreement> agreements = (status == AgreementStatus.ACTIVE)
                    ? agreementService.getBrokerActiveAgreements(brokerId)
                    : agreementService.getBrokerAgreements(brokerId);

            return ResponseEntity.ok(Map.of(
                    "total", agreements.size(),
                    "agreements", agreements
            ));

        } catch (Exception e) {
            logger.error("Error getting broker agreements", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Client'in tüm anlaşmaları
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientAgreements(
            @PathVariable Long clientId,
            @RequestParam(required = false) AgreementStatus status) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Company client = companyRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client company not found"));

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(client)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            List<AgencyAgreement> agreements = (status == AgreementStatus.ACTIVE)
                    ? agreementService.getClientActiveAgreements(clientId)
                    : agreementService.getClientAgreements(clientId);

            return ResponseEntity.ok(Map.of(
                    "total", agreements.size(),
                    "agreements", agreements
            ));

        } catch (Exception e) {
            logger.error("Error getting client agreements", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Aktif anlaşma kontrolü
    @GetMapping("/check")
    public ResponseEntity<?> checkActiveAgreement(
            @RequestParam Long brokerId,
            @RequestParam Long clientId) {
        try {
            boolean hasActive = agreementService.hasActiveAgreement(brokerId, clientId);

            return ResponseEntity.ok(Map.of(
                    "hasActiveAgreement", hasActive,
                    "message", hasActive ?
                            "✅ Active agreement exists" :
                            "❌ No active agreement found"
            ));

        } catch (Exception e) {
            logger.error("Error checking agreement", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Tüm anlaşmaları listele (SUPER_ADMIN only)
    @GetMapping
    public ResponseEntity<?> getAllAgreements() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Only SUPER_ADMIN can view all agreements"));
            }

            List<AgencyAgreement> agreements = agreementService.getAllAgreements();

            return ResponseEntity.ok(Map.of(
                    "total", agreements.size(),
                    "agreements", agreements
            ));

        } catch (Exception e) {
            logger.error("Error getting all agreements", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Son anlaşmalar
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentAgreements() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!currentUser.isSuperAdmin()) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Only SUPER_ADMIN can view recent agreements"));
            }

            List<AgencyAgreement> agreements = agreementService.getRecentAgreements();

            return ResponseEntity.ok(Map.of(
                    "total", agreements.size(),
                    "agreements", agreements
            ));

        } catch (Exception e) {
            logger.error("Error getting recent agreements", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Broker istatistikleri
    @GetMapping("/stats/broker/{brokerId}")
    public ResponseEntity<?> getBrokerAgreementStats(@PathVariable Long brokerId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Company broker = companyRepository.findById(brokerId)
                    .orElseThrow(() -> new RuntimeException("Broker company not found"));

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(broker)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            long activeClientCount = agreementService.getBrokerActiveClientCount(brokerId);
            List<AgencyAgreement> allAgreements = agreementService.getBrokerAgreements(brokerId);
            List<AgencyAgreement> activeAgreements = agreementService.getBrokerActiveAgreements(brokerId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAgreements", allAgreements.size());
            stats.put("activeAgreements", activeAgreements.size());
            stats.put("activeClientCount", activeClientCount);
            stats.put("inactiveAgreements", allAgreements.size() - activeAgreements.size());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting broker agreement stats", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Client istatistikleri
    @GetMapping("/stats/client/{clientId}")
    public ResponseEntity<?> getClientAgreementStats(@PathVariable Long clientId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Company client = companyRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client company not found"));

            if (!currentUser.isSuperAdmin() && !currentUser.isAdminOfCompany(client)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            long activeBrokerCount = agreementService.getClientActiveBrokerCount(clientId);
            List<AgencyAgreement> allAgreements = agreementService.getClientAgreements(clientId);
            List<AgencyAgreement> activeAgreements = agreementService.getClientActiveAgreements(clientId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAgreements", allAgreements.size());
            stats.put("activeAgreements", activeAgreements.size());
            stats.put("activeBrokerCount", activeBrokerCount);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting client agreement stats", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }
}