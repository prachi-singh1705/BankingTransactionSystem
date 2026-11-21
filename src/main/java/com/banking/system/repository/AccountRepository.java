
package com.banking.system.repository;

import com.banking.system.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Account save(Account account) {

        String sql = """
                INSERT INTO account
                (customer_id, account_number, account_type, balance)
                VALUES (?, ?, ?, ?)
                RETURNING account_id, status, created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {
                    account.setAccountId(rs.getLong("account_id"));
                    account.setStatus(rs.getString("status"));
                    account.setCreatedAt(
                            rs.getObject("created_at", OffsetDateTime.class)
                    );
                    return account;
                },
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance()
        );
    }

    public List<Account> findByCustomerId(Long customerId) {

        String sql = """
                SELECT account_id, customer_id, account_number,
                       account_type, balance, status, created_at
                FROM account
                WHERE customer_id = ?
                ORDER BY account_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Account account = new Account();
            account.setAccountId(rs.getLong("account_id"));
            account.setCustomerId(rs.getLong("customer_id"));
            account.setAccountNumber(rs.getString("account_number"));
            account.setAccountType(rs.getString("account_type"));
            account.setBalance(rs.getBigDecimal("balance"));
            account.setStatus(rs.getString("status"));
            account.setCreatedAt(
                    rs.getObject("created_at", OffsetDateTime.class)
            );
            return account;
        }, customerId);
    }

    // Fetch account and lock its database row
    public Optional<Account> findByIdForUpdate(Long accountId) {

        String sql = """
                SELECT account_id, customer_id, account_number,
                       account_type, balance, status, created_at
                FROM account
                WHERE account_id = ?
                FOR UPDATE
                """;

        List<Account> accounts = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Account account = new Account();
            account.setAccountId(rs.getLong("account_id"));
            account.setCustomerId(rs.getLong("customer_id"));
            account.setAccountNumber(rs.getString("account_number"));
            account.setAccountType(rs.getString("account_type"));
            account.setBalance(rs.getBigDecimal("balance"));
            account.setStatus(rs.getString("status"));
            account.setCreatedAt(
                    rs.getObject("created_at", OffsetDateTime.class)
            );
            return account;
        }, accountId);

        return accounts.stream().findFirst();
    }

    // Update account balance
    public int updateBalance(Long accountId, BigDecimal newBalance) {

        String sql = """
                UPDATE account
                SET balance = ?
                WHERE account_id = ?
                """;

        return jdbcTemplate.update(sql, newBalance, accountId);
    }
}
