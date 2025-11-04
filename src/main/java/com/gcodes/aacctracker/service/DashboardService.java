package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.dto.DashboardStatsResponse;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserService userService;

    public DashboardStatsResponse getDashboardStats(User currentUser) {
        if (currentUser.isSuperAdmin()) {
            return getSuperAdminStats();
        } else {
            return getUserStats(currentUser);
        }
    }

    private DashboardStatsResponse getSuperAdminStats() {
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        long activeUsers = userRepository.findByIsActiveTrue().size();
        long activeCompanies = companyRepository.findAll().stream()
                .filter(c -> c.getIsActive())
                .count();

        return new DashboardStatsResponse(
                totalUsers,
                totalCompanies,
                activeUsers,
                activeCompanies,
                totalCompanies, // Super admin tüm şirketleri görebilir
                totalUsers, // Super admin tüm kullanıcıları yönetebilir
                "SUPER_ADMIN",
                "System Administrator"
        );
    }

    private DashboardStatsResponse getUserStats(User currentUser) {
        long myCompanies = userService.getUserAccessibleCompanies(currentUser).size();
        long managedUsers = userService.getUserManageableCompanies(currentUser).stream()
                .mapToLong(company -> userService.getCompanyUsers(company.getId()).size())
                .sum();

        return new DashboardStatsResponse(
                0, // Normal kullanıcı total user sayısını görmez
                0, // Normal kullanıcı total company sayısını görmez
                0,
                0,
                myCompanies,
                managedUsers,
                "USER",
                "Company User"
        );
    }

    public List<Map<String, Object>> getRecentActivities(User currentUser) {
        List<Map<String, Object>> activities = new ArrayList<>();

        // Basit aktivite listesi (gerçek implementasyonda veritabanından gelir)
        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("action", "Login");
        activity1.put("timestamp", System.currentTimeMillis());
        activity1.put("description", "User logged in successfully");

        activities.add(activity1);

        return activities;
    }

    public List<Map<String, Object>> getMenuItems(User currentUser) {
        List<Map<String, Object>> menuItems = new ArrayList<>();

        // Dashboard - herkes görebilir
        addMenuItem(menuItems, "Dashboard", "/dashboard", "dashboard", true);

        // Profil - herkes görebilir
        addMenuItem(menuItems, "Profile", "/profile", "user", true);

        // Şirketlerim - herkes görebilir
        addMenuItem(menuItems, "My Companies", "/my-companies", "building", true);

        if (currentUser.isSuperAdmin()) {
            // Super admin menüleri
            addMenuItem(menuItems, "All Companies", "/companies", "buildings", true);
            addMenuItem(menuItems, "All Users", "/users", "users", true);
            addMenuItem(menuItems, "System Settings", "/settings", "settings", true);
        } else {
            // Normal kullanıcı - sadece yönetebileceği şirketler varsa
            if (!userService.getUserManageableCompanies(currentUser).isEmpty()) {
                addMenuItem(menuItems, "Manage Users", "/manage-users", "user-plus", true);
            }
        }

        return menuItems;
    }

    private void addMenuItem(List<Map<String, Object>> menuItems, String label, String path, String icon, boolean visible) {
        Map<String, Object> item = new HashMap<>();
        item.put("label", label);
        item.put("path", path);
        item.put("icon", icon);
        item.put("visible", visible);
        menuItems.add(item);
    }
}