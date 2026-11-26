
package com.banking.system.repository;

import com.banking.system.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private AccountRepository accountRepository;

    @Test
    void findByIdShouldReturnAccountWhenAccountExists() throws Exception {

        OffsetDateTime createdAt =
                OffsetDateTime.parse("2026-11-20T10:00:00+05:30");

        when(resultSet.getLong("account_id")).thenReturn(1L);
        when(resultSet.getLong("customer_id")).thenReturn(101L);
        when(resultSet.getString("account_number")).thenReturn("ACC1001");
        when(resultSet.getString("account_type")).thenReturn("SAVINGS");
        when(resultSet.getBigDecimal("balance"))
                .thenReturn(new BigDecimal("5000.00"));
        when(resultSet.getString("status")).thenReturn("ACTIVE");
        when(resultSet.getObject("created_at", OffsetDateTime.class))
                .thenReturn(createdAt);

        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                eq(1L)
        )).thenAnswer(invocation -> {

            RowMapper<Account> mapper = invocation.getArgument(1);

            return List.of(mapper.mapRow(resultSet, 0));
        });

        Optional<Account> result = accountRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getAccountId());
        assertEquals(101L, result.get().getCustomerId());
        assertEquals("ACC1001", result.get().getAccountNumber());
        assertEquals("SAVINGS", result.get().getAccountType());
        assertEquals(new BigDecimal("5000.00"), result.get().getBalance());
        assertEquals("ACTIVE", result.get().getStatus());
        assertEquals(createdAt, result.get().getCreatedAt());
    }

@Test
void findByIdShouldReturnEmptyWhenAccountDoesNotExist() {

    when(jdbcTemplate.query(
            anyString(),
            any(RowMapper.class),
            eq(999L)
    )).thenReturn(List.of());

    Optional<Account> result = accountRepository.findById(999L);

    assertTrue(result.isEmpty());

    verify(jdbcTemplate).query(
            anyString(),
            any(RowMapper.class),
            eq(999L)
    );
}

@Test
void findByIdForUpdateShouldReturnAccountWhenAccountExists() throws Exception {

    OffsetDateTime createdAt =
            OffsetDateTime.parse("2026-11-20T10:00:00+05:30");

    when(resultSet.getLong("account_id")).thenReturn(1L);
    when(resultSet.getLong("customer_id")).thenReturn(101L);
    when(resultSet.getString("account_number")).thenReturn("ACC1001");
    when(resultSet.getString("account_type")).thenReturn("SAVINGS");
    when(resultSet.getBigDecimal("balance"))
            .thenReturn(new BigDecimal("5000.00"));
    when(resultSet.getString("status")).thenReturn("ACTIVE");
    when(resultSet.getObject("created_at", OffsetDateTime.class))
            .thenReturn(createdAt);

    when(jdbcTemplate.query(
            contains("FOR UPDATE"),
            any(RowMapper.class),
            eq(1L)
    )).thenAnswer(invocation -> {

        RowMapper<Account> mapper = invocation.getArgument(1);

        return List.of(mapper.mapRow(resultSet, 0));
    });

    Optional<Account> result = accountRepository.findByIdForUpdate(1L);

    assertTrue(result.isPresent());
    assertEquals(1L, result.get().getAccountId());
    assertEquals("ACTIVE", result.get().getStatus());
}
@Test
void findByCustomerIdShouldReturnAccounts() throws Exception {

    OffsetDateTime createdAt =
            OffsetDateTime.parse("2026-11-20T10:00:00+05:30");

    when(resultSet.getLong("account_id")).thenReturn(1L);
    when(resultSet.getLong("customer_id")).thenReturn(101L);
    when(resultSet.getString("account_number")).thenReturn("ACC1001");
    when(resultSet.getString("account_type")).thenReturn("SAVINGS");
    when(resultSet.getBigDecimal("balance"))
            .thenReturn(new BigDecimal("5000.00"));
    when(resultSet.getString("status")).thenReturn("ACTIVE");
    when(resultSet.getObject("created_at", OffsetDateTime.class))
            .thenReturn(createdAt);

    when(jdbcTemplate.query(
            anyString(),
            any(RowMapper.class),
            eq(101L)
    )).thenAnswer(invocation -> {

        RowMapper<Account> mapper = invocation.getArgument(1);

        return List.of(mapper.mapRow(resultSet, 0));
    });

    List<Account> result = accountRepository.findByCustomerId(101L);

    assertEquals(1, result.size());
    assertEquals(101L, result.get(0).getCustomerId());
    assertEquals("ACC1001", result.get(0).getAccountNumber());
}

@Test
void updateBalanceShouldReturnNumberOfUpdatedRows() {

    BigDecimal newBalance = new BigDecimal("7500.00");

    when(jdbcTemplate.update(
            anyString(),
            eq(newBalance),
            eq(1L)
    )).thenReturn(1);

    int result = accountRepository.updateBalance(1L, newBalance);

    assertEquals(1, result);

    verify(jdbcTemplate).update(
            anyString(),
            eq(newBalance),
            eq(1L)
    );
}

@Test
void findAccountsForTransferShouldReturnLockedAccounts() throws Exception {

    OffsetDateTime createdAt =
            OffsetDateTime.parse("2026-11-20T10:00:00+05:30");

    when(resultSet.getLong("account_id")).thenReturn(1L, 2L);
    when(resultSet.getLong("customer_id")).thenReturn(101L);
    when(resultSet.getString("account_number")).thenReturn("ACC1001");
    when(resultSet.getString("account_type")).thenReturn("SAVINGS");
    when(resultSet.getBigDecimal("balance"))
            .thenReturn(new BigDecimal("5000.00"));
    when(resultSet.getString("status")).thenReturn("ACTIVE");
    when(resultSet.getObject("created_at", OffsetDateTime.class))
            .thenReturn(createdAt);

    when(jdbcTemplate.query(
            contains("FOR UPDATE"),
            any(RowMapper.class),
            eq(1L),
            eq(2L)
    )).thenAnswer(invocation -> {

        RowMapper<Account> mapper = invocation.getArgument(1);

        return List.of(
                mapper.mapRow(resultSet, 0),
                mapper.mapRow(resultSet, 1)
        );
    });

    List<Account> result =
            accountRepository.findAccountsForTransfer(1L, 2L);

    assertEquals(2, result.size());
    assertEquals(1L, result.get(0).getAccountId());
    assertEquals(2L, result.get(1).getAccountId());
}

@Test
void saveShouldInsertAccountAndReturnSavedAccount() throws Exception {

    OffsetDateTime createdAt =
            OffsetDateTime.parse("2026-11-20T10:00:00+05:30");

    BigDecimal balance = new BigDecimal("5000.00");

    Account account = new Account();
    account.setCustomerId(101L);
    account.setAccountNumber("ACC1001");
    account.setAccountType("SAVINGS");
    account.setBalance(balance);

    when(resultSet.getLong("account_id")).thenReturn(1L);
    when(resultSet.getString("status")).thenReturn("ACTIVE");
    when(resultSet.getObject("created_at", OffsetDateTime.class))
            .thenReturn(createdAt);

    when(jdbcTemplate.queryForObject(
            anyString(),
            any(RowMapper.class),
            any(Object[].class)
    )).thenAnswer(invocation -> {

        RowMapper<Account> mapper = invocation.getArgument(1);

        return mapper.mapRow(resultSet, 0);
    });

    Account result = accountRepository.save(account);

    assertEquals(1L, result.getAccountId());
    assertEquals("ACTIVE", result.getStatus());
    assertEquals(createdAt, result.getCreatedAt());
    assertEquals("ACC1001", result.getAccountNumber());
}

@Test
void findByCustomerIdShouldReturnEmptyListWhenNoAccountsExist() {

    when(jdbcTemplate.query(
            anyString(),
            any(RowMapper.class),
            eq(999L)
    )).thenReturn(List.of());

    List<Account> result = accountRepository.findByCustomerId(999L);

    assertTrue(result.isEmpty());
}
}
