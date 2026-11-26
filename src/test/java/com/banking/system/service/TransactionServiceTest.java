
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
    @Test
    void transferShouldRejectSameAccount() {

        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("500.00");
        UUID transactionId = UUID.fromString(
                "99999999-9999-4999-8999-999999999999"
        );

        when(transactionRepository.findByTransactionId(transactionId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transfer(
                        accountId,
                        accountId,
                        amount,
                        transactionId
                )
        );

        assertEquals(
                "Cannot transfer money to the same account",
                exception.getMessage()
        );

        verifyNoInteractions(accountRepository);
        verify(transactionRepository).findByTransactionId(transactionId);
    }


@Test
void transferShouldUpdateBothBalancesAndSaveTransactionAndAudit() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    BigDecimal amount = new BigDecimal("300.00");

    UUID transactionId = UUID.randomUUID();

    Account sender = createActiveAccount(fromAccountId, "1000.00");
    Account receiver = createActiveAccount(toAccountId, "500.00");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of(sender, receiver));

    when(transactionRepository.save(any(BankTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    BankTransaction result = transactionService.transfer(
            fromAccountId,
            toAccountId,
            amount,
            transactionId
    );

    assertNotNull(result);
    assertEquals(transactionId, result.getTransactionId());
    assertEquals("TRANSFER", result.getTransactionType());
    assertEquals(amount, result.getAmount());
    assertEquals("SUCCESS", result.getStatus());
    assertEquals(fromAccountId, result.getFromAccountId());
    assertEquals(toAccountId, result.getToAccountId());

    verify(accountRepository).updateBalance(
            eq(fromAccountId),
            eq(new BigDecimal("700.00"))
    );

    verify(accountRepository).updateBalance(
            eq(toAccountId),
            eq(new BigDecimal("800.00"))
    );

    verify(transactionRepository).save(any(BankTransaction.class));

    verify(failedTransactionRepository).saveAudit(
            eq(transactionId),
            eq("TRANSFER_COMPLETED"),
            eq("SUCCESS"),
            isNull(),
            anyString()
    );
}
@Test
void transferShouldRejectInsufficientBalance() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    UUID transactionId = UUID.randomUUID();

    Account sender = createActiveAccount(fromAccountId, "100.00");
    Account receiver = createActiveAccount(toAccountId, "500.00");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of(sender, receiver));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Insufficient balance", exception.getMessage());

    verify(accountRepository, never())
            .updateBalance(anyLong(), any());

    verify(transactionRepository, never()).save(any());

    verify(failedTransactionRepository, never()).saveAudit(
            any(), anyString(), anyString(), any(), anyString()
    );
}

@Test
void transferShouldRejectInactiveSenderAccount() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    UUID transactionId = UUID.randomUUID();

    Account sender = createActiveAccount(fromAccountId, "1000.00");
    sender.setStatus("INACTIVE");

    Account receiver = createActiveAccount(toAccountId, "500.00");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of(sender, receiver));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account is not active", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}
@Test
void transferShouldRejectInactiveReceiverAccount() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    UUID transactionId = UUID.randomUUID();

    Account sender = createActiveAccount(fromAccountId, "1000.00");
    Account receiver = createActiveAccount(toAccountId, "500.00");
    receiver.setStatus("INACTIVE");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of(sender, receiver));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account is not active", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}

@Test
void transferShouldRejectWhenAccountsAreNotFound() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    UUID transactionId = UUID.randomUUID();

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of());

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account not found", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}
@Test
void transferShouldRejectWhenSenderAccountIsNotFound() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    UUID transactionId = UUID.randomUUID();

    Account receiver = createActiveAccount(toAccountId, "500.00");
    Account anotherAccount = createActiveAccount(3L, "700.00");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of(receiver, anotherAccount));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Sender account not found", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}
@Test
void transferShouldRejectWhenReceiverAccountIsNotFound() {

    Long fromAccountId = 1L;
    Long toAccountId = 2L;

    UUID transactionId = UUID.randomUUID();

    Account sender = createActiveAccount(fromAccountId, "1000.00");
    Account anotherAccount = createActiveAccount(3L, "700.00");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findAccountsForTransfer(
            fromAccountId, toAccountId))
            .thenReturn(java.util.List.of(sender, anotherAccount));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    fromAccountId,
                    toAccountId,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Receiver account not found", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}

@Test
void transferShouldRejectNullAccountIds() {

    UUID transactionId = UUID.randomUUID();

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    null,
                    2L,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account IDs are required", exception.getMessage());

    verifyNoInteractions(accountRepository);
    verify(transactionRepository, never()).save(any());
}

@Test
void transferShouldRejectNullDestinationAccountId() {

    UUID transactionId = UUID.randomUUID();

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    1L,
                    null,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account IDs are required", exception.getMessage());

    verifyNoInteractions(accountRepository);
    verify(transactionRepository, never()).save(any());
}

@Test
void transferShouldRejectDuplicateTransactionId() {

    UUID transactionId = UUID.randomUUID();

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.of(new BankTransaction()));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(
                    1L,
                    2L,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Duplicate transaction ID", exception.getMessage());

    verifyNoInteractions(accountRepository);
    verify(transactionRepository, never()).save(any());
}

@Test
void getAccountTransactionHistoryShouldMergeAndSortTransactions() {

    Account account = createActiveAccount(1L, "5000.00");

    when(accountRepository.findById(1L))
            .thenReturn(Optional.of(account));

    BankTransaction oldTransaction = new BankTransaction();
    oldTransaction.setId(1L);
    oldTransaction.setCreatedAt(
            java.time.OffsetDateTime.parse("2026-11-20T10:00:00+05:30")
    );

    BankTransaction newTransaction = new BankTransaction();
    newTransaction.setId(2L);
    newTransaction.setCreatedAt(
            java.time.OffsetDateTime.parse("2026-11-25T10:00:00+05:30")
    );

    BankTransaction failedTransaction = new BankTransaction();
    failedTransaction.setId(3L);
    failedTransaction.setCreatedAt(
            java.time.OffsetDateTime.parse("2026-11-23T10:00:00+05:30")
    );

    when(transactionRepository.findByAccountId(1L))
            .thenReturn(java.util.List.of(oldTransaction, newTransaction));

    when(failedTransactionRepository.findFailedByAccountId(1L))
            .thenReturn(java.util.List.of(failedTransaction));

    java.util.List<BankTransaction> result =
            transactionService.getAccountTransactionHistory(1L);

    assertEquals(3, result.size());

    assertEquals(2L, result.get(0).getId());
    assertEquals(3L, result.get(1).getId());
    assertEquals(1L, result.get(2).getId());
}

@Test
void getAccountTransactionHistoryShouldRejectInvalidAccountId() {

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getAccountTransactionHistory(0L)
    );

    assertEquals("Invalid account ID", exception.getMessage());

    verifyNoInteractions(accountRepository);
    verifyNoInteractions(transactionRepository);
    verifyNoInteractions(failedTransactionRepository);
}

@Test
void getAccountTransactionHistoryShouldRejectWhenAccountDoesNotExist() {

    when(accountRepository.findById(999L))
            .thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getAccountTransactionHistory(999L)
    );

    assertEquals("Account not found", exception.getMessage());

    verify(accountRepository).findById(999L);
    verifyNoInteractions(transactionRepository);
    verifyNoInteractions(failedTransactionRepository);
}

@Test
void withdrawalShouldUpdateBalanceAndSaveTransactionAndAudit() {

    UUID transactionId = UUID.randomUUID();
    Account account = createActiveAccount(1L, "1000.00");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(account));

    BankTransaction savedTransaction = new BankTransaction();

    when(transactionRepository.save(any(BankTransaction.class)))
            .thenReturn(savedTransaction);

    BankTransaction result = transactionService.withdraw(
            1L,
            new BigDecimal("300.00"),
            transactionId
    );

    assertNotNull(result);

    verify(accountRepository).updateBalance(
            1L,
            new BigDecimal("700.00")
    );

    verify(transactionRepository).save(any(BankTransaction.class));

    verify(failedTransactionRepository).saveAudit(
            eq(transactionId),
            eq("WITHDRAWAL_COMPLETED"),
            eq("SUCCESS"),
            isNull(),
            anyString()
    );
}

@Test
void withdrawalShouldRejectInactiveAccount() {

    UUID transactionId = UUID.randomUUID();
    Account account = createActiveAccount(1L, "1000.00");
    account.setStatus("INACTIVE");

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(account));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.withdraw(
                    1L,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account is not active", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}

@Test
void withdrawalShouldRejectWhenAccountDoesNotExist() {

    UUID transactionId = UUID.randomUUID();

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.empty());

    when(accountRepository.findByIdForUpdate(999L))
            .thenReturn(Optional.empty());

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.withdraw(
                    999L,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Account not found", exception.getMessage());

    verify(accountRepository, never()).updateBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
}

@Test
void withdrawalShouldRejectDuplicateTransactionId() {

    UUID transactionId = UUID.randomUUID();

    when(transactionRepository.findByTransactionId(transactionId))
            .thenReturn(Optional.of(new BankTransaction()));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.withdraw(
                    1L,
                    new BigDecimal("300.00"),
                    transactionId
            )
    );

    assertEquals("Duplicate transaction ID", exception.getMessage());

    verifyNoInteractions(accountRepository);
    verify(transactionRepository, never()).save(any());
}

@Test
void getAccountTransactionHistoryShouldRejectNullAccountId() {

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getAccountTransactionHistory(null)
    );

    assertEquals("Invalid account ID", exception.getMessage());

    verifyNoInteractions(accountRepository);
    verifyNoInteractions(transactionRepository);
    verifyNoInteractions(failedTransactionRepository);
}
}
