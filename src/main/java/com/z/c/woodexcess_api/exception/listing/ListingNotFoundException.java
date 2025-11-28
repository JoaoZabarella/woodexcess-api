package com.z.c.woodexcess_api.exception.listing;

import com.z.c.woodexcess_api.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exceção lançada quando um anúncio não é encontrado
 */
public class ListingNotFoundException extends ResourceNotFoundException {

    public ListingNotFoundException(UUID id) {
        super("Listing not found with id: " + id);
    }

    public ListingNotFoundException(String message) {
        super(message);
    }
}
