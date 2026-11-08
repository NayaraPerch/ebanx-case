package br.com.ebanx.model;

public class TransferResponse {
    private final Account origin;
    private final Account destination;

    public TransferResponse(Account origin, Account destination) {
        this.origin = origin;
        this.destination = destination;
    }

    public Account getOrigin() {
        return origin;
    }

    public Account getDestination() {
        return destination;
    }
}
