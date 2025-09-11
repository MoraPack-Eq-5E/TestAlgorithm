package com.grupo5e.morapack.algorithm.tabu;

import java.util.List;

@FunctionalInterface
public interface Neighborhood<S> {
    List<Move<S>> generate(S solution);
}
