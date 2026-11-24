
package com.banking.system.service;

import com.banking.system.model.BankTransaction;
import com.banking.system.model.BulkTransactionRequest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ConcurrentTransactionService {

    private final TransactionService transactionService;
    private final FailedTransactionService failedTransactionService;
    private final Executor transactionExecutor;

    public ConcurrentTransactionService(
            TransactionService transactionService,
            FailedTransactionService failedTransactionService,
            @Qualifier("transactionExecutor") Executor transactionExecutor) {

        this.transactionService = transactionService;
        this.failedTransactionService = failedTransactionService;
        this.transactionExecutor = transactionExecutor;
    }

    // Process multiple transactions concurrently
    public CompletableFuture<List<Map<String, Object>>> processBulkTransactions(
            List<BulkTransactionRequest> requests) {

        List<CompletableFuture<Map<String, Object>>> futures =
                requests.stream()
                        .map(this::processSingleTransaction)
                        .toList();

        return CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        ).thenApply(ignored ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
        );
    }

    // Process one transaction on a worker thread
    private CompletableFuture<Map<String, Object>> processSingleTransaction(
            BulkTransactionRequest request) {

        return CompletableFuture.supplyAsync(() -> {

            Map<String, Object> result = new LinkedHashMap<>();

            UUID transactionId = request.getTransactionId();

            try {

                BankTransaction transaction;

                String type = request.getTransactionType();

                if (type == null) {
                    throw new IllegalArgumentException(
                            "Transaction type is required");
                }

                switch (type.toUpperCase()) {

                    case "DEPOSIT":

                        transaction = transactionService.deposit(
                                request.getAccountId(),
                                request.getAmount(),
                                transactionId
                        );

                        break;

                    case "WITHDRAWAL":

                        transaction = transactionService.withdraw(
                                request.getAccountId(),
                                request.getAmount(),
                                transactionId
                        );

                        break;

                    case "TRANSFER":

                        transaction = transactionService.transfer(
                                request.getFromAccountId(),
                                request.getToAccountId(),
                                request.getAmount(),
                                transactionId
                        );

                        break;

                    default:

                        throw new IllegalArgumentException(
                                "Unsupported transaction type");
                }

                result.put("transactionId", transactionId);
                result.put("status", "SUCCESS");
                result.put("message", "Transaction processed successfully");
                result.put("transaction", transaction);

            } catch (Exception exception) {

                String failureReason = exception.getMessage();

                result.put("transactionId", transactionId);
                result.put("status", "FAILED");
                result.put("message", failureReason);

                // Record failed withdrawal and transfer
                try {

                    if ("Insufficient balance".equals(failureReason)
                            && transactionId != null
                            && request.getTransactionType() != null) {

                        String failedType =
                                request.getTransactionType().toUpperCase();

                        if ("WITHDRAWAL".equals(failedType)
                                || "TRANSFER".equals(failedType)) {

                            failedTransactionService.recordBulkFailedTransaction(
                                    request,
                                    failureReason
                            );
                        }
                    }

                } catch (Exception recordingException) {

                    result.put(
                            "recordingError",
                            recordingException.getMessage()
                    );
                }
            }

            result.put(
                    "threadName",
                    Thread.currentThread().getName()
            );

            return result;

        }, transactionExecutor);
    }
}
