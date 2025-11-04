package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Basit in-memory token storage (Production'da Redis kullanın)
    private Map<String, ResetTokenData> resetTokens = new HashMap<>();

    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();

            // Token'ı 1 saat süreyle sakla
            long expiryTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour
            resetTokens.put(token, new ResetTokenData(user.getId(), expiryTime));

            // TODO: Email gönderme servisini buraya ekleyin
            logger.info("Password reset token generated for user: {} (Token: {})", email, token);

            // Development için console'a yazdır
            System.out.println("🔑 Password Reset Token for " + email + ": " + token);
            System.out.println("🔗 Reset URL: http://localhost:3000/reset-password?token=" + token);
        } else {
            logger.warn("Password reset requested for non-existent email: {}", email);
        }
    }

    public boolean validateResetToken(String token) {
        ResetTokenData tokenData = resetTokens.get(token);
        if (tokenData == null) {
            return false;
        }

        if (System.currentTimeMillis() > tokenData.getExpiryTime()) {
            resetTokens.remove(token); // Expired token'ı sil
            return false;
        }

        return true;
    }

    public void resetPassword(String token, String newPassword) {
        if (!validateResetToken(token)) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        ResetTokenData tokenData = resetTokens.get(token);
        Optional<User> userOpt = userRepository.findById(tokenData.getUserId());

        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Token'ı kullanıldıktan sonra sil
        resetTokens.remove(token);

        logger.info("Password reset successfully for user: {}", user.getEmail());
    }

    // Token data class
    private static class ResetTokenData {
        private Long userId;
        private long expiryTime;

        public ResetTokenData(Long userId, long expiryTime) {
            this.userId = userId;
            this.expiryTime = expiryTime;
        }

        public Long getUserId() {
            return userId;
        }

        public long getExpiryTime() {
            return expiryTime;
        }
    }
}