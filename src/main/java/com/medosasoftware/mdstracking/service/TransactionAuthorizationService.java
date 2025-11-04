package com.medosasoftware.mdstracking.service;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.model.CompanyRole;
import com.medosasoftware.mdstracking.model.CustomsTransaction;
import com.medosasoftware.mdstracking.model.User;
import com.medosasoftware.mdstracking.repository.CustomsTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionAuthorizationService.class);

    @Autowired
    private CustomsTransactionRepository transactionRepository;

    // ✅ İşlem oluşturma yetkisi
    public boolean canCreateTransaction(User user, Long brokerId) {
        // SUPER_ADMIN her zaman oluşturabilir
        if (user.isSuperAdmin()) {
            return true;
        }

        // Broker'ın ADMIN veya MANAGER'ı
        CompanyRole userRole = user.getRoleInCompany(
                new Company() {{
                    setId(brokerId);
                }}
        );

        return userRole == CompanyRole.COMPANY_ADMIN ||
                userRole == CompanyRole.COMPANY_MANAGER;
    }

    // ✅ İşlem güncelleme yetkisi
    public boolean canUpdateTransaction(User user, Long transactionId) {
        // SUPER_ADMIN her zaman güncelleyebilir
        if (user.isSuperAdmin()) {
            return true;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);

        if (transaction == null) {
            return false;
        }

        // İşlemi giren broker'ın ADMIN veya MANAGER'ı güncelleyebilir
        Company broker = transaction.getBrokerCompany();
        CompanyRole userRole = user.getRoleInCompany(broker);

        // ADMIN veya MANAGER yetkisine sahip olmalı
        return userRole == CompanyRole.COMPANY_ADMIN ||
                userRole == CompanyRole.COMPANY_MANAGER;
    }

    // ✅ İşlem silme yetkisi
    public boolean canDeleteTransaction(User user, Long transactionId) {
        // Sadece SUPER_ADMIN silebilir
        if (user.isSuperAdmin()) {
            return true;
        }

        // Normal kullanıcılar silemez
        return false;
    }

    // ✅ İşlem görüntüleme yetkisi
    public boolean canViewTransaction(User user, Long transactionId) {
        // SUPER_ADMIN herkesi görebilir
        if (user.isSuperAdmin()) {
            return true;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);

        if (transaction == null) {
            return false;
        }

        Company brokerCompany = transaction.getBrokerCompany();
        Company clientCompany = transaction.getClientCompany();

        // ===== BROKER PERSONELI =====
        // Kendi brokerindeki işlemleri görebilir
        if (user.getRoleInCompany(brokerCompany) != null) {
            return true;
        }

        // ===== CLIENT PERSONELI =====
        // Kendi client'teki işlemleri görebilir (READ ONLY)
        if (user.getRoleInCompany(clientCompany) != null) {
            return true;
        }

        return false;
    }

    // ✅ İşlem durumunu değiştirme yetkisi
    public boolean canChangeTransactionStatus(User user, Long transactionId) {
        // SUPER_ADMIN her zaman değiştirebilir
        if (user.isSuperAdmin()) {
            return true;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);

        if (transaction == null) {
            return false;
        }

        // Broker'ın ADMIN veya MANAGER'ı durumu değiştirebilir
        Company broker = transaction.getBrokerCompany();
        CompanyRole userRole = user.getRoleInCompany(broker);

        return userRole == CompanyRole.COMPANY_ADMIN ||
                userRole == CompanyRole.COMPANY_MANAGER;
    }

    // ✅ Client'in işlemleri görüntülemek için yetkisi
    public boolean isClientUser(User user, Long transactionId) {
        // Client kullanıcısı mı?
        if (user.isSuperAdmin()) {
            return false;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);

        if (transaction == null) {
            return false;
        }

        Company clientCompany = transaction.getClientCompany();
        CompanyRole role = user.getRoleInCompany(clientCompany);

        // CLIENT_ADMIN veya CLIENT_USER rolüne sahipse
        return role == CompanyRole.COMPANY_ADMIN ||
                role == CompanyRole.COMPANY_USER;
    }

    // ✅ Broker'ın işlemleri yönetmek için yetkisi
    public boolean isBrokerManager(User user, Long transactionId) {
        // Broker yöneticisi mi?
        if (user.isSuperAdmin()) {
            return true;
        }

        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);

        if (transaction == null) {
            return false;
        }

        Company brokerCompany = transaction.getBrokerCompany();
        CompanyRole role = user.getRoleInCompany(brokerCompany);

        // ADMIN veya MANAGER rolüne sahipse
        return role == CompanyRole.COMPANY_ADMIN ||
                role == CompanyRole.COMPANY_MANAGER;
    }

    // ✅ Broker'ın müşteri listesini görmek için yetkisi
    public boolean canViewBrokerClients(User user, Long brokerId) {
        // SUPER_ADMIN görebilir
        if (user.isSuperAdmin()) {
            return true;
        }

        // Broker'ın ADMIN'i görebilir
        Company broker = new Company();
        broker.setId(brokerId);
        CompanyRole role = user.getRoleInCompany(broker);

        return role == CompanyRole.COMPANY_ADMIN ||
                role == CompanyRole.COMPANY_MANAGER;
    }

    // ✅ Client'in işlem istatistiklerini görmek için yetkisi
    public boolean canViewClientStats(User user, Long clientId) {
        // SUPER_ADMIN görebilir
        if (user.isSuperAdmin()) {
            return true;
        }

        // Client'in ADMIN'i veya kullanıcısı görebilir
        Company client = new Company();
        client.setId(clientId);
        CompanyRole role = user.getRoleInCompany(client);

        return role != null;
    }

    // ✅ Logging helper
    public void logAccessDenied(User user, String action, Long resourceId) {
        logger.warn("Access denied - User: {}, Action: {}, ResourceId: {}",
                user.getEmail(), action, resourceId);
    }

    public void logAccessGranted(User user, String action, Long resourceId) {
        logger.debug("Access granted - User: {}, Action: {}, ResourceId: {}",
                user.getEmail(), action, resourceId);
    }
}