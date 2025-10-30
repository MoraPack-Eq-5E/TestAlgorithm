package com.grupo5e.morapack.simulation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationalOrderDTO {
    private String codigo;              // opcional
    private String aeropuertoOrigen;    // IATA: "SPIM"
    private String aeropuertoDestino;   // IATA
    private Integer cantidadProductos;  // 1..N
    private LocalDateTime fechaPedido;  // si no viene, usar ahora
}

