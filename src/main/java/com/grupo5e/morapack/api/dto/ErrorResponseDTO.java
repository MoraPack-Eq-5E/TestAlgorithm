package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO estándar para respuestas de error")
public class ErrorResponseDTO {

    @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "404")
    private Integer status;

    @Schema(description = "Nombre del error HTTP", example = "Not Found")
    private String error;

    @Schema(description = "Mensaje descriptivo del error", example = "Pedido no encontrado")
    private String mensaje;

    @Schema(description = "Lista de detalles de validación (si aplica)")
    private List<String> detalles;

    @Schema(description = "Path del endpoint donde ocurrió el error", example = "/api/pedidos/999")
    private String path;
}

