package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "rutas")
public class Ruta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToMany (mappedBy = "rutaAsignada")
    private List<Vuelo> vuelos;

    // Aeropuerto origen de la ruta
    @ManyToOne
    @JoinColumn(name = "aeropuerto_origen_id", referencedColumnName = "id", nullable = false)
    private Aeropuerto aeropuertoOrigen;

    // Aeropuerto destino de la ruta
    @ManyToOne
    @JoinColumn(name = "aeropuerto_destino_id", referencedColumnName = "id", nullable = false)
    private Aeropuerto aeropuertoDestino;

    private double tiempoTotal;
    private double costoTotal;

    // Relación: muchos pedidos pueden usar muchas rutas
    @ManyToMany(mappedBy = "rutas")
    private List<Pedido> pedidos;
}
