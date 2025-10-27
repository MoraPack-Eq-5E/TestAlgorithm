package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO genérico para respuestas paginadas")
public class PaginatedResponseDTO<T> {

    @Schema(description = "Lista de elementos en la página actual")
    private List<T> contenido;

    @Schema(description = "Número de página actual (0-indexed)", example = "0")
    private Integer paginaActual;

    @Schema(description = "Tamaño de la página", example = "20")
    private Integer tamañoPagina;

    @Schema(description = "Número total de elementos", example = "150")
    private Long totalElementos;

    @Schema(description = "Número total de páginas", example = "8")
    private Integer totalPaginas;

    @Schema(description = "Indica si es la primera página", example = "true")
    private Boolean esPrimera;

    @Schema(description = "Indica si es la última página", example = "false")
    private Boolean esUltima;

    @Schema(description = "Indica si hay página siguiente", example = "true")
    private Boolean tieneSiguiente;

    @Schema(description = "Indica si hay página anterior", example = "false")
    private Boolean tieneAnterior;
}

