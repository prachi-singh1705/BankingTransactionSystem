
package com.banking.system.service;

import com.banking.system.repository.FailedTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class FailedTransactionService {

    private final FailedTransactionRepository failedTransactionRepository;

    public FailedTransactionService(
            FailedTransactionRepository failedTransactionRepository) {
        this.failedTransactionRepository = failedTransactionRepository;
    }

    // Record failed withdrawal
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedWithdrawal(
            UUID transactionId,
            Long accountId,
            BigDecimal amount,
            String failureReason) {

        failedTransactionRepository.saveFailedTransaction(
                transactionId,
                accountId,
                null,
                "WITHDRAWAL",
                amount,
                failureReason
        );

        failedTransactionRepository.saveAudit(
                transactionId,
                "WITHDRAWAL_REJECTED",
                "FAILED",
                failureReason,
                Thread.currentThread().getName()
        );
    }

    // Record failed money transfer
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedTransfer(
            UUID transactionId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            String failureReason) {

        failedTransactionRepository.saveFailedTransaction(
                transactionId,
                fromAccountId,
                toAccountId,
                "TRANSFER",
                amount,
                failureReason
        );

        failedTransactionRepository.saveAudit(
                transactionId,
                "TRANSFER_REJECTED",
                "FAILED",
                failureReason,
                Thread.currentThread().getName()
        );
    }
}
