package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import com.medosasoftware.mdstracking.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createCompany(@RequestBody Company company) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!currentUser.isSuperAdmin()) {
            return ResponseEntity.status(403).body("❌ Only SUPER_ADMIN can create companies");
        }

        try {
            Company savedCompany = companyRepository.save(company);
            logger.info("Company created successfully: {} by user: {}", savedCompany.getName(), currentUser.getEmail());
            return ResponseEntity.ok("✅ Company created successfully: " + savedCompany.getName());
        } catch (Exception e) {
            logger.error("Error creating company", e);
            return ResponseEntity.badRequest().body("❌ Error creating company: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Company> companies;

        if (currentUser.isSuperAdmin()) {
            logger.debug("SUPER_ADMIN user - showing all companies");
            companies = companyRepository.findAll();
        } else {
            logger.debug("USER - showing only accessible companies");
            companies = userService.getUserAccessibleCompanies(currentUser);
        }

        return ResponseEntity.ok(companies);
    }

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

        if (!userService.canUserViewCompany(currentUser, company)) {
            return ResponseEntity.status(403).body("❌ Access denied to this company");
        }

        return ResponseEntity.ok(company);
    }

    @GetMapping("/my-companies")
    public ResponseEntity<List<Company>> getMyCompanies() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Company> companies = userService.getUserAccessibleCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/manageable")
    public ResponseEntity<List<Company>> getManageableCompanies() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Company> companies = userService.getUserManageableCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }

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

        if (!currentUser.isSuperAdmin() && !currentUser.isCompanyAdmin(company)) {
            return ResponseEntity.status(403).body("❌ Insufficient permissions to update this company");
        }

        company.setName(companyDetails.getName());
        company.setDescription(companyDetails.getDescription());

        Company updatedCompany = companyRepository.save(company);
        return ResponseEntity.ok(updatedCompany);
    }
}