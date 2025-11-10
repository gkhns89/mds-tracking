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
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
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
    // ✅ Write işlemi - Transactional
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public Company createBrokerCompany(Company company, BrokerSubscription subscription, User createdBy) {
        logger.info("Creating broker company: {} by {}", company.getName(), createdBy.getEmail());

        if (!createdBy.isSuperAdmin()) {
            throw new UnauthorizedException("Only SUPER_ADMIN can create broker companies");
        }

        if (!company.isBroker()) {
            throw new RuntimeException("Company type must be CUSTOMS_BROKER");
        }

        try {
            // Firma adı uniqueness kontrolü
            if (companyRepository.findByName(company.getName()).isPresent()) {
                throw new RuntimeException("Company name already exists: " + company.getName());
            }

            // 1. Firmayı kaydet
            Company savedCompany = companyRepository.save(company);

            // 2. Abonelik oluştur (aynı transaction içinde)
            subscription.setBrokerCompany(savedCompany);
            subscription.setCreatedByAdmin(createdBy);
            subscriptionRepository.save(subscription);

            // 3. UsageTracking oluştur (aynı transaction içinde)
            UsageTracking tracking = new UsageTracking();
            tracking.setBrokerCompany(savedCompany);
            usageTrackingRepository.save(tracking);

            // 4. Firma kodunu oluştur (aynı transaction içinde)
            generateCompanyCodeIfNeeded(savedCompany);

            logger.info("Broker company created successfully: {} (Code: {})",
                    savedCompany.getName(), savedCompany.getCompanyCode());

            return savedCompany;

        } catch (Exception e) {
            logger.error("Broker company creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create broker company: " + e.getMessage(), e);
        }
    }

    /**
     * Müşteri firma oluştur (BROKER_ADMIN)
     */
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public Company createClientCompany(Company clientCompany, Long brokerCompanyId, User createdBy) {
        logger.info("Creating client company: {} for broker: {} by {}",
                clientCompany.getName(), brokerCompanyId, createdBy.getEmail());

        Company brokerCompany = companyRepository.findById(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        try {
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

            // 1. Client'ı kaydet
            Company savedClient = companyRepository.save(clientCompany);

            // 2. UsageTracking güncelle (aynı transaction içinde)
            updateUsageTrackingAfterClientAdd(brokerCompany);

            logger.info("Client company created successfully: {} for broker: {}",
                    savedClient.getName(), brokerCompany.getName());

            return savedClient;

        } catch (LimitExceededException e) {
            logger.warn("Client company creation failed - Limit exceeded: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Client company creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create client company: " + e.getMessage(), e);
        }
    }

    /**
     * Firma güncelle
     */
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public Company updateCompany(Long companyId, String name, String description, User updatingUser) {
        logger.info("Updating company: {} by {}", companyId, updatingUser.getEmail());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        try {
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

            Company updated = companyRepository.save(company);
            logger.info("Company updated successfully: {}", updated.getName());

            return updated;

        } catch (Exception e) {
            logger.error("Company update failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update company: " + e.getMessage(), e);
        }
    }

    /**
     * Firma silme (soft delete)
     */
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public void deleteCompany(Long companyId, User deletingUser) {
        logger.info("Deleting company: {} by {}", companyId, deletingUser.getEmail());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        try {
            // Yetki kontrolü
            if (!canUserDeleteCompany(deletingUser, company)) {
                throw new UnauthorizedException("Insufficient permissions to delete this company");
            }

            // Soft delete
            company.setIsActive(false);
            companyRepository.save(company);

            // CLIENT firma ise UsageTracking güncelle (aynı transaction içinde)
            if (company.isClient() && company.getParentBroker() != null) {
                updateUsageTrackingAfterClientRemove(company.getParentBroker());
            }

            logger.info("Company soft deleted successfully: {}", company.getName());

        } catch (Exception e) {
            logger.error("Company deletion failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete company: " + e.getMessage(), e);
        }
    }

    /**
     * Gümrük firmasının müşteri firmalarını getir
     */
    public List<Company> getClientCompanies(Long brokerCompanyId, User requestingUser) {
        Company brokerCompany = companyRepository.findById(brokerCompanyId)
                .orElseThrow(() -> new RuntimeException("Broker company not found"));

        if (!canUserViewCompany(requestingUser, brokerCompany)) {
            throw new UnauthorizedException("Access denied");
        }

        return companyRepository.findClientsByBrokerId(brokerCompanyId); // ✅ Optimized
    }

    // ✅ Optimized - N+1 problemi çözüldü
    public List<Company> getAllCompanies(User requestingUser) {
        if (requestingUser.isSuperAdmin()) {
            return companyRepository.findAllActiveWithParentBroker(); // ✅ Optimized
        } else if (requestingUser.isBrokerStaff()) {
            Company brokerCompany = requestingUser.getBrokerCompany();
            List<Company> companies = new ArrayList<>();
            companies.add(brokerCompany);
            companies.addAll(companyRepository.findClientsByBrokerId(brokerCompany.getId())); // ✅ Optimized
            return companies;
        } else if (requestingUser.isClientUser()) {
            return List.of(requestingUser.getCompany());
        }
        return List.of();
    }

    // ✅ Tek firma getirme - optimized
    public Company getCompanyById(Long companyId) {
        return companyRepository.findByIdWithParentBroker(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    // ✅ Detaylı bilgi ile getirme
    public Company getCompanyWithDetails(Long companyId) {
        return companyRepository.findByIdWithDetails(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    // ===== HELPER METODLARI =====

    private boolean canUserManageCompany(User user, Company company) {
        if (user.isSuperAdmin()) return true;

        if (company.isBroker()) {
            return user.isBrokerAdmin() && user.getCompany().getId().equals(company.getId());
        } else {
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
                    logger.debug("Usage tracking updated - Clients incremented for company: {}",
                            brokerCompany.getId());
                });
    }

    private void updateUsageTrackingAfterClientRemove(Company brokerCompany) {
        usageTrackingRepository.findByBrokerCompanyId(brokerCompany.getId())
                .ifPresent(tracking -> {
                    tracking.decrementClientCompanies();
                    usageTrackingRepository.save(tracking);
                    logger.debug("Usage tracking updated - Clients decremented for company: {}",
                            brokerCompany.getId());
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