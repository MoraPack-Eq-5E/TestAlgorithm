package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para la ruta completa de un paquete")
public class RutaPaqueteDTO {

    @Schema(description = "ID del pedido", example = "45")
    private Long pedidoId;

    @Schema(description = "Código IATA del origen", example = "SKBO")
    private String codigoOrigen;

    @Schema(description = "Código IATA del destino", example = "EDDI")
    private String codigoDestino;

    @Schema(description = "Cliente propietario del pedido")
    private String nombreCliente;

    @Schema(description = "Cantidad de productos en el pedido", example = "5")
    private Integer cantidadProductos;

    @Schema(description = "Estado del pedido", example = "EN_TRANSITO")
    private String estadoPedido;

    @Schema(description = "Lista de tramos (vuelos) de la ruta")
    private List<TramoRutaDTO> tramos;

    @Schema(description = "Duración total estimada en horas", example = "18.5")
    private Double duracionTotalHoras;

    @Schema(description = "Está a tiempo según el deadline", example = "true")
    private Boolean aTiempo;
}

