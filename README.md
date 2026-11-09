# Desafio Backend EBANX

## Tecnologias

- Java 21
- Spring Boot
- Spring JDBC
- H2 Database
- Maven
- JUnit 5 e Mockito

## Como executar

**Pré-requisitos:** Java 21 e Maven.

Clone o repositório:

```bash
git clone https://github.com/NayaraPerch/ebanx-case.git
cd ebanx-case
```

### Pelo terminal

Execute a aplicação com:

```bash
mvn spring-boot:run
```

### Pela IDE

Importe o projeto como um projeto Maven e execute o método `main()` da classe `br.com.ebanx.Application`.

A aplicação estará disponível em:

```text
http://localhost:8080
```

## Executar os testes

Para executar todos os testes:

```bash
mvn test
```

## Principais decisões técnicas

- **Spring Boot:** utilizado para simplificar a configuração da aplicação e a exposição dos endpoints REST.
- **Spring JDBC:** utilizado para acesso ao banco de dados por meio do `JdbcTemplate`.
- **H2 Database:** banco de dados relacional em memória, atendendo ao requisito de não exigir persistência durável.
- **Transações:** utilização de `@Transactional` na operação de transferência para garantir a atomicidade das atualizações das contas.
- **Abstração do repositório:** utilização da interface `AccountRepositoryInterface` para desacoplar as regras de negócio da implementação de armazenamento.
- **Injeção de dependência:** utilização de injeção via construtor para reduzir o acoplamento e facilitar os testes.
