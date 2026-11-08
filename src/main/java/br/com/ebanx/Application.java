package br.com.ebanx;

import br.com.ebanx.controller.AccountController;
import br.com.ebanx.repository.AccountRepository;
import br.com.ebanx.repository.InMemoryAccountRepository;
import br.com.ebanx.service.AccountService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Application {

    public static void main(String[] args) throws IOException {
        AccountRepository accountRepository = new InMemoryAccountRepository();
        AccountService accountService = new AccountService(accountRepository);
        AccountController accountController = new AccountController(accountService);

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0
        );
        server.createContext("/reset", accountController);
        server.createContext("/balance", accountController);
        server.createContext("/event", accountController);

        server.start();

        System.out.println("Servidor Iniciado");
    }
}
