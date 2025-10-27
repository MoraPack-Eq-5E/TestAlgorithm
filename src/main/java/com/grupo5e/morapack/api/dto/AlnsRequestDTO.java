package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para solicitud de ejecución del algoritmo ALNS")
public class AlnsRequestDTO {

    @Schema(description = "Lista de IDs de pedidos a optimizar (vacío = todos los pedidos pendientes)")
    private List<Long> pedidosIds;

    @Min(value = 1, message = "El número de iteraciones debe ser al menos 1")
    @Max(value = 10000, message = "El número de iteraciones no puede exceder 10000")
    @Schema(description = "Número de iteraciones del algoritmo", example = "1000")
    private Integer iteraciones;

    @Min(value = 1, message = "El tiempo límite debe ser al menos 1 segundo")
    @Schema(description = "Tiempo límite de ejecución en segundos (0 = sin límite)", example = "300")
    private Integer tiempoLimiteSegundos;

    @Schema(description = "Habilitar modo de unitización de productos", example = "true")
    private Boolean habilitarUnitizacion;

    @Schema(description = "Modo de logging verboso para debug", example = "false")
    private Boolean modoDebug;

    @Schema(description = "Días de horizonte para planificación", example = "4")
    @Min(value = 1, message = "El horizonte debe ser al menos 1 día")
    @Max(value = 30, message = "El horizonte no puede exceder 30 días")
    private Integer diasHorizonte;
}

