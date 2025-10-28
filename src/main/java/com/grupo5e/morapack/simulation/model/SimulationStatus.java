package com.grupo5e.morapack.simulation.model;

/**
 * Estados posibles de una simulación en tiempo real
 */
public enum SimulationStatus {
    /**
     * Simulación en ejecución normal
     */
    RUNNING,
    
    /**
     * Simulación pausada temporalmente
     */
    PAUSED,
    
    /**
     * Simulación detenida (no se puede resumir)
     */
    STOPPED,
    
    /**
     * Simulación completada exitosamente
     */
    COMPLETED
}

