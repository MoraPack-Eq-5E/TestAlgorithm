package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.constants.ConstantesMoraPack;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Simple sede selector for MoraPack.
 * Uses basic randomness without complex logic.
 */
public class SedeSelector {
    
    // Simple randomization
    private static final Random random = new Random();
    
    public SedeSelector(ContextoProblema contexto) {
        // Context not used in simple approach
    }
    
    /**
     * Selects a sede for sending an order to a specific destination.
     * Uses simple random selection.
     * 
     * @param aeropuertoDestino Destination airport code (e.g., EDDI, SKBO)
     * @param fechaEnvio When the shipment is required
     * @param cantidadPaquetes Number of packages to send
     * @return Selected sede code (SPIM, EBCI, or UBBB)
     */
    public String seleccionarMejorSede(String aeropuertoDestino, LocalDateTime fechaEnvio, int cantidadPaquetes) {
        
        // Simple random selection
        List<String> sedesDisponibles = new ArrayList<String>();
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            sedesDisponibles.add(sede);
        }
        Collections.shuffle(sedesDisponibles, random);
        
        // Return first sede (already randomized)
        String sedeSeleccionada = sedesDisponibles.get(0);
        
        
        return sedeSeleccionada;
    }
    
    /**
     * Gets sede usage statistics (method required by ProcesadorPedidosCSV).
     * Simple approach: returns basic statistics.
     * 
     * @return Map with usage statistics for each sede
     */
    public Map<String, Integer> obtenerEstadisticasUso() {
        // Simple approach: no real statistics since selection is random
        Map<String, Integer> estadisticas = new HashMap<>();
        for (String sede : ConstantesMoraPack.SEDES_MORAPACK) {
            estadisticas.put(sede, 0);
        }
        return estadisticas;
    }
}