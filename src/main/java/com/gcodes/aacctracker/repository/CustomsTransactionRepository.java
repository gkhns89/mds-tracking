package com.gcodes.aacctracker.repository;

import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.CustomsTransaction;
import com.gcodes.aacctracker.model.TransactionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomsTransactionRepository extends JpaRepository<CustomsTransaction, Long> {

    // ✅ Dosya numarasına göre işlem bul
    Optional<CustomsTransaction> findByFileNo(String fileNo);

    // ✅ YENİ: İlişkili entity'lerle birlikte getir
    @Query("SELECT t FROM CustomsTransaction t " +
            "LEFT JOIN FETCH t.brokerCompany " +
            "LEFT JOIN FETCH t.clientCompany " +
            "LEFT JOIN FETCH t.createdByUser " +
            "WHERE t.id = :id")
    Optional<CustomsTransaction> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM CustomsTransaction t " +
            "LEFT JOIN FETCH t.brokerCompany " +
            "LEFT JOIN FETCH t.clientCompany " +
            "WHERE t.fileNo = :fileNo")
    Optional<CustomsTransaction> findByFileNoWithDetails(@Param("fileNo") String fileNo);

    // ✅ YENİ: Broker işlemlerini optimize et
    @Query("SELECT DISTINCT t FROM CustomsTransaction t " +
            "LEFT JOIN FETCH t.brokerCompany " +
            "LEFT JOIN FETCH t.clientCompany " +
            "WHERE t.brokerCompany.id = :brokerId " +
            "ORDER BY t.createdAt DESC")
    List<CustomsTransaction> findByBrokerIdWithDetails(@Param("brokerId") Long brokerId);

    @Query("SELECT DISTINCT t FROM CustomsTransaction t " +
            "LEFT JOIN FETCH t.brokerCompany " +
            "LEFT JOIN FETCH t.clientCompany " +
            "WHERE t.clientCompany.id = :clientId " +
            "ORDER BY t.createdAt DESC")
    List<CustomsTransaction> findByClientIdWithDetails(@Param("clientId") Long clientId);

    // ✅ YENİ: EntityGraph ile
    @EntityGraph(attributePaths = {"brokerCompany", "clientCompany", "createdByUser"})
    List<CustomsTransaction> findTop10ByOrderByCreatedAtDesc();

    // ✅ Belirli gümrük firmasının tüm işlemleri
    List<CustomsTransaction> findByBrokerCompany(Company brokerCompany);

    // ✅ Belirli müşteri firmasının tüm işlemleri
    List<CustomsTransaction> findByClientCompany(Company clientCompany);

    // ✅ Belirli gümrük firması ve müşteri firmasının işlemleri
    List<CustomsTransaction> findByBrokerCompanyAndClientCompany(Company brokerCompany, Company clientCompany);

    // ✅ Belirli durumda olan işlemler
    List<CustomsTransaction> findByStatus(TransactionStatus status);

    // ✅ Gümrük firmasının belirli durumda olan işlemleri
    List<CustomsTransaction> findByBrokerCompanyAndStatus(Company brokerCompany, TransactionStatus status);

    // ✅ Müşteri firmasının belirli durumda olan işlemleri
    List<CustomsTransaction> findByClientCompanyAndStatus(Company clientCompany, TransactionStatus status);

    // ✅ Tarih aralığında işlemler
    List<CustomsTransaction> findByRegistrationDateBetween(LocalDate startDate, LocalDate endDate);

    // ✅ Gecikme olan işlemler
    @Query("SELECT ct FROM CustomsTransaction ct WHERE ct.delayReason IS NOT NULL AND ct.delayReason != ''")
    List<CustomsTransaction> findTransactionsWithDelay();

    // ✅ Broker'a ait gecikme olan işlemler
    @Query("SELECT ct FROM CustomsTransaction ct WHERE ct.brokerCompany = :broker " +
            "AND ct.delayReason IS NOT NULL AND ct.delayReason != ''")
    List<CustomsTransaction> findDelayedTransactionsByBroker(@Param("broker") Company broker);

    // ✅ Client'in tüm işlemleri (READ ONLY olarak)
    List<CustomsTransaction> findByClientCompanyOrderByCreatedAtDesc(Company clientCompany);

    // ✅ UPDATED: Son 10 işlem (JOIN FETCH ile optimize edildi)
    @Query("SELECT DISTINCT ct FROM CustomsTransaction ct " +
            "LEFT JOIN FETCH ct.brokerCompany " +
            "LEFT JOIN FETCH ct.clientCompany " +
            "ORDER BY ct.createdAt DESC LIMIT 10")
    List<CustomsTransaction> findRecentTransactions();

    // ✅ Broker'ın son işlemleri
    @Query("SELECT ct FROM CustomsTransaction ct WHERE ct.brokerCompany = :broker " +
            "ORDER BY ct.createdAt DESC LIMIT 10")
    List<CustomsTransaction> findRecentTransactionsByBroker(@Param("broker") Company broker);

    // ✅ İstatistik: Broker'ın toplam işlem sayısı
    long countByBrokerCompany(Company brokerCompany);

    // ✅ İstatistik: Broker'ın tamamlanan işlem sayısı
    long countByBrokerCompanyAndStatus(Company brokerCompany, TransactionStatus status);

    // ✅ İstatistik: Client'in işlem sayısı
    long countByClientCompany(Company clientCompany);
}