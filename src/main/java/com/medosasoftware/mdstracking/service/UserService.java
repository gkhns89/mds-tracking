package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.model.Role;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import com.medosasoftware.mdstracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void assignCompanyToUser(Long userId, Long companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        user.getCompanies().add(company);
        userRepository.save(user);
    }

    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
