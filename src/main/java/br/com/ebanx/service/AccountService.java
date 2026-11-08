package br.com.ebanx.service;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.model.*;
import br.com.ebanx.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.Optional;

public class AccountService {

    private final AccountRepository accountRepository;
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public DepositResponse deposit(OperationEvent operationEvent) {
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
        return new DepositResponse(account);
    }

    public BigDecimal getBalance(String accountId) {
        Optional<Account> account = accountRepository.findAccountById(accountId);

        if (account.isEmpty()) {
            throw new AccountNotFoundException(
                    "Account not found: " + accountId
            );
        }

        return account.get().getBalance();
    }

    public WithdrawResponse withdraw(OperationEvent operationEvent) throws AccountNotFoundException {
        Optional<Account> existentAccount = accountRepository.findAccountById(operationEvent.getOrigin());

        if (existentAccount.isEmpty()) {
            throw new AccountNotFoundException(
                    "Account not found: " + operationEvent.getOrigin()
            );
        }
        Account account = existentAccount.get();
        account.withdraw(operationEvent.getAmount());

        accountRepository.save(existentAccount.get());
        return new WithdrawResponse(existentAccount.get());
    }

    public TransferResponse transfer(OperationEvent operationEvent) {
        Optional<Account> originAccount = accountRepository.findAccountById(operationEvent.getOrigin());

        if (originAccount.isEmpty()) {
            throw new AccountNotFoundException(
                    "Account not found: " + operationEvent.getOrigin()
            );
        }

        Optional<Account> optionalDestination = accountRepository.findAccountById(operationEvent.getDestination());
        Account destinationAccount = optionalDestination.orElseGet(
                () -> new Account(
                        operationEvent.getDestination(),
                        BigDecimal.ZERO
                )
        );

        Account origin = originAccount.get();

        //debita
        origin.withdraw(operationEvent.getAmount());
        accountRepository.save(origin);

        //deposita
        destinationAccount.deposit(operationEvent.getAmount());
        accountRepository.save(destinationAccount);


        return new TransferResponse(origin, destinationAccount);
    }
}