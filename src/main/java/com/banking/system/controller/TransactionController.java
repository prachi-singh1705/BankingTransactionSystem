
package com.banking.system.controller;

import java.util.concurrent.CompletableFuture;

import com.banking.system.model.BankTransaction;
import com.banking.system.model.BulkTransactionRequest;
import com.banking.system.model.MoneyTransactionRequest;
import com.banking.system.model.TransferRequest;

import com.banking.system.service.TransactionService;
import com.banking.system.service.FailedTransactionService;
import com.banking.system.service.ConcurrentTransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

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
            @Valid @RequestBody MoneyTransactionRequest request) {

        BankTransaction transaction = transactionService.deposit(
                request.getAccountId(),
                request.getAmount(),
                request.getTransactionId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transaction);
    }

    // Withdraw money from an account
    @PostMapping("/withdraw")
    public ResponseEntity<BankTransaction> withdraw(
            @Valid @RequestBody MoneyTransactionRequest request) {

        Long accountId = request.getAccountId();
        var amount = request.getAmount();
        var transactionId = request.getTransactionId();

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
            @Valid @RequestBody TransferRequest request) {

        Long fromAccountId = request.getFromAccountId();
        Long toAccountId = request.getToAccountId();
        var amount = request.getAmount();
        var transactionId = request.getTransactionId();

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
            @RequestBody List<@Valid BulkTransactionRequest> requests) {

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
