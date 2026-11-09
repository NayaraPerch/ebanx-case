
package br.com.ebanx.service;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.exception.InsuficientFundsException;
import br.com.ebanx.model.*;
import br.com.ebanx.repository.AccountRepository;
import br.com.ebanx.repository.AccountRepositoryInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {

   private AccountRepositoryInterface accountRepository;
   private AccountService accountService;

   @BeforeEach
   void setUp() {

      String databaseName = "testdb_" + UUID.randomUUID();

      DataSource dataSource = new DriverManagerDataSource(
              "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1",
              "sa",
              ""
      );

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

      // Creates the table before instantiating the repository.
      jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id VARCHAR(50) PRIMARY KEY,
                    balance DECIMAL(19, 2) NOT NULL
                )
                """);

      accountRepository = new AccountRepository(jdbcTemplate);
      accountService = new AccountService(accountRepository);
   }

   private void assertBalance(BigDecimal expected, BigDecimal actual) {
      assertEquals(0, expected.compareTo(actual));
   }

   // DEPOSIT TESTS //

   @Test
   void shouldCreateAccountWhenDepositingIntoNonexistentAccount() {

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.DEPOSIT);
      operationEvent.setAmount(new BigDecimal("150"));
      operationEvent.setDestination("1234");

      Optional<Account> account =
              accountRepository.findAccountById(operationEvent.getDestination());

      assertFalse(account.isPresent());

      DepositResponse depositResponse = accountService.deposit(operationEvent);

      account = accountRepository.findAccountById(operationEvent.getDestination());

      assertTrue(account.isPresent());
      assertEquals(operationEvent.getDestination(), account.get().getId());
      assertBalance(operationEvent.getAmount(), account.get().getBalance());

      assertNotNull(depositResponse);
      assertNotNull(depositResponse.getDestination());
   }

   @Test
   void shouldIncreaseBalanceWhenDepositingIntoExistingAccount() {

      Account accountFake = new Account("1234", new BigDecimal("50"));
      BigDecimal initialBalance = accountFake.getBalance();

      accountRepository.save(accountFake);

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.DEPOSIT);
      operationEvent.setAmount(new BigDecimal("150"));
      operationEvent.setDestination("1234");

      DepositResponse depositResponse = accountService.deposit(operationEvent);

      Optional<Account> account =
              accountRepository.findAccountById(operationEvent.getDestination());

      assertTrue(account.isPresent());
      assertEquals(operationEvent.getDestination(), account.get().getId());

      assertBalance(
              initialBalance.add(operationEvent.getAmount()),
              account.get().getBalance()
      );

      assertNotNull(depositResponse);
      assertNotNull(depositResponse.getDestination());
   }

   // BALANCE TESTS //

   @Test
   void shouldReturnBalanceWhenAccountExists() {

      Account accountFake = new Account("1111", new BigDecimal("123"));
      accountRepository.save(accountFake);

      BigDecimal balance = accountService.getBalance(accountFake.getId());

      assertNotNull(balance);
      assertBalance(accountFake.getBalance(), balance);
   }

   @Test
   void shouldThrowExceptionWhenAccountDoesNotExist() {

      assertThrows(
              AccountNotFoundException.class,
              () -> accountService.getBalance("456")
      );
   }

   // WITHDRAW TESTS //

   @Test
   void shouldDecreaseBalanceWhenWithdrawingFromExistingAccount() {

      Account accountFake = new Account("1234", new BigDecimal("50"));
      BigDecimal initialBalance = accountFake.getBalance();

      accountRepository.save(accountFake);

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.WITHDRAW);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");

      WithdrawResponse accountOrigin = accountService.withdraw(operationEvent);

      Optional<Account> account =
              accountRepository.findAccountById(operationEvent.getOrigin());

      assertTrue(account.isPresent());
      assertEquals(operationEvent.getOrigin(), account.get().getId());

      assertBalance(
              initialBalance.subtract(operationEvent.getAmount()),
              account.get().getBalance()
      );

      assertNotNull(accountOrigin);
      assertNotNull(accountOrigin.getOrigin());
   }

   @Test
   void shouldThrowExceptionWhenWithdrawingFromNonexistentAccount() {

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.WITHDRAW);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");

      assertThrows(
              AccountNotFoundException.class,
              () -> accountService.withdraw(operationEvent)
      );

      assertTrue(accountRepository.findAccountById("1234").isEmpty());
   }

   @Test
   void shouldThrowExceptionWhenWithdrawingWithInsufficientFunds() {

      accountRepository.save(new Account("1234", new BigDecimal("50")));

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.WITHDRAW);
      operationEvent.setAmount(new BigDecimal("100"));
      operationEvent.setOrigin("1234");

      assertThrows(
              InsuficientFundsException.class,
              () -> accountService.withdraw(operationEvent)
      );

      assertBalance(
              new BigDecimal("50"),
              accountRepository.findAccountById("1234").get().getBalance()
      );
   }

   // TRANSFER TESTS //

   @Test
   void shouldTransferAmountBetweenExistingAccounts() {

      Account originAccount = new Account("1234", new BigDecimal("50"));
      BigDecimal initialOriginBalance = originAccount.getBalance();
      accountRepository.save(originAccount);

      Account destinationAccount = new Account("1111", new BigDecimal("50"));
      BigDecimal initialDestinationBalance = destinationAccount.getBalance();
      accountRepository.save(destinationAccount);

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.TRANSFER);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");
      operationEvent.setDestination("1111");

      TransferResponse transferResponse = accountService.transfer(operationEvent);

      Optional<Account> accountOrigin =
              accountRepository.findAccountById(operationEvent.getOrigin());

      Optional<Account> accountDestination =
              accountRepository.findAccountById(operationEvent.getDestination());

      assertTrue(accountOrigin.isPresent());
      assertTrue(accountDestination.isPresent());

      assertEquals(operationEvent.getDestination(), accountDestination.get().getId());
      assertEquals(operationEvent.getOrigin(), accountOrigin.get().getId());

      assertBalance(
              initialOriginBalance.subtract(operationEvent.getAmount()),
              accountOrigin.get().getBalance()
      );

      assertBalance(
              initialDestinationBalance.add(operationEvent.getAmount()),
              accountDestination.get().getBalance()
      );

      assertNotNull(transferResponse);
      assertNotNull(transferResponse.getDestination());
      assertNotNull(transferResponse.getOrigin());

      assertBalance(
              initialOriginBalance.subtract(operationEvent.getAmount()),
              transferResponse.getOrigin().getBalance()
      );

      assertBalance(
              initialDestinationBalance.add(operationEvent.getAmount()),
              transferResponse.getDestination().getBalance()
      );
   }

   @Test
   void shouldCreateDestinationAccountWhenTransferringToNonexistentAccount() {

      Account originAccount = new Account("1234", new BigDecimal("50"));
      accountRepository.save(originAccount);

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.TRANSFER);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");
      operationEvent.setDestination("2222");

      assertTrue(accountRepository.findAccountById("2222").isEmpty());

      TransferResponse transferResponse = accountService.transfer(operationEvent);

      Optional<Account> accountOrigin =
              accountRepository.findAccountById("1234");

      Optional<Account> accountDestination =
              accountRepository.findAccountById("2222");

      assertTrue(accountOrigin.isPresent());
      assertTrue(accountDestination.isPresent());

      assertBalance(new BigDecimal("40"), accountOrigin.get().getBalance());
      assertBalance(new BigDecimal("10"), accountDestination.get().getBalance());

      assertNotNull(transferResponse);

      assertEquals("1234", transferResponse.getOrigin().getId());
      assertEquals("2222", transferResponse.getDestination().getId());

      assertBalance(
              new BigDecimal("40"),
              transferResponse.getOrigin().getBalance()
      );

      assertBalance(
              new BigDecimal("10"),
              transferResponse.getDestination().getBalance()
      );
   }

   @Test
   void shouldThrowExceptionWhenTransferringFromNonexistentAccount() {

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.TRANSFER);
      operationEvent.setAmount(new BigDecimal("10"));
      operationEvent.setOrigin("1234");
      operationEvent.setDestination("2222");

      assertThrows(
              AccountNotFoundException.class,
              () -> accountService.transfer(operationEvent)
      );

      assertTrue(accountRepository.findAccountById("1234").isEmpty());
      assertTrue(accountRepository.findAccountById("2222").isEmpty());
   }

   @Test
   void shouldThrowExceptionWhenTransferringWithInsufficientFunds() {

      accountRepository.save(new Account("1234", new BigDecimal("50")));
      accountRepository.save(new Account("2222", new BigDecimal("20")));

      OperationEvent operationEvent = new OperationEvent();

      operationEvent.setType(TypeEnum.TRANSFER);
      operationEvent.setAmount(new BigDecimal("100"));
      operationEvent.setOrigin("1234");
      operationEvent.setDestination("2222");

      assertThrows(
              InsuficientFundsException.class,
              () -> accountService.transfer(operationEvent)
      );

      assertBalance(
              new BigDecimal("50"),
              accountRepository.findAccountById("1234").get().getBalance()
      );

      assertBalance(
              new BigDecimal("20"),
              accountRepository.findAccountById("2222").get().getBalance()
      );
   }
}
