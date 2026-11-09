package br.com.ebanx.repository;

import br.com.ebanx.model.Account;

import java.util.Optional;

public interface AccountRepositoryInterface {

    Optional<Account> findAccountById(String id);

    void save(Account account);

    void clear();
}
