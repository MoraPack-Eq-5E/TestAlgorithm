package com.grupo5e.morapack.api.exception;

/**
 * Excepción lanzada cuando hay un error en la ejecución del algoritmo ALNS
 */
public class AlnsExecutionException extends RuntimeException {
    
    public AlnsExecutionException(String mensaje) {
        super(mensaje);
    }
    
    public AlnsExecutionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

