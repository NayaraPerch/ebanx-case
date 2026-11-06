package br.com.ebanx.repository;

import br.com.ebanx.model.Account;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findAccountById(String id);

    void save(Account account);
}
