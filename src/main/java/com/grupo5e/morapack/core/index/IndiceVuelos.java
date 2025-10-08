package com.grupo5e.morapack.core.index;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Índice espacial para búsqueda eficiente de vuelos por ruta.
 * Reduce búsqueda de O(N) a O(1) mediante indexación por ruta y aeropuerto.
 * 
 * Patrón: Index/Cache Pattern para optimización de rendimiento
 */
public class IndiceVuelos {
    
    // Índice: "ORIGEN-DESTINO" → Lista de vuelos
    private final Map<String, List<Vuelo>> vuelosPorRuta;
    
    // Índice: Aeropuerto origen → Lista de vuelos salientes
    private final Map<Aeropuerto, List<Vuelo>> vuelosSalientes;
    
    // Índice: Aeropuerto destino → Lista de vuelos entrantes
    private final Map<Aeropuerto, List<Vuelo>> vuelosEntrantes;
    
    /**
     * Construye el índice a partir de una lista de vuelos.
     * Complejidad: O(N) donde N = número de vuelos
     * 
     * @param vuelos Lista de todos los vuelos del sistema
     */
    public IndiceVuelos(List<Vuelo> vuelos) {
        this.vuelosPorRuta = new HashMap<>();
        this.vuelosSalientes = new HashMap<>();
        this.vuelosEntrantes = new HashMap<>();
        
        construirIndices(vuelos);
    }
    
    /**
     * Construye los tres índices a partir de la lista de vuelos.
     */
    private void construirIndices(List<Vuelo> vuelos) {
        for (Vuelo vuelo : vuelos) {
            if (vuelo == null || vuelo.getAeropuertoOrigen() == null || 
                vuelo.getAeropuertoDestino() == null) {
                continue; // Saltar vuelos inválidos
            }
            
            // Índice por ruta (origen-destino)
            String claveRuta = generarClaveRuta(
                vuelo.getAeropuertoOrigen(), 
                vuelo.getAeropuertoDestino()
            );
            vuelosPorRuta.computeIfAbsent(claveRuta, k -> new ArrayList<>()).add(vuelo);
            
            // Índice por aeropuerto de origen
            vuelosSalientes.computeIfAbsent(vuelo.getAeropuertoOrigen(), k -> new ArrayList<>()).add(vuelo);
            
            // Índice por aeropuerto de destino
            vuelosEntrantes.computeIfAbsent(vuelo.getAeropuertoDestino(), k -> new ArrayList<>()).add(vuelo);
        }
    }
    
    /**
     * Obtiene vuelos directos entre dos aeropuertos.
     * Complejidad: O(1) lookup + O(M) donde M = vuelos en esa ruta (típicamente < 10)
     * 
     * @param origen Aeropuerto de origen
     * @param destino Aeropuerto de destino
     * @return Lista de vuelos directos, o lista vacía si no existen
     */
    public List<Vuelo> obtenerVuelosDirectos(Aeropuerto origen, Aeropuerto destino) {
        if (origen == null || destino == null) {
            return Collections.emptyList();
        }
        
        String clave = generarClaveRuta(origen, destino);
        return vuelosPorRuta.getOrDefault(clave, Collections.emptyList());
    }
    
    /**
     * Obtiene todos los vuelos que salen de un aeropuerto.
     * Complejidad: O(1)
     * 
     * @param aeropuerto Aeropuerto de origen
     * @return Lista de vuelos salientes, o lista vacía si no existen
     */
    public List<Vuelo> obtenerVuelosSalientes(Aeropuerto aeropuerto) {
        if (aeropuerto == null) {
            return Collections.emptyList();
        }
        return vuelosSalientes.getOrDefault(aeropuerto, Collections.emptyList());
    }
    
    /**
     * Obtiene todos los vuelos que llegan a un aeropuerto.
     * Complejidad: O(1)
     * 
     * @param aeropuerto Aeropuerto de destino
     * @return Lista de vuelos entrantes, o lista vacía si no existen
     */
    public List<Vuelo> obtenerVuelosEntrantes(Aeropuerto aeropuerto) {
        if (aeropuerto == null) {
            return Collections.emptyList();
        }
        return vuelosEntrantes.getOrDefault(aeropuerto, Collections.emptyList());
    }
    
    /**
     * Genera la clave única para indexar una ruta.
     * Formato: "ORIGEN-DESTINO"
     * 
     * @param origen Aeropuerto de origen
     * @param destino Aeropuerto de destino
     * @return Clave de ruta, o null si algún parámetro es inválido
     */
    private String generarClaveRuta(Aeropuerto origen, Aeropuerto destino) {
        if (origen == null || destino == null || 
            origen.getCodigoIATA() == null || destino.getCodigoIATA() == null) {
            return null;
        }
        return origen.getCodigoIATA() + "-" + destino.getCodigoIATA();
    }
    
    // ========== Métodos de estadísticas y debugging ==========
    
    /**
     * Obtiene el número total de rutas únicas indexadas.
     * 
     * @return Número de rutas directas únicas en el sistema
     */
    public int getTotalRutasUnicas() {
        return vuelosPorRuta.size();
    }
    
    /**
     * Obtiene el número total de aeropuertos con vuelos salientes.
     * 
     * @return Número de aeropuertos con al menos un vuelo saliente
     */
    public int getAeropuertosConVuelosSalientes() {
        return vuelosSalientes.size();
    }
    
    /**
     * Obtiene el número total de aeropuertos con vuelos entrantes.
     * 
     * @return Número de aeropuertos con al menos un vuelo entrante
     */
    public int getAeropuertosConVuelosEntrantes() {
        return vuelosEntrantes.size();
    }
    
    /**
     * Calcula el número promedio de vuelos por ruta.
     * 
     * @return Promedio de vuelos por ruta única
     */
    public double getPromedioVuelosPorRuta() {
        if (vuelosPorRuta.isEmpty()) return 0.0;
        
        int totalVuelos = vuelosPorRuta.values().stream()
            .mapToInt(List::size)
            .sum();
        
        return (double) totalVuelos / vuelosPorRuta.size();
    }
    
    /**
     * Imprime estadísticas del índice para debugging.
     */
    public void imprimirEstadisticas() {
        System.out.println("=== Estadísticas de Índice de Vuelos ===");
        System.out.println("Rutas únicas: " + getTotalRutasUnicas());
        System.out.println("Aeropuertos con vuelos salientes: " + getAeropuertosConVuelosSalientes());
        System.out.println("Aeropuertos con vuelos entrantes: " + getAeropuertosConVuelosEntrantes());
        System.out.println("Promedio de vuelos por ruta: " + String.format("%.2f", getPromedioVuelosPorRuta()));
        System.out.println("=========================================");
    }
}
