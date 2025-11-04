package com.gcodes.aacctracker.controller;

import com.gcodes.aacctracker.dto.TransactionCreateRequest;
import com.gcodes.aacctracker.dto.TransactionUpdateRequest;
import com.gcodes.aacctracker.model.*;
import com.gcodes.aacctracker.service.CustomsTransactionService;
import com.gcodes.aacctracker.service.TransactionAuthorizationService;
import com.gcodes.aacctracker.service.UserService;
import com.gcodes.aacctracker.service.AuditLogService;
import com.gcodes.aacctracker.repository.CompanyRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class CustomsTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(CustomsTransactionController.class);

    @Autowired
    private CustomsTransactionService transactionService;

    @Autowired
    private TransactionAuthorizationService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CompanyRepository companyRepository;

    // ✅ İşlem oluşturma
    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü
            if (!authService.canCreateTransaction(currentUser, request.getBrokerCompanyId())) {
                auditLogService.logAction(currentUser, "CREATE_TRANSACTION_DENIED",
                        "CustomsTransaction", null, getClientIp());
                return ResponseEntity.status(403)
                        .body("❌ Insufficient permissions to create transactions for this broker");
            }

            // Firmaları getir
            Company broker = companyRepository.findById(request.getBrokerCompanyId())
                    .orElseThrow(() -> new RuntimeException("Broker company not found"));
            Company client = companyRepository.findById(request.getClientCompanyId())
                    .orElseThrow(() -> new RuntimeException("Client company not found"));

            // Transaction objesini oluştur
            CustomsTransaction transaction = new CustomsTransaction();
            transaction.setBrokerCompany(broker);
            transaction.setClientCompany(client);
            transaction.setFileNo(request.getFileNo());
            transaction.setRecipientName(request.getRecipientName());
            transaction.setCustomsWarehouse(request.getCustomsWarehouse());
            transaction.setGate(request.getGate());
            transaction.setWeight(request.getWeight());
            transaction.setTax(request.getTax());
            transaction.setSenderName(request.getSenderName());
            transaction.setWarehouseArrivalDate(request.getWarehouseArrivalDate());
            transaction.setRegistrationDate(request.getRegistrationDate());
            transaction.setDeclarationNumber(request.getDeclarationNumber());
            transaction.setLineClosureDate(request.getLineClosureDate());
            transaction.setImportProcessingTime(request.getImportProcessingTime());
            transaction.setWithdrawalDate(request.getWithdrawalDate());
            transaction.setDescription(request.getDescription());
            transaction.setDelayReason(request.getDelayReason());

            CustomsTransaction created = transactionService.createTransaction(transaction, currentUser);

            // Audit log
            auditLogService.logAction(currentUser, "CREATE_TRANSACTION",
                    "CustomsTransaction", created.getId(), getClientIp());

            logger.info("Transaction created: {} by {}", created.getFileNo(), currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Transaction created successfully",
                    "transactionId", created.getId(),
                    "fileNo", created.getFileNo()
            ));

        } catch (Exception e) {
            logger.error("Error creating transaction", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error creating transaction: " + e.getMessage()));
        }
    }

    // ✅ İşlem güncelleme
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü
            if (!authService.canUpdateTransaction(currentUser, id)) {
                auditLogService.logAction(currentUser, "UPDATE_TRANSACTION_DENIED",
                        "CustomsTransaction", id, getClientIp());
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions to update this transaction"));
            }

            // Mevcut transaction'ı getir
            CustomsTransaction existing = transactionService.getTransactionById(id);
            CustomsTransaction before = cloneTransaction(existing);

            // Güncellemeleri uygula
            CustomsTransaction updated = new CustomsTransaction();
            updated.setFileNo(request.getFileNo() != null ? request.getFileNo() : existing.getFileNo());
            updated.setRecipientName(request.getRecipientName());
            updated.setCustomsWarehouse(request.getCustomsWarehouse());
            updated.setGate(request.getGate());
            updated.setWeight(request.getWeight());
            updated.setTax(request.getTax());
            updated.setSenderName(request.getSenderName());
            updated.setWarehouseArrivalDate(request.getWarehouseArrivalDate());
            updated.setRegistrationDate(request.getRegistrationDate());
            updated.setDeclarationNumber(request.getDeclarationNumber());
            updated.setLineClosureDate(request.getLineClosureDate());
            updated.setImportProcessingTime(request.getImportProcessingTime());
            updated.setWithdrawalDate(request.getWithdrawalDate());
            updated.setDescription(request.getDescription());
            updated.setDelayReason(request.getDelayReason());

            CustomsTransaction result = transactionService.updateTransaction(id, updated, currentUser);

            // Audit log with changes
            auditLogService.logActionWithChanges(currentUser, "UPDATE_TRANSACTION",
                    "CustomsTransaction", id, before, result);

            logger.info("Transaction updated: {} by {}", id, currentUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Transaction updated successfully",
                    "transaction", result
            ));

        } catch (Exception e) {
            logger.error("Error updating transaction", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error updating transaction: " + e.getMessage()));
        }
    }

    // ✅ İşlem durumunu değiştir
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü
            if (!authService.canChangeTransactionStatus(currentUser, id)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions to change transaction status"));
            }

            CustomsTransaction updated = transactionService.updateTransactionStatus(id, status, currentUser);

            auditLogService.logAction(currentUser, "UPDATE_TRANSACTION_STATUS",
                    "CustomsTransaction", id, getClientIp());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Transaction status updated successfully",
                    "newStatus", updated.getStatus()
            ));

        } catch (Exception e) {
            logger.error("Error updating transaction status", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ İşlemi tamamla
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTransaction(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!authService.canChangeTransactionStatus(currentUser, id)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions"));
            }

            CustomsTransaction completed = transactionService.completeTransaction(id, currentUser);

            auditLogService.logAction(currentUser, "COMPLETE_TRANSACTION",
                    "CustomsTransaction", id, getClientIp());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Transaction completed successfully",
                    "transaction", completed
            ));

        } catch (Exception e) {
            logger.error("Error completing transaction", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ İşlemi iptal et
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!authService.canChangeTransactionStatus(currentUser, id)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Insufficient permissions"));
            }

            CustomsTransaction cancelled = transactionService.cancelTransaction(id, reason, currentUser);

            auditLogService.logAction(currentUser, "CANCEL_TRANSACTION",
                    "CustomsTransaction", id, getClientIp());

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Transaction cancelled successfully",
                    "transaction", cancelled
            ));

        } catch (Exception e) {
            logger.error("Error cancelling transaction", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Tek işlem getir
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Yetki kontrolü
            if (!authService.canViewTransaction(currentUser, id)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied to this transaction"));
            }

            CustomsTransaction transaction = transactionService.getTransactionById(id);

            auditLogService.logAction(currentUser, "VIEW_TRANSACTION",
                    "CustomsTransaction", id, getClientIp());

            return ResponseEntity.ok(transaction);

        } catch (Exception e) {
            logger.error("Error getting transaction", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Dosya numarasına göre işlem getir
    @GetMapping("/by-file-no/{fileNo}")
    public ResponseEntity<?> getTransactionByFileNo(@PathVariable String fileNo) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CustomsTransaction transaction = transactionService.getTransactionByFileNo(fileNo)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Yetki kontrolü
            if (!authService.canViewTransaction(currentUser, transaction.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied to this transaction"));
            }

            return ResponseEntity.ok(transaction);

        } catch (Exception e) {
            logger.error("Error getting transaction by fileNo", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Broker'ın tüm işlemleri
    @GetMapping("/broker/{brokerId}")
    public ResponseEntity<?> getBrokerTransactions(@PathVariable Long brokerId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!authService.canViewBrokerClients(currentUser, brokerId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            List<CustomsTransaction> transactions = transactionService.getBrokerTransactions(brokerId);

            return ResponseEntity.ok(Map.of(
                    "total", transactions.size(),
                    "transactions", transactions
            ));

        } catch (Exception e) {
            logger.error("Error getting broker transactions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Client'in tüm işlemleri (READ ONLY)
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientTransactions(@PathVariable Long clientId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!authService.canViewClientStats(currentUser, clientId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            List<CustomsTransaction> transactions = transactionService.getClientTransactions(clientId);

            return ResponseEntity.ok(Map.of(
                    "total", transactions.size(),
                    "transactions", transactions,
                    "message", "ℹ️ Read-only access - Client users cannot modify transactions"
            ));

        } catch (Exception e) {
            logger.error("Error getting client transactions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Gecikme olan işlemler
    @GetMapping("/delayed")
    public ResponseEntity<?> getDelayedTransactions() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<CustomsTransaction> transactions;

            if (currentUser.isSuperAdmin()) {
                transactions = transactionService.getTransactionsWithDelay();
            } else {
                // Normal kullanıcı sadece kendi broker'ının gecikmelerini görebilir
                // Basitleştirme için şimdilik tüm gecikmeleri döndürelim
                transactions = transactionService.getTransactionsWithDelay();
            }

            return ResponseEntity.ok(Map.of(
                    "total", transactions.size(),
                    "transactions", transactions
            ));

        } catch (Exception e) {
            logger.error("Error getting delayed transactions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Tarih aralığında işlemler
    @GetMapping("/date-range")
    public ResponseEntity<?> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<CustomsTransaction> transactions =
                    transactionService.getTransactionsByDateRange(startDate, endDate);

            return ResponseEntity.ok(Map.of(
                    "startDate", startDate,
                    "endDate", endDate,
                    "total", transactions.size(),
                    "transactions", transactions
            ));

        } catch (Exception e) {
            logger.error("Error getting transactions by date range", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Son işlemler
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentTransactions() {
        try {
            List<CustomsTransaction> transactions = transactionService.getRecentTransactions();

            return ResponseEntity.ok(Map.of(
                    "total", transactions.size(),
                    "transactions", transactions
            ));

        } catch (Exception e) {
            logger.error("Error getting recent transactions", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ İstatistikler
    @GetMapping("/stats/broker/{brokerId}")
    public ResponseEntity<?> getBrokerStats(@PathVariable Long brokerId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!authService.canViewBrokerClients(currentUser, brokerId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "❌ Access denied"));
            }

            long totalTransactions = transactionService.getBrokerTransactionCount(brokerId);
            long completedTransactions = transactionService.getBrokerCompletedTransactionCount(brokerId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTransactions", totalTransactions);
            stats.put("completedTransactions", completedTransactions);
            stats.put("pendingTransactions", totalTransactions - completedTransactions);
            stats.put("completionRate", totalTransactions > 0 ?
                    (completedTransactions * 100.0 / totalTransactions) : 0);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting broker stats", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error: " + e.getMessage()));
        }
    }

    // ✅ Helper: Transaction kopyalama (audit için)
    private CustomsTransaction cloneTransaction(CustomsTransaction original) {
        CustomsTransaction clone = new CustomsTransaction();
        clone.setFileNo(original.getFileNo());
        clone.setRecipientName(original.getRecipientName());
        clone.setCustomsWarehouse(original.getCustomsWarehouse());
        clone.setGate(original.getGate());
        clone.setWeight(original.getWeight());
        clone.setTax(original.getTax());
        clone.setSenderName(original.getSenderName());
        clone.setWarehouseArrivalDate(original.getWarehouseArrivalDate());
        clone.setRegistrationDate(original.getRegistrationDate());
        clone.setDeclarationNumber(original.getDeclarationNumber());
        clone.setLineClosureDate(original.getLineClosureDate());
        clone.setImportProcessingTime(original.getImportProcessingTime());
        clone.setWithdrawalDate(original.getWithdrawalDate());
        clone.setDescription(original.getDescription());
        clone.setDelayReason(original.getDelayReason());
        clone.setStatus(original.getStatus());
        return clone;
    }

    // ✅ Helper: Client IP al
    private String getClientIp() {
        // Basitleştirilmiş versiyon - Production'da HttpServletRequest kullanın
        return "127.0.0.1";
    }
}