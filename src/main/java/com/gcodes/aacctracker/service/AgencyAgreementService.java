package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.model.AgencyAgreement;
import com.gcodes.aacctracker.model.AgreementStatus;
import com.gcodes.aacctracker.model.Company;
import com.gcodes.aacctracker.model.User;
import com.gcodes.aacctracker.repository.AgencyAgreementRepository;
import com.gcodes.aacctracker.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AgencyAgreementService {

    private static final Logger logger = LoggerFactory.getLogger(AgencyAgreementService.class);

    @Autowired
    private AgencyAgreementRepository agencyAgreementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    // ✅ YENİ: Anlaşma oluşturma
    public AgencyAgreement createAgreement(Long brokerId, Long clientId, User createdBy) {
        // ✅ Broker ve Client firmalarını getir
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        // ✅ Validasyon: Broker gerçekten CUSTOMS_BROKER mı?
        if (!broker.isBroker()) {
            throw new RuntimeException("Company must be a CUSTOMS_BROKER to create agreements");
        }

        // ✅ Validasyon: Client gerçekten CLIENT mı?
        if (!client.isClient()) {
            throw new RuntimeException("Company must be a CLIENT to have agreements");
        }

        // ✅ Zaten bu broker-client arasında aktif anlaşma var mı?
        if (agencyAgreementRepository.existsByBrokerCompanyAndClientCompanyAndStatus(
                broker, client, AgreementStatus.ACTIVE)) {
            throw new RuntimeException("Active agreement already exists between this broker and client");
        }

        // ✅ Yeni anlaşma oluştur
        AgencyAgreement agreement = new AgencyAgreement();
        agreement.setBrokerCompany(broker);
        agreement.setClientCompany(client);
        agreement.setCreatedBy(createdBy);
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setStartDate(LocalDateTime.now());
        agreement.setAgreementNumber(generateAgreementNumber());

        AgencyAgreement saved = agencyAgreementRepository.save(agreement);
        logger.info("Agreement created: {} - Broker: {}, Client: {}",
                saved.getAgreementNumber(), broker.getName(), client.getName());

        return saved;
    }

    // ✅ Anlaşmayı duraklatma
    public AgencyAgreement suspendAgreement(Long agreementId, String reason) {
        AgencyAgreement agreement = agencyAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));

        if (!agreement.isActive()) {
            throw new RuntimeException("Can only suspend active agreements");
        }

        agreement.setStatus(AgreementStatus.SUSPENDED);
        agreement.setNotes(reason);

        AgencyAgreement updated = agencyAgreementRepository.save(agreement);
        logger.info("Agreement suspended: {} - Reason: {}", agreement.getAgreementNumber(), reason);

        return updated;
    }

    // ✅ Anlaşmayı sonlandırma
    public AgencyAgreement terminateAgreement(Long agreementId, String reason) {
        AgencyAgreement agreement = agencyAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));

        agreement.setStatus(AgreementStatus.TERMINATED);
        agreement.setEndDate(LocalDateTime.now());
        agreement.setNotes(reason);

        AgencyAgreement updated = agencyAgreementRepository.save(agreement);
        logger.info("Agreement terminated: {} - Reason: {}", agreement.getAgreementNumber(), reason);

        return updated;
    }

    // ✅ Anlaşmayı reaktivleştirme
    public AgencyAgreement reactivateAgreement(Long agreementId) {
        AgencyAgreement agreement = agencyAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));

        if (agreement.isExpired()) {
            throw new RuntimeException("Cannot reactivate expired agreements");
        }

        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setEndDate(null);

        AgencyAgreement updated = agencyAgreementRepository.save(agreement);
        logger.info("Agreement reactivated: {}", agreement.getAgreementNumber());

        return updated;
    }

    // ✅ Broker'ın tüm anlaşmalarını getir
    public List<AgencyAgreement> getBrokerAgreements(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return agencyAgreementRepository.findByBrokerCompany(broker);
    }

    // ✅ Broker'ın aktif anlaşmalarını getir
    public List<AgencyAgreement> getBrokerActiveAgreements(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return agencyAgreementRepository.findByBrokerCompanyAndStatus(broker, AgreementStatus.ACTIVE);
    }

    // ✅ Client'in tüm anlaşmalarını getir
    public List<AgencyAgreement> getClientAgreements(Long clientId) {
        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return agencyAgreementRepository.findByClientCompany(client);
    }

    // ✅ Client'in aktif anlaşmalarını getir
    public List<AgencyAgreement> getClientActiveAgreements(Long clientId) {
        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return agencyAgreementRepository.findByClientCompanyAndStatus(client, AgreementStatus.ACTIVE);
    }

    // ✅ Broker ve Client arasında aktif anlaşma var mı?
    public boolean hasActiveAgreement(Long brokerId, Long clientId) {
        Company broker = companyRepository.findById(brokerId).orElse(null);
        Company client = companyRepository.findById(clientId).orElse(null);

        if (broker == null || client == null) {
            return false;
        }

        return agencyAgreementRepository.existsByBrokerCompanyAndClientCompanyAndStatus(
                broker, client, AgreementStatus.ACTIVE);
    }

    // ✅ Broker'ın aktif müşteri sayısı
    public long getBrokerActiveClientCount(Long brokerId) {
        Company broker = companyRepository.findById(brokerId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        return agencyAgreementRepository.countActiveClientsByBroker(broker);
    }

    // ✅ Client'in aktif broker sayısı
    public long getClientActiveBrokerCount(Long clientId) {
        Company client = companyRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client company not found"));

        return agencyAgreementRepository.countActiveBrokersByClient(client);
    }

    // ✅ Anlaşma detaylarını getir
    public AgencyAgreement getAgreementById(Long agreementId) {
        return agencyAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));
    }

    // ✅ Anlaşma numarasına göre getir
    public Optional<AgencyAgreement> getAgreementByNumber(String agreementNumber) {
        return agencyAgreementRepository.findByAgreementNumber(agreementNumber);
    }

    // ✅ Tüm anlaşmaları listele
    public List<AgencyAgreement> getAllAgreements() {
        return agencyAgreementRepository.findAll();
    }

    // ✅ Son 10 anlaşmayı getir
    public List<AgencyAgreement> getRecentAgreements() {
        return agencyAgreementRepository.findRecentAgreements();
    }

    // ✅ Helper: Anlaşma numarası üret
    private String generateAgreementNumber() {
        return "AGR-" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        ) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}