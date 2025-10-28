package com.grupo5e.morapack.simulation.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa el estado de un vuelo en un momento específico de la simulación.
 * Se usa para tracking en tiempo real durante la visualización.
 * 
 * Este objeto se mantiene en MEMORIA (no en BD) y se actualiza en cada request
 * del frontend para calcular la posición actual mediante interpolación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSnapshot {
    
    /**
     * ID del vuelo (referencia a entidad Vuelo en BD)
     */
    private Integer flightId;
    
    /**
     * Código identificador del vuelo para mostrar en UI
     */
    private String flightCode;
    
    /**
     * Ruta del vuelo: [[longitudOrigen, latitudOrigen], [longitudDestino, latitudDestino]]
     * Formato GeoJSON estándar
     */
    private double[][] route;
    
    /**
     * Coordenadas de origen
     */
    private double originLat;
    private double originLng;
    
    /**
     * Coordenadas de destino
     */
    private double destinationLat;
    private double destinationLng;
    
    /**
     * POSICIÓN ACTUAL CALCULADA (se actualiza en cada request)
     * Esto es lo que mueve el ícono del avión en el mapa
     */
    private double currentLat;
    private double currentLng;
    
    /**
     * Tiempo simulado de salida y llegada
     */
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    
    /**
     * Estado actual del vuelo
     */
    private FlightStatus status;
    
    /**
     * Progreso del vuelo (0.0 a 1.0)
     * 0.0 = en origen, 0.5 = mitad del camino, 1.0 = en destino
     */
    private double progress;
    
    /**
     * Porcentaje de progreso para UI (0-100)
     */
    private double progressPercentage;
    
    /**
     * Paquetes a bordo de este vuelo
     */
    private List<Long> packagesOnBoard;
    
    /**
     * Capacidad utilizada y máxima
     */
    private int capacityUsed;
    private int capacityMax;
    
    /**
     * Porcentaje de ocupación (0-100)
     */
    private double occupancyPercentage;
    
    /**
     * Información adicional para UI
     */
    private String originCode;
    private String destinationCode;
    private String originCity;
    private String destinationCity;
    
    /**
     * Duración del vuelo en minutos
     */
    private int durationMinutes;
}

