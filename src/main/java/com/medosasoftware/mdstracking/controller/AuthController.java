package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.dto.AuthRequest;
import com.medosasoftware.mdstracking.dto.AuthResponse;
import com.medosasoftware.mdstracking.dto.RegisterRequest;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
