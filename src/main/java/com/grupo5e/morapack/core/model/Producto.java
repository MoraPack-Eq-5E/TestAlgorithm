package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: muchos productos pertenecen a un pedido
    @ManyToOne
    @JoinColumn(name = "paquete_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private EstadoProducto estado;
}
