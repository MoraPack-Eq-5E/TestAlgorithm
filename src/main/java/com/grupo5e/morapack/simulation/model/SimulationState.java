package com.grupo5e.morapack.simulation.model;

import com.grupo5e.morapack.core.model.SimulacionSemanal;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Estado completo de una simulación activa en memoria.
 * Se almacena en ConcurrentHashMap para acceso rápido durante polling.
 * 
 * Esta clase contiene TODA la información necesaria para calcular
 * el estado actual de la simulación en cualquier momento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationState {
    
    /**
     * ID de la simulación (referencia a SimulacionSemanal en BD)
     */
    private Long simulationId;
    
    /**
     * Timestamp real cuando se inició la visualización (en milisegundos)
     * Se usa para calcular cuánto tiempo real ha transcurrido
     */
    private long realStartTimeMillis;
    
    /**
     * Timestamp cuando se pausó (si está pausada)
     */
    private Long pausedAtMillis;
    
    /**
     * Tiempo simulado transcurrido antes de pausar (acumulado)
     */
    private long accumulatedSimulatedMillis;
    
    /**
     * T0: Tiempo simulado de inicio (fecha/hora base de la simulación)
     * Ejemplo: 2025-01-20 08:00:00
     */
    private LocalDateTime simulatedStartTime;
    
    /**
     * Factor de aceleración del tiempo
     * Ejemplo: 112 significa que 1 segundo real = 112 segundos simulados
     */
    private int timeScale;
    
    /**
     * Duración total de la simulación en días
     */
    private int simulationDurationDays;
    
    /**
     * Estado actual de la simulación
     */
    private SimulationStatus status;
    
    /**
     * Referencia a la entidad persistente (para consultas adicionales)
     */
    private SimulacionSemanal simulacionEntity;
    
    /**
     * Lista de todos los vuelos en esta simulación
     * Se mantiene en memoria para cálculos rápidos
     */
    @Builder.Default
    private List<FlightSnapshot> flights = new ArrayList<>();
    
    /**
     * Lista de almacenes/aeropuertos
     * Se mantiene en memoria para cálculos rápidos
     */
    @Builder.Default
    private List<WarehouseSnapshot> warehouses = new ArrayList<>();
    
    /**
     * Métricas actuales de la simulación
     */
    @Builder.Default
    private SimulationMetrics metrics = new SimulationMetrics();
    
    /**
     * Cola de eventos recientes (últimos 50)
     * Se usa ConcurrentLinkedQueue para thread-safety
     */
    @Builder.Default
    private ConcurrentLinkedQueue<SimulationEvent> recentEvents = new ConcurrentLinkedQueue<>();
    
    /**
     * Máximo de eventos a mantener en memoria
     */
    private static final int MAX_EVENTS = 50;
    
    /**
     * Agrega un evento a la cola, manteniendo el límite
     */
    public void addEvent(SimulationEvent event) {
        recentEvents.offer(event);
        while (recentEvents.size() > MAX_EVENTS) {
            recentEvents.poll();
        }
    }
    
    /**
     * Calcula el tiempo simulado actual basado en el tiempo real transcurrido
     * Considera pausas y factor de aceleración
     */
    public LocalDateTime getCurrentSimulatedTime() {
        long elapsedSimulatedMillis = calculateElapsedSimulatedMillis();
        return simulatedStartTime.plusNanos(elapsedSimulatedMillis * 1_000_000);
    }
    
    /**
     * Calcula cuántos milisegundos simulados han transcurrido
     */
    public long calculateElapsedSimulatedMillis() {
        if (status == SimulationStatus.PAUSED) {
            return accumulatedSimulatedMillis;
        }
        
        long currentRealTimeMillis = System.currentTimeMillis();
        long elapsedRealMillis = currentRealTimeMillis - realStartTimeMillis;
        long newSimulatedMillis = elapsedRealMillis * timeScale;
        
        return accumulatedSimulatedMillis + newSimulatedMillis;
    }
    
    /**
     * Calcula el progreso total de la simulación (0.0 a 1.0)
     */
    public double calculateProgress() {
        long totalSimulatedMillis = simulationDurationDays * 24L * 60L * 60L * 1000L;
        long elapsedSimulatedMillis = calculateElapsedSimulatedMillis();
        
        return Math.min(1.0, (double) elapsedSimulatedMillis / totalSimulatedMillis);
    }
    
    /**
     * Verifica si la simulación ha completado
     */
    public boolean isCompleted() {
        return calculateProgress() >= 1.0;
    }
}

