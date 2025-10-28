package com.grupo5e.morapack.simulation.model;

import lombok.*;

/**
 * Representa el estado de un almacén/aeropuerto en un momento específico.
 * Se usa para tracking de capacidad y paquetes en tránsito.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseSnapshot {
    
    /**
     * ID del aeropuerto (referencia a entidad Aeropuerto en BD)
     */
    private Long warehouseId;
    
    /**
     * Código IATA del aeropuerto
     */
    private String code;
    
    /**
     * Nombre de la ciudad
     */
    private String cityName;
    
    /**
     * Coordenadas fijas del aeropuerto (no cambian durante simulación)
     */
    private double latitude;
    private double longitude;
    
    /**
     * Capacidad total del almacén
     */
    private int capacity;
    
    /**
     * Ocupación actual
     */
    private int currentOccupancy;
    
    /**
     * Espacio disponible
     */
    private int available;
    
    /**
     * Porcentaje de ocupación (0-100)
     */
    private double occupancyPercentage;
    
    /**
     * Estado del almacén basado en ocupación
     */
    private WarehouseStatus status;
    
    /**
     * Paquetes actualmente en este almacén
     */
    private int packagesInWarehouse;
    
    /**
     * Paquetes en tránsito hacia este almacén
     */
    private int packagesInTransit;
    
    /**
     * Paquetes cuyo destino final es este almacén
     */
    private int packagesAtDestination;
    
    /**
     * Indica si es un aeropuerto principal de MoraPack
     */
    private boolean isPrincipal;
}

