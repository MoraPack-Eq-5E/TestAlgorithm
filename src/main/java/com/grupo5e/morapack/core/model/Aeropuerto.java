package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Aeropuerto {
    private String codigoIATA;
    private String ciudad;
    private String pais;
    private String continente;
    private double latitud;
    private double longitud;
    private int capacidadAlmacen; // Capacidad entre 600-1000 paquetes
    private int paquetesEnAlmacen; // Paquetes actualmente en almacén
    private boolean esSedeMoraPack; // Lima, Bruselas, Baku
    
    public Aeropuerto(String codigoIATA, String ciudad, String pais, String continente, 
                      double latitud, double longitud, int capacidadAlmacen, boolean esSedeMoraPack) {
        this.codigoIATA = codigoIATA;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.latitud = latitud;
        this.longitud = longitud;
        this.capacidadAlmacen = capacidadAlmacen;
        this.paquetesEnAlmacen = 0;
        this.esSedeMoraPack = esSedeMoraPack;
    }
    
    public boolean puedeAlmacenar(int cantidad) {
        return paquetesEnAlmacen + cantidad <= capacidadAlmacen;
    }
    
    public void agregarPaquetes(int cantidad) {
        if (puedeAlmacenar(cantidad)) {
            paquetesEnAlmacen += cantidad;
        } else {
            throw new IllegalStateException("No hay suficiente capacidad en el almacén");
        }
    }
    
    public void removerPaquetes(int cantidad) {
        if (paquetesEnAlmacen >= cantidad) {
            paquetesEnAlmacen -= cantidad;
        } else {
            throw new IllegalStateException("No hay suficientes paquetes en el almacén");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Aeropuerto that = (Aeropuerto) o;
        return Objects.equals(codigoIATA, that.codigoIATA);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(codigoIATA);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s, %s)", codigoIATA, ciudad, pais);
    }
}
