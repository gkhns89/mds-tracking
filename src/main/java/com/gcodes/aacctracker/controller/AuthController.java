package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.AuthRequest;
import com.gcodes.aacctracker.dto.AuthResponse;
import com.gcodes.aacctracker.dto.RegisterRequest;
import com.gcodes.aacctracker.model.GlobalRole;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.service.AuthService;
import com.gcodes.aacctracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserService userService;

    /**
     * Kullanıcı kaydı (register)
     * <p>
     * NOT: Normal kullanıcılar USER rolü ile kayıt olur.
     * SUPER_ADMIN ve diğer roller setup/admin tarafından atanır.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);

            logger.info("User registered: {}", request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Registration successful",
                    "token", response.getToken()
            ));
        } catch (Exception e) {
            logger.error("Registration error", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Registration failed: " + e.getMessage()));
        }
    }

    /**
     * Giriş (login)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);

            // Kullanıcı bilgilerini de döndür
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.info("User logged in: {} - Role: {}", request.getEmail(), user.getGlobalRole());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Login successful",
                    "token", response.getToken(),
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "username", user.getUsername(),
                            "globalRole", user.getGlobalRole(),
                            "isActive", user.getIsActive(),
                            "company", user.getCompany() != null ?
                                    Map.of(
                                            "id", user.getCompany().getId(),
                                            "name", user.getCompany().getName(),
                                            "type", user.getCompany().getCompanyType()
                                    ) : null
                    )
            ));
        } catch (Exception e) {
            logger.error("Login error for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Invalid credentials"));
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
     * <p>
     * NOT: JWT stateless olduğu için backend'de logout işlemi yok.
     * Frontend token'ı silmeli.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of(
                "message", "✅ Logout successful. Please delete the token on client side."
        ));
    }
}