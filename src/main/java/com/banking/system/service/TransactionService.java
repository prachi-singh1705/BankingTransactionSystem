
package com.banking.system.service;

import com.banking.system.model.Account;
import com.banking.system.model.BankTransaction;
import com.banking.system.repository.AccountRepository;
import com.banking.system.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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

    // Deposit money into an account
    @Transactional
    public BankTransaction deposit(
            Long accountId,
            BigDecimal amount,
            UUID transactionId) {

        validateTransaction(amount, transactionId);
        checkDuplicateTransaction(transactionId);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        checkDuplicateTransaction(transactionId);
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

    // Withdraw money from an account
    @Transactional
    public BankTransaction withdraw(
            Long accountId,
            BigDecimal amount,
            UUID transactionId) {

        validateTransaction(amount, transactionId);
        checkDuplicateTransaction(transactionId);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        checkDuplicateTransaction(transactionId);
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

    // Transfer money between two accounts
    @Transactional
    public BankTransaction transfer(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            UUID transactionId) {

        validateTransaction(amount, transactionId);
        checkDuplicateTransaction(transactionId);

        if (fromAccountId == null || toAccountId == null) {
            throw new IllegalArgumentException("Account IDs are required");
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "Cannot transfer money to the same account");
        }

        // Lock both accounts in ascending account ID order
        List<Account> accounts =
                accountRepository.findAccountsForTransfer(
                        fromAccountId, toAccountId);

        if (accounts.size() != 2) {
            throw new IllegalArgumentException("Account not found");
        }

        checkDuplicateTransaction(transactionId);

        Account sender = accounts.stream()
                .filter(a -> a.getAccountId().equals(fromAccountId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Sender account not found"));

        Account receiver = accounts.stream()
                .filter(a -> a.getAccountId().equals(toAccountId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Receiver account not found"));

        validateActiveAccount(sender);
        validateActiveAccount(receiver);

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Calculate updated balances
        BigDecimal senderBalance =
                sender.getBalance().subtract(amount);

        BigDecimal receiverBalance =
                receiver.getBalance().add(amount);

        // Update both accounts in the same database transaction
        accountRepository.updateBalance(fromAccountId, senderBalance);
        accountRepository.updateBalance(toAccountId, receiverBalance);

        // Save transfer history
        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setFromAccountId(fromAccountId);
        transaction.setToAccountId(toAccountId);
        transaction.setTransactionType("TRANSFER");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Money transferred successfully");

        return transactionRepository.save(transaction);
    }

    // Get complete transaction history for an account
    public List<BankTransaction> getAccountTransactionHistory(
            Long accountId) {

        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Invalid account ID");
        }

        // Check account existence without locking its row
        accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        // Fetch all transaction records for this account
        return transactionRepository.findByAccountId(accountId);
    }

    // Check duplicate transaction ID
    private void checkDuplicateTransaction(UUID transactionId) {

        if (transactionRepository.findByTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException("Duplicate transaction ID");
        }
    }

    // Validate transaction amount and ID
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

    // Validate account status
    private void validateActiveAccount(Account account) {

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }
    }
}
