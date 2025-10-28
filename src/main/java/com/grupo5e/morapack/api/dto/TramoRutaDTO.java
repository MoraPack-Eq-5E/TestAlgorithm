package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para un tramo (vuelo) en la ruta de un paquete")
public class TramoRutaDTO {

    @Schema(description = "Secuencia del tramo en la ruta", example = "1")
    private Integer secuencia;

    @Schema(description = "ID del vuelo", example = "15")
    private Integer vueloId;

    @Schema(description = "Código IATA del origen", example = "SKBO")
    private String codigoOrigen;

    @Schema(description = "Código IATA del destino", example = "SPIM")
    private String codigoDestino;

    @Schema(description = "Ciudad origen", example = "Bogota")
    private String ciudadOrigen;

    @Schema(description = "Ciudad destino", example = "Lima")
    private String ciudadDestino;

    @Schema(description = "Minuto de inicio desde T0", example = "120")
    private Integer minutoInicio;

    @Schema(description = "Minuto de fin desde T0", example = "480")
    private Integer minutoFin;

    @Schema(description = "Duración del vuelo en horas", example = "6.0")
    private Double duracionHoras;

    @Schema(description = "Latitud del origen", example = "4.7011")
    private Double latitudOrigen;

    @Schema(description = "Longitud del origen", example = "-74.1469")
    private Double longitudOrigen;

    @Schema(description = "Latitud del destino", example = "-12.0219")
    private Double latitudDestino;

    @Schema(description = "Longitud del destino", example = "-77.1147")
    private Double longitudDestino;
}

