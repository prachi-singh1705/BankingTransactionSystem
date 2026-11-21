
package com.banking.system.controller;

import com.banking.system.model.BankTransaction;
import com.banking.system.service.TransactionService;

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

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<BankTransaction> deposit(
            @RequestBody Map<String, String> request) {

        Long accountId = Long.valueOf(request.get("accountId"));

        BigDecimal amount = new BigDecimal(request.get("amount"));

        UUID transactionId = UUID.fromString(
                request.get("transactionId"));

        BankTransaction transaction = transactionService.deposit(
                accountId,
                amount,
                transactionId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transaction);
    }
}
