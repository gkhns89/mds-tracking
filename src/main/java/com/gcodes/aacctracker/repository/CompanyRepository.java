package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
