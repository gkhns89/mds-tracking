package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.dto.AuthRequest;
import com.medosasoftware.mdstracking.dto.AuthResponse;
import com.medosasoftware.mdstracking.dto.RegisterRequest;
import com.medosasoftware.mdstracking.model.Role;
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

    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER); // Varsayılan olarak USER rolü atanıyor

        userService.createUser(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new AuthResponse(token);
    }
}