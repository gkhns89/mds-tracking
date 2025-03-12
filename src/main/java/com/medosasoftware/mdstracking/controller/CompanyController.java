package com.medosasoftware.mdstracking.controller;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    @Autowired
    private CompanyRepository companyRepository;

    @PostMapping("/create")
    public Company createCompany(@RequestBody Company company) {
        return companyRepository.save(company);
    }

    @GetMapping
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }
}
