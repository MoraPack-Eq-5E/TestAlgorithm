package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoPaquete;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: muchos pedidos pertenecen a un cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Aeropuerto destino
    @JoinColumn(name = "aeropuerto_destino_codigo", nullable = false)
    private String aeropuertoDestinoCodigo;

    private LocalDateTime fechaPedido;
    private LocalDateTime fechaLimiteEntrega;

    @Enumerated(EnumType.STRING)
    private EstadoPaquete estado;

    // Aeropuerto donde se encuentra actualmente
    @JoinColumn(name = "aeropuerto_origen_codigo")
    private String aeropuertoOrigenCodigo;

    // Relación con Ruta (opcional si ya tienes la clase)
    @ManyToOne
    @JoinColumn(name = "ruta_id")
    private Ruta rutaAsignada;

    private double prioridad;

    // Relación: un paquete puede contener varios productos
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Producto> productos;
}