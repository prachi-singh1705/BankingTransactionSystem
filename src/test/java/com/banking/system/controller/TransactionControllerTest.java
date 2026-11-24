
package com.banking.system.controller;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import com.banking.system.model.BankTransaction;
import com.banking.system.model.BulkTransactionRequest;
import com.banking.system.service.TransactionService;
import com.banking.system.service.FailedTransactionService;
import com.banking.system.service.ConcurrentTransactionService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private FailedTransactionService failedTransactionService;

    @MockitoBean
    private ConcurrentTransactionService concurrentTransactionService;

    private BankTransaction createTransaction(
            UUID transactionId,
            String type,
            BigDecimal amount) {

        BankTransaction transaction = new BankTransaction();

        transaction.setId(1L);
        transaction.setTransactionId(transactionId);
        transaction.setFromAccountId(1L);
        transaction.setToAccountId(2L);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Test transaction");
        transaction.setCreatedAt(OffsetDateTime.now());

        return transaction;
    }

    @Test
    void testDeposit() throws Exception {

        UUID transactionId =
                UUID.fromString("11111111-1111-4111-8111-111111111111");

        BankTransaction transaction = createTransaction(
                transactionId,
                "DEPOSIT",
                new BigDecimal("100.00")
        );

        when(transactionService.deposit(
                eq(1L),
                eq(new BigDecimal("100.00")),
                eq(transactionId)
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType("application/json")
                        .content("""
                                {
                                    "accountId": "1",
                                    "amount": "100.00",
                                    "transactionId": "11111111-1111-4111-8111-111111111111"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(transactionService).deposit(
                1L,
                new BigDecimal("100.00"),
                transactionId
        );
    }

    @Test
    void testWithdraw() throws Exception {

        UUID transactionId =
                UUID.fromString("22222222-2222-4222-8222-222222222222");

        BankTransaction transaction = createTransaction(
                transactionId,
                "WITHDRAWAL",
                new BigDecimal("50.00")
        );

        when(transactionService.withdraw(
                eq(1L),
                eq(new BigDecimal("50.00")),
                eq(transactionId)
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType("application/json")
                        .content("""
                                {
                                    "accountId": "1",
                                    "amount": "50.00",
                                    "transactionId": "22222222-2222-4222-8222-222222222222"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(transactionService).withdraw(
                1L,
                new BigDecimal("50.00"),
                transactionId
        );
    }

    @Test
    void testTransfer() throws Exception {

        UUID transactionId =
                UUID.fromString("33333333-3333-4333-8333-333333333333");

        BankTransaction transaction = createTransaction(
                transactionId,
                "TRANSFER",
                new BigDecimal("200.00")
        );

        when(transactionService.transfer(
                eq(1L),
                eq(2L),
                eq(new BigDecimal("200.00")),
                eq(transactionId)
        )).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType("application/json")
                        .content("""
                                {
                                    "fromAccountId": "1",
                                    "toAccountId": "2",
                                    "amount": "200.00",
                                    "transactionId": "33333333-3333-4333-8333-333333333333"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(transactionService).transfer(
                1L,
                2L,
                new BigDecimal("200.00"),
                transactionId
        );
    }

    @Test
    void testAccountTransactionHistory() throws Exception {

        UUID transactionId =
                UUID.fromString("44444444-4444-4444-8444-444444444444");

        BankTransaction transaction = createTransaction(
                transactionId,
                "DEPOSIT",
                new BigDecimal("500.00")
        );

        when(transactionService.getAccountTransactionHistory(1L))
                .thenReturn(List.of(transaction));

        mockMvc.perform(get("/api/transactions/account/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(500.00));

        verify(transactionService).getAccountTransactionHistory(1L);
    }

   
    
@Test
void testBulkTransactions() throws Exception {

    Map<String, Object> result = Map.of(
            "status", "SUCCESS",
            "message", "Transaction processed"
    );

    List<Map<String, Object>> results = List.of(result);

    when(concurrentTransactionService.processBulkTransactions(anyList()))
            .thenReturn(CompletableFuture.completedFuture(results));

    var mvcResult = mockMvc.perform(post("/api/transactions/bulk")
                    .contentType("application/json")
                    .content("""
                            [
                                {
                                    "transactionType": "DEPOSIT",
                                    "accountId": 1,
                                    "amount": 100.00,
                                    "transactionId": "55555555-5555-4555-8555-555555555555"
                                }
                            ]
                            """))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("SUCCESS"));

    verify(concurrentTransactionService).processBulkTransactions(anyList());
}

}
