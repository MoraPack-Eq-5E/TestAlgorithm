package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoPaquete;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Paquete {
    private int id;
    private Cliente cliente;
    private Ciudad ciudadDestino;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaLimiteEntrega;
    private EstadoPaquete estado;
    private Ciudad ubicacionActual;
    private Ruta rutaAsignada;
    private double prioridad;
    private ArrayList<Producto> productos;
}