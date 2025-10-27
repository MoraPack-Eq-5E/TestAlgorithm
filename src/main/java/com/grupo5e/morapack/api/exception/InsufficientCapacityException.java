package com.grupo5e.morapack.api.exception;

/**
 * Excepción lanzada cuando no hay capacidad suficiente (vuelo, aeropuerto, etc.)
 */
public class InsufficientCapacityException extends RuntimeException {
    
    public InsufficientCapacityException(String mensaje) {
        super(mensaje);
    }
    
    public InsufficientCapacityException(String recurso, int requerida, int disponible) {
        super(String.format("Capacidad insuficiente en %s. Requerida: %d, Disponible: %d", 
            recurso, requerida, disponible));
    }
}

