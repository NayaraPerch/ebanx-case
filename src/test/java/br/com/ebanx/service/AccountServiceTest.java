package br.com.ebanx.service;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.model.*;
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

       DepositResponse depositResponse = accountService.deposit(operationEvent);

       account = accountRepository.findAccountById(operationEvent.getDestination());
       assertTrue(account.isPresent());
       assertEquals(account.get().getId(), operationEvent.getDestination());
       assertEquals(account.get().getBalance(), operationEvent.getAmount());
       assertNotNull(depositResponse);
       assertNotNull(depositResponse.getDestination());

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

       DepositResponse depositResponse = accountService.deposit(operationEvent);

       Optional<Account> account = accountRepository.findAccountById(operationEvent.getDestination());
       assertTrue(account.isPresent());
       assertEquals(operationEvent.getDestination(), account.get().getId());
       assertEquals(initialBalance.add(operationEvent.getAmount()),
               account.get().getBalance());
       assertNotNull(depositResponse);
       assertNotNull(depositResponse.getDestination());
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
   void shouldThrowExceptionWhenAccountDontExists(){
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      assertThrows(
              AccountNotFoundException.class,
              () -> accountService.getBalance("456")
      );
   }

   @Test
   void shouldDecreaseBalanceWhenWithdrawExistingAccount(){
      OperationEvent operationEvent = new OperationEvent();
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      Account accountFake = new Account("1234", new BigDecimal("50"));
      BigDecimal initialBalance = accountFake.getBalance();
      accountRepository.save(accountFake);

      operationEvent.setType(TypeEnum.WITHDRAW);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");

      WithdrawResponse accountOrigin = accountService.withdraw(operationEvent);

      Optional<Account> account = accountRepository.findAccountById(operationEvent.getOrigin());
      assertTrue(account.isPresent());
      assertEquals(operationEvent.getOrigin(), account.get().getId());
      assertEquals(initialBalance.subtract(operationEvent.getAmount()),
              account.get().getBalance());
      assertNotNull(accountOrigin);
      assertNotNull(accountOrigin.getOrigin());
   }

   @Test
   void shouldThrowExceptionWhenWithdrawAndAccountDontExists() throws AccountNotFoundException {
      OperationEvent operationEvent = new OperationEvent();
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      operationEvent.setType(TypeEnum.WITHDRAW);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");

      assertThrows(
              AccountNotFoundException.class,
              () -> accountService.withdraw(operationEvent)
      );
      assertTrue(accountRepository.findAccountById("1234").isEmpty());
   }

   // TRANSFER TESTS // Falta o teste da excecao
   @Test
   void shouldTransferAmountBetweenExistingAccounts(){
      OperationEvent operationEvent = new OperationEvent();
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      Account originAccount = new Account("1234", new BigDecimal("50"));
      BigDecimal initialOriginBalance = originAccount.getBalance();
      accountRepository.save(originAccount);

      Account destinationAccount = new Account("1111", new BigDecimal("50"));
      BigDecimal initialDestinationBalance = destinationAccount.getBalance();
      accountRepository.save(destinationAccount);

      operationEvent.setType(TypeEnum.TRANSFER);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");
      operationEvent.setDestination("1111");

      TransferResponse transferResponse = accountService.transfer(operationEvent);

      Optional<Account> accountOrigin = accountRepository.findAccountById(operationEvent.getOrigin());
      Optional<Account> accountDestination = accountRepository.findAccountById(operationEvent.getDestination());

      assertTrue(accountOrigin.isPresent());
      assertTrue(accountDestination.isPresent());
      assertEquals(operationEvent.getDestination(), accountDestination.get().getId());
      assertEquals(operationEvent.getOrigin(), accountOrigin.get().getId());
      assertEquals(initialOriginBalance.subtract(operationEvent.getAmount()),
              accountOrigin.get().getBalance());
      assertEquals(initialDestinationBalance.add(operationEvent.getAmount()),
              accountDestination.get().getBalance());
      assertNotNull(transferResponse);
      assertNotNull(transferResponse.getDestination());
      assertNotNull(transferResponse.getOrigin());
      assertEquals(
              initialOriginBalance.subtract(operationEvent.getAmount()),
              transferResponse.getOrigin().getBalance()
      );

      assertEquals(
              initialDestinationBalance.add(operationEvent.getAmount()),
              transferResponse.getDestination().getBalance()
      );
   }


   @Test
   void shouldCreateDestinationAccountWhenTransferringToNonexistentAccount() {
      OperationEvent operationEvent = new OperationEvent();
      AccountRepository accountRepository = new InMemoryAccountRepository();
      AccountService accountService = new AccountService(accountRepository);

      Account originAccount = new Account("1234", new BigDecimal("50"));
      accountRepository.save(originAccount);

      operationEvent.setType(TypeEnum.TRANSFER);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");
      operationEvent.setDestination("2222");

      // Verifica que a conta de destino ainda não existe
      assertTrue(accountRepository.findAccountById("2222").isEmpty());

      TransferResponse transferResponse = accountService.transfer(operationEvent);

      Optional<Account> accountOrigin =
              accountRepository.findAccountById("1234");

      Optional<Account> accountDestination =
              accountRepository.findAccountById("2222");

      // Verifica a existência das duas contas
      assertTrue(accountOrigin.isPresent());
      assertTrue(accountDestination.isPresent());

      // Verifica os saldos após a transferência
      assertEquals(new BigDecimal("40"), accountOrigin.get().getBalance());
      assertEquals(new BigDecimal("10"), accountDestination.get().getBalance());

      // Verifica a resposta da transferência
      assertNotNull(transferResponse);
      assertEquals("1234", transferResponse.getOrigin().getId());
      assertEquals("2222", transferResponse.getDestination().getId());
      assertEquals(new BigDecimal("40"), transferResponse.getOrigin().getBalance());
      assertEquals(new BigDecimal("10"), transferResponse.getDestination().getBalance());
   }


}
