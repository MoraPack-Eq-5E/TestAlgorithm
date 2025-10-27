package com.grupo5e.morapack.api.exception;

/**
 * Excepción lanzada cuando se intenta crear un recurso que ya existe
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String mensaje) {
        super(mensaje);
    }
    
    public DuplicateResourceException(String recurso, String campo, Object valor) {
        super(String.format("%s ya existe con %s: '%s'", recurso, campo, valor));
    }
}

