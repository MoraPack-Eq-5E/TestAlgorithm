package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.TipoVuelo;
import com.grupo5e.morapack.core.enums.EstadoGeneral;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Vuelo {
    private String numeroVuelo;
    private String aeropuertoOrigen;
    private String aeropuertoDestino;
    private LocalTime horaSalida;
    private LocalTime horaLlegada;
    private int capacidadMaxima; // 200-300 mismo continente, 250-400 distinto continente
    private int paquetesReservados; // Paquetes ya reservados en este vuelo
    private boolean mismoContinente;
    private double duracionHoras; // 0.5 días mismo continente, 1 día distinto continente
    private TipoVuelo tipoVuelo;
    private int frecuenciaDiaria; // Cuántas veces al día opera este vuelo
    
    // Nuevos campos para cancelaciones y demoras (especificaciones)
    private EstadoGeneral estadoVuelo;
    private LocalDateTime fechaCancelacion;
    private String motivoCancelacion; // "manual", "programada", "demora"
    private int horasDemora; // 0 = sin demora, 3 = demora de 3 horas
    private boolean puedeCancelar; // true si no ha despegado
    private LocalDateTime horaDespegueReal; // null si no ha despegado
    
    public Vuelo(String numeroVuelo, String aeropuertoOrigen, String aeropuertoDestino, 
                 boolean mismoContinente, int capacidadMaxima) {
        this.numeroVuelo = numeroVuelo;
        this.aeropuertoOrigen = aeropuertoOrigen;
        this.aeropuertoDestino = aeropuertoDestino;
        this.mismoContinente = mismoContinente;
        this.capacidadMaxima = capacidadMaxima;
        this.paquetesReservados = 0;
        this.duracionHoras = mismoContinente ? 12.0 : 24.0; // 0.5 o 1 día en horas
        this.tipoVuelo = mismoContinente ? TipoVuelo.DOMESTICO : TipoVuelo.INTERNACIONAL;
        this.frecuenciaDiaria = mismoContinente ? 2 : 1; // Valor por defecto
        
        // Horarios por defecto (se pueden personalizar)
        this.horaSalida = LocalTime.of(8, 0);
        this.horaLlegada = horaSalida.plusHours((long)duracionHoras);
        
        // Inicializar nuevos campos
        this.estadoVuelo = EstadoGeneral.PLANIFICADO;
        this.puedeCancelar = true; // Puede cancelar hasta que despegue
        this.horasDemora = 0;
        this.horaDespegueReal = null;
    }
    
    public boolean puedeCargar(int cantidadPaquetes) {
        return paquetesReservados + cantidadPaquetes <= capacidadMaxima;
    }
    
    public void reservarPaquetes(int cantidad) {
        if (puedeCargar(cantidad)) {
            paquetesReservados += cantidad;
        } else {
            throw new IllegalStateException("No hay suficiente capacidad en el vuelo");
        }
    }
    
    public void liberarPaquetes(int cantidad) {
        if (paquetesReservados >= cantidad) {
            paquetesReservados -= cantidad;
        } else {
            throw new IllegalStateException("No se pueden liberar más paquetes de los reservados");
        }
    }
    
    public int getCapacidadDisponible() {
        return capacidadMaxima - paquetesReservados;
    }
    
    public double getPorcentajeOcupacion() {
        return (double) paquetesReservados / capacidadMaxima * 100;
    }
    
    public LocalDateTime calcularHoraLlegada(LocalDateTime horaSalidaReal) {
        return horaSalidaReal.plusHours((long)duracionHoras);
    }
    
    // Métodos para cancelaciones y demoras (nuevas especificaciones)
    
    /**
     * Cancela el vuelo manualmente (solo si no ha despegado)
     */
    public boolean cancelarManual(String motivo) {
        if (!puedeCancelar) {
            return false; // No se puede cancelar si ya despegó
        }
        
        this.estadoVuelo = EstadoGeneral.CANCELADO_MANUAL;
        this.fechaCancelacion = LocalDateTime.now();
        this.motivoCancelacion = motivo;
        this.puedeCancelar = false;
        
        // Liberar todos los paquetes reservados
        this.paquetesReservados = 0;
        
        return true;
    }
    
    /**
     * Cancela el vuelo programadamente (desde archivo)
     */
    public void cancelarProgramado(String motivo) {
        this.estadoVuelo = EstadoGeneral.CANCELADO_PROGRAMADO;
        this.fechaCancelacion = LocalDateTime.now();
        this.motivoCancelacion = motivo;
        this.puedeCancelar = false;
        
        // Liberar todos los paquetes reservados
        this.paquetesReservados = 0;
    }
    
    /**
     * Aplica demora de 3 horas al vuelo
     */
    public void aplicarDemora() {
        if (this.estadoVuelo == EstadoGeneral.PLANIFICADO || 
            this.estadoVuelo == EstadoGeneral.EN_PROCESO) {
            
            this.estadoVuelo = EstadoGeneral.DEMORADO;
            this.horasDemora = 3;
            
            // Ajustar horarios
            this.horaSalida = this.horaSalida.plusHours(3);
            this.horaLlegada = this.horaLlegada.plusHours(3);
        }
    }
    
    /**
     * Registra el despegue del vuelo
     */
    public void registrarDespegue() {
        this.horaDespegueReal = LocalDateTime.now();
        this.puedeCancelar = false; // Ya no se puede cancelar
        this.estadoVuelo = EstadoGeneral.EN_PROCESO;
    }
    
    /**
     * Verifica si el vuelo está operativo (no cancelado)
     */
    public boolean estaOperativo() {
        return !this.estadoVuelo.esVueloCancelado();
    }
    
    /**
     * Obtiene la hora de llegada real considerando demoras
     */
    public LocalTime getHoraLlegadaReal() {
        return this.horaLlegada.plusHours(this.horasDemora);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vuelo vuelo = (Vuelo) o;
        return Objects.equals(numeroVuelo, vuelo.numeroVuelo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(numeroVuelo);
    }
    
    @Override
    public String toString() {
        return String.format("Vuelo[%s: %s -> %s, Capacidad: %d/%d, %s]", 
                           numeroVuelo, aeropuertoOrigen, aeropuertoDestino, 
                           paquetesReservados, capacidadMaxima,
                           mismoContinente ? "Mismo Continente" : "Distinto Continente");
    }
}
