package com.medosasoftware.mdstracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medosasoftware.mdstracking.model.AuditLog;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Basit loglama (action + entity bilgisi)
    public AuditLog logAction(User performedBy, String action, String entityType, Long entityId) {
        return logAction(performedBy, action, entityType, entityId, null, null, "SUCCESS", null, null);
    }

    // ✅ IP adresi ile loglama
    public AuditLog logAction(User performedBy, String action, String entityType, Long entityId, String ipAddress) {
        return logAction(performedBy, action, entityType, entityId, null, ipAddress, "SUCCESS", null, null);
    }

    // ✅ Detaylı loglama (before/after değişiklikler)
    public AuditLog logActionWithChanges(User performedBy, String action, String entityType, Long entityId,
                                         Object before, Object after) {
        return logAction(performedBy, action, entityType, entityId, before, after, "SUCCESS", null, null);
    }

    // ✅ Hata ile loglama
    public AuditLog logActionError(User performedBy, String action, String entityType, Long entityId,
                                   String ipAddress, Exception exception) {
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
        return logAction(performedBy, action, entityType, entityId, null, ipAddress, "FAILURE", errorMessage, null);
    }

    // ✅ Tam loglama metodu
    private AuditLog logAction(User performedBy, String action, String entityType, Long entityId,
                               Object before, Object after, String result,
                               String errorMessage, String ipAddress) {
        try {
            AuditLog log = new AuditLog();
            log.setPerformedBy(performedBy);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setResult(result);
            log.setErrorMessage(errorMessage);
            log.setIpAddress(ipAddress);
            log.setTimestamp(LocalDateTime.now());

            // Before/After değişikliklerini JSON olarak sakla
            if (before != null || after != null) {
                Map<String, Object> changes = new HashMap<>();
                if (before != null) {
                    changes.put("before", before);
                }
                if (after != null) {
                    changes.put("after", after);
                }
                try {
                    log.setChangeDetails(objectMapper.writeValueAsString(changes));
                } catch (Exception e) {
                    logger.error("Error serializing changes", e);
                }
            }

            AuditLog saved = auditLogRepository.save(log);

            logger.info("Action logged: {} - User: {}, Entity: {} ({}), Result: {}",
                    action, performedBy.getEmail(), entityType, entityId, result);

            return saved;
        } catch (Exception e) {
            logger.error("Error logging action", e);
            return null;
        }
    }

    // ✅ Kullanıcının tüm aktiviteleri
    public List<AuditLog> getUserActivities(User user) {
        return auditLogRepository.findByPerformedBy(user);
    }

    // ✅ Kullanıcının son 10 aktivitesi
    public List<AuditLog> getUserRecentActivities(User user) {
        return auditLogRepository.findUserRecentActivities(user);
    }

    // ✅ Kullanıcının belirli tarih aralığındaki aktiviteleri
    public List<AuditLog> getUserActivitiesByDateRange(User user, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findUserActivitiesByDateRange(user, startTime, endTime);
    }

    // ✅ Entity'nin değişiklik geçmişi
    public List<AuditLog> getEntityChangeHistory(String entityType, Long entityId) {
        return auditLogRepository.findEntityChangeHistory(entityType, entityId);
    }

    // ✅ Belirli işlem tipinin logları
    public List<AuditLog> getActionLogs(String action) {
        return auditLogRepository.findByAction(action);
    }

    // ✅ Belirli entity tipinin logları
    public List<AuditLog> getEntityTypeLogs(String entityType) {
        return auditLogRepository.findByEntityType(entityType);
    }

    // ✅ Belirli entity'nin tüm logları
    public List<AuditLog> getEntityLogs(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    // ✅ Tarih aralığında tüm loglar
    public List<AuditLog> getLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime);
    }

    // ✅ Son 10 aktivite (sistem geneli)
    public List<AuditLog> getRecentActivities() {
        return auditLogRepository.findRecentActivities();
    }

    // ✅ Başarısız işlemler
    public List<AuditLog> getFailedActions() {
        return auditLogRepository.findByResult("FAILURE");
    }

    // ✅ Belirli tarihte başarısız işlemler
    public List<AuditLog> getFailedActionsInDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findFailuresInDateRange(startTime, endTime);
    }

    // ✅ İstatistik: Kullanıcı başına aktivite sayısı
    public long getUserActivityCount(User user) {
        return auditLogRepository.countActivitiesByUser(user);
    }

    // ✅ İstatistik: Entity tipi başına aktivite sayısı
    public long getEntityTypeActivityCount(String entityType) {
        return auditLogRepository.countActivitiesByEntityType(entityType);
    }

    // ✅ Log detaylarını getir
    public AuditLog getLogById(Long logId) {
        return auditLogRepository.findById(logId).orElse(null);
    }

    // ✅ Tüm logları temizle (Production'da kullanmayın!)
    public void clearAllLogs() {
        logger.warn("⚠️ All audit logs are being cleared!");
        auditLogRepository.deleteAll();
    }

    // ✅ Eski logları temizle (X gün öncesindeki)
    public int clearOldLogs(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<AuditLog> oldLogs = auditLogRepository.findByTimestampBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                cutoffDate
        );
        auditLogRepository.deleteAll(oldLogs);
        logger.info("Cleared {} old audit logs (older than {} days)", oldLogs.size(), daysOld);
        return oldLogs.size();
    }
}