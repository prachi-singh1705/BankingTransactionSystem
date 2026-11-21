
package com.banking.system.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class FailedTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public FailedTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
}
