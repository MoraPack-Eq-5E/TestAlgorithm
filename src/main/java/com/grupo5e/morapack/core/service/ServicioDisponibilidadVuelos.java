package com.grupo5e.morapack.core.service;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.utils.LectorCancelaciones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Servicio que gestiona la disponibilidad de vuelos considerando cancelaciones.
 * Responsabilidad única: determinar si un vuelo está disponible en un día específico.
 * 
 * Patrón: Service Layer + Single Responsibility Principle
 */
public class ServicioDisponibilidadVuelos {
    
    // Mapa: identificadorVuelo → Set de días cancelados
    private final Map<String, Set<Integer>> cancelacionesPorVuelo;
    
    public ServicioDisponibilidadVuelos() {
        this.cancelacionesPorVuelo = new HashMap<>();
    }
    
    /**
     * Registra una cancelación de vuelo para un día específico.
     * 
     * @param identificadorVuelo Identificador único del vuelo (formato: "ORIGEN-DESTINO-HH:MM")
     * @param dia Día de la cancelación (1-based)
     */
    public void registrarCancelacion(String identificadorVuelo, int dia) {
        if (identificadorVuelo == null || identificadorVuelo.trim().isEmpty()) {
            return;
        }
        
        cancelacionesPorVuelo
            .computeIfAbsent(identificadorVuelo, k -> new HashSet<>())
            .add(dia);
    }
    
    /**
     * Verifica si un vuelo está disponible en un día específico.
     * 
     * @param vuelo Vuelo a verificar
     * @param dia Día a consultar (1-based)
     * @return true si el vuelo está disponible, false si está cancelado
     */
    public boolean estaDisponible(Vuelo vuelo, int dia) {
        if (vuelo == null) {
            return false;
        }
        
        String identificador = vuelo.getIdentificadorVuelo();
        if (identificador == null) {
            // Si no tiene identificador, asumimos que está disponible
            // (compatibilidad con vuelos antiguos sin horarios)
            return true;
        }
        
        Set<Integer> diasCancelados = cancelacionesPorVuelo.get(identificador);
        if (diasCancelados == null) {
            return true; // No hay cancelaciones registradas para este vuelo
        }
        
        return !diasCancelados.contains(dia);
    }
    
    /**
     * Carga cancelaciones desde un lector.
     * 
     * @param lector Lector de cancelaciones configurado
     */
    public void cargarCancelaciones(LectorCancelaciones lector) {
        Map<String, Set<Integer>> cancelaciones = lector.leerCancelaciones();
        cancelacionesPorVuelo.putAll(cancelaciones);
    }
    
    /**
     * Obtiene el número total de cancelaciones registradas.
     * Útil para estadísticas y debugging.
     * 
     * @return Número total de instancias de cancelación (día × vuelo)
     */
    public int getTotalCancelaciones() {
        return cancelacionesPorVuelo.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    /**
     * Obtiene el número de vuelos únicos con al menos una cancelación.
     * 
     * @return Número de vuelos afectados por cancelaciones
     */
    public int getVuelosAfectados() {
        return cancelacionesPorVuelo.size();
    }
    
    /**
     * Verifica si un vuelo específico tiene cancelaciones registradas.
     * 
     * @param identificadorVuelo Identificador del vuelo
     * @return true si tiene al menos una cancelación
     */
    public boolean tieneCancelaciones(String identificadorVuelo) {
        Set<Integer> cancelaciones = cancelacionesPorVuelo.get(identificadorVuelo);
        return cancelaciones != null && !cancelaciones.isEmpty();
    }
    
    /**
     * Obtiene los días en que un vuelo específico está cancelado.
     * 
     * @param identificadorVuelo Identificador del vuelo
     * @return Set de días cancelados (inmutable), o set vacío si no hay cancelaciones
     */
    public Set<Integer> obtenerDiasCancelados(String identificadorVuelo) {
        Set<Integer> cancelaciones = cancelacionesPorVuelo.get(identificadorVuelo);
        if (cancelaciones == null) {
            return Set.of(); // Set inmutable vacío
        }
        return Set.copyOf(cancelaciones); // Copia inmutable para evitar modificaciones externas
    }
}
