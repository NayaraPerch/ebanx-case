package br.com.ebanx.controller;

import br.com.ebanx.model.Account;
import br.com.ebanx.repository.AccountRepository;
import br.com.ebanx.repository.InMemoryAccountRepository;
import br.com.ebanx.service.AccountService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountControllerTest {

    private HttpServer server;
    private AccountRepository repository;
    @BeforeEach
    void setUp() throws IOException {

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        repository = new InMemoryAccountRepository();
        AccountService service = new AccountService(repository);
        server.createContext("/balance", new AccountController(service));
        server.createContext("/event", new AccountController(service));
        server.createContext("/reset", new AccountController(service));
        server.start();

    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldReturn404WhenAccountDoesNotExist() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/balance?account_id=1234"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals("0", response.body());
    }
    @Test
    void shouldReturn200WhenBalanceAccount() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        repository.save(new Account("1234", new BigDecimal("150")));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/balance?account_id=1234"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("150", response.body());
    }

    @Test
    void shouldReturn201WhenDepositingIntoNonexistentAccount() throws IOException, InterruptedException {

        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"type\":\"deposit\", \"destination\":\"100\", \"amount\":10}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    @Test
    void shouldReturn201WhenDepositingIntoExistentAccount() throws IOException, InterruptedException {

        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"type\":\"deposit\", \"destination\":\"1234\", \"amount\":10}";
        repository.save(new Account("1234", new BigDecimal("150")));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    @Test
    void shouldReturn404WhenWithDrawNonExistentAccount() throws IOException, InterruptedException {

        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"type\":\"withdraw\", \"origin\":\"1234\", \"amount\":10}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals("0", response.body());
    }

    @Test
    void shouldReturn201WhenWithdrawingFromExistingAccount() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"type\":\"withdraw\", \"origin\":\"1234\", \"amount\":10}";
        repository.save(new Account("1234", new BigDecimal("150")));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    @Test
    void shouldReturn404WhenTransferFromNonExistentAccount() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"type\":\"transfer\", \"origin\":\"1234\", \"amount\":10, \"origin\":\"4567\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals("0", response.body());
    }

    @Test
    void shouldReturn201WhenTransferFromExistentAccountToNonExistentAccount() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();
        String json = "{\"type\":\"transfer\", \"origin\":\"1234\", \"amount\":10, \"destination\":\"4567\"}";
        repository.save(new Account("1234", new BigDecimal("150")));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:"+port+"/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // Valida o corpo da resposta
        String expectedJson = """
        {
            "origin": {
                "id": "1234",
                "balance": 140
            },
            "destination": {
                "id": "4567",
                "balance": 10
            }
        }
        """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expected = objectMapper.readTree(expectedJson);
        JsonNode actual = objectMapper.readTree(response.body());

        assertEquals(expected, actual);

       // Valida os saldos no repositório
        assertEquals(
                new BigDecimal("140"),
                repository.findAccountById("1234").get().getBalance()
        );

        assertEquals(
                new BigDecimal("10"),
                repository.findAccountById("4567").get().getBalance()
        );
        assertEquals(201, response.statusCode());
    }

    @Test
    void shouldReturn201WhenTransferBetweenTwoExistingAccounts() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();

        String json = """
            {
                "type": "transfer",
                "origin": "1234",
                "amount": 10,
                "destination": "4567"
            }
            """;

        repository.save(new Account("1234", new BigDecimal("150")));
        repository.save(new Account("4567", new BigDecimal("50")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/event"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(201, response.statusCode());

        String expectedJson = """
            {
                "origin": {
                    "id": "1234",
                    "balance": 140
                },
                "destination": {
                    "id": "4567",
                    "balance": 60
                }
            }
            """;

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode expected = objectMapper.readTree(expectedJson);
        JsonNode actual = objectMapper.readTree(response.body());

        assertEquals(expected, actual);

        // Valida os saldos no repositório
        assertEquals(
                new BigDecimal("140"),
                repository.findAccountById("1234").get().getBalance()
        );

        assertEquals(
                new BigDecimal("60"),
                repository.findAccountById("4567").get().getBalance()
        );
    }

    @Test
    void shouldReturn200AndResetAllAccounts() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();

        repository.save(new Account("1234", new BigDecimal("150")));
        repository.save(new Account("4567", new BigDecimal("50")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());

        assertTrue(repository.findAccountById("1234").isEmpty());
        assertTrue(repository.findAccountById("4567").isEmpty());
    }

    @Test
    void shouldReturn200WhenResettingEmptyRepository() throws IOException, InterruptedException {
        int port = server.getAddress().getPort();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }
}
