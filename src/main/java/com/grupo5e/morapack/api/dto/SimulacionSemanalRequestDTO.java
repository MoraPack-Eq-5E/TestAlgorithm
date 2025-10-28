package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para iniciar una simulación semanal")
public class SimulacionSemanalRequestDTO {

    @Builder.Default
    @Schema(description = "Número de días a simular", example = "7")
    @Min(value = 1, message = "Debe simular al menos 1 día")
    @Max(value = 30, message = "No se puede simular más de 30 días")
    private Integer diasSimulacion = 7;

    @Builder.Default
    @Schema(description = "Tiempo límite de ejecución del ALNS en segundos (0 = sin límite)", example = "5400")
    @Min(value = 0, message = "El tiempo límite no puede ser negativo")
    private Integer tiempoLimiteSegundos = 5400; // 90 minutos por defecto

    @Builder.Default
    @Schema(description = "Número de iteraciones del algoritmo ALNS", example = "1000")
    @Min(value = 1, message = "Debe haber al menos 1 iteración")
    private Integer iteracionesAlns = 1000;

    @Schema(description = "IDs de pedidos específicos a incluir (vacío = todos los pedidos disponibles)")
    private List<Long> pedidosIds;

    @Builder.Default
    @Schema(description = "Habilitar unitización de productos", example = "true")
    private Boolean habilitarUnitizacion = true;

    @Builder.Default
    @Schema(description = "Modo de logging verboso para debug", example = "false")
    private Boolean modoDebug = false;

    @Builder.Default
    @Schema(description = "Factor de aceleración para la visualización (1x, 10x, 100x, etc.)", example = "100")
    @Min(value = 1, message = "El factor de aceleración debe ser al menos 1")
    private Integer factorAceleracion = 100; // 1 segundo real = 100 minutos simulados
}

