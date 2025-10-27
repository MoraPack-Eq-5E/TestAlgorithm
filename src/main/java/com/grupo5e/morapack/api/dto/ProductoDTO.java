package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un producto")
public class ProductoDTO {

    @Schema(description = "ID único del producto", example = "1")
    private Long id;

    @NotNull(message = "El ID del pedido es obligatorio")
    @Schema(description = "ID del pedido al que pertenece", example = "1", required = true)
    private Long pedidoId;

    @NotNull(message = "El estado del producto es obligatorio")
    @Schema(description = "Estado actual del producto", example = "EN_ALMACEN", required = true)
    private EstadoProducto estado;
}

