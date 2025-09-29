package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Almacen {
    private int id;
    private Aeropuerto aeropuerto;
    private int capacidadMaxima;
    private int capacidadUsada;
    private String nombre;
    private boolean esPrincipal;
}
