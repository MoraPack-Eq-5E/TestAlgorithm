package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
}
