package br.com.ebanx.controller;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.exception.InsuficientFundsException;
import br.com.ebanx.model.Account;
import br.com.ebanx.model.OperationEvent;
import br.com.ebanx.model.TransferResponse;
import br.com.ebanx.service.AccountService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void shouldReturn404WhenAccountDoesNotExist() throws Exception {

        when(accountService.getBalance("1234"))
                .thenThrow(new AccountNotFoundException("Account not found: 1234"));

        mockMvc.perform(get("/balance")
                        .param("accountId", "1234"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    void shouldReturn200WhenBalanceAccount() throws Exception {

        when(accountService.getBalance("1234"))
                .thenReturn(new BigDecimal("150"));

        mockMvc.perform(get("/balance")
                        .param("accountId", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    void shouldReturn201WhenDepositingIntoNonexistentAccount() throws Exception {

        String json = """
                {
                    "type": "deposit",
                    "destination": "100",
                    "amount": 10
                }
                """;

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(accountService).deposit(any(OperationEvent.class));
    }

    @Test
    void shouldReturn201WhenDepositingIntoExistentAccount() throws Exception {

        String json = """
                {
                    "type": "deposit",
                    "destination": "1234",
                    "amount": 10
                }
                """;

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(accountService).deposit(any(OperationEvent.class));
    }

    @Test
    void shouldReturn404WhenWithdrawingFromNonexistentAccount() throws Exception {

        String json = """
                {
                    "type": "withdraw",
                    "origin": "1234",
                    "amount": 10
                }
                """;

        when(accountService.withdraw(any(OperationEvent.class)))
                .thenThrow(new AccountNotFoundException("Account not found: 1234"));

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    void shouldReturn201WhenWithdrawingFromExistingAccount() throws Exception {

        String json = """
                {
                    "type": "withdraw",
                    "origin": "1234",
                    "amount": 10
                }
                """;

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(accountService).withdraw(any(OperationEvent.class));
    }

    @Test
    void shouldReturn422WhenWithdrawingWithInsufficientFunds() throws Exception {

        String json = """
                {
                    "type": "withdraw",
                    "origin": "1234",
                    "amount": 200
                }
                """;

        when(accountService.withdraw(any(OperationEvent.class)))
                .thenThrow(new InsuficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("Insufficient funds"));
    }

    @Test
    void shouldReturn404WhenTransferFromNonexistentAccount() throws Exception {

        String json = """
                {
                    "type": "transfer",
                    "origin": "1234",
                    "destination": "4567",
                    "amount": 10
                }
                """;

        when(accountService.transfer(any(OperationEvent.class)))
                .thenThrow(new AccountNotFoundException("Account not found: 1234"));

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    void shouldReturn201WhenTransferFromExistentAccountToNonexistentAccount()
            throws Exception {

        String json = """
                {
                    "type": "transfer",
                    "origin": "1234",
                    "destination": "4567",
                    "amount": 10
                }
                """;

        Account origin = new Account("1234", new BigDecimal("140"));
        Account destination = new Account("4567", new BigDecimal("10"));

        when(accountService.transfer(any(OperationEvent.class)))
                .thenReturn(new TransferResponse(origin, destination));

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.id").value("1234"))
                .andExpect(jsonPath("$.origin.balance").value(140))
                .andExpect(jsonPath("$.destination.id").value("4567"))
                .andExpect(jsonPath("$.destination.balance").value(10));

        verify(accountService).transfer(any(OperationEvent.class));
    }

    @Test
    void shouldReturn201WhenTransferBetweenTwoExistingAccounts()
            throws Exception {

        String json = """
                {
                    "type": "transfer",
                    "origin": "1234",
                    "destination": "4567",
                    "amount": 10
                }
                """;

        Account origin = new Account("1234", new BigDecimal("140"));
        Account destination = new Account("4567", new BigDecimal("60"));

        when(accountService.transfer(any(OperationEvent.class)))
                .thenReturn(new TransferResponse(origin, destination));

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.id").value("1234"))
                .andExpect(jsonPath("$.origin.balance").value(140))
                .andExpect(jsonPath("$.destination.id").value("4567"))
                .andExpect(jsonPath("$.destination.balance").value(60));

        verify(accountService).transfer(any(OperationEvent.class));
    }

    @Test
    void shouldReturn422WhenTransferWithInsufficientFunds() throws Exception {

        String json = """
                {
                    "type": "transfer",
                    "origin": "1234",
                    "destination": "4567",
                    "amount": 200
                }
                """;

        when(accountService.transfer(any(OperationEvent.class)))
                .thenThrow(new InsuficientFundsException("Insufficient funds"));

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("Insufficient funds"));
    }

    @Test
    void shouldReturn200AndResetAllAccounts() throws Exception {

        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(accountService).reset();
    }

    @Test
    void shouldReturn200WhenResettingEmptyRepository() throws Exception {

        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(accountService).reset();
    }
}
