
package com.banking.system.controller;

import com.banking.system.model.Account;
import com.banking.system.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestBody Account account) {

        Account savedAccount = accountService.createAccount(account);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedAccount);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Account>> getCustomerAccounts(
            @PathVariable Long customerId) {

        List<Account> accounts =
                accountService.getAccountsByCustomerId(customerId);

        return ResponseEntity.ok(accounts);
    }
}
