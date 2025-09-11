package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Continente {
    private String nombre;
    private String codigo;
    private Set<String> codigosIATAAeropuertos;
    private String sedeAsignada; // Lima, Bruselas o Baku
    
    public Continente(String nombre, String codigo, String sedeAsignada) {
        this.nombre = nombre;
        this.codigo = codigo;
        this.sedeAsignada = sedeAsignada;
        this.codigosIATAAeropuertos = new HashSet<>();
    }
    
    public void agregarAeropuerto(String codigoIATA) {
        codigosIATAAeropuertos.add(codigoIATA);
    }
    
    public boolean contieneAeropuerto(String codigoIATA) {
        return codigosIATAAeropuertos.contains(codigoIATA);
    }
    
    public int getCantidadAeropuertos() {
        return codigosIATAAeropuertos.size();
    }
    
    public static boolean sonMismoContinente(String aeropuerto1, String aeropuerto2, Set<Continente> continentes) {
        for (Continente continente : continentes) {
            if (continente.contieneAeropuerto(aeropuerto1) && continente.contieneAeropuerto(aeropuerto2)) {
                return true;
            }
        }
        return false;
    }
    
    public static Continente encontrarContinentePorAeropuerto(String codigoIATA, Set<Continente> continentes) {
        for (Continente continente : continentes) {
            if (continente.contieneAeropuerto(codigoIATA)) {
                return continente;
            }
        }
        return null;
    }
    
    public static int calcularDiasPlazo(String aeropuertoOrigen, String aeropuertoDestino, Set<Continente> continentes) {
        if (sonMismoContinente(aeropuertoOrigen, aeropuertoDestino, continentes)) {
            return 2; // Mismo continente: 2 días
        } else {
            return 3; // Distinto continente: 3 días
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Continente that = (Continente) o;
        return Objects.equals(codigo, that.codigo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
    
    @Override
    public String toString() {
        return String.format("Continente[%s: %s, %d aeropuertos, Sede: %s]", 
                           codigo, nombre, codigosIATAAeropuertos.size(), sedeAsignada);
    }
}
