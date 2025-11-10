package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.*;
import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.service.AuthService;
import com.gcodes.aacctracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserService userService;
    private final CompanyRepository companyRepository;

    /**
     * ❌ DEPRECATED - Kullanıcı kaydı (eski)
     * Artık /register-client kullanılmalı
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "❌ This endpoint is deprecated. Please use /register-client instead.",
                        "message", "Public registration is disabled. Use /register-client with a broker code."
                ));
    }

    /**
     * ✅ YENİ: Client (müşteri) kaydı - Firma kodu ile
     */
    @PostMapping("/register-client")
    public ResponseEntity<?> registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
        try {
            ContextualAuthResponse response = authService.registerClient(request);

            logger.info("Client registered: {} with broker code: {}",
                    request.getEmail(), request.getBrokerCode());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Registration successful",
                    "token", response.getToken(),
                    "user", response.getUser(),
                    "availableBrokers", response.getAvailableBrokers(),
                    "selectedBroker", response.getSelectedBroker()
            ));
        } catch (Exception e) {
            logger.error("Client registration error", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Registration failed: " + e.getMessage()));
        }
    }

    /**
     * ✅ Giriş - Basit (eski format, geriye dönük uyumluluk için)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);

            // Kullanıcı bilgilerini de döndür
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.info("User logged in: {} - Role: {}", request.getEmail(), user.getGlobalRole());

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("email", user.getEmail());
            userMap.put("username", user.getUsername());
            userMap.put("globalRole", user.getGlobalRole());
            userMap.put("isActive", user.getIsActive());

            if (user.getCompany() != null) {
                userMap.put("company", Map.of(
                        "id", user.getCompany().getId(),
                        "name", user.getCompany().getName(),
                        "type", user.getCompany().getCompanyType()
                ));
            } else {
                userMap.put("company", null);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Login successful",
                    "token", response.getToken(),
                    "user", userMap
            ));
        } catch (Exception e) {
            logger.error("Login error for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Invalid credentials"));
        }
    }

    /**
     * ✅ YENİ: Context-aware login (broker seçimi ile)
     * Frontend'in tercih etmesi gereken endpoint
     */
    @PostMapping("/login-with-context")
    public ResponseEntity<?> loginWithContext(@Valid @RequestBody ContextualLoginRequest request) {
        try {
            ContextualAuthResponse response = authService.authenticateWithContext(request);

            logger.info("User logged in with context: {} - Broker: {}",
                    request.getEmail(),
                    response.getSelectedBroker() != null ? response.getSelectedBroker().get("code") : "N/A");

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Login successful",
                    "token", response.getToken(),
                    "user", response.getUser(),
                    "availableBrokers", response.getAvailableBrokers(),
                    "selectedBroker", response.getSelectedBroker()
            ));
        } catch (Exception e) {
            logger.error("Login with context error for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Invalid credentials"));
        }
    }

    /**
     * ✅ YENİ: Aktif broker'ları listele (kayıt ekranı için)
     * Public endpoint - authentication gerektirmez
     */
    @GetMapping("/available-brokers")
    public ResponseEntity<?> getAvailableBrokers() {
        try {
            List<Company> brokers = companyRepository.findAllActiveBrokersWithCodes();

            List<Map<String, Object>> brokerList = brokers.stream()
                    .map(broker -> {
                        Map<String, Object> brokerMap = new HashMap<>();
                        brokerMap.put("code", broker.getCompanyCode());
                        brokerMap.put("name", broker.getName());
                        brokerMap.put("description", broker.getPublicDescription() != null ?
                                broker.getPublicDescription() : "");
                        return brokerMap;
                    })
                    .collect(Collectors.toList());

            logger.debug("Available brokers requested. Total: {}", brokerList.size());

            return ResponseEntity.ok(Map.of(
                    "total", brokerList.size(),
                    "brokers", brokerList
            ));
        } catch (Exception e) {
            logger.error("Error getting available brokers", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    /**
     * Token doğrulama
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        try {
            // SecurityContext'ten kullanıcı bilgisini al
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "❌ Invalid or expired token"));
            }

            String email = auth.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "username", user.getUsername(),
                            "globalRole", user.getGlobalRole()
                    )
            ));
        } catch (Exception e) {
            logger.error("Token validation error", e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "❌ Token validation failed"));
        }
    }

    /**
     * Çıkış (logout)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of(
                "message", "✅ Logout successful. Please delete the token on client side."
        ));
    }
}