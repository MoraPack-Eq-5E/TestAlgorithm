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
public class Paquete {
    private String id;
    private EstadoGeneral estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLimiteEntrega;
    private String aeropuertoOrigen;
    private String aeropuertoDestino;
    private String clienteId;
    private List<String> rutaPlanificada; // Lista de códigos IATA de aeropuertos
    private List<String> rutaActual; // Ruta actual del paquete
    private String aeropuertoActual; // Dónde está actualmente el paquete
    private int prioridad; // 1 = alta, 2 = media, 3 = baja
    
    public Paquete(String id, String aeropuertoOrigen, String aeropuertoDestino, String clienteId) {
        this.id = id;
        this.estado = EstadoGeneral.CREADO;
        this.fechaCreacion = LocalDateTime.now();
        this.aeropuertoOrigen = aeropuertoOrigen;
        this.aeropuertoDestino = aeropuertoDestino;
        this.aeropuertoActual = aeropuertoOrigen;
        this.clienteId = clienteId;
        this.rutaPlanificada = new ArrayList<>();
        this.rutaActual = new ArrayList<>();
        this.prioridad = 2; // Prioridad media por defecto
        
        // Calcular fecha límite de entrega basado en continentes
        // Esta lógica se puede mejorar con información de continentes
        this.fechaLimiteEntrega = LocalDateTime.now().plusDays(3); // Default 3 días
    }
    
    public void actualizarUbicacion(String nuevoAeropuerto) {
        this.aeropuertoActual = nuevoAeropuerto;
        if (!rutaActual.contains(nuevoAeropuerto)) {
            rutaActual.add(nuevoAeropuerto);
        }
    }
    
    public boolean haLlegadoADestino() {
        return aeropuertoActual.equals(aeropuertoDestino);
    }
    
    public boolean estaEnPlazo() {
        return LocalDateTime.now().isBefore(fechaLimiteEntrega);
    }
    
    public void calcularFechaLimite(boolean mismoContinente, int diasPlazo) {
        this.fechaLimiteEntrega = fechaCreacion.plusDays(diasPlazo);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Paquete paquete = (Paquete) o;
        return Objects.equals(id, paquete.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Paquete[%s: %s -> %s, Estado: %s]", 
                           id, aeropuertoOrigen, aeropuertoDestino, estado);
    }
}
