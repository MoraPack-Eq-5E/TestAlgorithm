package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ruta {
    private int id;
    private List<Vuelo> vuelos;
    private Ciudad ciudadOrigen;
    private Ciudad ciudadDestino;
    private double tiempoTotal;
    private double costoTotal;
    private List<Paquete> paquetes;
}
