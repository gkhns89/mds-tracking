package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import com.medosasoftware.mdstracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserService userService;

    // ✅ Firma oluşturma (Sadece SUPER_ADMIN)
    @PostMapping("/create")
    public ResponseEntity<?> createCompany(@RequestBody Company company) {
        try {
            Company savedCompany = companyRepository.save(company);
            return ResponseEntity.ok("✅ Company created successfully: " + savedCompany.getName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error creating company: " + e.getMessage());
        }
    }

    // ✅ Firmaları listeleme (Yetki bazlı)
    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Company> companies;

        if (currentUser.isSuperAdmin()) {
            System.out.println("👑 SUPER_ADMIN user - showing all companies");
            companies = companyRepository.findAll(); // Tüm şirketler
        } else {
            System.out.println("👤 USER - showing only accessible companies");
            companies = userService.getUserAccessibleCompanies(currentUser); // Sadece erişebildiği şirketler
        }

        return ResponseEntity.ok(companies);
    }

    // ✅ Belirli firmayı getirme
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Company> companyOpt = companyRepository.findById(id);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Company company = companyOpt.get();

        // Yetki kontrolü
        if (!userService.canUserViewCompany(currentUser, company)) {
            return ResponseEntity.status(403).body("❌ Access denied to this company");
        }

        return ResponseEntity.ok(company);
    }

    // ✅ Kullanıcının sadece kendi erişebildiği firmaları görmesi için ek endpoint
    @GetMapping("/my-companies")
    public ResponseEntity<List<Company>> getMyCompanies() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("📋 User " + email + " requesting their accessible companies");
        List<Company> companies = userService.getUserAccessibleCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    // ✅ Kullanıcının yönetebileceği firmalar
    @GetMapping("/manageable")
    public ResponseEntity<List<Company>> getManageableCompanies() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Company> companies = userService.getUserManageableCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    // ✅ Firma güncelleme
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody Company companyDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Company> companyOpt = companyRepository.findById(id);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Company company = companyOpt.get();

        // Yetki kontrolü - Sadece SUPER_ADMIN veya COMPANY_ADMIN
        if (!currentUser.isSuperAdmin() && !currentUser.isCompanyAdmin(company)) {
            return ResponseEntity.status(403).body("❌ Insufficient permissions to update this company");
        }

        company.setName(companyDetails.getName());
        company.setDescription(companyDetails.getDescription());

        Company updatedCompany = companyRepository.save(company);
        return ResponseEntity.ok(updatedCompany);
    }
}