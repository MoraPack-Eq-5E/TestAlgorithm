package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import com.grupo5e.morapack.core.model.Vuelo;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Selector inteligente de sedes MoraPack.
 * Decide automáticamente desde qué sede (Lima, Bruselas o Baku) enviar cada pedido
 * considerando plazos de entrega, disponibilidad de vuelos y optimización logística.
 */
public class SedeSelector {
    
    private final ContextoProblema contexto;
    
    // Cache de evaluaciones para evitar recálculos
    private final Map<String, String> cacheSedeOptima = new HashMap<>();
    
    public SedeSelector(ContextoProblema contexto) {
        this.contexto = contexto;
    }
    
    /**
     * Selecciona la mejor sede para enviar un pedido hacia un destino específico.
     * 
     * @param aeropuertoDestino Código ICAO del aeropuerto destino (ej: EDDI, SKBO)
     * @param fechaEnvio Cuándo se requiere el envío
     * @param cantidadPaquetes Número de paquetes a enviar
     * @return Código de la sede óptima (SPIM, EBCI, o UBBB)
     */
    public String seleccionarMejorSede(String aeropuertoDestino, LocalDateTime fechaEnvio, int cantidadPaquetes) {
        
        // Usar cache si ya se calculó
        String cacheKey = aeropuertoDestino + "_" + cantidadPaquetes;
        if (cacheSedeOptima.containsKey(cacheKey)) {
            return cacheSedeOptima.get(cacheKey);
        }
        
        List<EvaluacionSede> evaluaciones = new ArrayList<>();
        
        // Evaluar cada sede de MoraPack
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            EvaluacionSede evaluacion = evaluarSede(sede, aeropuertoDestino, fechaEnvio, cantidadPaquetes);
            evaluaciones.add(evaluacion);
        }
        
        // Ordenar por puntuación (mayor es mejor)
        evaluaciones.sort((a, b) -> Double.compare(b.puntuacion, a.puntuacion));
        
        String sedeOptima = evaluaciones.get(0).sede;
        
        // Guardar en cache
        cacheSedeOptima.put(cacheKey, sedeOptima);
        
        if (com.grupo5e.morapack.algorithm.alns.ALNSConfig.getInstance().isEnableVerboseLogging()) {
            System.out.printf("📍 Sede óptima para %s: %s (%.2f pts)%n", 
                            aeropuertoDestino, sedeOptima, evaluaciones.get(0).puntuacion);
        }
        
        return sedeOptima;
    }
    
    /**
     * Evalúa qué tan conveniente es usar una sede específica para un destino
     */
    private EvaluacionSede evaluarSede(String sede, String destino, LocalDateTime fechaEnvio, int cantidadPaquetes) {
        
        EvaluacionSede evaluacion = new EvaluacionSede(sede);
        
        // 1. FACTOR GEOGRÁFICO - Mismo continente es mejor (40% peso)
        double factorGeografico = evaluarFactorGeografico(sede, destino);
        evaluacion.puntuacion += factorGeografico * 0.4;
        
        // 2. DISPONIBILIDAD DE VUELOS - Más opciones = mejor (30% peso)
        double factorVuelos = evaluarDisponibilidadVuelos(sede, destino, cantidadPaquetes);
        evaluacion.puntuacion += factorVuelos * 0.3;
        
        // 3. CUMPLIMIENTO DE PLAZOS - Que llegue a tiempo (20% peso)
        double factorPlazo = evaluarCumplimientoPlazo(sede, destino);
        evaluacion.puntuacion += factorPlazo * 0.2;
        
        // 4. EFICIENCIA LOGÍSTICA - Menos transbordos = mejor (10% peso)
        double factorEficiencia = evaluarEficienciaLogistica(sede, destino);
        evaluacion.puntuacion += factorEficiencia * 0.1;
        
        evaluacion.detalles = String.format(
            "Geo:%.1f Vuelos:%.1f Plazo:%.1f Efic:%.1f", 
            factorGeografico, factorVuelos, factorPlazo, factorEficiencia
        );
        
        return evaluacion;
    }
    
    /**
     * Evalúa si la sede y destino están en el mismo continente
     */
    private double evaluarFactorGeografico(String sede, String destino) {
        String continenteSede = contexto.obtenerContinente(sede);
        String continenteDestino = contexto.obtenerContinente(destino);
        
        if (continenteSede != null && continenteSede.equals(continenteDestino)) {
            return 100.0; // Mismo continente = excelente
        } else {
            // Diferentes continentes - evaluar proximidad geográfica
            return evaluarProximidadContinental(continenteSede, continenteDestino);
        }
    }
    
    /**
     * Evalúa proximidad entre continentes cuando no son iguales
     */
    private double evaluarProximidadContinental(String continenteSede, String continenteDestino) {
        if (continenteSede == null || continenteDestino == null) {
            return 50.0; // Neutral si no se puede determinar
        }
        
        // Matriz de proximidad entre continentes
        Map<String, Map<String, Double>> proximidad = Map.of(
            "AM", Map.of("EU", 60.0, "AS", 40.0),  // América -> Europa (mejor que Asia)
            "EU", Map.of("AM", 60.0, "AS", 70.0),  // Europa -> Asia (mejor que América)
            "AS", Map.of("AM", 40.0, "EU", 70.0)   // Asia -> Europa (mejor que América)
        );
        
        return proximidad.getOrDefault(continenteSede, Map.of())
                        .getOrDefault(continenteDestino, 50.0);
    }
    
    /**
     * Evalúa disponibilidad y capacidad de vuelos desde la sede al destino
     */
    private double evaluarDisponibilidadVuelos(String sede, String destino, int cantidadPaquetes) {
        
        // Buscar vuelos directos
        List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(sede, destino);
        int capacidadDirecta = vuelosDirectos.stream()
                                           .mapToInt(Vuelo::getCapacidadMaxima)
                                           .sum();
        
        if (capacidadDirecta >= cantidadPaquetes) {
            return 100.0; // Vuelo directo con capacidad suficiente = excelente
        }
        
        // Evaluar opciones con conexiones
        List<String> rutaConConexion = contexto.encontrarRutaMasCorta(sede, destino);
        if (rutaConConexion != null && rutaConConexion.size() <= 3) { // Máximo 1 conexión
            return 70.0; // Una conexión = bueno
        } else if (rutaConConexion != null && rutaConConexion.size() <= 4) { // Máximo 2 conexiones
            return 50.0; // Dos conexiones = regular
        }
        
        return 20.0; // Sin opciones viables = malo
    }
    
    /**
     * Evalúa si se puede cumplir con los plazos de entrega
     */
    private double evaluarCumplimientoPlazo(String sede, String destino) {
        String continenteSede = contexto.obtenerContinente(sede);
        String continenteDestino = contexto.obtenerContinente(destino);
        
        boolean mismoContinente = continenteSede != null && continenteSede.equals(continenteDestino);
        
        // Tiempo estimado según las reglas de MoraPack
        double tiempoVueloHoras = ConstantesMoraPack.obtenerTiempoVueloHoras(mismoContinente);
        int plazoDias = ConstantesMoraPack.obtenerPlazoDias(mismoContinente);
        double plazoHoras = plazoDias * 24.0;
        
        // Verificar si hay ruta viable
        List<String> ruta = contexto.encontrarRutaMasCorta(sede, destino);
        if (ruta == null) {
            return 0.0; // No hay ruta = imposible cumplir plazo
        }
        
        // Estimar tiempo total considerando conexiones
        double tiempoEstimado = tiempoVueloHoras;
        if (ruta.size() > 2) { // Hay conexiones
            tiempoEstimado *= (ruta.size() - 1); // Cada segmento toma tiempo de vuelo
            tiempoEstimado += (ruta.size() - 2) * 2; // 2 horas por conexión
        }
        
        if (tiempoEstimado <= plazoHoras * 0.7) {
            return 100.0; // Mucho margen = excelente
        } else if (tiempoEstimado <= plazoHoras) {
            return 80.0;  // Cumple plazo = bueno
        } else {
            return 30.0;  // No cumple plazo = malo
        }
    }
    
    /**
     * Evalúa eficiencia logística (menos transbordos = mejor)
     */
    private double evaluarEficienciaLogistica(String sede, String destino) {
        List<Vuelo> vuelosDirectos = contexto.getVuelosDirectos(sede, destino);
        if (!vuelosDirectos.isEmpty()) {
            return 100.0; // Vuelo directo = máxima eficiencia
        }
        
        List<String> ruta = contexto.encontrarRutaMasCorta(sede, destino);
        if (ruta == null) {
            return 0.0;
        }
        
        int numConexiones = ruta.size() - 2;
        switch (numConexiones) {
            case 0: return 100.0; // Directo
            case 1: return 70.0;  // Una conexión
            case 2: return 50.0;  // Dos conexiones
            default: return 30.0; // Muchas conexiones
        }
    }
    
    /**
     * Obtiene estadísticas de uso de sedes para monitoreo
     */
    public Map<String, Integer> obtenerEstadisticasUso() {
        Map<String, Integer> estadisticas = new HashMap<>();
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            estadisticas.put(sede, 0);
        }
        
        for (String sedeSeleccionada : cacheSedeOptima.values()) {
            estadisticas.put(sedeSeleccionada, 
                           estadisticas.getOrDefault(sedeSeleccionada, 0) + 1);
        }
        
        return estadisticas;
    }
    
    /**
     * Limpia el cache de evaluaciones (útil para tests o cambios de contexto)
     */
    public void limpiarCache() {
        cacheSedeOptima.clear();
    }
    
    /**
     * Clase interna para evaluación de sedes
     */
    private static class EvaluacionSede {
        final String sede;
        double puntuacion;
        String detalles;
        
        EvaluacionSede(String sede) {
            this.sede = sede;
            this.puntuacion = 0.0;
            this.detalles = "";
        }
        
        @Override
        public String toString() {
            return String.format("%s: %.2f pts (%s)", sede, puntuacion, detalles);
        }
    }
}
