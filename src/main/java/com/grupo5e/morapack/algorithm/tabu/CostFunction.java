package com.grupo5e.morapack.algorithm.tabu;

/** Evalúa el costo total (incluye penalizaciones por restricciones). */
@FunctionalInterface
public interface CostFunction<S> {
    double evaluate(S solution);
}
