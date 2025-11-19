package com.z.c.woodexcess_api.exception.address;

public class CepApiException extends RuntimeException {
    public CepApiException(String message) {
        super(message);
    }
    public CepApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
