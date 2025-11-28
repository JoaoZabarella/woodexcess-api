package com.z.c.woodexcess_api.exception.address;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String message) {
        super(message);
    }

    public AddressNotFoundException(UUID id) {
        super("Address not found with id: " + id);
    }
}
