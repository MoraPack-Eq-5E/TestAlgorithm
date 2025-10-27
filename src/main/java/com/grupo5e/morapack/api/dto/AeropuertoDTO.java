package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un aeropuerto")
public class AeropuertoDTO {
    
    @Schema(description = "ID único del aeropuerto", example = "1")
    private Long id;

    @NotBlank(message = "El código IATA es obligatorio")
    @Size(min = 4, max = 4, message = "El código IATA debe tener exactamente 4 caracteres")
    @Pattern(regexp = "^[A-Z]{4}$", message = "El código IATA debe contener solo letras mayúsculas")
    @Schema(description = "Código IATA del aeropuerto", example = "SKBO", required = true)
    private String codigoIATA;

    @NotNull(message = "La zona horaria UTC es obligatoria")
    @Min(value = -12, message = "La zona horaria debe estar entre -12 y +14")
    @Max(value = 14, message = "La zona horaria debe estar entre -12 y +14")
    @Schema(description = "Zona horaria UTC", example = "-5", required = true)
    private Integer zonaHorariaUTC;

    @Schema(description = "Latitud del aeropuerto", example = "4.7110")
    private String latitud;

    @Schema(description = "Longitud del aeropuerto", example = "-74.0721")
    private String longitud;

    @Min(value = 0, message = "La capacidad actual no puede ser negativa")
    @Schema(description = "Capacidad actual utilizada", example = "150")
    private Integer capacidadActual;

    @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    @Schema(description = "Capacidad máxima del aeropuerto", example = "500", required = true)
    private Integer capacidadMaxima;

    @NotNull(message = "El ID de ciudad es obligatorio")
    @Schema(description = "ID de la ciudad donde se encuentra el aeropuerto", example = "1", required = true)
    private Integer ciudadId;
    
    @Schema(description = "Información básica de la ciudad asociada")
    private CiudadSimpleDTO ciudad;

    @Schema(description = "Estado actual del aeropuerto", example = "OPERATIVO")
    private EstadoAeropuerto estado;
}

