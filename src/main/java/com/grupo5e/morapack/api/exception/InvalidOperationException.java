package com.grupo5e.morapack.api.exception;

/**
 * Excepción lanzada cuando se intenta realizar una operación inválida
 */
public class InvalidOperationException extends RuntimeException {
    
    public InvalidOperationException(String mensaje) {
        super(mensaje);
    }
}

