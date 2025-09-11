package com.grupo5e.morapack.core.enums;

public enum TipoVuelo {
    DOMESTICO("Vuelo dentro del mismo continente"),
    INTERNACIONAL("Vuelo entre diferentes continentes"),
    CHARTER("Vuelo charter especial"),
    CARGO("Vuelo exclusivo de carga");
    
    private final String descripcion;
    
    TipoVuelo(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}
