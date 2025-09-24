package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoGeneral;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ruta {
    private String id;
    private String paqueteId;
    private List<SegmentoRuta> segmentos;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFinEstimada;
    private LocalDateTime fechaFinReal;
    private EstadoGeneral estado;
    private double costoTotal;
    private double tiempoTotalHoras;
    private boolean cumplePlazo;
    
    public Ruta(String id, String paqueteId) {
        this.id = id;
        this.paqueteId = paqueteId;
        this.segmentos = new ArrayList<>();
        this.estado = EstadoGeneral.PLANIFICADO;
        this.costoTotal = 0.0;
        this.tiempoTotalHoras = 0.0;
        this.cumplePlazo = true;
    }
    
    public void agregarSegmento(SegmentoRuta segmento) {
        segmentos.add(segmento);
        recalcularMetricas();
    }
    
    public void removerSegmento(int indice) {
        if (indice >= 0 && indice < segmentos.size()) {
            segmentos.remove(indice);
            recalcularMetricas();
        }
    }
    
    public void insertarSegmento(int indice, SegmentoRuta segmento) {
        segmentos.add(indice, segmento);
        recalcularMetricas();
    }
    
    private void recalcularMetricas() {
        costoTotal = segmentos.stream()
                .mapToDouble(SegmentoRuta::getCosto)
                .sum();

        tiempoTotalHoras = segmentos.stream()
                .mapToDouble(SegmentoRuta::getDuracionHoras)
                .sum();

        // Actualizar fecha fin estimada con minutos (no truncar decimales)
        if (fechaInicio != null) {
            long minutos = Math.round(tiempoTotalHoras * 60.0);
            fechaFinEstimada = fechaInicio.plusMinutes(minutos);
        } else {
            fechaFinEstimada = null;
        }
    }
    
    public String getAeropuertoOrigen() {
        return segmentos.isEmpty() ? null : segmentos.get(0).getAeropuertoOrigen();
    }
    
    public String getAeropuertoDestino() {
        return segmentos.isEmpty() ? null : segmentos.get(segmentos.size() - 1).getAeropuertoDestino();
    }
    
    public int getCantidadSegmentos() {
        return segmentos.size();
    }
    
    public boolean esRutaDirecta() {
        return segmentos.size() == 1;
    }
    
    public List<String> getAeropuertosEnRuta() {
        List<String> aeropuertos = new ArrayList<>();
        for (SegmentoRuta segmento : segmentos) {
            if (aeropuertos.isEmpty()) {
                aeropuertos.add(segmento.getAeropuertoOrigen());
            }
            aeropuertos.add(segmento.getAeropuertoDestino());
        }
        return aeropuertos;
    }
    
    public void validarRuta() {
        if (segmentos.isEmpty()) {
            throw new IllegalStateException("La ruta no puede estar vacía");
        }
        
        // Aeropuertos no nulos y duraciones positivas
        for (SegmentoRuta s : segmentos) {
            if (s.getAeropuertoOrigen() == null || s.getAeropuertoDestino() == null) {
                throw new IllegalStateException("Segmento con aeropuerto nulo");
            }
            if (s.getDuracionHoras() <= 0.0) {
                throw new IllegalStateException("Segmento con duración no positiva");
            }
        }
        
        // Conectividad estricta
        for (int i = 1; i < segmentos.size(); i++) {
            if (!segmentos.get(i - 1).getAeropuertoDestino()
                    .equals(segmentos.get(i).getAeropuertoOrigen())) {
                throw new IllegalStateException("Los segmentos de la ruta no están conectados correctamente");
            }
        }
    }
    
    /**
     * Tiempo total incluyendo conexiones fijas entre segmentos (no al final).
     * Útil para validaciones de deadline que consideren tiempos de conexión.
     */
    public double getTiempoTotalHorasConConexiones(double horasConexion) {
        int conexiones = Math.max(0, segmentos.size() - 1);
        return tiempoTotalHoras + horasConexion * conexiones;
    }
    
    public Ruta copiar() {
        Ruta copia = new Ruta(this.id + "_copia", this.paqueteId);
        for (SegmentoRuta segmento : this.segmentos) {
            copia.agregarSegmento(segmento.copiar());
        }
        copia.fechaInicio = this.fechaInicio;
        copia.estado = this.estado;
        return copia;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ruta ruta = (Ruta) o;
        return Objects.equals(id, ruta.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        if (segmentos.isEmpty()) {
            return String.format("Ruta[%s: Vacía]", id);
        }
        return String.format("Ruta[%s: %s -> %s, %d segmentos, %.2f horas, $%.2f]", 
                           id, getAeropuertoOrigen(), getAeropuertoDestino(), 
                           segmentos.size(), tiempoTotalHoras, costoTotal);
    }
}
