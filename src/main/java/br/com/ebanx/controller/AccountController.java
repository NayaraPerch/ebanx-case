
package br.com.ebanx.controller;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.exception.InsuficientFundsException;
import br.com.ebanx.model.OperationEvent;
import br.com.ebanx.service.AccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        System.out.println("Account Controller - Iniciando reset");

        accountService.reset();

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/balance")
    public ResponseEntity<String> getBalance(@RequestParam("account_id") String accountId) {
        System.out.println("Account Controller - Buscando saldo da conta: " + accountId);

        try {
            BigDecimal balance = accountService.getBalance(accountId);
            return ResponseEntity.ok(balance.toString());
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(404).body("0");
        }
    }

    @PostMapping("/event")
    public ResponseEntity<?> processEvent(
            @RequestBody OperationEvent event) {

        Object response;

        switch (event.getType()) {

            case DEPOSIT -> {
                System.out.println(
                        "Account Controller - Iniciando deposito. Conta destino: "
                                + event.getDestination()
                                + " valor: " + event.getAmount()
                );

                response = accountService.deposit(event);
            }

            case TRANSFER -> {
                try {
                    System.out.println(
                            "Account Controller - Iniciando transferencia. Conta origem: "
                                    + event.getOrigin()
                                    + " Conta destino: " + event.getDestination()
                                    + " valor: " + event.getAmount()
                    );

                    response = accountService.transfer(event);

                } catch (AccountNotFoundException e) {
                    return ResponseEntity.status(404).body("0");

                } catch (InsuficientFundsException e) {
                    return ResponseEntity.status(422).body(e.getMessage());
                }
            }

            case WITHDRAW -> {
                try {
                    System.out.println(
                            "Account Controller - Iniciando saque. Conta origem: "
                                    + event.getOrigin()
                                    + " valor: " + event.getAmount()
                    );

                    response = accountService.withdraw(event);

                } catch (AccountNotFoundException e) {
                    return ResponseEntity.status(404).body("0");

                } catch (InsuficientFundsException e) {
                    return ResponseEntity.status(422).body(e.getMessage());
                }
            }

            default -> throw new IllegalStateException(
                    "Unexpected value: " + event.getType()
            );
        }

        return ResponseEntity.status(201).body(response);
    }
}
