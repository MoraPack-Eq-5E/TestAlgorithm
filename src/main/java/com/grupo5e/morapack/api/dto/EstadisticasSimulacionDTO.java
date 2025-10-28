package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estadísticas adicionales de la simulación")
public class EstadisticasSimulacionDTO {

    @Schema(description = "Rutas directas (sin escalas)", example = "45")
    private Integer rutasDirectas;

    @Schema(description = "Rutas con una escala", example = "78")
    private Integer rutasUnaEscala;

    @Schema(description = "Rutas con dos escalas", example = "22")
    private Integer rutasDosEscalas;

    @Schema(description = "Rutas dentro del mismo continente", example = "90")
    private Integer rutasMismoContinente;

    @Schema(description = "Rutas intercontinentales", example = "55")
    private Integer rutasIntercontinentales;

    @Schema(description = "Entregas a tiempo", example = "140")
    private Integer entregasATiempo;

    @Schema(description = "Porcentaje de entregas a tiempo", example = "96.55")
    private Double porcentajeEntregasATiempo;

    @Schema(description = "Vuelos totales utilizados", example = "250")
    private Integer vuelosUtilizados;

    @Schema(description = "Ocupación promedio de vuelos (%)", example = "75.5")
    private Double ocupacionPromedioVuelos;

    @Schema(description = "Ocupación promedio de aeropuertos (%)", example = "62.3")
    private Double ocupacionPromedioAeropuertos;
}

