package com.bestbuy.api.exception;

public class ReadOnlyException extends RuntimeException {
    public ReadOnlyException() {
        super("Provider is configured read-only");
    }
}
