package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cliente {
    private String id;
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String ciudad;
    private String pais;
    private String codigoIATAPreferido; // Aeropuerto donde prefiere recoger
    private List<String> historialPedidos;
    private boolean clienteVIP;
    
    public Cliente(String id, String nombre, String email, String codigoIATAPreferido) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.codigoIATAPreferido = codigoIATAPreferido;
        this.historialPedidos = new ArrayList<>();
        this.clienteVIP = false;
    }
    
    public void agregarPedido(String pedidoId) {
        historialPedidos.add(pedidoId);
        // Un cliente se vuelve VIP después de 10 pedidos
        if (historialPedidos.size() >= 10) {
            this.clienteVIP = true;
        }
    }
    
    public int getCantidadPedidos() {
        return historialPedidos.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(id, cliente.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Cliente[%s: %s, Aeropuerto: %s, VIP: %s]", 
                           id, nombre, codigoIATAPreferido, clienteVIP);
    }
}
