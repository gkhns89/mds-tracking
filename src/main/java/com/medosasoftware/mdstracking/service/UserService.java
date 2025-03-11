package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.repository.UserRepository;
import com.medosasoftware.mdstracking.repository.CompanyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CompanyRepository companyRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String username, String password, String companyName) {
        Optional<Company> companyOptional = companyRepository.findByName(companyName);
        if (companyOptional.isEmpty()) {
            throw new RuntimeException("Şirket bulunamadı!");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setCompany(companyOptional.get());

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
