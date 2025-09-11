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
public class Pedido {
    private String id;
    private String clienteId;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaLimiteEntrega;
    private String aeropuertoDestino;
    private List<String> paqueteIds; // Lista de paquetes que pertenecen a este pedido
    private EstadoGeneral estado;
    private int cantidadProductosMPE;
    private int prioridadPedido; // 1 = alta, 2 = media, 3 = baja
    private String sedeOrigenAsignada; // Lima, Bruselas o Baku
    
    public Pedido(String id, String clienteId, String aeropuertoDestino, int cantidadProductosMPE) {
        this.id = id;
        this.clienteId = clienteId;
        this.fechaPedido = LocalDateTime.now();
        this.aeropuertoDestino = aeropuertoDestino;
        this.cantidadProductosMPE = cantidadProductosMPE;
        this.paqueteIds = new ArrayList<>();
        this.estado = EstadoGeneral.CREADO;
        this.prioridadPedido = 2; // Prioridad media por defecto
        
        // Calcular fecha límite (se puede mejorar con lógica de continentes)
        this.fechaLimiteEntrega = fechaPedido.plusDays(3); // Default 3 días
    }
    
    public void agregarPaquete(String paqueteId) {
        if (!paqueteIds.contains(paqueteId)) {
            paqueteIds.add(paqueteId);
        }
    }
    
    public void removerPaquete(String paqueteId) {
        paqueteIds.remove(paqueteId);
    }
    
    public int getCantidadPaquetes() {
        return paqueteIds.size();
    }
    
    public boolean estaCompleto() {
        return cantidadProductosMPE == paqueteIds.size();
    }
    
    public boolean estaEnPlazo() {
        return LocalDateTime.now().isBefore(fechaLimiteEntrega);
    }
    
    public long getDiasRestantes() {
        return java.time.Duration.between(LocalDateTime.now(), fechaLimiteEntrega).toDays();
    }
    
    public void calcularFechaLimite(boolean mismoContinente) {
        int diasPlazo = mismoContinente ? 2 : 3;
        this.fechaLimiteEntrega = fechaPedido.plusDays(diasPlazo);
    }
    
    public void asignarSedeOptima(String continenteDestino) {
        // Lógica simplificada para asignar la sede más cercana
        switch (continenteDestino.toLowerCase()) {
            case "america":
                this.sedeOrigenAsignada = "LIM"; // Lima
                break;
            case "europa":
                this.sedeOrigenAsignada = "BRU"; // Bruselas
                break;
            case "asia":
                this.sedeOrigenAsignada = "BAK"; // Baku
                break;
            default:
                this.sedeOrigenAsignada = "LIM"; // Lima por defecto
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Pedido[%s: Cliente %s, %d MPE -> %s, Estado: %s]", 
                           id, clienteId, cantidadProductosMPE, aeropuertoDestino, estado);
    }
}
