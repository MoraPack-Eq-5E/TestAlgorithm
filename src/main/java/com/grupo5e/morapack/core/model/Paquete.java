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
    
    // Nuevos campos para reasignación (especificaciones)
    private boolean esReasignable; // true si puede ser reasignado
    private String tipoAlmacen; // "paso" o "entrega"
    private String pedidoOriginalId; // ID del pedido original
    private String clienteOriginalId; // ID del cliente original
    private LocalDateTime fechaReasignacion; // Cuándo fue reasignado
    
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
        
        // Inicializar nuevos campos
        this.esReasignable = true; // Por defecto es reasignable
        this.tipoAlmacen = "paso"; // Por defecto es almacén de paso
        this.pedidoOriginalId = null; // Se asignará cuando se cree el pedido
        this.clienteOriginalId = clienteId;
        this.fechaReasignacion = null;
        
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
    
    // Métodos para reasignación (nuevas especificaciones)
    
    /**
     * Verifica si el paquete puede ser reasignado según su ubicación
     */
    public boolean puedeSerReasignado() {
        // Según especificaciones:
        // - En tránsito (vuelo): SÍ puede ser reasignado
        // - En almacén de paso: SÍ puede ser reasignado  
        // - En almacén de entrega: NO puede ser reasignado
        
        if (this.estado == EstadoGeneral.EN_TRANSITO) {
            return true; // En vuelo, siempre reasignable
        }
        
        if (this.estado == EstadoGeneral.EN_ALMACEN) {
            return "paso".equals(this.tipoAlmacen); // Solo si es almacén de paso
        }
        
        return false; // En otros estados no es reasignable
    }
    
    /**
     * Reasigna el paquete a un nuevo cliente y destino
     */
    public boolean reasignar(String nuevoClienteId, String nuevoDestino, String nuevoPedidoId) {
        if (!puedeSerReasignado()) {
            return false; // No se puede reasignar
        }
        
        // Guardar información original
        this.clienteOriginalId = this.clienteId;
        // this.pedidoOriginalId se mantiene como está (ya tiene el valor original)
        
        // Actualizar información
        this.clienteId = nuevoClienteId;
        this.aeropuertoDestino = nuevoDestino;
        this.fechaReasignacion = LocalDateTime.now();
        this.estado = EstadoGeneral.REASIGNABLE;
        
        // Recalcular fecha límite (máximo 3 días para vuelos entre continentes)
        this.fechaLimiteEntrega = LocalDateTime.now().plusDays(3);
        
        return true;
    }
    
    /**
     * Actualiza el tipo de almacén y determina si es reasignable
     */
    public void actualizarTipoAlmacen(String tipoAlmacen) {
        this.tipoAlmacen = tipoAlmacen;
        
        if ("entrega".equals(tipoAlmacen)) {
            this.esReasignable = false; // Almacén de entrega no es reasignable
            this.estado = EstadoGeneral.NO_REASIGNABLE;
        } else if ("paso".equals(tipoAlmacen)) {
            this.esReasignable = true; // Almacén de paso sí es reasignable
            this.estado = EstadoGeneral.EN_ALMACEN_PASO;
        }
    }
    
    /**
     * Verifica si el paquete está en su destino final
     */
    public boolean estaEnDestinoFinal() {
        return this.aeropuertoActual.equals(this.aeropuertoDestino) && 
               "entrega".equals(this.tipoAlmacen);
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
