package com.grupo5e.morapack.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * Representa un vuelo activo (en el aire) en un momento específico
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vuelo actualmente en el aire")
public class ActiveFlightDTO {
    
    @Schema(description = "ID del vuelo", example = "123")
    private Integer flightId;
    
    @Schema(description = "Código del vuelo", example = "MP-123")
    private String flightCode;
    
    @Schema(description = "Latitud actual (interpolada)", example = "-12.5432")
    private Double currentLat;
    
    @Schema(description = "Longitud actual (interpolada)", example = "-77.0123")
    private Double currentLng;
    
    @Schema(description = "Latitud de origen", example = "-12.0219")
    private Double originLat;
    
    @Schema(description = "Longitud de origen", example = "-77.0433")
    private Double originLng;
    
    @Schema(description = "Latitud de destino", example = "50.9014")
    private Double destinationLat;
    
    @Schema(description = "Longitud de destino", example = "4.4844")
    private Double destinationLng;
    
    @Schema(description = "Código IATA origen", example = "SPIM")
    private String originCode;
    
    @Schema(description = "Código IATA destino", example = "EBCI")
    private String destinationCode;
    
    @Schema(description = "Ciudad origen", example = "Lima")
    private String originCity;
    
    @Schema(description = "Ciudad destino", example = "Bruselas")
    private String destinationCity;
    
    @Schema(description = "Estado del vuelo", example = "IN_FLIGHT")
    private String status;
    
    @Schema(description = "Progreso del vuelo (0-100%)", example = "45.23")
    private Double progressPercentage;
    
    @Schema(description = "IDs de paquetes a bordo", example = "[1, 5, 12]")
    private List<Long> packagesOnBoard;
    
    @Schema(description = "Capacidad utilizada", example = "150")
    private Integer capacityUsed;
    
    @Schema(description = "Capacidad máxima", example = "200")
    private Integer capacityMax;
    
    @Schema(description = "Porcentaje de ocupación", example = "75.0")
    private Double occupancyPercentage;
}

