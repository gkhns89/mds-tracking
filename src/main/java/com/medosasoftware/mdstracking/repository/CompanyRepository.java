package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
}
