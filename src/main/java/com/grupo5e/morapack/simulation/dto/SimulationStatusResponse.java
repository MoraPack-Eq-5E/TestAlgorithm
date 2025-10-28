package com.grupo5e.morapack.simulation.dto;

import com.grupo5e.morapack.simulation.model.SimulationEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta del endpoint de polling /api/simulations/{id}/status
 * Se actualiza cada 2-3 segundos desde el frontend
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estado actual de la simulación para polling en tiempo real")
public class SimulationStatusResponse {
    
    @Schema(description = "ID de la simulación", example = "1")
    private Long simulationId;
    
    @Schema(description = "Estado actual", example = "RUNNING")
    private String status;
    
    @Schema(description = "Tiempo simulado actual (fecha/hora en la simulación)", example = "2025-01-20T14:30:00")
    private LocalDateTime currentSimulatedTime;
    
    @Schema(description = "Segundos simulados transcurridos desde T0", example = "23400")
    private Long elapsedSimulatedSeconds;
    
    @Schema(description = "Progreso total de la simulación (0-100%)", example = "38.5")
    private Double progressPercentage;
    
    @Schema(description = "Día actual en la simulación", example = "2")
    private Integer currentDay;
    
    @Schema(description = "Hora actual en la simulación (0-23)", example = "14")
    private Integer currentHour;
    
    @Schema(description = "Minuto actual en la simulación (0-59)", example = "30")
    private Integer currentMinute;
    
    @Schema(description = "Vuelos actualmente EN EL AIRE (solo in_flight)")
    private List<ActiveFlightDTO> activeFlights;
    
    @Schema(description = "Estado actual de todos los almacenes")
    private List<WarehouseStateDTO> warehouses;
    
    @Schema(description = "Métricas generales de la simulación")
    private MetricsDTO metrics;
    
    @Schema(description = "Eventos recientes (últimos 20)")
    private List<SimulationEvent> recentEvents;
    
    @Schema(description = "Información del factor de aceleración", example = "112")
    private Integer timeScale;
}

