package com.grupo5e.morapack.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Métricas generales de la simulación
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Métricas de la simulación")
public class MetricsDTO {
    
    @Schema(description = "Total de vuelos", example = "45")
    private Integer totalFlights;
    
    @Schema(description = "Vuelos programados (no despegaron)", example = "12")
    private Integer flightsScheduled;
    
    @Schema(description = "Vuelos en el aire", example = "8")
    private Integer flightsInAir;
    
    @Schema(description = "Vuelos completados", example = "25")
    private Integer flightsCompleted;
    
    @Schema(description = "Total de pedidos", example = "150")
    private Integer totalOrders;
    
    @Schema(description = "Pedidos entregados", example = "85")
    private Integer ordersDelivered;
    
    @Schema(description = "Pedidos en tránsito", example = "45")
    private Integer ordersInTransit;
    
    @Schema(description = "Pedidos esperando", example = "20")
    private Integer ordersWaiting;
    
    @Schema(description = "Porcentaje cumplimiento SLA", example = "92.5")
    private Double slaCompliancePercentage;
    
    @Schema(description = "Ocupación promedio almacenes", example = "67.3")
    private Double averageWarehouseOccupancy;
}

