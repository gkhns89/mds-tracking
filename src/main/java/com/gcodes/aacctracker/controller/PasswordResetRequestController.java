package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.PasswordResetRequestCreateDto;
import com.gcodes.aacctracker.dto.PasswordResetApproveDto;
import com.gcodes.aacctracker.dto.PasswordResetWithTokenDto;
import com.gcodes.aacctracker.model.PasswordResetRequest;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.service.PasswordResetRequestService;
import com.gcodes.aacctracker.service.UserService;
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
@RequestMapping("/api/password-reset-requests")
@CrossOrigin(origins = "*")
public class PasswordResetRequestController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetRequestController.class);

    @Autowired
    private PasswordResetRequestService requestService;

    @Autowired
    private UserService userService;

    // ==========================================
    // İSTEK OLUŞTURMA
    // ==========================================

    /**
     * Şifre sıfırlama isteği oluştur
     */
    @PostMapping
    public ResponseEntity<?> createResetRequest(@Valid @RequestBody PasswordResetRequestCreateDto request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PasswordResetRequest created = requestService.createResetRequest(
                    request.getUserId(),
                    request.getReason(),
                    currentUser
            );

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Password reset request created successfully",
                    "requestId", created.getId(),
                    "status", created.getStatus(),
                    "approver", created.getApprover() != null ? created.getApprover().getEmail() : "N/A"
            ));

        } catch (Exception e) {
            logger.error("Error creating reset request", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // İSTEK ONAYLAMA/REDDETME
    // ==========================================

    /**
     * İsteği onayla
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,
            @Valid @RequestBody PasswordResetApproveDto request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PasswordResetRequest approved = requestService.approveRequest(
                    id,
                    currentUser,
                    request.getAdminNotes()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Password reset request approved",
                    "requestId", approved.getId(),
                    "resetToken", approved.getResetToken(),
                    "tokenExpiresAt", approved.getTokenExpiresAt()
            ));

        } catch (Exception e) {
            logger.error("Error approving request", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * İsteği reddet
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PasswordResetRequest rejected = requestService.rejectRequest(id, currentUser, reason);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Password reset request rejected",
                    "requestId", rejected.getId()
            ));

        } catch (Exception e) {
            logger.error("Error rejecting request", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // ŞİFRE SIFIRLAMA
    // ==========================================

    /**
     * Token ile şifreyi sıfırla
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPasswordWithToken(@Valid @RequestBody PasswordResetWithTokenDto request) {
        try {
            requestService.resetPasswordWithToken(request.getToken(), request.getNewPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Password has been reset successfully"
            ));

        } catch (Exception e) {
            logger.error("Error resetting password", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // İSTEK SORGULAMA
    // ==========================================

    /**
     * Bekleyen istekleri getir
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<PasswordResetRequest> requests = requestService.getPendingRequests(currentUser);

            return ResponseEntity.ok(Map.of(
                    "total", requests.size(),
                    "requests", requests.stream().map(this::mapRequestToResponse).toList()
            ));

        } catch (Exception e) {
            logger.error("Error getting pending requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Kullanıcının isteklerini getir
     */
    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<PasswordResetRequest> requests = requestService.getUserRequests(currentUser.getId());

            return ResponseEntity.ok(Map.of(
                    "total", requests.size(),
                    "requests", requests.stream().map(this::mapRequestToResponse).toList()
            ));

        } catch (Exception e) {
            logger.error("Error getting user requests", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // HELPER METODLARI
    // ==========================================

    private Map<String, Object> mapRequestToResponse(PasswordResetRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", request.getId());
        response.put("user", Map.of(
                "id", request.getUser().getId(),
                "email", request.getUser().getEmail(),
                "username", request.getUser().getUsername()
        ));
        response.put("status", request.getStatus());
        response.put("requestReason", request.getRequestReason());
        response.put("createdAt", request.getCreatedAt());

        if (request.getApprover() != null) {
            response.put("approver", Map.of(
                    "id", request.getApprover().getId(),
                    "email", request.getApprover().getEmail()
            ));
        }

        response.put("resolvedAt", request.getResolvedAt());
        response.put("adminNotes", request.getAdminNotes());

        return response;
    }
}