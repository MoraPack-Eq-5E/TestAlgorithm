package com.grupo5e.morapack.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Estado de un almacén/aeropuerto en un momento específico
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estado actual de un almacén")
public class WarehouseStateDTO {
    
    @Schema(description = "ID del almacén", example = "1")
    private Long warehouseId;
    
    @Schema(description = "Código IATA", example = "SPIM")
    private String code;
    
    @Schema(description = "Nombre de la ciudad", example = "Lima")
    private String cityName;
    
    @Schema(description = "Latitud", example = "-12.0219")
    private Double latitude;
    
    @Schema(description = "Longitud", example = "-77.0433")
    private Double longitude;
    
    @Schema(description = "Capacidad total", example = "5000")
    private Integer capacity;
    
    @Schema(description = "Ocupación actual", example = "3250")
    private Integer current;
    
    @Schema(description = "Espacio disponible", example = "1750")
    private Integer available;
    
    @Schema(description = "Porcentaje de ocupación", example = "65.0")
    private Double occupancyPercentage;
    
    @Schema(description = "Estado del almacén", example = "NORMAL")
    private String status;
    
    @Schema(description = "¿Es aeropuerto principal?", example = "true")
    private Boolean isPrincipal;
}

