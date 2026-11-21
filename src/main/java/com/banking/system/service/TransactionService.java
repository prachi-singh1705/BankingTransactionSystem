
package com.banking.system.service;

import com.banking.system.model.Account;
import com.banking.system.model.BankTransaction;
import com.banking.system.repository.AccountRepository;
import com.banking.system.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BankTransaction deposit(
            Long accountId,
            BigDecimal amount,
            UUID transactionId) {

        // Validate transaction ID
        if (transactionId == null) {
            throw new IllegalArgumentException(
                    "Transaction ID is required");
        }

        // Validate amount
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be greater than zero");
        }

        // Lock account row
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        // Check account status
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Calculate new balance
        BigDecimal newBalance = account.getBalance().add(amount);

        // Update balance
        accountRepository.updateBalance(accountId, newBalance);

        // Create transaction record
        BankTransaction transaction = new BankTransaction();

        transaction.setTransactionId(transactionId);
        transaction.setFromAccountId(null);
        transaction.setToAccountId(accountId);
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Money deposited");

        return transactionRepository.save(transaction);
    }
}
