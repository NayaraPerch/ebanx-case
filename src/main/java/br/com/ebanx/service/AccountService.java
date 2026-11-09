package br.com.ebanx.service;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.model.*;
import br.com.ebanx.repository.AccountRepositoryInterface;

import java.math.BigDecimal;
import java.util.Optional;

public class AccountService {

    private final AccountRepositoryInterface accountRepositoryInterface;
    public AccountService(AccountRepositoryInterface accountRepositoryInterface) {
        this.accountRepositoryInterface = accountRepositoryInterface;
    }

    public DepositResponse deposit(OperationEvent operationEvent) {
        Optional<Account> existentAccount = accountRepositoryInterface.findAccountById(operationEvent.getDestination());

        Account account = existentAccount.orElseGet(
                () -> new Account(
                        operationEvent.getDestination(),
                        operationEvent.getAmount()
                )
        );

        if (existentAccount.isPresent()) {
            account.deposit(operationEvent.getAmount());
        }

        accountRepositoryInterface.save(account);
        return new DepositResponse(account);
    }

    public BigDecimal getBalance(String accountId) {
        Optional<Account> account = accountRepositoryInterface.findAccountById(accountId);

        if (account.isEmpty()) {
            throw new AccountNotFoundException(
                    "Account not found: " + accountId
            );
        }

        return account.get().getBalance();
    }

    public WithdrawResponse withdraw(OperationEvent operationEvent) throws AccountNotFoundException {
        Optional<Account> existentAccount = accountRepositoryInterface.findAccountById(operationEvent.getOrigin());

        if (existentAccount.isEmpty()) {
            throw new AccountNotFoundException(
                    "Account not found: " + operationEvent.getOrigin()
            );
        }
        Account account = existentAccount.get();
        account.withdraw(operationEvent.getAmount());

        accountRepositoryInterface.save(existentAccount.get());
        return new WithdrawResponse(existentAccount.get());
    }

    public TransferResponse transfer(OperationEvent operationEvent) {
        Optional<Account> originAccount = accountRepositoryInterface.findAccountById(operationEvent.getOrigin());

        if (originAccount.isEmpty()) {
            throw new AccountNotFoundException(
                    "Account not found: " + operationEvent.getOrigin()
            );
        }

        Optional<Account> optionalDestination = accountRepositoryInterface.findAccountById(operationEvent.getDestination());
        Account destinationAccount = optionalDestination.orElseGet(
                () -> new Account(
                        operationEvent.getDestination(),
                        BigDecimal.ZERO
                )
        );

        Account origin = originAccount.get();

        //debita
        origin.withdraw(operationEvent.getAmount());
        accountRepositoryInterface.save(origin);

        //deposita
        destinationAccount.deposit(operationEvent.getAmount());
        accountRepositoryInterface.save(destinationAccount);


        return new TransferResponse(origin, destinationAccount);
    }

    public void reset() {
        accountRepositoryInterface.clear();
    }
}