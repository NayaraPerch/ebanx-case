# Desafio Backend EBANX

## Como executar

**Pré-requisitos:** Java 21 e Maven.

Clone o repositório:

```bash
git clone https://github.com/NayaraPerch/ebanx-case.git
cd ebanx-case
```

Execute os testes:

```bash
mvn test
```

Para iniciar a aplicação, execute o método `main()` da classe `br.com.ebanx.Application` pela sua IDE.

O servidor HTTP estará disponível em `http://localhost:8080`.

## Principais decisões

- **Abstração do repositório:** utilização da interface `AccountRepository` para desacoplar as regras de negócio da implementação de armazenamento, permitindo substituir a implementação sem modificar o serviço.

- **Injeção de dependência:** utilização de injeção via construtor para explicitar as dependências, facilitar os testes e reduzir o acoplamento entre as classes.
