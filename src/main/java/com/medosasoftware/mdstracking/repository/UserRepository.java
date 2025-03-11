package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}