package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.Company;
import com.medosasoftware.mdstracking.model.CustomsTransaction;
import com.medosasoftware.mdstracking.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomsTransactionRepository extends JpaRepository<CustomsTransaction, Long> {

    // ✅ Dosya numarasına göre işlem bul
    Optional<CustomsTransaction> findByFileNo(String fileNo);

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

    // ✅ Son 10 işlem
    @Query("SELECT ct FROM CustomsTransaction ct ORDER BY ct.createdAt DESC LIMIT 10")
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