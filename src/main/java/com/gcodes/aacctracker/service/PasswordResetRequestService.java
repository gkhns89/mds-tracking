package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.exception.UnauthorizedException;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.PasswordResetRequestRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetRequestService.class);

    @Autowired
    private PasswordResetRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // @Autowired
    // private EmailService emailService; // İleride eklenecek

    /**
     * Şifre sıfırlama isteği oluştur
     */
    public PasswordResetRequest createResetRequest(Long userId, String reason, User requestingUser) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kullanıcı kendisi için mi istek yapıyor?
        if (!requestingUser.getId().equals(userId)) {
            throw new UnauthorizedException("You can only request password reset for yourself");
        }

        // Kim onaylayacak?
        User approver = determineApprover(targetUser);

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUser(targetUser);
        request.setApprover(approver);
        request.setRequestReason(reason);
        request.setStatus(ResetRequestStatus.PENDING);

        PasswordResetRequest saved = requestRepository.save(request);

        // Email gönder (approver'a)
        // if (emailService != null) {
        //     emailService.sendPasswordResetRequestNotification(approver, saved);
        // }

        logger.info("Password reset request created for user: {} - Approver: {}",
                targetUser.getEmail(), approver.getEmail());

        return saved;
    }

    /**
     * İsteği onayla
     */
    public PasswordResetRequest approveRequest(Long requestId, User approver, String adminNotes) {
        PasswordResetRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.isPending()) {
            throw new RuntimeException("Request is not in PENDING status");
        }

        // Yetki kontrolü
        if (!canUserApproveRequest(approver, request)) {
            throw new UnauthorizedException("Insufficient permissions to approve this request");
        }

        // Token oluştur
        String resetToken = UUID.randomUUID().toString();
        request.approve(approver, resetToken);
        request.setAdminNotes(adminNotes);

        PasswordResetRequest saved = requestRepository.save(request);

        // Email gönder (kullanıcıya)
        // if (emailService != null) {
        //     emailService.sendPasswordResetApprovedEmail(request.getUser(), resetToken);
        // }

        logger.info("Password reset request approved: {} by: {}",
                requestId, approver.getEmail());

        return saved;
    }

    /**
     * İsteği reddet
     */
    public PasswordResetRequest rejectRequest(Long requestId, User approver, String reason) {
        PasswordResetRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.isPending()) {
            throw new RuntimeException("Request is not in PENDING status");
        }

        // Yetki kontrolü
        if (!canUserApproveRequest(approver, request)) {
            throw new UnauthorizedException("Insufficient permissions to reject this request");
        }

        request.reject(approver, reason);

        PasswordResetRequest saved = requestRepository.save(request);

        // Email gönder (kullanıcıya)
        // if (emailService != null) {
        //     emailService.sendPasswordResetRejectedEmail(request.getUser(), reason);
        // }

        logger.info("Password reset request rejected: {} by: {}",
                requestId, approver.getEmail());

        return saved;
    }

    /**
     * Token ile şifreyi sıfırla
     */
    public void resetPasswordWithToken(String token, String newPassword) {
        PasswordResetRequest request = requestRepository.findByResetTokenAndStatus(
                token, ResetRequestStatus.APPROVED
        ).orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        // Token süresi dolmuş mu?
        if (request.getTokenExpiresAt() != null &&
                LocalDateTime.now().isAfter(request.getTokenExpiresAt())) {
            throw new RuntimeException("Reset token has expired");
        }

        // Şifreyi değiştir
        User user = request.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // İsteği tamamlanmış olarak işaretle
        request.complete();
        requestRepository.save(request);

        logger.info("Password reset completed for user: {}", user.getEmail());
    }

    /**
     * Bekleyen istekleri getir
     */
    public List<PasswordResetRequest> getPendingRequests(User approver) {
        if (approver.isSuperAdmin()) {
            // SUPER_ADMIN tüm BROKER_ADMIN isteklerini görebilir
            return requestRepository.findByStatusAndUserGlobalRoleIn(
                    ResetRequestStatus.PENDING,
                    List.of(GlobalRole.BROKER_ADMIN)
            );
        } else if (approver.isBrokerAdmin()) {
            // BROKER_ADMIN kendi firmasındaki istekleri görebilir
            return requestRepository.findByApproverAndStatus(approver, ResetRequestStatus.PENDING);
        }

        return Collections.emptyList();
    }

    /**
     * Kullanıcının isteklerini getir
     */
    public List<PasswordResetRequest> getUserRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return requestRepository.findByUser(user);
    }

    // ===== HELPER METODLARI =====

    /**
     * Kim onaylayacak?
     */
    private User determineApprover(User targetUser) {
        if (targetUser.isBrokerAdmin()) {
            // BROKER_ADMIN → SUPER_ADMIN onaylar
            return userRepository.findFirstByGlobalRole(GlobalRole.SUPER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("No SUPER_ADMIN found"));
        } else if (targetUser.isBrokerUser() || targetUser.isClientUser()) {
            // BROKER_USER veya CLIENT_USER → BROKER_ADMIN onaylar
            Company brokerCompany = targetUser.getBrokerCompany();
            if (brokerCompany == null) {
                throw new RuntimeException("User does not belong to any broker company");
            }

            return userRepository.findByCompanyAndGlobalRole(brokerCompany, GlobalRole.BROKER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("No BROKER_ADMIN found for this company"));
        }

        throw new RuntimeException("Cannot determine approver for this user");
    }

    /**
     * Kullanıcı bu isteği onaylayabilir mi?
     */
    private boolean canUserApproveRequest(User user, PasswordResetRequest request) {
        if (user.isSuperAdmin()) return true;

        if (user.isBrokerAdmin()) {
            // BROKER_ADMIN sadece kendi firmasındaki kullanıcıların isteklerini onaylayabilir
            Company userBroker = user.getCompany();
            User requestUser = request.getUser();
            Company requestBroker = requestUser.getBrokerCompany();

            return userBroker != null && requestBroker != null &&
                    userBroker.getId().equals(requestBroker.getId());
        }

        return false;
    }
}