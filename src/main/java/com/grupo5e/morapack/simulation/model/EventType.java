package com.grupo5e.morapack.simulation.model;

/**
 * Tipos de eventos que pueden ocurrir durante la simulación
 */
public enum EventType {
    /**
     * Vuelo despegó de origen
     */
    FLIGHT_DEPARTURE,
    
    /**
     * Vuelo aterrizó en destino
     */
    FLIGHT_ARRIVAL,
    
    /**
     * Paquete fue entregado a su destino final
     */
    ORDER_DELIVERED,
    
    /**
     * Almacén alcanzó nivel de advertencia
     */
    WAREHOUSE_WARNING,
    
    /**
     * Almacén alcanzó nivel crítico
     */
    WAREHOUSE_CRITICAL,
    
    /**
     * Almacén llegó a capacidad máxima
     */
    WAREHOUSE_FULL,
    
    /**
     * Paquete puede incumplir SLA
     */
    SLA_RISK,
    
    /**
     * Información general
     */
    INFO
}

