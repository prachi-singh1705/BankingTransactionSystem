
package com.banking.system.controller;

import com.banking.system.model.BankTransaction;
import com.banking.system.service.TransactionService;
import com.banking.system.service.FailedTransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final FailedTransactionService failedTransactionService;

    public TransactionController(
            TransactionService transactionService,
            FailedTransactionService failedTransactionService) {

        this.transactionService = transactionService;
        this.failedTransactionService = failedTransactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<BankTransaction> deposit(
            @RequestBody Map<String, String> request) {

        BankTransaction transaction = transactionService.deposit(
                Long.valueOf(request.get("accountId")),
                new BigDecimal(request.get("amount")),
                UUID.fromString(request.get("transactionId"))
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

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

            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);

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
}
