package br.com.ebanx.controller;

import br.com.ebanx.exception.AccountNotFoundException;
import br.com.ebanx.model.OperationEvent;
import br.com.ebanx.service.AccountService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class AccountController implements HttpHandler {

    private final AccountService accountService;

    private final ObjectMapper objectMapper;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
        this.objectMapper = new ObjectMapper();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("POST") && path.equals("/reset")) {
            System.out.println("Account Controller - Iniciando reset");
            accountService.reset();
            sendResponse(exchange, 200, "OK");
        }

        if (method.equals("GET") && path.equals("/balance")) {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String accountId = query.split("=")[1];
            System.out.println("Account Controller - Buscando saldo da conta: " + accountId);
            try {
                BigDecimal balance = accountService.getBalance(accountId);
                sendResponse(exchange, 200, balance.toString());
            } catch (AccountNotFoundException e) {
                sendResponse(exchange, 404, "0");
            }
        } else if (method.equals("POST") && path.equals("/event")) {
            OperationEvent event;
            try {
                event = objectMapper.readValue(
                        exchange.getRequestBody(),
                        OperationEvent.class
                );
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 400, "Erro ao ler JSON");
                return;
            }

            Object response;
            switch (event.getType()) {
                case DEPOSIT -> {
                    System.out.println("Account Controller - Iniciando deposito. Conta destino: " + event.getDestination() + " valor: " + event.getAmount());
                    response = accountService.deposit(event);
                }
                case TRANSFER -> {
                    try{
                        System.out.println("Account Controller - Iniciando transferencia. Conta origem: " + event.getOrigin()
                                + " Conta destino: " + event.getDestination()
                                + " valor: " + event.getAmount());
                        response = accountService.transfer(event);
                    }catch (AccountNotFoundException e){
                        sendResponse(exchange, 404, "0");
                        return;
                    }
                }
                case WITHDRAW -> {
                   try{
                       System.out.println("Account Controller - Iniciando saque. Conta origem: " + event.getOrigin() + " valor: " + event.getAmount());
                        response = accountService.withdraw(event);
                    }catch (AccountNotFoundException e){
                        sendResponse(exchange, 404, "0");
                        return;
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + event.getType());
            }
            String json = objectMapper.writeValueAsString(response);
            sendResponse(exchange, 201, json);
        }

        sendResponse(exchange, 404, "Endpoint not found");
    }

    private void sendResponse(HttpExchange exchange,int statusCode,String body) throws IOException{
        byte[] bodyByte = body.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, bodyByte.length);

        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(bodyByte);
        }
    }
}
