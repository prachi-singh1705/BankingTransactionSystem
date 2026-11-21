
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

        validateTransaction(amount, transactionId);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        validateActiveAccount(account);

        BigDecimal newBalance = account.getBalance().add(amount);

        accountRepository.updateBalance(accountId, newBalance);

        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setToAccountId(accountId);
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Money deposited");

        return transactionRepository.save(transaction);
    }

    @Transactional
    public BankTransaction withdraw(
            Long accountId,
            BigDecimal amount,
            UUID transactionId) {

        validateTransaction(amount, transactionId);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        validateActiveAccount(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);

        accountRepository.updateBalance(accountId, newBalance);

        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setFromAccountId(accountId);
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Money withdrawn");

        return transactionRepository.save(transaction);
    }

    private void validateTransaction(
            BigDecimal amount,
            UUID transactionId) {

        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }
    }

    private void validateActiveAccount(Account account) {
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }
    }
}
