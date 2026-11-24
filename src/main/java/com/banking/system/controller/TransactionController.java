
package com.banking.system.controller;

import com.banking.system.model.BankTransaction;
import com.banking.system.model.BulkTransactionRequest;

import com.banking.system.service.TransactionService;
import com.banking.system.service.FailedTransactionService;
import com.banking.system.service.ConcurrentTransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final FailedTransactionService failedTransactionService;
    private final ConcurrentTransactionService concurrentTransactionService;

    public TransactionController(
            TransactionService transactionService,
            FailedTransactionService failedTransactionService,
            ConcurrentTransactionService concurrentTransactionService) {

        this.transactionService = transactionService;
        this.failedTransactionService = failedTransactionService;
        this.concurrentTransactionService = concurrentTransactionService;
    }

    // Deposit money into an account
    @PostMapping("/deposit")
    public ResponseEntity<BankTransaction> deposit(
            @RequestBody Map<String, String> request) {

        BankTransaction transaction = transactionService.deposit(
                Long.valueOf(request.get("accountId")),
                new BigDecimal(request.get("amount")),
                UUID.fromString(request.get("transactionId"))
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transaction);
    }

    // Withdraw money from an account
    @PostMapping("/withdraw")
    public ResponseEntity<BankTransaction> withdraw(
            @RequestBody Map<String, String> request) {

        Long accountId = Long.valueOf(request.get("accountId"));
        BigDecimal amount = new BigDecimal(request.get("amount"));
        UUID transactionId = UUID.fromString(request.get("transactionId"));

        try {
            BankTransaction transaction = transactionService.withdraw(
                    accountId,
                    amount,
                    transactionId
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(transaction);

        } catch (IllegalArgumentException exception) {

            if ("Insufficient balance".equals(exception.getMessage())) {

                failedTransactionService.recordFailedWithdrawal(
                        transactionId,
                        accountId,
                        amount,
                        exception.getMessage()
                );
            }

            throw exception;
        }
    }

    // Transfer money between two accounts
    @PostMapping("/transfer")
    public ResponseEntity<BankTransaction> transfer(
            @RequestBody Map<String, String> request) {

        Long fromAccountId = Long.valueOf(request.get("fromAccountId"));
        Long toAccountId = Long.valueOf(request.get("toAccountId"));
        BigDecimal amount = new BigDecimal(request.get("amount"));
        UUID transactionId = UUID.fromString(request.get("transactionId"));

        try {
            BankTransaction transaction = transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    amount,
                    transactionId
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(transaction);

        } catch (IllegalArgumentException exception) {

            if ("Insufficient balance".equals(exception.getMessage())) {

                failedTransactionService.recordFailedTransfer(
                        transactionId,
                        fromAccountId,
                        toAccountId,
                        amount,
                        exception.getMessage()
                );
            }

            throw exception;
        }
    }

    // Get transaction history for an account
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<BankTransaction>> getAccountTransactionHistory(
            @PathVariable Long accountId) {

        List<BankTransaction> transactions =
                transactionService.getAccountTransactionHistory(accountId);

        return ResponseEntity.ok(transactions);
    }

    // Process multiple transactions concurrently
    @PostMapping("/bulk")
    public CompletableFuture<ResponseEntity<List<Map<String, Object>>>> processBulkTransactions(
            @RequestBody List<BulkTransactionRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one transaction is required");
        }

        if (requests.size() > 1000) {
            throw new IllegalArgumentException(
                    "Maximum 1000 transactions allowed per request");
        }

        return concurrentTransactionService
                .processBulkTransactions(requests)
                .thenApply(ResponseEntity::ok);
    }
}
