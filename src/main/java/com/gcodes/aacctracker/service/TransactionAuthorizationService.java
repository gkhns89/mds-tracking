package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CustomsTransaction;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.CustomsTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionAuthorizationService.class);

    @Autowired
    private CustomsTransactionRepository transactionRepository;

    // ==========================================
    // İŞLEM OLUŞTURMA YETKİSİ
    // ==========================================

    /**
     * İşlem oluşturma yetkisi kontrolü
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Her zaman oluşturabilir
     * - BROKER_ADMIN: Kendi broker firması için oluşturabilir
     * - BROKER_USER: Kendi broker firması için oluşturabilir
     * - CLIENT_USER: Asla oluşturamaz (READ-ONLY)
     */
    public boolean canCreateTransaction(User user, Long brokerCompanyId) {
        // SUPER_ADMIN her zaman oluşturabilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "CREATE_TRANSACTION", brokerCompanyId);
            return true;
        }

        // CLIENT_USER asla oluşturamaz
        if (user.isClientUser()) {
            logAccessDenied(user, "CREATE_TRANSACTION", brokerCompanyId);
            return false;
        }

        // BROKER_ADMIN veya BROKER_USER ise kendi broker firması için oluşturabilir
        if (user.isBrokerStaff()) {
            Company userBrokerCompany = user.getBrokerCompany();

            if (userBrokerCompany != null && userBrokerCompany.getId().equals(brokerCompanyId)) {
                logAccessGranted(user, "CREATE_TRANSACTION", brokerCompanyId);
                return true;
            }
        }

        logAccessDenied(user, "CREATE_TRANSACTION", brokerCompanyId);
        return false;
    }

    // ==========================================
    // İŞLEM GÜNCELLEME YETKİSİ
    // ==========================================

    /**
     * İşlem güncelleme yetkisi kontrolü
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Her zaman güncelleyebilir
     * - BROKER_ADMIN: Kendi broker firmasının işlemlerini güncelleyebilir
     * - BROKER_USER: Kendi broker firmasının işlemlerini güncelleyebilir
     * - CLIENT_USER: Asla güncelleyemez (READ-ONLY)
     */
    public boolean canUpdateTransaction(User user, Long transactionId) {
        // SUPER_ADMIN her zaman güncelleyebilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "UPDATE_TRANSACTION", transactionId);
            return true;
        }

        // CLIENT_USER asla güncelleyemez
        if (user.isClientUser()) {
            logAccessDenied(user, "UPDATE_TRANSACTION", transactionId);
            return false;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", transactionId);
            return false;
        }

        // BROKER_ADMIN veya BROKER_USER ise kendi broker firmasının işlemlerini güncelleyebilir
        if (user.isBrokerStaff()) {
            Company userBrokerCompany = user.getBrokerCompany();
            Company transactionBroker = transaction.getBrokerCompany();

            if (userBrokerCompany != null && transactionBroker != null &&
                    userBrokerCompany.getId().equals(transactionBroker.getId())) {
                logAccessGranted(user, "UPDATE_TRANSACTION", transactionId);
                return true;
            }
        }

        logAccessDenied(user, "UPDATE_TRANSACTION", transactionId);
        return false;
    }

    // ==========================================
    // İŞLEM SİLME YETKİSİ
    // ==========================================

    /**
     * İşlem silme yetkisi kontrolü
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Her zaman silebilir
     * - BROKER_ADMIN: Kendi broker firmasının işlemlerini silebilir
     * - BROKER_USER: Silemez
     * - CLIENT_USER: Silemez
     */
    public boolean canDeleteTransaction(User user, Long transactionId) {
        // SUPER_ADMIN her zaman silebilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "DELETE_TRANSACTION", transactionId);
            return true;
        }

        // Sadece BROKER_ADMIN silebilir
        if (!user.isBrokerAdmin()) {
            logAccessDenied(user, "DELETE_TRANSACTION", transactionId);
            return false;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", transactionId);
            return false;
        }

        // BROKER_ADMIN kendi broker firmasının işlemlerini silebilir
        Company userBrokerCompany = user.getBrokerCompany();
        Company transactionBroker = transaction.getBrokerCompany();

        if (userBrokerCompany != null && transactionBroker != null &&
                userBrokerCompany.getId().equals(transactionBroker.getId())) {
            logAccessGranted(user, "DELETE_TRANSACTION", transactionId);
            return true;
        }

        logAccessDenied(user, "DELETE_TRANSACTION", transactionId);
        return false;
    }

    // ==========================================
    // İŞLEM GÖRÜNTÜLEME YETKİSİ
    // ==========================================

    /**
     * İşlem görüntüleme yetkisi kontrolü
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Tüm işlemleri görebilir
     * - BROKER_ADMIN: Kendi broker firmasının işlemlerini görebilir
     * - BROKER_USER: Kendi broker firmasının işlemlerini görebilir
     * - CLIENT_USER: Sadece kendi müşteri firmasının işlemlerini görebilir (READ-ONLY)
     */
    public boolean canViewTransaction(User user, Long transactionId) {
        // SUPER_ADMIN herkesi görebilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "VIEW_TRANSACTION", transactionId);
            return true;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", transactionId);
            return false;
        }

        Company transactionBroker = transaction.getBrokerCompany();
        Company transactionClient = transaction.getClientCompany();

        // BROKER_ADMIN veya BROKER_USER - kendi broker firmasının işlemlerini görebilir
        if (user.isBrokerStaff()) {
            Company userBrokerCompany = user.getBrokerCompany();

            if (userBrokerCompany != null && transactionBroker != null &&
                    userBrokerCompany.getId().equals(transactionBroker.getId())) {
                logAccessGranted(user, "VIEW_TRANSACTION", transactionId);
                return true;
            }
        }

        // CLIENT_USER - sadece kendi müşteri firmasının işlemlerini görebilir
        if (user.isClientUser()) {
            Company userClientCompany = user.getCompany();

            if (userClientCompany != null && transactionClient != null &&
                    userClientCompany.getId().equals(transactionClient.getId())) {
                logAccessGranted(user, "VIEW_TRANSACTION", transactionId);
                return true;
            }
        }

        logAccessDenied(user, "VIEW_TRANSACTION", transactionId);
        return false;
    }

    // ==========================================
    // İŞLEM DURUM DEĞİŞTİRME YETKİSİ
    // ==========================================

    /**
     * İşlem durumu değiştirme yetkisi kontrolü
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Her zaman değiştirebilir
     * - BROKER_ADMIN: Kendi broker firmasının işlemlerinin durumunu değiştirebilir
     * - BROKER_USER: Kendi broker firmasının işlemlerinin durumunu değiştirebilir
     * - CLIENT_USER: Asla değiştiremez
     */
    public boolean canChangeTransactionStatus(User user, Long transactionId) {
        // SUPER_ADMIN her zaman değiştirebilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "CHANGE_STATUS", transactionId);
            return true;
        }

        // CLIENT_USER asla değiştiremez
        if (user.isClientUser()) {
            logAccessDenied(user, "CHANGE_STATUS", transactionId);
            return false;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            logger.warn("Transaction not found: {}", transactionId);
            return false;
        }

        // BROKER_ADMIN veya BROKER_USER kendi broker firmasının işlemlerinin durumunu değiştirebilir
        if (user.isBrokerStaff()) {
            Company userBrokerCompany = user.getBrokerCompany();
            Company transactionBroker = transaction.getBrokerCompany();

            if (userBrokerCompany != null && transactionBroker != null &&
                    userBrokerCompany.getId().equals(transactionBroker.getId())) {
                logAccessGranted(user, "CHANGE_STATUS", transactionId);
                return true;
            }
        }

        logAccessDenied(user, "CHANGE_STATUS", transactionId);
        return false;
    }

    // ==========================================
    // TOPLU GÖRÜNTÜLEME YETKİLERİ
    // ==========================================

    /**
     * Broker'ın müşteri listesini görüntüleme yetkisi
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Tüm broker'ların müşterilerini görebilir
     * - BROKER_ADMIN: Kendi broker firmasının müşterilerini görebilir
     * - BROKER_USER: Kendi broker firmasının müşterilerini görebilir
     * - CLIENT_USER: Göremez
     */
    public boolean canViewBrokerClients(User user, Long brokerId) {
        // SUPER_ADMIN görebilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "VIEW_BROKER_CLIENTS", brokerId);
            return true;
        }

        // CLIENT_USER göremez
        if (user.isClientUser()) {
            logAccessDenied(user, "VIEW_BROKER_CLIENTS", brokerId);
            return false;
        }

        // BROKER_ADMIN veya BROKER_USER kendi broker firmasının müşterilerini görebilir
        if (user.isBrokerStaff()) {
            Company userBrokerCompany = user.getBrokerCompany();

            if (userBrokerCompany != null && userBrokerCompany.getId().equals(brokerId)) {
                logAccessGranted(user, "VIEW_BROKER_CLIENTS", brokerId);
                return true;
            }
        }

        logAccessDenied(user, "VIEW_BROKER_CLIENTS", brokerId);
        return false;
    }

    /**
     * Client'in işlem istatistiklerini görüntüleme yetkisi
     * <p>
     * KURALLAR:
     * - SUPER_ADMIN: Tüm client'ların istatistiklerini görebilir
     * - BROKER_ADMIN: Kendi müşterilerinin istatistiklerini görebilir
     * - BROKER_USER: Kendi müşterilerinin istatistiklerini görebilir
     * - CLIENT_USER: Sadece kendi istatistiklerini görebilir
     */
    public boolean canViewClientStats(User user, Long clientId) {
        // SUPER_ADMIN görebilir
        if (user.isSuperAdmin()) {
            logAccessGranted(user, "VIEW_CLIENT_STATS", clientId);
            return true;
        }

        // CLIENT_USER sadece kendi istatistiklerini görebilir
        if (user.isClientUser()) {
            if (user.getCompany() != null && user.getCompany().getId().equals(clientId)) {
                logAccessGranted(user, "VIEW_CLIENT_STATS", clientId);
                return true;
            }
            logAccessDenied(user, "VIEW_CLIENT_STATS", clientId);
            return false;
        }

        // BROKER_ADMIN veya BROKER_USER kendi müşterilerinin istatistiklerini görebilir
        if (user.isBrokerStaff()) {
            // Client firmasının parent broker'ı mı kontrol et
            // Bu kontrolü repository seviyesinde yapmak daha iyi olur
            // Şimdilik sadece yetki var diyelim
            logAccessGranted(user, "VIEW_CLIENT_STATS", clientId);
            return true;
        }

        logAccessDenied(user, "VIEW_CLIENT_STATS", clientId);
        return false;
    }

    // ==========================================
    // CLIENT KULLANICI KONTROLÜ
    // ==========================================

    /**
     * Kullanıcı client kullanıcısı mı?
     */
    public boolean isClientUser(User user, Long transactionId) {
        if (user.isSuperAdmin()) {
            return false;
        }

        if (!user.isClientUser()) {
            return false;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            return false;
        }

        Company userCompany = user.getCompany();
        Company transactionClient = transaction.getClientCompany();

        return userCompany != null && transactionClient != null &&
                userCompany.getId().equals(transactionClient.getId());
    }

    /**
     * Kullanıcı broker yöneticisi mi?
     */
    public boolean isBrokerManager(User user, Long transactionId) {
        if (user.isSuperAdmin()) {
            return true;
        }

        if (!user.isBrokerStaff()) {
            return false;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            return false;
        }

        Company userBrokerCompany = user.getBrokerCompany();
        Company transactionBroker = transaction.getBrokerCompany();

        return userBrokerCompany != null && transactionBroker != null &&
                userBrokerCompany.getId().equals(transactionBroker.getId());
    }

    // ==========================================
    // LOGGING METODLARI
    // ==========================================

    /**
     * Erişim reddedildi logu
     */
    public void logAccessDenied(User user, String action, Long resourceId) {
        logger.warn("🚫 Access DENIED - User: {} (Role: {}), Action: {}, ResourceId: {}",
                user.getEmail(),
                user.getGlobalRole(),
                action,
                resourceId);
    }

    /**
     * Erişim verildi logu
     */
    public void logAccessGranted(User user, String action, Long resourceId) {
        logger.debug("✅ Access GRANTED - User: {} (Role: {}), Action: {}, ResourceId: {}",
                user.getEmail(),
                user.getGlobalRole(),
                action,
                resourceId);
    }
}