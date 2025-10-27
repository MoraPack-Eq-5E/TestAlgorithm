package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.Continente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar una ciudad")
public class CiudadDTO {

    @Schema(description = "ID único de la ciudad", example = "1")
    private Integer id;

    @NotBlank(message = "El código de ciudad es obligatorio")
    @Size(min = 4, max = 4, message = "El código debe tener exactamente 4 caracteres")
    @Schema(description = "Código único de la ciudad", example = "BOGO", required = true)
    private String codigo;

    @NotBlank(message = "El nombre de la ciudad es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Schema(description = "Nombre de la ciudad", example = "Bogotá", required = true)
    private String nombre;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El nombre del país no puede exceder 100 caracteres")
    @Schema(description = "País donde se encuentra la ciudad", example = "Colombia", required = true)
    private String pais;

    @NotNull(message = "El continente es obligatorio")
    @Schema(description = "Continente donde se encuentra la ciudad", example = "AMERICA_SUR", required = true)
    private Continente continente;
}

