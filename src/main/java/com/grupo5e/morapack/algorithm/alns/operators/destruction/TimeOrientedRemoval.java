package com.grupo5e.morapack.algorithm.alns.operators.destruction;

import com.grupo5e.morapack.algorithm.alns.operators.AbstractOperator;
import com.grupo5e.morapack.algorithm.alns.operators.OperadorDestruccion;
import com.grupo5e.morapack.algorithm.alns.ContextoProblema;
import com.grupo5e.morapack.core.model.*;

import java.util.*;

/**
 * Operador de destrucción Time-Oriented basado en el ejemplo VRPTWFL
 * Remueve paquetes basándose en ventanas de tiempo y urgencia de entrega
 */
public class TimeOrientedRemoval extends AbstractOperator implements OperadorDestruccion {
    
    private final boolean isRandom;
    private final Random random;
    
    public TimeOrientedRemoval(boolean isRandom, double weightStartTime) {
        super("TimeOrientedRemoval" + (isRandom ? "Random" : "Deterministic"), "destruction");
        this.isRandom = isRandom;
        this.random = new Random();
        // weightStartTime se mantiene en el constructor por compatibilidad
    }
    
    @Override
    public List<String> destruir(Solucion solucion, int cantidadRemover) {
        // Limitar la cantidad de paquetes a remover para evitar ser demasiado agresivo
        int paquetesDisponibles = solucion.getRutasPaquetes().size();
        int cantidadLimitada = Math.min(cantidadRemover, Math.max(1, Math.min(3, paquetesDisponibles / 5)));
        
        // Usar la implementación con contexto
        return destruirConContexto(solucion, cantidadLimitada, null);
    }
    
    /**
     * Versión mejorada que puede usar el contexto para criterios de tiempo reales
     */
    public List<String> destruirConContexto(Solucion solucion, int cantidadRemover, ContextoProblema contexto) {
        List<String> paquetesRemovidos = new ArrayList<>();
        
        // Obtener paquetes disponibles en la solución
        List<String> paquetesDisponibles = new ArrayList<>(solucion.getRutasPaquetes().keySet());
        
        if (paquetesDisponibles.isEmpty()) {
            return paquetesRemovidos;
        }
        
        // Calcular urgencia para cada paquete
        List<PaqueteUrgencia> paquetesConUrgencia = new ArrayList<>();
        for (String paqueteId : paquetesDisponibles) {
            double urgencia = calcularUrgencia(paqueteId, solucion, contexto);
            paquetesConUrgencia.add(new PaqueteUrgencia(paqueteId, urgencia));
        }
        
        // Ordenar por urgencia (mayor urgencia = más probable de ser removido)
        paquetesConUrgencia.sort(Comparator.comparingDouble(pu -> -pu.urgencia));
        
        // Seleccionar paquetes para remover
        int cantidadARemover = Math.min(cantidadRemover, paquetesConUrgencia.size());
        
        for (int i = 0; i < cantidadARemover; i++) {
            PaqueteUrgencia paqueteUrgencia;
            
            if (isRandom) {
                // Versión randomizada: sesgar hacia los más urgentes
                double rand = random.nextDouble();
                int idx = (int) Math.floor(Math.pow(rand, 2.0) * paquetesConUrgencia.size());
                paqueteUrgencia = paquetesConUrgencia.get(idx);
            } else {
                // Versión determinística: seleccionar el más urgente
                paqueteUrgencia = paquetesConUrgencia.get(0);
            }
            
            paquetesRemovidos.add(paqueteUrgencia.paqueteId);
            paquetesConUrgencia.remove(paqueteUrgencia);
            
            // Remover de la solución
            solucion.removerRuta(paqueteUrgencia.paqueteId);
        }
        
        return paquetesRemovidos;
    }
    
    
    /**
     * Calcula la urgencia de un paquete basándose en criterios temporales
     */
    private double calcularUrgencia(String paqueteId, Solucion solucion, ContextoProblema contexto) {
        try {
            if (contexto == null) {
                // Fallback: usar implementación simplificada
                return calcularUrgenciaSimplificada(paqueteId);
            }
            
            // Obtener el paquete del contexto
            Paquete paquete = contexto.getPaquete(paqueteId);
            if (paquete == null) {
                return calcularUrgenciaSimplificada(paqueteId);
            }
            
            // Calcular urgencia basada en múltiples factores temporales
            double urgencia = 0.0;
            
            // 1. Factor de tiempo restante hasta fecha límite
            double factorTiempoRestante = calcularFactorTiempoRestante(paquete);
            urgencia += factorTiempoRestante * 0.4; // 40% del peso
            
            // 2. Factor de prioridad
            double factorPrioridad = calcularFactorPrioridad(paquete);
            urgencia += factorPrioridad * 0.3; // 30% del peso
            
            // 3. Factor de tiempo en solución (si está en una ruta)
            double factorTiempoEnSolucion = calcularFactorTiempoEnSolucion(paqueteId, solucion, contexto);
            urgencia += factorTiempoEnSolucion * 0.3; // 30% del peso
            
            return urgencia;
            
        } catch (Exception e) {
            return calcularUrgenciaSimplificada(paqueteId);
        }
    }
    
    /**
     * Calcula el factor de tiempo restante hasta la fecha límite
     */
    private double calcularFactorTiempoRestante(Paquete paquete) {
        if (paquete.getFechaLimiteEntrega() == null) {
            return 0.5; // Valor neutro si no hay fecha límite
        }
        
        long horasRestantes = java.time.Duration.between(
            java.time.LocalDateTime.now(), 
            paquete.getFechaLimiteEntrega()
        ).toHours();
        
        // Normalizar: menos horas = mayor urgencia
        if (horasRestantes <= 0) {
            return 1.0; // Muy urgente (ya pasó la fecha límite)
        } else if (horasRestantes <= 24) {
            return 0.9; // Muy urgente (menos de 1 día)
        } else if (horasRestantes <= 48) {
            return 0.7; // Urgente (menos de 2 días)
        } else if (horasRestantes <= 72) {
            return 0.5; // Moderado (menos de 3 días)
        } else {
            return 0.3; // Poco urgente (más de 3 días)
        }
    }
    
    /**
     * Calcula el factor de prioridad del paquete
     */
    private double calcularFactorPrioridad(Paquete paquete) {
        switch (paquete.getPrioridad()) {
            case 1: return 1.0; // Alta prioridad
            case 2: return 0.6; // Media prioridad
            case 3: return 0.3; // Baja prioridad
            default: return 0.6; // Default media
        }
    }
    
    /**
     * Calcula el factor de tiempo en la solución actual
     */
    private double calcularFactorTiempoEnSolucion(String paqueteId, Solucion solucion, ContextoProblema contexto) {
        // Obtener la ruta del paquete
        Ruta ruta = solucion.getRutasPaquetes().get(paqueteId);
        if (ruta == null) {
            return 0.5; // Valor neutro si no está en una ruta
        }
        
        // Calcular tiempo total de la ruta
        double tiempoTotalRuta = ruta.getTiempoTotalHoras();
        
        // Normalizar: rutas más largas = mayor urgencia de reoptimización
        if (tiempoTotalRuta <= 12) {
            return 0.3; // Ruta corta
        } else if (tiempoTotalRuta <= 24) {
            return 0.5; // Ruta media
        } else if (tiempoTotalRuta <= 48) {
            return 0.7; // Ruta larga
        } else {
            return 0.9; // Ruta muy larga
        }
    }
    
    /**
     * Implementación simplificada como fallback
     */
    private double calcularUrgenciaSimplificada(String paqueteId) {
        // Usar hash del ID para simular urgencia
        int hash = paqueteId.hashCode();
        return Math.abs(hash) % 1000 / 1000.0; // Normalizar entre 0 y 1
    }
    
    /**
     * Clase auxiliar para representar urgencia de paquetes
     */
    private static class PaqueteUrgencia {
        String paqueteId;
        double urgencia;
        
        PaqueteUrgencia(String paqueteId, double urgencia) {
            this.paqueteId = paqueteId;
            this.urgencia = urgencia;
        }
    }
    
    @Override
    public String getNombre() {
        return getName();
    }

    @Override
    public String getDescripcion() {
        return "Time-Oriented Removal: remueve paquetes basados en urgencia temporal, prioridad y tiempo en solución.";
    }
}
