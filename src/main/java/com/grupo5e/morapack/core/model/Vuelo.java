package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.grupo5e.morapack.core.enums.EstadoVuelo;

import java.sql.Time;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vuelos")
public class Vuelo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Número de frecuencias por día (ejemplo: 2 vuelos diarios)
    private double frecuenciaPorDia;
    private Time horaSalida;
    private Time horaLlegada;

    // Relación: un vuelo tiene un aeropuerto origen
    @ManyToOne
    @JoinColumn(name = "aeropuerto_origen_id", referencedColumnName = "id", nullable = false)
    private Aeropuerto aeropuertoOrigen;

    // Relación: un vuelo tiene un aeropuerto destino
    @ManyToOne
    @JoinColumn(name = "aeropuerto_destino_id", referencedColumnName = "id", nullable = false)
    private Aeropuerto aeropuertoDestino;

    private int capacidadMaxima;
    private int capacidadUsada;

    // Tiempo de transporte (ejemplo: 2.5 horas)
    private double tiempoTransporte;
    private double costo;
    private String latitudActual;
    private String longitudActual;

    @Enumerated(EnumType.STRING)
    private EstadoVuelo estado;

    @ManyToMany(mappedBy = "vuelos") // Relación bidireccional
    private List<Ruta> rutas;
}
