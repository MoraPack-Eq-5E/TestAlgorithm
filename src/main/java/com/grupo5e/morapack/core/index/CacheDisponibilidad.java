package com.grupo5e.morapack.core.index;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.service.ServicioDisponibilidadVuelos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cache de vuelos disponibles por día.
 * Evita verificar disponibilidad repetidamente para la misma ruta y día.
 * 
 * Patrón: Cache Pattern para optimización de consultas repetidas
 */
public class CacheDisponibilidad {
    
    private final ServicioDisponibilidadVuelos servicioDisponibilidad;
    private final IndiceVuelos indiceVuelos;
    
    // Cache: día → (clave_ruta → vuelos disponibles)
    private final Map<Integer, Map<String, List<Vuelo>>> cachePorDia;
    
    // Límite de días en cache (para evitar consumo excesivo de memoria)
    private static final int MAX_DIAS_EN_CACHE = 30;
    
    // Estadísticas de cache
    private int hits = 0;
    private int misses = 0;
    
    /**
     * Constructor del cache de disponibilidad.
     * 
     * @param servicio Servicio de disponibilidad para verificar cancelaciones
     * @param indice Índice de vuelos para búsqueda eficiente
     */
    public CacheDisponibilidad(ServicioDisponibilidadVuelos servicio, IndiceVuelos indice) {
        this.servicioDisponibilidad = servicio;
        this.indiceVuelos = indice;
        this.cachePorDia = new HashMap<>();
    }
    
    /**
     * Obtiene vuelos disponibles para una ruta en un día específico.
     * Usa cache para evitar recalcular. Si no está en cache, calcula y almacena.
     * 
     * @param origen Aeropuerto de origen
     * @param destino Aeropuerto de destino
     * @param dia Día de operación (1-based)
     * @return Lista INMUTABLE de vuelos disponibles (filtrados por cancelaciones)
     */
    public List<Vuelo> obtenerVuelosDisponibles(Aeropuerto origen, Aeropuerto destino, int dia) {
        // Validar día
        if (dia < 1) {
            return List.of(); // Día inválido
        }
        
        // Generar clave de ruta
        String claveRuta = generarClaveRuta(origen, destino);
        if (claveRuta == null) {
            return List.of(); // Retornar lista vacía si parámetros inválidos
        }
        
        // Verificar si existe en cache
        Map<String, List<Vuelo>> cacheDia = cachePorDia.get(dia);
        if (cacheDia != null) {
            List<Vuelo> cached = cacheDia.get(claveRuta);
            if (cached != null) {
                hits++;
                // CRÍTICO: Retornar copia inmutable para proteger el cache
                return List.copyOf(cached); // Cache hit
            }
        }
        
        // Cache miss: calcular vuelos disponibles
        misses++;
        List<Vuelo> todosVuelos = indiceVuelos.obtenerVuelosDirectos(origen, destino);
        List<Vuelo> disponibles = todosVuelos.stream()
            .filter(v -> servicioDisponibilidad.estaDisponible(v, dia))
            .collect(Collectors.toList());
        
        // Guardar en cache
        cachePorDia.computeIfAbsent(dia, k -> new HashMap<>())
                   .put(claveRuta, disponibles);
        
        // CRÍTICO: Retornar copia inmutable para proteger el cache
        return List.copyOf(disponibles);
    }
    
    /**
     * Limpia el cache para días que ya pasaron.
     * Útil para gestión de memoria en ejecuciones largas.
     * 
     * @param diaActual Día actual de operación
     */
    public void limpiarDiasAnteriores(int diaActual) {
        cachePorDia.keySet().removeIf(dia -> dia < diaActual);
    }
    
    /**
     * Limpia el cache cuando excede el límite de días permitidos.
     * Elimina los días más antiguos para mantener el cache acotado.
     * 
     * @param diaActual Día actual de operación
     */
    public void aplicarLimiteCache(int diaActual) {
        if (cachePorDia.size() > MAX_DIAS_EN_CACHE) {
            // Eliminar días más antiguos que (diaActual - MAX_DIAS_EN_CACHE)
            int diaMinimo = diaActual - MAX_DIAS_EN_CACHE;
            cachePorDia.keySet().removeIf(dia -> dia < diaMinimo);
        }
    }
    
    /**
     * Limpia completamente el cache.
     * Útil para reiniciar el cache entre iteraciones o fases del algoritmo.
     */
    public void limpiarCache() {
        cachePorDia.clear();
        resetearEstadisticas();
    }
    
    /**
     * Genera la clave única para una ruta.
     * 
     * @param origen Aeropuerto de origen
     * @param destino Aeropuerto de destino
     * @return Clave de ruta, o null si parámetros inválidos
     */
    private String generarClaveRuta(Aeropuerto origen, Aeropuerto destino) {
        if (origen == null || destino == null || 
            origen.getCodigoIATA() == null || destino.getCodigoIATA() == null) {
            return null;
        }
        return origen.getCodigoIATA() + "-" + destino.getCodigoIATA();
    }
    
    // ========== Estadísticas y debugging ==========
    
    /**
     * Obtiene la tasa de aciertos del cache (hit rate).
     * 
     * @return Porcentaje de hits (0.0 - 1.0)
     */
    public double getHitRate() {
        int total = hits + misses;
        if (total == 0) return 0.0;
        return (double) hits / total;
    }
    
    /**
     * Obtiene el número total de días cacheados.
     * 
     * @return Número de días en cache
     */
    public int getDiasCacheados() {
        return cachePorDia.size();
    }
    
    /**
     * Obtiene el número total de entradas en cache.
     * 
     * @return Número total de (día, ruta) cacheados
     */
    public int getTotalEntradasCache() {
        return cachePorDia.values().stream()
            .mapToInt(Map::size)
            .sum();
    }
    
    /**
     * Resetea las estadísticas del cache.
     */
    public void resetearEstadisticas() {
        hits = 0;
        misses = 0;
    }
    
    /**
     * Imprime estadísticas del cache para debugging.
     */
    public void imprimirEstadisticas() {
        System.out.println("=== Estadísticas de Cache de Disponibilidad ===");
        System.out.println("Hits: " + hits);
        System.out.println("Misses: " + misses);
        System.out.println("Hit Rate: " + String.format("%.2f%%", getHitRate() * 100));
        System.out.println("Días cacheados: " + getDiasCacheados());
        System.out.println("Total entradas: " + getTotalEntradasCache());
        System.out.println("==============================================");
    }
}
