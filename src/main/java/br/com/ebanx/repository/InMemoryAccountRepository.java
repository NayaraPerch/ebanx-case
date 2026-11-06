package br.com.ebanx.repository;

import br.com.ebanx.model.Account;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accountMap = new HashMap<>();
    @Override
    public Optional<Account> findAccountById(String id) {
      return Optional.ofNullable(accountMap.get(id));
    }

    @Override
    public void save(Account account) {
        accountMap.put(account.getId(), account);
    }
}
