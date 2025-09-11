package com.grupo5e.morapack.algorithm.tabu;

/** Regla de "romper tabú": si se cumple, puedes aceptar un movimiento tabú. */
@FunctionalInterface
public interface Aspiration<S> {
    boolean allow(S current, S candidate, double candidateCost, double globalBestCost);
}