package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoGeneral;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SegmentoRuta {
    private String id;
    private String aeropuertoOrigen;
    private String aeropuertoDestino;
    private String numeroVuelo;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaLlegada;
    private double duracionHoras;
    private double costo;
    private boolean mismoContinente;
    private EstadoGeneral estado;
    private int paquetesEnSegmento;
    
    public SegmentoRuta(String id, String aeropuertoOrigen, String aeropuertoDestino, 
                        String numeroVuelo, boolean mismoContinente) {
        this.id = id;
        this.aeropuertoOrigen = aeropuertoOrigen;
        this.aeropuertoDestino = aeropuertoDestino;
        this.numeroVuelo = numeroVuelo;
        this.mismoContinente = mismoContinente;
        this.duracionHoras = mismoContinente ? 12.0 : 24.0; // 0.5 o 1 día
        this.costo = calcularCostoBase();
        this.estado = EstadoGeneral.PLANIFICADO;
        this.paquetesEnSegmento = 0;
    }
    
    private double calcularCostoBase() {
        // Costo base simulado según distancia y tipo de vuelo
        double costoBase = mismoContinente ? 100.0 : 200.0;
        return costoBase;
    }
    
    public void programarVuelo(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
        this.fechaLlegada = fechaSalida.plusHours((long)duracionHoras);
    }
    
    public boolean estaEnTiempo() {
        if (fechaLlegada == null) return true;
        return LocalDateTime.now().isBefore(fechaLlegada);
    }
    
    public void agregarPaquetes(int cantidad) {
        this.paquetesEnSegmento += cantidad;
    }
    
    public void removerPaquetes(int cantidad) {
        if (paquetesEnSegmento >= cantidad) {
            this.paquetesEnSegmento -= cantidad;
        }
    }
    
    public SegmentoRuta copiar() {
        SegmentoRuta copia = new SegmentoRuta(
            this.id + "_copia", 
            this.aeropuertoOrigen, 
            this.aeropuertoDestino, 
            this.numeroVuelo, 
            this.mismoContinente
        );
        copia.fechaSalida = this.fechaSalida;
        copia.fechaLlegada = this.fechaLlegada;
        copia.estado = this.estado;
        copia.paquetesEnSegmento = this.paquetesEnSegmento;
        return copia;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SegmentoRuta that = (SegmentoRuta) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Segmento[%s: %s -> %s, Vuelo: %s, %.1fh, $%.2f]", 
                           id, aeropuertoOrigen, aeropuertoDestino, numeroVuelo, duracionHoras, costo);
    }
}
