package com.medosasoftware.mdstracking.repository;

import com.medosasoftware.mdstracking.model.AgencyAgreement;
import com.medosasoftware.mdstracking.model.AgreementStatus;
import com.medosasoftware.mdstracking.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgencyAgreementRepository extends JpaRepository<AgencyAgreement, Long> {

    // ✅ Broker ve Client arasında anlaşma var mı?
    Optional<AgencyAgreement> findByBrokerCompanyAndClientCompany(
            Company brokerCompany, Company clientCompany);

    // ✅ Belirli Broker'ın tüm anlaşmaları
    List<AgencyAgreement> findByBrokerCompany(Company brokerCompany);

    // ✅ Belirli Client'in tüm anlaşmaları
    List<AgencyAgreement> findByClientCompany(Company clientCompany);

    // ✅ Broker'ın aktif anlaşmaları
    List<AgencyAgreement> findByBrokerCompanyAndStatus(Company brokerCompany, AgreementStatus status);

    // ✅ Client'in aktif anlaşmaları
    List<AgencyAgreement> findByClientCompanyAndStatus(Company clientCompany, AgreementStatus status);

    // ✅ Belirli durumda olan tüm anlaşmalar
    List<AgencyAgreement> findByStatus(AgreementStatus status);

    // ✅ Anlaşma numarasına göre bul
    Optional<AgencyAgreement> findByAgreementNumber(String agreementNumber);

    // ✅ Broker'ın belirli bir Client ile aktif anlaşması var mı?
    boolean existsByBrokerCompanyAndClientCompanyAndStatus(
            Company brokerCompany, Company clientCompany, AgreementStatus status);

    // ✅ Broker'ın müşteri sayısı (aktif anlaşmalar)
    @Query("SELECT COUNT(DISTINCT aa.clientCompany) FROM AgencyAgreement aa " +
            "WHERE aa.brokerCompany = :broker AND aa.status = 'ACTIVE'")
    long countActiveClientsByBroker(@Param("broker") Company broker);

    // ✅ Client'in broker sayısı (aktif anlaşmalar)
    @Query("SELECT COUNT(DISTINCT aa.brokerCompany) FROM AgencyAgreement aa " +
            "WHERE aa.clientCompany = :client AND aa.status = 'ACTIVE'")
    long countActiveBrokersByClient(@Param("client") Company client);

    // ✅ Son 10 anlaşma
    @Query("SELECT aa FROM AgencyAgreement aa ORDER BY aa.createdAt DESC LIMIT 10")
    List<AgencyAgreement> findRecentAgreements();
}