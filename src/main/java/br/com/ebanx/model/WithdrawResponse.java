package br.com.ebanx.model;

public class WithdrawResponse {
    private Account origin;

    public WithdrawResponse(Account origin) {
        this.origin = origin;
    }

    public Account getOrigin() {
        return origin;
    }
}
