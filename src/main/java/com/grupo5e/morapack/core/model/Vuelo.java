package com.grupo5e.morapack.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Vuelo {
    private int id;
    private double frecuenciaPorDia;
    private Aeropuerto aeropuertoOrigen;
    private Aeropuerto aeropuertoDestino;
    private int capacidadMaxima;
    private int capacidadUsada;
    private double tiempoTransporte;
    private double costo;
    
    // Campos temporales para identificación y cancelaciones
    private LocalTime horaSalida;
    private LocalTime horaLlegada;
    
    /**
     * Genera el identificador único del vuelo basado en ruta y horario.
     * Formato: "ORIGEN-DESTINO-HH:MM"
     * Ejemplo: "SKBO-SEQM-03:34"
     * 
     * @return Identificador único del vuelo, o null si faltan datos
     */
    public String getIdentificadorVuelo() {
        if (aeropuertoOrigen == null || aeropuertoDestino == null || horaSalida == null) {
            return null;
        }
        return String.format("%s-%s-%02d:%02d",
            aeropuertoOrigen.getCodigoIATA(),
            aeropuertoDestino.getCodigoIATA(),
            horaSalida.getHour(),
            horaSalida.getMinute()
        );
    }
}
