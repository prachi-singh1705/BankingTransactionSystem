
package com.banking.system.service;

import com.banking.system.model.Account;
import com.banking.system.repository.AccountRepository;
import com.banking.system.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Account createAccount(Account account) {

        if (account.getCustomerId() == null ||
                account.getCustomerId() <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        if (customerRepository.findById(account.getCustomerId()).isEmpty()) {
            throw new IllegalArgumentException("Customer not found");
        }

        if (account.getAccountType() == null ||
                !(account.getAccountType().equalsIgnoreCase("SAVINGS") ||
                  account.getAccountType().equalsIgnoreCase("CURRENT"))) {
            throw new IllegalArgumentException("Invalid account type");
        }

        if (account.getAccountNumber() == null ||
                account.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        if (account.getBalance() == null ||
                account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid opening balance");
        }

        account.setAccountType(account.getAccountType().toUpperCase());
        account.setStatus("ACTIVE");

        return accountRepository.save(account);
    }

    public List<Account> getAccountsByCustomerId(Long customerId) {

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        return accountRepository.findByCustomerId(customerId);
    }
}
