package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO simplificado de ciudad para evitar referencias circulares")
public class CiudadSimpleDTO {

    @Schema(description = "ID de la ciudad", example = "1")
    private Integer id;

    @Schema(description = "Código de la ciudad", example = "BOGO")
    private String codigo;

    @Schema(description = "Nombre de la ciudad", example = "Bogotá")
    private String nombre;

    @Schema(description = "País", example = "Colombia")
    private String pais;
}

