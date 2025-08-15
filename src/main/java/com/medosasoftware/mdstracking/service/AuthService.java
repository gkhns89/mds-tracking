package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.dto.AuthRequest;
import com.medosasoftware.mdstracking.dto.AuthResponse;
import com.medosasoftware.mdstracking.dto.RegisterRequest;
import com.medosasoftware.mdstracking.model.GlobalRole;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // ✅ Normal kullanıcı kaydı
    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGlobalRole(GlobalRole.USER); // ✅ Varsayılan USER rolü

        userService.createUser(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    // ✅ Super Admin kullanıcı oluşturma metodu
    public AuthResponse registerSuperAdmin(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGlobalRole(GlobalRole.SUPER_ADMIN); // ✅ SUPER_ADMIN rolü

        userService.createUser(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

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