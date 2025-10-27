package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para errores en operaciones bulk")
public class BulkErrorDTO {

    @Schema(description = "Índice del elemento en la lista original", example = "3")
    private Integer indice;

    @Schema(description = "Identificador del elemento (si está disponible)", example = "12345")
    private String identificador;

    @Schema(description = "Mensaje de error", example = "Código IATA duplicado")
    private String mensaje;

    @Schema(description = "Detalles técnicos del error")
    private String detalles;
}

