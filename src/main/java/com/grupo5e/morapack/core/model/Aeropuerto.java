package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
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
@Table(name = "aeropuertos")
public class Aeropuerto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código IATA
    @Column(nullable = false, unique = true, length = 4)
    private String codigo;

    private int zonaHorariaUTC;
    private String latitud;
    private String longitud;

    private int capacidadActual;
    private int capacidadMaxima;

    // Relación 1:1 con Ciudad
    @OneToOne
    @JoinColumn(name = "ciudad_id", referencedColumnName = "id", nullable = false, unique = true)
    private Ciudad ciudad;

    @Enumerated(EnumType.STRING)
    private EstadoAeropuerto estado;
}
