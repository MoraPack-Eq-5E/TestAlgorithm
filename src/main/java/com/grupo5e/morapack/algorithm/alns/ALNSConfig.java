package com.grupo5e.morapack.algorithm.alns;

import java.util.*;

/**
 * Configuración centralizada para ALNS
 * Maneja todos los parámetros adaptativos y configuración de operadores
 */
public class ALNSConfig {
    private static ALNSConfig instance;
    
    // Parámetros de Simulated Annealing - OPTIMIZADOS
    private double startTempControlParam = 0.2;  // Aumentado para mayor exploración inicial
    private double bigOmega = 0.15;              // Aumentado para mayor flexibilidad
    private double minTempPercent = 0.005;       // Reducido para permitir más exploración
    private double coolingRate = 0.995;          // Enfriamiento más rápido para mejor convergencia
    private double epsilon = 1e-6;
    
    // Parámetros adaptativos de operadores - OPTIMIZADOS
    private double reactionFactor = 0.15;        // Mayor adaptabilidad
    private int updateInterval = 30;             // Actualización más frecuente
    private double minOpProb = 0.02;             // Probabilidad mínima más alta
    private double bigMRegret = 10000.0;
    
    // Valores sigma para scoring de operadores
    private double sigma1 = 33.0;  // Mejor solución global
    private double sigma2 = 9.0;   // Mejor solución actual
    private double sigma3 = 13.0;  // Solución aceptada por SA
    
    // Configuración de operadores habilitados
    private boolean useGreedyInsert = true;
    private boolean useNRegret2 = true;
    private boolean useNRegret3 = true;
    private boolean useNRegret4 = true;
    private boolean useNRegret5 = false;
    private boolean useNRegret6 = false;
    
    private boolean useRandomRemoval = true;
    private boolean useRandomRouteRemoval = true;
    private boolean useWorstRemovalDeterministic = true;
    private boolean useWorstRemovalRandom = true;
    private boolean useShawSimplifiedRemovalDeterministic = true;
    private boolean useShawSimplifiedRemovalRandom = true;
    private boolean useTimeOrientedRemovalDeterministic = true;
    private boolean useTimeOrientedRemovalRandom = true;
    private boolean useClusterRemovalKruskal = true;
    
    // Parámetros específicos de operadores
    private double timeOrientedJungwirthWeightStartTimeIinSolution = 0.5;
    private List<Integer> kMeansClusterSettings = Arrays.asList(2, 3, 5);
    
    // Configuración de logging y debugging
    private boolean enableVerboseLogging = true;
    private boolean enableOperatorStatistics = true;
    
    // Parámetros de penalización (para futuras extensiones)
    private boolean enableGLS = false;
    private boolean enableSchiffer = false;
    private int glsIterUntilPenaltyUpdate = 100;
    private int penaltyWeightUpdateIteration = 200;
    
    private ALNSConfig() {
        // Constructor privado para Singleton
    }
    
    public static ALNSConfig getInstance() {
        if (instance == null) {
            instance = new ALNSConfig();
        }
        return instance;
    }
    
    // Getters para parámetros de Simulated Annealing
    public double getStartTempControlParam() { return startTempControlParam; }
    public double getBigOmega() { return bigOmega; }
    public double getMinTempPercent() { return minTempPercent; }
    public double getCoolingRate() { return coolingRate; }
    public double getEpsilon() { return epsilon; }
    
    // Getters para parámetros adaptativos
    public double getReactionFactor() { return reactionFactor; }
    public int getUpdateInterval() { return updateInterval; }
    public double getMinOpProb() { return minOpProb; }
    public double getBigMRegret() { return bigMRegret; }
    
    // Getters para valores sigma
    public double getSigma1() { return sigma1; }
    public double getSigma2() { return sigma2; }
    public double getSigma3() { return sigma3; }
    
    // Getters para configuración de operadores
    public boolean isUseGreedyInsert() { return useGreedyInsert; }
    public boolean isUseNRegret2() { return useNRegret2; }
    public boolean isUseNRegret3() { return useNRegret3; }
    public boolean isUseNRegret4() { return useNRegret4; }
    public boolean isUseNRegret5() { return useNRegret5; }
    public boolean isUseNRegret6() { return useNRegret6; }
    
    public boolean isUseRandomRemoval() { return useRandomRemoval; }
    public boolean isUseRandomRouteRemoval() { return useRandomRouteRemoval; }
    public boolean isUseWorstRemovalDeterministic() { return useWorstRemovalDeterministic; }
    public boolean isUseWorstRemovalRandom() { return useWorstRemovalRandom; }
    public boolean isUseShawSimplifiedRemovalDeterministic() { return useShawSimplifiedRemovalDeterministic; }
    public boolean isUseShawSimplifiedRemovalRandom() { return useShawSimplifiedRemovalRandom; }
    public boolean isUseTimeOrientedRemovalDeterministic() { return useTimeOrientedRemovalDeterministic; }
    public boolean isUseTimeOrientedRemovalRandom() { return useTimeOrientedRemovalRandom; }
    public boolean isUseClusterRemovalKruskal() { return useClusterRemovalKruskal; }
    
    public double getTimeOrientedJungwirthWeightStartTimeIinSolution() { 
        return timeOrientedJungwirthWeightStartTimeIinSolution; 
    }
    public List<Integer> getKMeansClusterSettings() { return kMeansClusterSettings; }
    
    // Getters para configuración de logging
    public boolean isEnableVerboseLogging() { return enableVerboseLogging; }
    public boolean isEnableOperatorStatistics() { return enableOperatorStatistics; }
    
    // Getters para penalización
    public boolean isEnableGLS() { return enableGLS; }
    public boolean isEnableSchiffer() { return enableSchiffer; }
    public int getGlsIterUntilPenaltyUpdate() { return glsIterUntilPenaltyUpdate; }
    public int getPenaltyWeightUpdateIteration() { return penaltyWeightUpdateIteration; }
    
    // Métodos para configurar parámetros (útil para testing)
    public void setReactionFactor(double reactionFactor) { 
        this.reactionFactor = reactionFactor; 
    }
    
    public void setUpdateInterval(int updateInterval) { 
        this.updateInterval = updateInterval; 
    }
    
    public void setCoolingRate(double coolingRate) { 
        this.coolingRate = coolingRate; 
    }
    
    public void setEnableVerboseLogging(boolean enableVerboseLogging) { 
        this.enableVerboseLogging = enableVerboseLogging; 
    }
    
    /**
     * Calcula la temperatura inicial basada en el costo de la solución inicial
     * MEJORADO: Considera la variabilidad del problema
     */
    public double calculateInitialTemperature(double costInitialSolution) {
        // Temperatura base
        double tempBase = -(startTempControlParam / Math.log(bigOmega)) * costInitialSolution;
        
        // Ajuste dinámico basado en el tamaño del problema
        double factorAjuste = Math.log(Math.max(1, costInitialSolution / 100.0));
        
        return tempBase * factorAjuste;
    }
    
    /**
     * Calcula la temperatura inicial adaptativa basada en estadísticas del problema
     */
    public double calculateAdaptiveInitialTemperature(double costInitialSolution, int numPaquetes, int numVuelos) {
        // Factor de complejidad del problema
        double factorComplejidad = Math.log(numPaquetes) * Math.log(numVuelos) / 10.0;
        
        // Temperatura base ajustada por complejidad
        double tempBase = -(startTempControlParam / Math.log(bigOmega)) * costInitialSolution;
        
        return tempBase * (1.0 + factorComplejidad);
    }
    
    /**
     * Calcula la temperatura final basada en la temperatura inicial
     */
    public double calculateFinalTemperature(double initialTemperature) {
        return minTempPercent * initialTemperature;
    }
}
