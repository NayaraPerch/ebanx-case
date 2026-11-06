package br.com.ebanx.service;

import br.com.ebanx.model.Account;
import br.com.ebanx.model.AccountDestination;
import br.com.ebanx.model.OperationEvent;
import br.com.ebanx.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.Optional;

public class AccountService {

    private final AccountRepository accountRepository;
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountDestination deposit(OperationEvent operationEvent) {
        Optional<Account> existentAccount = accountRepository.findAccountById(operationEvent.getDestination());

        Account account = existentAccount.orElseGet(
                () -> new Account(
                        operationEvent.getDestination(),
                        operationEvent.getAmount()
                )
        );

        if (existentAccount.isPresent()) {
            account.deposit(operationEvent.getAmount());
        }

        accountRepository.save(account);
        return new AccountDestination(account);
    }

    public BigDecimal getBalance(String accountId) {
        Optional<Account> account = accountRepository.findAccountById(accountId);

        if (account.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return account.get().getBalance();
    }
}
