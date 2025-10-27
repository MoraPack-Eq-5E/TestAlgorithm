package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.EstadoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un pedido/paquete")
public class PedidoDTO {

    @Schema(description = "ID único del pedido", example = "1")
    private Long id;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Schema(description = "ID del cliente que realiza el pedido", example = "1", required = true)
    private Long clienteId;

    @Schema(description = "Información básica del cliente")
    private ClienteSimpleDTO cliente;

    @NotBlank(message = "El código del aeropuerto destino es obligatorio")
    @Size(min = 4, max = 4, message = "El código IATA debe tener 4 caracteres")
    @Schema(description = "Código IATA del aeropuerto destino", example = "SKBO", required = true)
    private String aeropuertoDestinoCodigo;

    @Schema(description = "Código IATA del aeropuerto origen actual", example = "SEQM")
    private String aeropuertoOrigenCodigo;

    @NotNull(message = "La fecha del pedido es obligatoria")
    @Schema(description = "Fecha y hora en que se realizó el pedido", example = "2024-01-15T10:30:00", required = true)
    private LocalDateTime fechaPedido;

    @NotNull(message = "La fecha límite de entrega es obligatoria")
    @Schema(description = "Fecha y hora límite para entregar el pedido", example = "2024-01-20T18:00:00", required = true)
    private LocalDateTime fechaLimiteEntrega;

    @NotNull(message = "El estado del pedido es obligatorio")
    @Schema(description = "Estado actual del pedido", example = "PENDIENTE", required = true)
    private EstadoPedido estado;

    @Min(value = 0, message = "La prioridad no puede ser negativa")
    @Max(value = 100, message = "La prioridad no puede exceder 100")
    @Schema(description = "Nivel de prioridad del pedido (0-100)", example = "75.5")
    private Double prioridad;

    @Min(value = 1, message = "Debe haber al menos 1 producto")
    @Schema(description = "Cantidad total de productos en el pedido", example = "3")
    private Integer cantidadProductos;

    @Schema(description = "Lista de productos incluidos en el pedido")
    private List<ProductoDTO> productos;

    @Schema(description = "Lista de IDs de rutas asignadas al pedido")
    private List<Integer> rutasIds;
}

