package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.exception.LimitExceededException;
import com.gcodes.aacctracker.exception.UnauthorizedException;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LimitCheckService limitCheckService;

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    @Autowired
    private BrokerSubscriptionRepository subscriptionRepository;

    /**
     * Gümrük firması oluştur (SUPER_ADMIN)
     */
    public Company createBrokerCompany(Company company, BrokerSubscription subscription, User createdBy) {
        if (!createdBy.isSuperAdmin()) {
            throw new UnauthorizedException("Only SUPER_ADMIN can create broker companies");
        }

        if (!company.isBroker()) {
            throw new RuntimeException("Company type must be CUSTOMS_BROKER");
        }

        // Firma adı uniqueness kontrolü
        if (companyRepository.findByName(company.getName()).isPresent()) {
            throw new RuntimeException("Company name already exists: " + company.getName());
        }

        Company savedCompany = companyRepository.save(company);

        // Abonelik oluştur
        subscription.setBrokerCompany(savedCompany);
        subscription.setCreatedByAdmin(createdBy);
        subscriptionRepository.save(subscription);

        // UsageTracking oluştur
        UsageTracking tracking = new UsageTracking();
        tracking.setBrokerCompany(savedCompany);
        usageTrackingRepository.save(tracking);

        // ✅ YENİ: Firma kodunu oluştur
        generateCompanyCodeIfNeeded(savedCompany);

        logger.info("Broker company created: Company ID: {} | {} by {}", savedCompany.getCompanyCode(), savedCompany.getName(), createdBy.getEmail());

        return savedCompany;
    }

    /**
     * Müşteri firma oluştur (BROKER_ADMIN)
     */
    public Company createClientCompany(Company clientCompany, Long brokerCompanyId, User createdBy) {
        Company brokerCompany = companyRepository.findById(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        // Yetki kontrolü
        if (!createdBy.isSuperAdmin() && !createdBy.isBrokerAdmin()) {
            throw new UnauthorizedException("Only SUPER_ADMIN or BROKER_ADMIN can create client companies");
        }

        if (createdBy.isBrokerAdmin() && !createdBy.getCompany().getId().equals(brokerCompanyId)) {
            throw new UnauthorizedException("You can only create clients for your own broker company");
        }

        // Limit kontrolü
        if (!limitCheckService.canAddClientCompany(brokerCompanyId)) {
            int remaining = limitCheckService.getRemainingClientQuota(brokerCompanyId);
            throw new LimitExceededException(
                    "Client company limit exceeded. Remaining quota: " + remaining
            );
        }

        // Firma adı uniqueness kontrolü
        if (companyRepository.findByName(clientCompany.getName()).isPresent()) {
            throw new RuntimeException("Company name already exists: " + clientCompany.getName());
        }

        clientCompany.setCompanyType(CompanyType.CLIENT);
        clientCompany.setParentBroker(brokerCompany);

        Company savedClient = companyRepository.save(clientCompany);

        // UsageTracking güncelle
        updateUsageTrackingAfterClientAdd(brokerCompany);

        logger.info("Client company created: {} for broker: {} by {}",
                savedClient.getName(), brokerCompany.getName(), createdBy.getEmail());

        return savedClient;
    }

    /**
     * Firma güncelle
     */
    public Company updateCompany(Long companyId, String name, String description, User updatingUser) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Yetki kontrolü
        if (!canUserManageCompany(updatingUser, company)) {
            throw new UnauthorizedException("Insufficient permissions to update this company");
        }

        // Firma adı güncellenecekse uniqueness kontrol et
        if (name != null && !company.getName().equals(name)) {
            if (companyRepository.findByName(name).isPresent()) {
                throw new RuntimeException("Company name already exists: " + name);
            }
            company.setName(name);
        }

        if (description != null) {
            company.setDescription(description);
        }

        return companyRepository.save(company);
    }

    /**
     * Firma silme (soft delete)
     */
    public void deleteCompany(Long companyId, User deletingUser) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Yetki kontrolü
        if (!canUserDeleteCompany(deletingUser, company)) {
            throw new UnauthorizedException("Insufficient permissions to delete this company");
        }

        // Soft delete
        company.setIsActive(false);
        companyRepository.save(company);

        // CLIENT firma ise UsageTracking güncelle
        if (company.isClient() && company.getParentBroker() != null) {
            updateUsageTrackingAfterClientRemove(company.getParentBroker());
        }

        logger.info("Company soft deleted: {} by {}", company.getName(), deletingUser.getEmail());
    }

    /**
     * Gümrük firmasının müşteri firmalarını getir
     */
    public List<Company> getClientCompanies(Long brokerCompanyId, User requestingUser) {
        Company brokerCompany = companyRepository.findById(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        // Yetki kontrolü
        if (!canUserViewCompany(requestingUser, brokerCompany)) {
            throw new UnauthorizedException("Access denied");
        }

        return companyRepository.findByParentBrokerAndIsActiveTrue(brokerCompany);
    }

    /**
     * Tüm firmaları getir (yetki bazlı)
     */
    public List<Company> getAllCompanies(User requestingUser) {
        if (requestingUser.isSuperAdmin()) {
            return companyRepository.findAll();
        } else if (requestingUser.isBrokerStaff()) {
            // Broker personeli kendi firmasını ve müşteri firmalarını görebilir
            Company brokerCompany = requestingUser.getBrokerCompany();
            List<Company> companies = List.of(brokerCompany);
            companies.addAll(companyRepository.findByParentBrokerAndIsActiveTrue(brokerCompany));
            return companies;
        } else if (requestingUser.isClientUser()) {
            // Client kullanıcısı sadece kendi firmasını görebilir
            return List.of(requestingUser.getCompany());
        }

        return List.of();
    }

    // ===== HELPER METODLARI =====

    private boolean canUserManageCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;

        if (company.isBroker()) {
            return user.isBrokerAdmin() && user.getCompany().getId().equals(company.getId());
        } else {
            // Client firma için, parent broker'ın admin'i yönetebilir
            return user.isBrokerAdmin() &&
                    company.getParentBroker() != null &&
                    user.getCompany().getId().equals(company.getParentBroker().getId());
        }
    }

    private boolean canUserDeleteCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;

        // BROKER_ADMIN sadece kendi müşteri firmalarını silebilir
        if (company.isClient() && user.isBrokerAdmin()) {
            return company.getParentBroker() != null &&
                    user.getCompany().getId().equals(company.getParentBroker().getId());
        }

        return false;
    }

    private boolean canUserViewCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;

        if (user.isBrokerStaff()) {
            Company userBroker = user.getBrokerCompany();
            Company targetBroker = company.getBrokerCompany();
            return userBroker != null && targetBroker != null &&
                    userBroker.getId().equals(targetBroker.getId());
        }

        if (user.isClientUser()) {
            return user.getCompany().getId().equals(company.getId());
        }

        return false;
    }

    private void updateUsageTrackingAfterClientAdd(Company brokerCompany) {
        usageTrackingRepository.findByBrokerCompanyId(brokerCompany.getId())
                .ifPresent(tracking -> {
                    tracking.incrementClientCompanies();
                    usageTrackingRepository.save(tracking);
                });
    }

    private void updateUsageTrackingAfterClientRemove(Company brokerCompany) {
        usageTrackingRepository.findByBrokerCompanyId(brokerCompany.getId())
                .ifPresent(tracking -> {
                    tracking.decrementClientCompanies();
                    usageTrackingRepository.save(tracking);
                });
    }

    /**
     * Broker firması oluşturulduktan sonra otomatik kod oluştur
     */
    private void generateCompanyCodeIfNeeded(Company company) {
        if (company.isBroker() && company.getCompanyCode() == null) {
            company.generateCompanyCode();
            companyRepository.save(company);
            logger.info("Generated company code for broker: {} - Code: {}",
                    company.getName(), company.getCompanyCode());
        }
    }
}