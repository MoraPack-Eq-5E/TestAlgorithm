package com.grupo5e.morapack.algorithm.tabu;

/** Valida la factibilidad (o devuélvela siempre true si TODO ya está penalizado). */
@FunctionalInterface
public interface Feasibility<S> {
    boolean isFeasible(S solution);
}
