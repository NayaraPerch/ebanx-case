package br.com.ebanx.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TypeEnum {
    DEPOSIT,
    WITHDRAW,
    TRANSFER;

    @JsonCreator
    public static TypeEnum fromValue(String value) {
        return TypeEnum.valueOf(value.toUpperCase());
    }
}
