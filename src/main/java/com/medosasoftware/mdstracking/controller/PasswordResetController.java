package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.dto.PasswordResetRequest;
import com.medosasoftware.mdstracking.dto.PasswordResetConfirmRequest;
import com.medosasoftware.mdstracking.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private PasswordResetService passwordResetService;

    // ✅ Şifre sıfırlama talebinde bulun
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());

            // Güvenlik için her zaman success döndür (email'in var olup olmadığını gizle)
            return ResponseEntity.ok("✅ If the email exists, a password reset link has been sent.");
        } catch (Exception e) {
            logger.error("Error in forgot password", e);
            return ResponseEntity.ok("✅ If the email exists, a password reset link has been sent.");
        }
    }

    // ✅ Reset token'ını doğrula
    @GetMapping("/reset-password/validate/{token}")
    public ResponseEntity<?> validateResetToken(@PathVariable String token) {
        try {
            boolean isValid = passwordResetService.validateResetToken(token);
            if (isValid) {
                return ResponseEntity.ok("✅ Reset token is valid");
            } else {
                return ResponseEntity.badRequest().body("❌ Reset token is invalid or expired");
            }
        } catch (Exception e) {
            logger.error("Error validating reset token", e);
            return ResponseEntity.badRequest().body("❌ Reset token is invalid or expired");
        }
    }

    // ✅ Şifreyi sıfırla
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("✅ Password has been reset successfully");
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            return ResponseEntity.badRequest().body("❌ Error resetting password: " + e.getMessage());
        }
    }
}