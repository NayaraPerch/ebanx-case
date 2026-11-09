package br.com.ebanx.repository;

import br.com.ebanx.model.Account;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepository implements AccountRepositoryInterface {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    private void createTableIfNotExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS accounts (
                    id VARCHAR(50) PRIMARY KEY,
                    balance DECIMAL(19, 2) NOT NULL
                )
                """;

        jdbcTemplate.execute(sql);
    }

    @Override
    public Optional<Account> findAccountById(String id) {
        String sql = "SELECT id, balance FROM accounts WHERE id = ?";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Account(
                        rs.getString("id"),
                        rs.getBigDecimal("balance")
                ),
                id
        ).stream().findFirst();
    }

    @Override
    public void save(Account account) {
        String sql = """
                MERGE INTO accounts (id, balance)
                KEY(id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(
                sql,
                account.getId(),
                account.getBalance()
        );
    }

    @Override
    public void clear() {
        jdbcTemplate.execute("TRUNCATE TABLE accounts");
    }
}
