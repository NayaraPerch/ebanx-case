package br.com.ebanx.repository;

import br.com.ebanx.model.Account;

import java.sql.*;
import java.util.Optional;
public class AccountRepository implements AccountRepositoryInterface {
    private final Connection connection;

    public AccountRepository(Connection connection) {
        this.connection = connection;
        this.createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "balance int NOT NULL)";
        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar a tabela de contas", e);
        }
    }


    @Override
    public Optional<Account> findAccountById(String id) {
        String sql = "SELECT id, balance FROM accounts WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Substitua pelo construtor correto da sua classe Account (ex: id, balance)
                    Account account = new Account(rs.getString("id"), rs.getBigDecimal("balance"));
                    return Optional.of(account);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta com id: " + id, e);
        }

        return Optional.empty();
    }

    @Override
    public void save(Account account) {

        String sql = "MERGE INTO accounts (id, balance) KEY(id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getId());
            stmt.setBigDecimal(2, account.getBalance());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar/atualizar a conta", e);
        }
    }

    @Override
    public void clear() {

        String sql = "TRUNCATE TABLE accounts";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao limpar a tabela de contas", e);
        }
    }
}
