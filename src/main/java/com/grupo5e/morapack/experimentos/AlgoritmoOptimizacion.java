package com.grupo5e.morapack.experimentos;

/**
 * Interfaz común para todos los algoritmos de optimización
 */
public interface AlgoritmoOptimizacion {
    ResultadoAlgoritmo resolver();
    String getNombreAlgoritmo();
}