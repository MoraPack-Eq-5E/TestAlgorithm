package com.grupo5e.morapack.simulation.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa un evento que ocurre durante la simulación
 * (despegues, aterrizajes, alertas, etc.)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationEvent {
    
    /**
     * ID único del evento
     */
    private String id;
    
    /**
     * Tipo de evento
     */
    private EventType type;
    
    /**
     * Mensaje descriptivo del evento
     */
    private String message;
    
    /**
     * Tiempo simulado cuando ocurrió el evento
     */
    private LocalDateTime simulatedTime;
    
    /**
     * Tiempo real cuando se generó el evento
     */
    private LocalDateTime realTime;
    
    /**
     * ID del vuelo relacionado (si aplica)
     */
    private Integer relatedFlightId;
    
    /**
     * ID del pedido relacionado (si aplica)
     */
    private Long relatedOrderId;
    
    /**
     * Código de aeropuerto relacionado (si aplica)
     */
    private String relatedAirportCode;
}

