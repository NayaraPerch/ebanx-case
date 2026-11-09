package br.com.ebanx.model;

import br.com.ebanx.exception.InsuficientFundsException;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {

    private String id;
    private BigDecimal balance;

    public Account(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public void deposit(BigDecimal amount){

        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if(this.balance.subtract(amount).compareTo(BigDecimal.ZERO) < 0){
            System.out.println("Saldo insuficiente. Saldo: " + this.balance + "valor a ser sacado: " + amount + " conta: " + this.id);
            throw new InsuficientFundsException("Saldo insuficiente");
        }else{
            this.balance = this.balance.subtract(amount);
        }
    }

    public String getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
