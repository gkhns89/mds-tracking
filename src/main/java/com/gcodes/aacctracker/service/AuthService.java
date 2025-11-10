package com.gcodes.aacctracker.service;

import com.gcodes.aacctracker.dto.*;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.repository.AgencyAgreementRepository;
import com.gcodes.aacctracker.repository.CompanyRepository;
import com.gcodes.aacctracker.repository.UserRepository;
import com.gcodes.aacctracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AgencyAgreementRepository agencyAgreementRepository;

    // ✅ Normal kullanıcı kaydı (CLIENT_USER olarak) - DEPRECATED
    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGlobalRole(GlobalRole.CLIENT_USER);

        userService.createUser(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    // ✅ Super Admin kullanıcı oluşturma metodu
    public AuthResponse registerSuperAdmin(RegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGlobalRole(GlobalRole.SUPER_ADMIN);

        userService.createUser(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    // ✅ YENİ: Client (müşteri) kaydı - Firma kodu ile
    @Transactional
    public ContextualAuthResponse registerClient(ClientRegistrationRequest request) {
        // Broker firmasını bul
        Company brokerCompany = companyRepository.findByCompanyCode(request.getBrokerCode())
                .orElseThrow(() -> new RuntimeException("Invalid broker code: " + request.getBrokerCode()));

        if (!brokerCompany.isBroker()) {
            throw new RuntimeException("Company code does not belong to a broker company");
        }

        if (!brokerCompany.getIsActive()) {
            throw new RuntimeException("Broker company is not active");
        }

        // Email uniqueness kontrolü
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Username uniqueness kontrolü
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Müşteri firmasını oluştur
        Company clientCompany = new Company();
        clientCompany.setName(request.getCompanyName());
        clientCompany.setDescription(request.getCompanyDescription());
        clientCompany.setCompanyType(CompanyType.CLIENT);
        clientCompany.setParentBroker(brokerCompany);
        clientCompany.setIsActive(true);
        Company savedClient = companyRepository.save(clientCompany);

        // Otomatik anlaşma oluştur
        AgencyAgreement agreement = new AgencyAgreement();
        agreement.setBrokerCompany(brokerCompany);
        agreement.setClientCompany(savedClient);
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setStartDate(LocalDateTime.now());
        agreement.setAgreementNumber(generateAgreementNumber());
        agencyAgreementRepository.save(agreement);

        // Client kullanıcısı oluştur
        User clientUser = new User();
        clientUser.setEmail(request.getEmail());
        clientUser.setUsername(request.getUsername());
        clientUser.setPassword(passwordEncoder.encode(request.getPassword()));
        clientUser.setGlobalRole(GlobalRole.CLIENT_USER);
        clientUser.setCompany(savedClient);
        clientUser.setIsActive(true);
        clientUser.setEmailVerified(false);
        User savedUser = userRepository.save(clientUser);

        // JWT token oluştur
        String token = jwtTokenProvider.generateToken(savedUser.getEmail());

        // Response hazırla
        Map<String, Object> userMap = Map.of(
                "id", savedUser.getId(),
                "email", savedUser.getEmail(),
                "username", savedUser.getUsername(),
                "globalRole", savedUser.getGlobalRole(),
                "company", Map.of(
                        "id", savedClient.getId(),
                        "name", savedClient.getName()
                )
        );

        Map<String, Object> brokerMap = Map.of(
                "id", brokerCompany.getId(),
                "name", brokerCompany.getName(),
                "code", brokerCompany.getCompanyCode()
        );

        logger.info("Client registered: {} for broker: {}", savedUser.getEmail(), brokerCompany.getName());

        return new ContextualAuthResponse(token, userMap, List.of(brokerMap), brokerMap);
    }

    // ✅ Giriş - Basit (eski)
    public AuthResponse authenticate(AuthRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    // ✅ YENİ: Context-aware login (broker seçimi ile)
    public ContextualAuthResponse authenticateWithContext(ContextualLoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        // Kullanıcının erişebildiği broker'ları bul
        List<Map<String, Object>> availableBrokers = getAvailableBrokersForUser(user);

        // Seçili broker'ı belirle
        Map<String, Object> selectedBroker = null;
        if (request.getBrokerCode() != null && !request.getBrokerCode().isEmpty()) {
            selectedBroker = availableBrokers.stream()
                    .filter(b -> request.getBrokerCode().equals(b.get("code")))
                    .findFirst()
                    .orElse(availableBrokers.isEmpty() ? null : availableBrokers.get(0));
        } else if (!availableBrokers.isEmpty()) {
            selectedBroker = availableBrokers.get(0); // İlk broker'ı default yap
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("username", user.getUsername());
        userMap.put("globalRole", user.getGlobalRole());

        if (user.getCompany() != null) {
            Map<String, Object> companyMap = new HashMap<>();
            companyMap.put("id", user.getCompany().getId());
            companyMap.put("name", user.getCompany().getName());
            companyMap.put("type", user.getCompany().getCompanyType());
            userMap.put("company", companyMap);
        } else {
            userMap.put("company", null);
        }

        logger.info("User logged in with context: {} - Selected broker: {}",
                user.getEmail(),
                selectedBroker != null ? selectedBroker.get("code") : "N/A");

        return new ContextualAuthResponse(token, userMap, availableBrokers, selectedBroker);
    }

    // ✅ Kullanıcının erişebildiği broker'ları getir
    private List<Map<String, Object>> getAvailableBrokersForUser(User user) {
        List<Map<String, Object>> brokers = new ArrayList<>();

        if (user.isSuperAdmin()) {
            // SUPER_ADMIN tüm broker'ları görebilir
            companyRepository.findAllActiveBrokersWithCodes().forEach(broker -> {
                Map<String, Object> brokerMap = new HashMap<>();
                brokerMap.put("id", broker.getId());
                brokerMap.put("name", broker.getName());
                brokerMap.put("code", broker.getCompanyCode());
                brokerMap.put("description", broker.getPublicDescription() != null ? broker.getPublicDescription() : "");
                brokers.add(brokerMap);
            });
        } else if (user.isBrokerStaff()) {
            // Broker staff kendi broker'ını görür
            Company broker = user.getBrokerCompany();
            if (broker != null && broker.getCompanyCode() != null) {
                Map<String, Object> brokerMap = new HashMap<>();
                brokerMap.put("id", broker.getId());
                brokerMap.put("name", broker.getName());
                brokerMap.put("code", broker.getCompanyCode());
                brokerMap.put("description", broker.getPublicDescription() != null ? broker.getPublicDescription() : "");
                brokers.add(brokerMap);
            }
        } else if (user.isClientUser()) {
            // Client kullanıcısı anlaşması olduğu tüm broker'ları görür
            Company clientCompany = user.getCompany();
            if (clientCompany != null) {
                List<AgencyAgreement> agreements = agencyAgreementRepository
                        .findByClientCompanyAndStatus(clientCompany, AgreementStatus.ACTIVE);

                agreements.forEach(agreement -> {
                    Company broker = agreement.getBrokerCompany();
                    if (broker.getCompanyCode() != null) {
                        Map<String, Object> brokerMap = new HashMap<>();
                        brokerMap.put("id", broker.getId());
                        brokerMap.put("name", broker.getName());
                        brokerMap.put("code", broker.getCompanyCode());
                        brokerMap.put("description", broker.getPublicDescription() != null ? broker.getPublicDescription() : "");
                        brokers.add(brokerMap);
                    }
                });
            }
        }

        return brokers;
    }

    // ✅ Helper: Anlaşma numarası oluştur
    private String generateAgreementNumber() {
        return "AGR-" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
        ) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}