package com.z.c.woodexcess_api.exception.listing;

import com.z.c.woodexcess_api.exception.BusinessException;

/**
 * Exceção lançada quando um usuário tenta acessar ou modificar um anúncio sem
 * permissão
 */
public class UnauthorizedListingAccessException extends BusinessException {

    public UnauthorizedListingAccessException() {
        super("You are not authorized to access or modify this listing");
    }

    public UnauthorizedListingAccessException(String message) {
        super(message);
    }
}
