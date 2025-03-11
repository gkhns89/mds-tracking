package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
