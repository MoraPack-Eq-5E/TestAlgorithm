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

    @ManyToMany
    @JoinTable(
            name = "rutas_vuelos", // nombre de la tabla intermedia
            joinColumns = @JoinColumn(name = "ruta_id"), // FK a ruta
            inverseJoinColumns = @JoinColumn(name = "vuelo_id") // FK a vuelo
    )
    private List<Vuelo> vuelos;

    // Ciudad origen de la ruta
    @ManyToOne
    @JoinColumn(name = "ciudad_origen_id", referencedColumnName = "id", nullable = false)
    private Ciudad ciudadOrigen;

    // Ciudad destino de la ruta
    @ManyToOne
    @JoinColumn(name = "ciudad_destino_id", referencedColumnName = "id", nullable = false)
    private Ciudad ciudadDestino;

    private double tiempoTotal;
    private double costoTotal;

    // Relación: muchos paquetes pueden usar una misma ruta
    @OneToMany(mappedBy = "rutaAsignada")
    private List<Paquete> paquetes;
}
