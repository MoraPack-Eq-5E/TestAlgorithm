package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.constants.Constantes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LectorVuelos {

    private ArrayList<Vuelo> vuelos;
    private final String rutaArchivo;
    private ArrayList<Aeropuerto> aeropuertos;

    public LectorVuelos(String rutaArchivo, ArrayList<Aeropuerto> aeropuertos) {
        this.rutaArchivo = rutaArchivo;
        this.vuelos = new ArrayList<>();
        this.aeropuertos = aeropuertos;
    }

    public ArrayList<Vuelo> leerVuelos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            int idVuelo = 1;
            Map<String, Aeropuerto> mapaAeropuertos = crearMapaAeropuertos();
            
            while ((linea = reader.readLine()) != null) {
                // Saltar líneas vacías
                if (linea.trim().isEmpty()) {
                    continue;
                }
                
                // Parsear datos del vuelo
                // Formato: ORIGEN-DESTINO-SALIDA-LLEGADA-CAPACIDAD
                String[] partes = linea.split("-");
                if (partes.length == 5) {
                    String codigoOrigen = partes[0];
                    String codigoDestino = partes[1];
                    String horaSalida = partes[2];
                    String horaLlegada = partes[3];
                    int capacidadMaxima = Integer.parseInt(partes[4]);
                    
                    // Buscar aeropuertos por código IATA
                    Aeropuerto aeropuertoOrigen = mapaAeropuertos.get(codigoOrigen);
                    Aeropuerto aeropuertoDestino = mapaAeropuertos.get(codigoDestino);
                    
                    if (aeropuertoOrigen != null && aeropuertoDestino != null) {
                        // Calcular tiempo de transporte en horas
                        double tiempoTransporte = calcularTiempoTransporte(horaSalida, horaLlegada);
                        
                        // Calcular costo (esto es un placeholder - podrías implementar un modelo de costo más sofisticado)
                        double costo = calcularCostoVuelo(aeropuertoOrigen, aeropuertoDestino, capacidadMaxima);
                        
                        // Crear objeto Vuelo
                        Vuelo vuelo = new Vuelo();
                        vuelo.setId(idVuelo++);
                        vuelo.setFrecuenciaPorDia(1.0); // Frecuencia por defecto
                        vuelo.setAeropuertoOrigen(aeropuertoOrigen);
                        vuelo.setAeropuertoDestino(aeropuertoDestino);
                        vuelo.setCapacidadMaxima(capacidadMaxima);
                        vuelo.setCapacidadUsada(0);
                        vuelo.setTiempoTransporte(tiempoTransporte);
                        vuelo.setCosto(costo);
                        
                        vuelos.add(vuelo);
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo datos de vuelos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return vuelos;
    }

    private Map<String, Aeropuerto> crearMapaAeropuertos() {
        Map<String, Aeropuerto> mapa = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertos) {
            mapa.put(aeropuerto.getCodigoIATA(), aeropuerto);
        }
        return mapa;
    }
    
    private double calcularTiempoTransporte(String horaSalida, String horaLlegada) {
        LocalTime salida = parsearHora(horaSalida);
        LocalTime llegada = parsearHora(horaLlegada);
        
        // Calcular duración entre salida y llegada
        long minutos;
        if (llegada.isBefore(salida)) {
            // Vuelo cruza medianoche
            minutos = Duration.between(salida, LocalTime.of(23, 59, 59)).toMinutes() + 
                     Duration.between(LocalTime.of(0, 0), llegada).toMinutes() + 1;
        } else {
            minutos = Duration.between(salida, llegada).toMinutes();
        }
        
        // Convertir minutos a horas
        return minutos / 60.0;
    }
    
    private LocalTime parsearHora(String horaStr) {
        int horas = Integer.parseInt(horaStr.substring(0, 2));
        int minutos = Integer.parseInt(horaStr.substring(3, 5));
        return LocalTime.of(horas, minutos);
    }
    
    private double calcularCostoVuelo(Aeropuerto origen, Aeropuerto destino, int capacidad) {
        // Modelo de costo simple basado en si los aeropuertos están en el mismo continente y capacidad
        boolean vueloMismoContinente = origen.getCiudad().getContinente() == destino.getCiudad().getContinente();
        
        double costoBase;
        if (vueloMismoContinente) {
            costoBase = Constantes.TIEMPO_TRANSPORTE_MISMO_CONTINENTE * 100;
        } else {
            costoBase = Constantes.TIEMPO_TRANSPORTE_DIFERENTE_CONTINENTE * 150;
        }
        
        // Ajustar costo basado en capacidad
        double factorCapacidad = capacidad / 300.0;
        
        return costoBase * factorCapacidad;
    }
}
