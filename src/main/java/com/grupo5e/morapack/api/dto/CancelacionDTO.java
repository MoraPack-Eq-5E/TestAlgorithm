package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar una cancelación de vuelo")
public class CancelacionDTO {

    @Schema(description = "ID único de la cancelación", example = "1")
    private Long id;

    @NotNull(message = "Los días de cancelación son obligatorios")
    @Min(value = 0, message = "Los días de cancelación no pueden ser negativos")
    @Schema(description = "Días desde el inicio del periodo hasta la cancelación", example = "5", required = true)
    private Integer diasCancelado;

    @NotBlank(message = "El código IATA origen es obligatorio")
    @Size(min = 4, max = 4, message = "El código IATA debe tener 4 caracteres")
    @Schema(description = "Código IATA del aeropuerto origen", example = "SKBO", required = true)
    private String codigoIATAOrigen;

    @NotBlank(message = "El código IATA destino es obligatorio")
    @Size(min = 4, max = 4, message = "El código IATA debe tener 4 caracteres")
    @Schema(description = "Código IATA del aeropuerto destino", example = "SEQM", required = true)
    private String codigoIATADestino;

    @NotNull(message = "La hora es obligatoria")
    @Min(value = 0, message = "La hora debe estar entre 0 y 23")
    @Max(value = 23, message = "La hora debe estar entre 0 y 23")
    @Schema(description = "Hora de la cancelación (0-23)", example = "15", required = true)
    private Integer hora;

    @NotNull(message = "El minuto es obligatorio")
    @Min(value = 0, message = "El minuto debe estar entre 0 y 59")
    @Max(value = 59, message = "El minuto debe estar entre 0 y 59")
    @Schema(description = "Minuto de la cancelación (0-59)", example = "30", required = true)
    private Integer minuto;

    @Schema(description = "Hora completa de la cancelación", example = "15:30")
    private LocalTime horaCancelacion;

    @Schema(description = "Fecha y hora completa de la cancelación", example = "2024-01-15T15:30:00")
    private LocalDateTime fechaHoraCancelacion;

    @Schema(description = "ID del vuelo cancelado (si está asociado)", example = "123")
    private Integer vueloId;

    @Schema(description = "Identificador del vuelo afectado", example = "SKBO-SEQM-15:30")
    private String identificadorVuelo;
}

