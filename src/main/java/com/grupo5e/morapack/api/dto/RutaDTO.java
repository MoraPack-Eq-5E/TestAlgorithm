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
@Schema(description = "DTO para representar una ruta de vuelo")
public class RutaDTO {

    @Schema(description = "ID único de la ruta", example = "1")
    private Integer id;

    @NotNull(message = "El ID del aeropuerto origen es obligatorio")
    @Schema(description = "ID del aeropuerto de origen", example = "1", required = true)
    private Long aeropuertoOrigenId;

    @Schema(description = "Código IATA del aeropuerto origen", example = "SKBO")
    private String aeropuertoOrigenCodigo;

    @NotNull(message = "El ID del aeropuerto destino es obligatorio")
    @Schema(description = "ID del aeropuerto de destino", example = "2", required = true)
    private Long aeropuertoDestinoId;

    @Schema(description = "Código IATA del aeropuerto destino", example = "SEQM")
    private String aeropuertoDestinoCodigo;

    @Min(value = 0, message = "El tiempo total no puede ser negativo")
    @Schema(description = "Tiempo total de la ruta en horas", example = "8.5")
    private Double tiempoTotal;

    @Min(value = 0, message = "El costo total no puede ser negativo")
    @Schema(description = "Costo total de la ruta", example = "3500.75")
    private Double costoTotal;

    @Schema(description = "Lista de IDs de vuelos que componen la ruta")
    private List<Integer> vuelosIds;

    @Schema(description = "Lista de IDs de pedidos asignados a esta ruta")
    private List<Long> pedidosIds;
}

