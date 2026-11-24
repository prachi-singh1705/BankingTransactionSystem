
package com.banking.system.service;

import com.banking.system.model.Account;
import com.banking.system.model.BankTransaction;
import com.banking.system.repository.AccountRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.repository.FailedTransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FailedTransactionRepository failedTransactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account createActiveAccount(Long accountId, String balance) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setBalance(new BigDecimal(balance));
        account.setStatus("ACTIVE");
        return account;
    }

    @Test
    void depositShouldUpdateBalanceAndSaveTransactionAndAudit() {

        Long accountId = 1L;

        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal depositAmount = new BigDecimal("100.00");
        BigDecimal expectedBalance = new BigDecimal("1100.00");

        UUID transactionId = UUID.randomUUID();

        Account account = createActiveAccount(accountId, "1000.00");

        when(transactionRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.empty());

        when(accountRepository.findByIdForUpdate(accountId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result = transactionService.deposit(
                accountId, depositAmount, transactionId
        );

        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals("DEPOSIT", result.getTransactionType());
        assertEquals(depositAmount, result.getAmount());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(accountId, result.getToAccountId());

        verify(accountRepository).updateBalance(
                eq(accountId), eq(expectedBalance)
        );

        verify(transactionRepository).save(any(BankTransaction.class));

        verify(failedTransactionRepository).saveAudit(
                eq(transactionId),
                eq("DEPOSIT_COMPLETED"),
                eq("SUCCESS"),
                isNull(),
                anyString()
        );
    }

    @Test
    void depositShouldRejectZeroAmount() {

        UUID transactionId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.deposit(
                        1L, BigDecimal.ZERO, transactionId
                )
        );

        verifyNoInteractions(accountRepository);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositShouldRejectNegativeAmount() {

        UUID transactionId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.deposit(
                        1L, new BigDecimal("-100.00"), transactionId
                )
        );

        verifyNoInteractions(accountRepository);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawalShouldRejectInsufficientBalance() {

        Long accountId = 1L;
        UUID transactionId = UUID.randomUUID();

        Account account = createActiveAccount(accountId, "100.00");

        when(transactionRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.empty());

        when(accountRepository.findByIdForUpdate(accountId))
                .thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.withdraw(
                        accountId,
                        new BigDecimal("500.00"),
                        transactionId
                )
        );

        verify(accountRepository, never()).updateBalance(anyLong(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositShouldRejectWhenAccountDoesNotExist() {

        Long accountId = 99L;
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.empty());

        when(accountRepository.findByIdForUpdate(accountId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.deposit(
                        accountId,
                        new BigDecimal("100.00"),
                        transactionId
                )
        );

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositShouldRejectDuplicateTransactionId() {

        UUID transactionId = UUID.randomUUID();

        BankTransaction existingTransaction = new BankTransaction();
        existingTransaction.setTransactionId(transactionId);

        when(transactionRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.of(existingTransaction));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.deposit(
                        1L,
                        new BigDecimal("100.00"),
                        transactionId
                )
        );

        verifyNoInteractions(accountRepository);
        verify(transactionRepository, never()).save(any());
    }
}
