package com.grupo5e.morapack.simulation.model;

/**
 * Estados de un almacén basados en su nivel de ocupación
 */
public enum WarehouseStatus {
    /**
     * Ocupación < 70% - Todo normal
     */
    NORMAL,
    
    /**
     * Ocupación 70-90% - Advertencia
     */
    WARNING,
    
    /**
     * Ocupación 90-99% - Crítico
     */
    CRITICAL,
    
    /**
     * Ocupación 100% - Lleno
     */
    FULL
}

