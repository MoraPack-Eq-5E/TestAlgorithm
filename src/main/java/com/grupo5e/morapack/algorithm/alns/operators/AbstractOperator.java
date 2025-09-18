package com.grupo5e.morapack.algorithm.alns.operators;

/**
 * Clase base abstracta para operadores ALNS con sistema de scoring adaptativo
 * Basada en el patrón del ejemplo VRPTWFL
 */
public abstract class AbstractOperator {
    
    // Sistema de scoring adaptativo
    private double pi = 0.0;           // Puntuación acumulada
    private double weight = 1.0;       // Peso actual del operador
    private double probability = 1.0;  // Probabilidad de selección
    private int draws = 0;             // Número de veces que se ha usado
    
    // Información del operador
    private final String name;
    private final String type; // "destruction" o "construction"
    
    public AbstractOperator(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    /**
     * Agrega puntuación al operador basado en el rendimiento
     * @param score Puntuación a agregar (sigma1, sigma2, sigma3, etc.)
     */
    public void addToPI(double score) {
        this.pi += score;
    }
    
    /**
     * Incrementa el contador de usos del operador
     */
    public void incrementDraws() {
        this.draws++;
    }
    
    /**
     * Actualiza el peso del operador basado en su rendimiento promedio
     * @param reactionFactor Factor de reacción para la actualización
     */
    public void updateWeight(double reactionFactor) {
        if (draws > 0) {
            double averageScore = pi / draws;
            double portionOldWeight = weight * (1 - reactionFactor);
            double updatedWeight = averageScore * reactionFactor;
            this.weight = portionOldWeight + updatedWeight;
        }
    }
    
    /**
     * Actualiza la probabilidad de selección basada en el peso
     * @param totalWeight Suma total de pesos de todos los operadores
     * @param minProb Probabilidad mínima permitida
     */
    public void updateProbability(double totalWeight, double minProb) {
        if (totalWeight > 0) {
            double newProb = weight / totalWeight;
            this.probability = Math.max(newProb, minProb);
        }
    }
    
    /**
     * Resetea las estadísticas del operador
     */
    public void resetStats() {
        this.pi = 0.0;
        this.draws = 0;
    }
    
    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
    public double getPi() { return pi; }
    public double getWeight() { return weight; }
    public double getProbability() { return probability; }
    public int getDraws() { return draws; }
    
    // Setters
    public void setPi(double pi) { this.pi = pi; }
    public void setWeight(double weight) { this.weight = weight; }
    public void setProbability(double probability) { this.probability = probability; }
    public void setDraws(int draws) { this.draws = draws; }
    
    /**
     * Obtiene el rendimiento promedio del operador
     */
    public double getAverageScore() {
        return draws > 0 ? pi / draws : 0.0;
    }
    
    /**
     * Obtiene información del operador para logging
     */
    public String getStatsInfo() {
        return String.format("%s: peso=%.3f, prob=%.3f, usos=%d, score_prom=%.3f", 
                           name, weight, probability, draws, getAverageScore());
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", name, type);
    }
}
