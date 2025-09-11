package com.grupo5e.morapack.algorithm.tabu;

import java.util.List;

public interface Neighborhood<S> {
    /** Genera movimientos candidatos desde el estado actual. Puede muestrear. */
    List<Move<S>> generate(S solution);
}
