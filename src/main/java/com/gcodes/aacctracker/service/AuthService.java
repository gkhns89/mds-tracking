package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.dto.AuthRequest;
import com.gcodes.aacctracker.dto.AuthResponse;
import com.gcodes.aacctracker.dto.RegisterRequest;
import com.gcodes.aacctracker.model.GlobalRole;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // @PostMapping("/register")
    // public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    //     // Public registration disabled
    //     return ResponseEntity.status(403)
    //             .body(Map.of("error", "Public registration is disabled. Contact administrator."));
    // }

    // ✅ Super Admin kullanıcı oluşturma metodu
//    public AuthResponse registerSuperAdmin(RegisterRequest request) {
//        User user = new User();
//        user.setEmail(request.getEmail());
//        user.setUsername(request.getUsername());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setGlobalRole(GlobalRole.SUPER_ADMIN); // ✅ SUPER_ADMIN rolü
//
//        userService.createUser(user);
//        String token = jwtTokenProvider.generateToken(user.getEmail());
//
//        return new AuthResponse(token);
//    }

    // ✅ Giriş
    public AuthResponse authenticate(AuthRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new AuthResponse(token);
    }
}