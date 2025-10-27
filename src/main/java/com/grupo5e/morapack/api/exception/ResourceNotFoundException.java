package com.grupo5e.morapack.api.exception;

/**
 * Excepción lanzada cuando un recurso no se encuentra en la base de datos
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
    
    public ResourceNotFoundException(String recurso, String campo, Object valor) {
        super(String.format("%s no encontrado con %s: '%s'", recurso, campo, valor));
    }
}

