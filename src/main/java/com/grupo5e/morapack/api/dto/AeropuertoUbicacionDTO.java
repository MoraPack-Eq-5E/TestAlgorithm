package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para ubicación de aeropuerto en el mapa")
public class AeropuertoUbicacionDTO {

    @Schema(description = "ID del aeropuerto", example = "1")
    private Long id;

    @Schema(description = "Código IATA", example = "SKBO")
    private String codigoIATA;

    @Schema(description = "Nombre de la ciudad", example = "Bogota")
    private String nombreCiudad;

    @Schema(description = "País", example = "Colombia")
    private String pais;

    @Schema(description = "Continente", example = "AMERICA")
    private String continente;

    @Schema(description = "Latitud en formato decimal", example = "4.701594")
    private Double latitud;

    @Schema(description = "Longitud en formato decimal", example = "-74.146947")
    private Double longitud;

    @Schema(description = "Zona horaria UTC", example = "-5")
    private Integer zonaHorariaUTC;

    @Schema(description = "Capacidad actual utilizada", example = "120")
    private Integer capacidadActual;

    @Schema(description = "Capacidad máxima", example = "430")
    private Integer capacidadMaxima;

    @Schema(description = "Porcentaje de ocupación", example = "27.91")
    private Double porcentajeOcupacion;

    @Schema(description = "Estado del aeropuerto", example = "DISPONIBLE")
    private String estado;

    @Schema(description = "Es un aeropuerto principal de MoraPack", example = "true")
    private Boolean esPrincipal;
}

