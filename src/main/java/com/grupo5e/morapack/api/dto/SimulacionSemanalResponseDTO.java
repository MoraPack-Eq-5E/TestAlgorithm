package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta con resultado de simulación semanal")
public class SimulacionSemanalResponseDTO {

    @Schema(description = "ID único de la simulación", example = "1")
    private Long simulacionId;

    @Schema(description = "Estado de la simulación", example = "COMPLETADA")
    private String estado;

    @Schema(description = "Fecha y hora de inicio", example = "2024-01-15T10:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha y hora de finalización", example = "2024-01-15T11:30:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Duración de la ejecución en milisegundos", example = "5400000")
    private Long duracionMs;

    @Schema(description = "Duración formateada (HH:mm:ss)", example = "01:30:00")
    private String duracionFormateada;

    @Schema(description = "Total de pedidos procesados", example = "150")
    private Integer totalPedidos;

    @Schema(description = "Pedidos asignados exitosamente", example = "145")
    private Integer pedidosAsignados;

    @Schema(description = "Pedidos no asignados", example = "5")
    private Integer pedidosNoAsignados;

    @Schema(description = "Porcentaje de asignación", example = "96.67")
    private Double porcentajeAsignacion;

    @Schema(description = "Costo total de la solución", example = "125000.50")
    private Double costoTotal;

    @Schema(description = "Tiempo promedio de entrega en horas", example = "24.5")
    private Double tiempoPromedioEntrega;

    @Schema(description = "Peso (fitness) de la solución", example = "1500000")
    private Integer pesoSolucion;

    @Schema(description = "Indica si la solución es válida", example = "true")
    private Boolean solucionValida;

    @Schema(description = "Progreso de la simulación (0-100%)", example = "100")
    private Integer progreso;

    @Schema(description = "Mensaje de error si la simulación falló")
    private String mensajeError;

    @Schema(description = "Tiempo inicial de referencia (T0) de la simulación")
    private LocalDateTime tiempoInicialReferencia;

    @Schema(description = "Días simulados", example = "7")
    private Integer tiempoSimuladoDias;

    @Schema(description = "Iteraciones del ALNS ejecutadas", example = "1000")
    private Integer iteracionesAlns;

    @Schema(description = "Solución: Map de pedidoId -> lista de vueloIds en orden")
    private Map<Long, List<Integer>> solucion;

    @Schema(description = "IDs de pedidos no asignados")
    private List<Long> pedidosNoAsignadosIds;

    @Schema(description = "Estadísticas adicionales de la simulación")
    private EstadisticasSimulacionDTO estadisticas;
}

