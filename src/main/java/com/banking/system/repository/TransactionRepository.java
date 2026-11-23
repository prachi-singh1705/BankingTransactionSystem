
package com.banking.system.repository;

import com.banking.system.model.BankTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Save transaction
    public BankTransaction save(BankTransaction transaction) {

        String sql = """
            INSERT INTO bank_transaction
            (transaction_id, from_account_id, to_account_id,
             transaction_type, amount, status, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {

            transaction.setId(rs.getLong("id"));
            transaction.setCreatedAt(
                rs.getObject("created_at", OffsetDateTime.class)
            );

            return transaction;

        },
            transaction.getTransactionId(),
            transaction.getFromAccountId(),
            transaction.getToAccountId(),
            transaction.getTransactionType(),
            transaction.getAmount(),
            transaction.getStatus(),
            transaction.getDescription()
        );
    }

    // Find transaction by transaction ID
    public Optional<BankTransaction> findByTransactionId(UUID transactionId) {

        String sql = """
            SELECT id, transaction_id, from_account_id, to_account_id,
                   transaction_type, amount, status, description, created_at
            FROM bank_transaction
            WHERE transaction_id = ?
            """;

        List<BankTransaction> transactions = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> mapTransaction(rs),
            transactionId
        );

        return transactions.stream().findFirst();
    }

    // Get complete transaction history for an account
    public List<BankTransaction> findByAccountId(Long accountId) {

        String sql = """
            SELECT id, transaction_id, from_account_id, to_account_id,
                   transaction_type, amount, status, description, created_at
            FROM bank_transaction
            WHERE from_account_id = ?
               OR to_account_id = ?
            ORDER BY created_at DESC, id DESC
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> mapTransaction(rs),
            accountId,
            accountId
        );
    }

    // Map database row to BankTransaction object
    private BankTransaction mapTransaction(
            java.sql.ResultSet rs) throws java.sql.SQLException {

        BankTransaction transaction = new BankTransaction();

        transaction.setId(rs.getLong("id"));
        transaction.setTransactionId(
                rs.getObject("transaction_id", UUID.class)
        );
        transaction.setFromAccountId(
                (Long) rs.getObject("from_account_id")
        );
        transaction.setToAccountId(
                (Long) rs.getObject("to_account_id")
        );
        transaction.setTransactionType(
                rs.getString("transaction_type")
        );
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setStatus(rs.getString("status"));
        transaction.setDescription(rs.getString("description"));
        transaction.setCreatedAt(
                rs.getObject("created_at", OffsetDateTime.class)
        );

        return transaction;
    }
}
