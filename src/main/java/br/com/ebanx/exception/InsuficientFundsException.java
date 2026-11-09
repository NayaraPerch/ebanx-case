package br.com.ebanx.exception;

public class InsuficientFundsException extends RuntimeException{
    public InsuficientFundsException(String message) {
        super(message);
    }
}
