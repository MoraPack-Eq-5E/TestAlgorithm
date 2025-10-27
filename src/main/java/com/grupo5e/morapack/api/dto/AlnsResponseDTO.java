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
@Schema(description = "DTO de respuesta con los resultados del algoritmo ALNS")
public class AlnsResponseDTO {

    @Schema(description = "ID único de la ejecución", example = "1")
    private Long ejecucionId;

    @Schema(description = "Fecha y hora de inicio de la ejecución", example = "2024-01-15T10:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha y hora de finalización de la ejecución", example = "2024-01-15T10:05:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Duración de la ejecución en milisegundos", example = "300000")
    private Long duracionMs;

    @Schema(description = "Estado de la ejecución", example = "COMPLETADO")
    private String estado;

    @Schema(description = "Número total de pedidos procesados", example = "150")
    private Integer totalPedidos;

    @Schema(description = "Número de pedidos asignados exitosamente", example = "145")
    private Integer pedidosAsignados;

    @Schema(description = "Número de pedidos no asignados", example = "5")
    private Integer pedidosNoAsignados;

    @Schema(description = "Costo total de la solución", example = "125000.50")
    private Double costoTotal;

    @Schema(description = "Tiempo total de entrega promedio en horas", example = "24.5")
    private Double tiempoPromedioEntrega;

    @Schema(description = "Solución optimizada: Map de pedidoId -> lista de vuelosIds")
    private Map<Long, List<Integer>> solucion;

    @Schema(description = "Lista de IDs de pedidos no asignados")
    private List<Long> pedidosNoAsignadosIds;

    @Schema(description = "Indica si la solución es válida", example = "true")
    private Boolean solucionValida;

    @Schema(description = "Indica si se respetan las capacidades", example = "true")
    private Boolean capacidadValida;

    @Schema(description = "Mensaje descriptivo del resultado")
    private String mensaje;

    @Schema(description = "Detalles adicionales o advertencias")
    private List<String> advertencias;

    @Schema(description = "Número de iteraciones ejecutadas", example = "1000")
    private Integer iteracionesEjecutadas;

    @Schema(description = "Mejor costo encontrado durante la ejecución", example = "120000.00")
    private Double mejorCosto;
}

