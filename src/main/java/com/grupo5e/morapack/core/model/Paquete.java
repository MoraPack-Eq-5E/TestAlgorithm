package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoPaquete;

import jakarta.persistence.*;
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
@Entity
@Table(name = "paquetes")
public class Paquete {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: muchos paquetes pertenecen a un cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Ciudad destino
    @ManyToOne
    @JoinColumn(name = "ciudad_destino_id", nullable = false)
    private Ciudad ciudadDestino;

    private LocalDateTime fechaPedido;
    private LocalDateTime fechaLimiteEntrega;

    @Enumerated(EnumType.STRING)
    private EstadoPaquete estado;

    // Ciudad donde se encuentra actualmente
    @ManyToOne
    @JoinColumn(name = "ubicacion_actual_id")
    private Ciudad ubicacionActual;

    // Relación con Ruta (opcional si ya tienes la clase)
    @ManyToOne
    @JoinColumn(name = "ruta_id")
    private Ruta rutaAsignada;

    private double prioridad;

    // Relación: un paquete puede contener varios productos
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    private ArrayList<Producto> productos;
}