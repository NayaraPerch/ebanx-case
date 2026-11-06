package br.com.ebanx.service;

import br.com.ebanx.model.Account;
import br.com.ebanx.model.AccountDestination;
import br.com.ebanx.model.OperationEvent;
import br.com.ebanx.model.TypeEnum;
import br.com.ebanx.repository.AccountRepository;
import br.com.ebanx.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class AccountServiceTest {

   // DEPOSIT TESTS //
    @Test
    void shouldCreateAccountWhenDepositingIntoNonexistentAccount(){
       OperationEvent operationEvent = new OperationEvent();
       AccountRepository accountRepository = new InMemoryAccountRepository();
       AccountService accountService = new AccountService(accountRepository);


       operationEvent.setType(TypeEnum.DEPOSIT);
       operationEvent.setAmount(new BigDecimal("150"));
       operationEvent.setDestination("1234");
       Optional<Account> account = accountRepository.findAccountById(operationEvent.getDestination());

       assertFalse(account.isPresent());

       AccountDestination accountDestination = accountService.deposit(operationEvent);

       account = accountRepository.findAccountById(operationEvent.getDestination());
       assertTrue(account.isPresent());
       assertEquals(account.get().getId(), operationEvent.getDestination());
       assertEquals(account.get().getBalance(), operationEvent.getAmount());
       assertNotNull(accountDestination);
       assertNotNull(accountDestination.getAccount());

    }

    @Test
    void shouldIncreaseBalanceWhenDepositingIntoExistingAccount(){
       OperationEvent operationEvent = new OperationEvent();
       AccountRepository accountRepository = new InMemoryAccountRepository();
       AccountService accountService = new AccountService(accountRepository);

       Account accountFake = new Account("1234", new BigDecimal("50"));
       BigDecimal initialBalance = accountFake.getBalance();
       accountRepository.save(accountFake);

       operationEvent.setType(TypeEnum.DEPOSIT);
       operationEvent.setAmount(new BigDecimal("150"));
       operationEvent.setDestination("1234");

       AccountDestination accountDestination = accountService.deposit(operationEvent);

       Optional<Account> account = accountRepository.findAccountById(operationEvent.getDestination());
       assertTrue(account.isPresent());
       assertEquals(operationEvent.getDestination(), account.get().getId());
       assertEquals(initialBalance.add(operationEvent.getAmount()),
               account.get().getBalance());
       assertNotNull(accountDestination);
       assertNotNull(accountDestination.getAccount());
    }

    // BALANCE TESTS //

   @Test
   void shouldReturnBalanceWhenAccountExists(){
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      Account accountFake = new Account("1111", new BigDecimal("123"));
      accountRepository.save(accountFake);

      BigDecimal balance = accountService.getBalance(accountFake.getId());

      assertNotNull(balance);
      assertEquals(accountFake.getBalance(), balance);
   }

   @Test
   void shouldReturnZeroWhenAccountDontExists(){
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      BigDecimal balance = accountService.getBalance("456");

      assertEquals(BigDecimal.ZERO,balance);
   }
}
