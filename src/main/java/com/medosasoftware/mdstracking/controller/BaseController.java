package com.medosasoftware.mdstracking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // ✅ Controller seviyesinde de CORS
public class BaseController {

    // ✅ Frontend'in backend'i test etmesi için basit endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "MDS Tracking API is running");
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }

    // ✅ CORS preflight test için
    @GetMapping("/cors-test")
    public ResponseEntity<Map<String, String>> corsTest() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "CORS is working correctly!");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}