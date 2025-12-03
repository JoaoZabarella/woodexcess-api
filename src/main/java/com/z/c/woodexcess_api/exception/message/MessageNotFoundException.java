package com.z.c.woodexcess_api.exception.message;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(String message) {
        super(message);
    }
    public MessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
