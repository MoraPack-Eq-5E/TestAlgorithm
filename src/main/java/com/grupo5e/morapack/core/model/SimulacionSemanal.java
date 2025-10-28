package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoSimulacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar ejecuciones de simulación semanal
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "simulacion_semanal")
public class SimulacionSemanal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EstadoSimulacion estado;

    @Column(name = "total_pedidos")
    private Integer totalPedidos;

    @Column(name = "pedidos_asignados")
    private Integer pedidosAsignados;

    @Column(name = "pedidos_no_asignados")
    private Integer pedidosNoAsignados;

    @Column(name = "costo_total")
    private Double costoTotal;

    @Column(name = "tiempo_promedio_entrega")
    private Double tiempoPromedioEntrega;

    @Column(name = "tiempo_simulado_dias")
    private Integer tiempoSimuladoDias = 7;

    @Column(name = "tiempo_inicial_referencia")
    private LocalDateTime tiempoInicialReferencia; // T0 del algoritmo

    @Column(name = "peso_solucion")
    private Integer pesoSolucion;

    @Column(name = "solucion_valida")
    private Boolean solucionValida;

    @Column(name = "progreso")
    private Integer progreso = 0; // 0-100%

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "iteraciones_alns")
    private Integer iteracionesAlns;

    @Column(name = "tiempo_limite_segundos")
    private Integer tiempoLimiteSegundos;
}

