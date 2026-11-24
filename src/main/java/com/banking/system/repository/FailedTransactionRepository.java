
package com.banking.system.repository;

import com.banking.system.model.BankTransaction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class FailedTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public FailedTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Save failed transaction
    public void saveFailedTransaction(
            UUID transactionId,
            Long fromAccountId,
            Long toAccountId,
            String transactionType,
            BigDecimal amount,
            String failureReason) {

        String sql = """
                INSERT INTO failed_transaction
                (transaction_id, from_account_id, to_account_id,
                 transaction_type, amount, failure_reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                transactionId,
                fromAccountId,
                toAccountId,
                transactionType,
                amount,
                failureReason
        );
    }

    // Save transaction audit
    public void saveAudit(
            UUID transactionId,
            String eventType,
            String status,
            String reason,
            String threadName) {

        String sql = """
                INSERT INTO transaction_audit
                (transaction_id, event_type, status, reason, thread_name)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                transactionId,
                eventType,
                status,
                reason,
                threadName
        );
    }

    // Get failed transaction history for an account
    public List<BankTransaction> findFailedByAccountId(Long accountId) {

        String sql = """
                SELECT failed_id, transaction_id,
                       from_account_id, to_account_id,
                       transaction_type, amount,
                       failure_reason, created_at
                FROM failed_transaction
                WHERE from_account_id = ?
                   OR to_account_id = ?
                ORDER BY created_at DESC, failed_id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            BankTransaction transaction = new BankTransaction();

            transaction.setId(rs.getLong("failed_id"));

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

            transaction.setAmount(
                    rs.getBigDecimal("amount")
            );

            transaction.setStatus("FAILED");

            transaction.setDescription(
                    rs.getString("failure_reason")
            );

            transaction.setCreatedAt(
                    rs.getObject("created_at", OffsetDateTime.class)
            );

            return transaction;

        }, accountId, accountId);
    }
}
