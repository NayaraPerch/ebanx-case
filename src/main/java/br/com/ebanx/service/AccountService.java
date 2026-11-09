
package br.com.ebanx.service;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.model.*;
import br.com.ebanx.repository.AccountRepositoryInterface;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepositoryInterface accountRepositoryInterface;

    public AccountService(AccountRepositoryInterface accountRepositoryInterface) {
        this.accountRepositoryInterface = accountRepositoryInterface;
    }

    public DepositResponse deposit(OperationEvent operationEvent) {
        Optional<Account> existentAccount =
                accountRepositoryInterface.findAccountById(operationEvent.getDestination());

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
        Account account = accountRepositoryInterface.findAccountById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId
                ));

        return account.getBalance();
    }

    public WithdrawResponse withdraw(OperationEvent operationEvent) {
        Account account = accountRepositoryInterface
                .findAccountById(operationEvent.getOrigin())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + operationEvent.getOrigin()
                ));

        account.withdraw(operationEvent.getAmount());

        accountRepositoryInterface.save(account);

        return new WithdrawResponse(account);
    }

    public TransferResponse transfer(OperationEvent operationEvent) {
        Account origin = accountRepositoryInterface
                .findAccountById(operationEvent.getOrigin())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + operationEvent.getOrigin()
                ));

        Optional<Account> optionalDestination =
                accountRepositoryInterface.findAccountById(operationEvent.getDestination());

        Account destination = optionalDestination.orElseGet(
                () -> new Account(
                        operationEvent.getDestination(),
                        BigDecimal.ZERO
                )
        );

        // Debita a conta de origem
        origin.withdraw(operationEvent.getAmount());
        accountRepositoryInterface.save(origin);

        // Credita a conta de destino
        destination.deposit(operationEvent.getAmount());
        accountRepositoryInterface.save(destination);

        return new TransferResponse(origin, destination);
    }

    public void reset() {
        accountRepositoryInterface.clear();
    }
}
