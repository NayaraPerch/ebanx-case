package br.com.ebanx.model;

public class DepositResponse {
    private Account destination;

    public DepositResponse(Account destination) {
        this.destination = destination;
    }

    public Account getDestination() {
        return destination;
    }
}
