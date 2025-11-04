package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.dto.DashboardStatsResponse;
import com.medosasoftware.mdstracking.service.DashboardService;
import com.medosasoftware.mdstracking.service.UserService;
import com.medosasoftware.mdstracking.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    // ✅ Dashboard istatistikleri
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        DashboardStatsResponse stats = dashboardService.getDashboardStats(currentUser);
        return ResponseEntity.ok(stats);
    }

    // ✅ Son aktiviteler
    @GetMapping("/recent-activities")
    public ResponseEntity<?> getRecentActivities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(dashboardService.getRecentActivities(currentUser));
    }

    // ✅ Kullanıcının erişebileceği menü itemları
    @GetMapping("/menu-items")
    public ResponseEntity<?> getMenuItems() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(dashboardService.getMenuItems(currentUser));
    }
}