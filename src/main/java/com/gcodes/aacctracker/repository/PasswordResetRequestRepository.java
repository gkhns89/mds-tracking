package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.GlobalRole;
import com.gcodes.aacctracker.model.PasswordResetRequest;
import com.gcodes.aacctracker.model.ResetRequestStatus;
import com.gcodes.aacctracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    // ===== TOKEN SORGU LARI =====

    Optional<PasswordResetRequest> findByResetToken(String resetToken);

    Optional<PasswordResetRequest> findByResetTokenAndStatus(String resetToken, ResetRequestStatus status);

    // ===== KULLANICI SORGU LARI =====

    List<PasswordResetRequest> findByUser(User user);

    List<PasswordResetRequest> findByUserAndStatus(User user, ResetRequestStatus status);

    @Query("SELECT prr FROM PasswordResetRequest prr " +
            "WHERE prr.user.id = :userId " +
            "AND prr.status = 'PENDING' " +
            "ORDER BY prr.createdAt DESC")
    List<PasswordResetRequest> findPendingRequestsByUserId(@Param("userId") Long userId);

    // ===== ONAYLAYICI SORGU LARI =====

    List<PasswordResetRequest> findByApprover(User approver);

    List<PasswordResetRequest> findByApproverAndStatus(User approver, ResetRequestStatus status);

    @Query("SELECT prr FROM PasswordResetRequest prr " +
            "WHERE prr.approver.id = :approverId " +
            "AND prr.status = :status " +
            "ORDER BY prr.createdAt DESC")
    List<PasswordResetRequest> findByApproverIdAndStatus(
            @Param("approverId") Long approverId,
            @Param("status") ResetRequestStatus status
    );

    // ===== DURUM SORGU LARI =====

    List<PasswordResetRequest> findByStatus(ResetRequestStatus status);

    @Query("SELECT prr FROM PasswordResetRequest prr " +
            "WHERE prr.status = :status " +
            "ORDER BY prr.createdAt DESC")
    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(@Param("status") ResetRequestStatus status);

    // ===== ROL BAZLI SORGULAR =====

    @Query("SELECT prr FROM PasswordResetRequest prr " +
            "WHERE prr.status = :status " +
            "AND prr.user.globalRole IN :roles " +
            "ORDER BY prr.createdAt DESC")
    List<PasswordResetRequest> findByStatusAndUserGlobalRoleIn(
            @Param("status") ResetRequestStatus status,
            @Param("roles") List<GlobalRole> roles
    );

    // ===== BROKER FİRMASI İÇİN =====

    @Query("SELECT prr FROM PasswordResetRequest prr " +
            "WHERE prr.approver.company.id = :brokerCompanyId " +
            "AND prr.status = :status " +
            "ORDER BY prr.createdAt DESC")
    List<PasswordResetRequest> findByBrokerCompanyAndStatus(
            @Param("brokerCompanyId") Long brokerCompanyId,
            @Param("status") ResetRequestStatus status
    );

    // ===== İSTATİSTİKLER =====

    long countByStatus(ResetRequestStatus status);

    long countByUserAndStatus(User user, ResetRequestStatus status);

    @Query("SELECT COUNT(prr) FROM PasswordResetRequest prr " +
            "WHERE prr.approver.id = :approverId AND prr.status = 'PENDING'")
    long countPendingRequestsByApproverId(@Param("approverId") Long approverId);
}