package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO simplificado de cliente para evitar referencias circulares")
public class ClienteSimpleDTO {

    @Schema(description = "ID del cliente", example = "1")
    private Long id;

    @Schema(description = "Nombres del cliente", example = "Juan Carlos")
    private String nombres;

    @Schema(description = "Apellidos del cliente", example = "Pérez González")
    private String apellidos;

    @Schema(description = "Correo electrónico", example = "juan.perez@email.com")
    private String correo;

    @Schema(description = "Número de documento", example = "12345678")
    private String numeroDocumento;
}

