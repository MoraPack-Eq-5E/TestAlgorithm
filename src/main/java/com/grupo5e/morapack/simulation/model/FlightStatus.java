package com.grupo5e.morapack.simulation.model;

/**
 * Estados posibles de un vuelo durante la simulación
 */
public enum FlightStatus {
    /**
     * Vuelo aún no ha despegado (esperando en origen)
     */
    SCHEDULED,
    
    /**
     * Vuelo en camino (entre origen y destino)
     */
    IN_FLIGHT,
    
    /**
     * Vuelo completado (llegó a destino)
     */
    LANDED
}

