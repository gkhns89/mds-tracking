package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CustomsTransaction;
import com.gcodes.aacctracker.model.TransactionStatus;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.CustomsTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomsTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(CustomsTransactionService.class);

    @Autowired
    private CustomsTransactionRepository transactionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AgencyAgreementService agencyAgreementService;

    // ✅ YENİ: İşlem oluşturma
    public CustomsTransaction createTransaction(CustomsTransaction transaction, User createdBy) {
        // ✅ Validasyon: fileNo benzersiz olmalı
        if (transactionRepository.findByFileNo(transaction.getFileNo()).isPresent()) {
            throw new RuntimeException("Transaction with this fileNo already exists: " + transaction.getFileNo());
        }

        // ✅ Validasyon: Broker ve Client firmalarını kontrol et
        Company broker = transaction.getBrokerCompany();
        Company client = transaction.getClientCompany();

        if (broker == null || client == null) {
            throw new RuntimeException("Broker and Client companies are required");
        }

        if (!broker.isBroker()) {
            throw new RuntimeException("Broker company must be of type CUSTOMS_BROKER");
        }

        if (!client.isClient()) {
            throw new RuntimeException("Client company must be of type CLIENT");
        }

        // ✅ Validasyon: Aktif anlaşma var mı?
        if (!agencyAgreementService.hasActiveAgreement(broker.getId(), client.getId())) {
            throw new RuntimeException("No active agreement exists between broker and client");
        }

        // ✅ Metadata ayarla
        transaction.setCreatedByUser(createdBy);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setLastModifiedBy(createdBy.getEmail());

        // ✅ İşlem süresini hesapla
        transaction.calculateProcessingTime();

        CustomsTransaction saved = transactionRepository.save(transaction);
        logger.info("Transaction created: {} - FileNo: {}, Broker: {}, Client: {}",
                saved.getId(), saved.getFileNo(), broker.getName(), client.getName());

        return saved;
    }

    // ✅ İşlem güncelleme
    public CustomsTransaction updateTransaction(Long transactionId, CustomsTransaction updatedData, User updatingUser) {
        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // ✅ Sadece PENDING durumunda güncelleme yapılabilir
        if (!TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new RuntimeException("Can only update transactions in PENDING status");
        }

        // ✅ FileNo değiştirilmeye çalışılıyorsa validasyon yap
        if (!transaction.getFileNo().equals(updatedData.getFileNo())) {
            if (transactionRepository.findByFileNo(updatedData.getFileNo()).isPresent()) {
                throw new RuntimeException("Another transaction with this fileNo already exists");
            }
            transaction.setFileNo(updatedData.getFileNo());
        }

        // ✅ Tüm alanları güncelle
        transaction.setRecipientName(updatedData.getRecipientName());
        transaction.setCustomsWarehouse(updatedData.getCustomsWarehouse());
        transaction.setGate(updatedData.getGate());
        transaction.setWeight(updatedData.getWeight());
        transaction.setTax(updatedData.getTax());
        transaction.setSenderName(updatedData.getSenderName());
        transaction.setWarehouseArrivalDate(updatedData.getWarehouseArrivalDate());
        transaction.setRegistrationDate(updatedData.getRegistrationDate());
        transaction.setDeclarationNumber(updatedData.getDeclarationNumber());
        transaction.setLineClosureDate(updatedData.getLineClosureDate());
        transaction.setImportProcessingTime(updatedData.getImportProcessingTime());
        transaction.setWithdrawalDate(updatedData.getWithdrawalDate());
        transaction.setDescription(updatedData.getDescription());
        transaction.setDelayReason(updatedData.getDelayReason());

        // ✅ İşlem süresini hesapla
        transaction.calculateProcessingTime();

        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setLastModifiedBy(updatingUser.getEmail());

        CustomsTransaction updated = transactionRepository.save(transaction);
        logger.info("Transaction updated: {} by {}", transactionId, updatingUser.getEmail());

        return updated;
    }

    // ✅ İşlem durumunu değiştir
    public CustomsTransaction updateTransactionStatus(Long transactionId, TransactionStatus newStatus, User updatingUser) {
        CustomsTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setLastModifiedBy(updatingUser.getEmail());

        CustomsTransaction updated = transactionRepository.save(transaction);
        logger.info("Transaction status updated: {} - New status: {} by {}",
                transactionId, newStatus, updatingUser.getEmail());

        return updated;
    }

    // ✅ İşlemi tamamla
    public CustomsTransaction completeTransaction(Long transactionId, User completedBy) {
        return updateTransactionStatus(transactionId, TransactionStatus.COMPLETED, completedBy);
    }

    // ✅ İşlemi iptal et
    public CustomsTransaction cancelTransaction(Long transactionId, String reason, User cancelledBy) {
        CustomsTransaction transaction = updateTransactionStatus(transactionId, TransactionStatus.CANCELLED, cancelledBy);
        transaction.setDelayReason(reason);
        return transactionRepository.save(transaction);
    }

    // ✅ Dosya numarasına göre işlem bul
    public Optional<CustomsTransaction> getTransactionByFileNo(String fileNo) {
        return transactionRepository.findByFileNo(fileNo);
    }

    // ✅ İşlem detaylarını getir
    public CustomsTransaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    // ✅ Broker'ın tüm işlemleri
    public List<CustomsTransaction> getBrokerTransactions(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return transactionRepository.findByBrokerCompany(broker);
    }

    // ✅ Client'in tüm işlemleri
    public List<CustomsTransaction> getClientTransactions(Long clientId) {
        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return transactionRepository.findByClientCompanyOrderByCreatedAtDesc(client);
    }

    // ✅ Broker ve Client arasındaki işlemler
    public List<CustomsTransaction> getTransactionsBetween(Long brokerId, Long clientId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return transactionRepository.findByBrokerCompanyAndClientCompany(broker, client);
    }

    // ✅ Belirli durumda olan işlemler
    public List<CustomsTransaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    // ✅ Broker'ın belirli durumda olan işlemleri
    public List<CustomsTransaction> getBrokerTransactionsByStatus(Long brokerId, TransactionStatus status) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return transactionRepository.findByBrokerCompanyAndStatus(broker, status);
    }

    // ✅ Gecikme olan işlemler
    public List<CustomsTransaction> getTransactionsWithDelay() {
        return transactionRepository.findTransactionsWithDelay();
    }

    // ✅ Broker'ın gecikme olan işlemleri
    public List<CustomsTransaction> getBrokerDelayedTransactions(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return transactionRepository.findDelayedTransactionsByBroker(broker);
    }

    // ✅ Tarih aralığında işlemler
    public List<CustomsTransaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByRegistrationDateBetween(startDate, endDate);
    }

    // ✅ Son 10 işlem
    public List<CustomsTransaction> getRecentTransactions() {
        return transactionRepository.findRecentTransactions();
    }

    // ✅ Broker'ın son işlemleri
    public List<CustomsTransaction> getBrokerRecentTransactions(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return transactionRepository.findRecentTransactionsByBroker(broker);
    }

    // ✅ İstatistik: Broker'ın toplam işlem sayısı
    public long getBrokerTransactionCount(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return transactionRepository.countByBrokerCompany(broker);
    }

    // ✅ İstatistik: Broker'ın tamamlanan işlem sayısı
    public long getBrokerCompletedTransactionCount(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return transactionRepository.countByBrokerCompanyAndStatus(broker, TransactionStatus.COMPLETED);
    }

    // ✅ İstatistik: Client'in işlem sayısı
    public long getClientTransactionCount(Long clientId) {
        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return transactionRepository.countByClientCompany(client);
    }
}