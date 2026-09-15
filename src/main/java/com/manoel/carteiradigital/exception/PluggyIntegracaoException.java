package com.manoel.carteiradigital.exception;

public class PluggyIntegracaoException extends RuntimeException {
    public PluggyIntegracaoException(String message) {
        super(message);
    }

    public PluggyIntegracaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
