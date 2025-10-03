package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.TipoDocumento;
import jakarta.persistence.*;
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
@Entity
@Table(name = "clientes")
public class Cliente extends Usuario {

    private String nombres;
    private String apellidos;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;

    private String correo;
    private String telefono; // Guardar en formato internacional E.164, ej: +51987654321

    @ManyToOne
    private Ciudad ciudadRecojo;
    
//    public Cliente(String id, String nombre, String email, String codigoIATAPreferido) {
//        this.id = id;
//        this.nombre = nombre;
//        this.correo = email;
//        this.codigoIATAPreferido = codigoIATAPreferido;
//        this.historialPedidos = new ArrayList<>();
//        this.clienteVIP = false;
//    }
}
