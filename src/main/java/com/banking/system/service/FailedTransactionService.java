
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
}
