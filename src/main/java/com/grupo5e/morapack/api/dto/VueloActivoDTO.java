package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para un vuelo activo en la simulación")
public class VueloActivoDTO {

    @Schema(description = "ID del vuelo", example = "1")
    private Integer vueloId;

    @Schema(description = "Código del vuelo", example = "MP-350")
    private String codigoVuelo;

    @Schema(description = "Código IATA del aeropuerto origen", example = "SKBO")
    private String codigoOrigen;

    @Schema(description = "Código IATA del aeropuerto destino", example = "SPIM")
    private String codigoDestino;

    @Schema(description = "Nombre de la ciudad origen", example = "Bogota")
    private String ciudadOrigen;

    @Schema(description = "Nombre de la ciudad destino", example = "Lima")
    private String ciudadDestino;

    @Schema(description = "Latitud actual del vuelo", example = "-2.5")
    private Double latitudActual;

    @Schema(description = "Longitud actual del vuelo", example = "-76.3")
    private Double longitudActual;

    @Schema(description = "Latitud del origen", example = "4.7011")
    private Double latitudOrigen;

    @Schema(description = "Longitud del origen", example = "-74.1469")
    private Double longitudOrigen;

    @Schema(description = "Latitud del destino", example = "-12.0219")
    private Double latitudDestino;

    @Schema(description = "Longitud del destino", example = "-77.1147")
    private Double longitudDestino;

    @Schema(description = "Estado del vuelo", example = "EN_CAMINO")
    private String estado;

    @Schema(description = "Progreso del vuelo (0.0 a 1.0)", example = "0.45")
    private Double progreso;

    @Schema(description = "Minuto de inicio del vuelo desde T0", example = "1200")
    private Integer minutoInicio;

    @Schema(description = "Minuto de fin del vuelo desde T0", example = "1680")
    private Integer minutoFin;

    @Schema(description = "IDs de los paquetes a bordo")
    private List<Long> paquetesABordo;

    @Schema(description = "Capacidad máxima del vuelo", example = "600")
    private Integer capacidadMaxima;

    @Schema(description = "Capacidad actual utilizada", example = "450")
    private Integer capacidadUsada;

    @Schema(description = "Porcentaje de ocupación", example = "75.0")
    private Double porcentajeOcupacion;
}

