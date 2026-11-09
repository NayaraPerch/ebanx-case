package br.com.ebanx;

import br.com.ebanx.controller.AccountController;
import br.com.ebanx.repository.AccountRepositoryInterface;
import br.com.ebanx.repository.AccountRepository;
import br.com.ebanx.service.AccountService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Application {
    // URL para banco em memória (apaga ao reiniciar). Para salvar em arquivo use: "jdbc:h2:./data/banco"
    private static final String DB_URL = "jdbc:h2:mem:accountdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    public static void main(String[] args) throws IOException, SQLException {

        try {
            // 1. Instancia a conexão com o banco de dados H2
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Conectado ao H2 com sucesso.");

            // 2. Injeta a conexão no novo repositório H2
            AccountRepositoryInterface accountRepositoryInterface = new AccountRepository(connection);
            AccountService accountService = new AccountService(accountRepositoryInterface);
            AccountController accountController = new AccountController(accountService);

            // 3. Inicializa o servidor HTTP padrão do Java
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(8080), 0
            );
            server.createContext("/reset", accountController);
            server.createContext("/balance", accountController);
            server.createContext("/event", accountController);

            server.start();
            System.out.println("Servidor Iniciado em http://localhost:8080");

            // Adiciona um gancho para fechar a conexão com segurança quando a aplicação for encerrada
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                        System.out.println("Conexão com o H2 fechada com sucesso.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }));

        } catch (SQLException e) {
            System.err.println("Erro crítico ao inicializar o banco de dados H2.");
            e.printStackTrace();
        }
    }
}
