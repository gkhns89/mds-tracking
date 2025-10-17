package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.AuditLog;
import com.medosasoftware.mdstracking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ✅ Belirli kullanıcının tüm aktiviteleri
    List<AuditLog> findByPerformedBy(User performedBy);

    // ✅ Belirli entity tipinin tüm logları
    List<AuditLog> findByEntityType(String entityType);

    // ✅ Belirli entity'nin tüm logları
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    // ✅ Belirli işlem tipinin logları
    List<AuditLog> findByAction(String action);

    // ✅ Tarih aralığında loglar
    List<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    // ✅ Kullanıcının belirli tarih aralığında yaptığı işlemler
    @Query("SELECT al FROM AuditLog al WHERE al.performedBy = :user " +
            "AND al.timestamp BETWEEN :startTime AND :endTime")
    List<AuditLog> findUserActivitiesByDateRange(
            @Param("user") User user,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ✅ Belirli entity'nin değişiklik tarihi
    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType " +
            "AND al.entityId = :entityId ORDER BY al.timestamp DESC")
    List<AuditLog> findEntityChangeHistory(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    // ✅ Son 10 aktivite
    @Query("SELECT al FROM AuditLog al ORDER BY al.timestamp DESC LIMIT 10")
    List<AuditLog> findRecentActivities();

    // ✅ Kullanıcının son 10 aktivitesi
    @Query("SELECT al FROM AuditLog al WHERE al.performedBy = :user " +
            "ORDER BY al.timestamp DESC LIMIT 10")
    List<AuditLog> findUserRecentActivities(@Param("user") User user);

    // ✅ Başarısız işlemler
    List<AuditLog> findByResult(String result);

    // ✅ Belirli tarihte başarısız işlemler
    @Query("SELECT al FROM AuditLog al WHERE al.result = 'FAILURE' " +
            "AND al.timestamp >= :startTime AND al.timestamp <= :endTime")
    List<AuditLog> findFailuresInDateRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ✅ İstatistik: Kullanıcı başına işlem sayısı
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.performedBy = :user")
    long countActivitiesByUser(@Param("user") User user);

    // ✅ İstatistik: Entity tipi başına işlem sayısı
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.entityType = :entityType")
    long countActivitiesByEntityType(@Param("entityType") String entityType);
}