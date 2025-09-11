package com.grupo5e.morapack.algorithm.tabu;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Motor genérico de Tabu Search:
 * - guarda el mejor global,
 * - aplica lista tabú para evitar ciclos,
 * - permite aspiración si mejora mejor-global,
 * - se detiene por iteraciones o por falta de mejoras.
 */
public class TabuSearchSolver<S> {
    /** Parámetros de control (ajustarlos según la instancia). */
    public static class Params {
        public int maxIterations = 2000;      // tope de iteraciones
        public int maxNoImprove = 300;        // tope de iteraciones sin mejorar el mejor-global
        public int tabuTenure = 15;           // "vida" de la prohibición tabú
        public int neighborhoodSampleSize = 64; // si tu vecindario potencial es enorme, muestrea
        public boolean acceptInfeasible = false; // true si exploras infeasible con penalizaciones
    }

    private final Neighborhood<S> neighborhood;
    private final CostFunction<S> costFunction;
    private final Feasibility<S> feasibility;
    private final Aspiration<S> aspiration;
    private final Function<S, S> deepCopy; // clon seguro de la solución
    private final Params p;

    public TabuSearchSolver(Neighborhood<S> neighborhood,
                            CostFunction<S> costFunction,
                            Feasibility<S> feasibility,
                            Aspiration<S> aspiration,
                            Function<S, S> deepCopy,
                            Params params) {
        this.neighborhood = Objects.requireNonNull(neighborhood);
        this.costFunction = Objects.requireNonNull(costFunction);
        this.feasibility = Objects.requireNonNull(feasibility);
        this.aspiration = Objects.requireNonNull(aspiration);
        this.deepCopy = Objects.requireNonNull(deepCopy);
        this.p = params == null ? new Params() : params;
    }

    /** Ejecuta la búsqueda desde una solución inicial. */
    public Result<S> run(S initial) {
        S current = deepCopy.apply(initial);
        double currentCost = costFunction.evaluate(current);

        S best = deepCopy.apply(current);
        double bestCost = currentCost;

        TabuList tabu = new TabuList(p.tabuTenure);
        int noImprove = 0;

        for (int iter = 0; iter < p.maxIterations && noImprove < p.maxNoImprove; iter++) {
            // 1) construye candidatos (vecinos) desde el estado actual
            List<Move<S>> candidates = neighborhood.generate(current);
            if (candidates == null || candidates.isEmpty()) break;

            Move<S> bestMove = null;
            double bestMoveCost = Double.POSITIVE_INFINITY;

            // 2) explora candidatos, respeta tabú salvo aspiración
            for (Move<S> mv : candidates) {
                boolean isTabu = tabu.isTabu(mv.attributeKey());

                mv.apply(current);
                double newCost = costFunction.evaluate(current);
                boolean feasibleOk = p.acceptInfeasible || feasibility.isFeasible(current);
                boolean aspir = aspiration.allow(current, current, newCost, bestCost);

                if (feasibleOk && (!isTabu || aspir) && newCost < bestMoveCost) {
                    bestMove = mv;
                    bestMoveCost = newCost;
                }
                mv.undo(current);
            }

            // 3) si no hay nada útil, paramos
            if (bestMove == null) break;

            // 4) aplica definitivamente el mejor candidato y avanza el tiempo tabú
            bestMove.apply(current);
            currentCost = costFunction.evaluate(current);
            tabu.add(bestMove.attributeKey());
            tabu.tick();

            // 5) actualiza mejor global y criterio de parada por no-mejora
            if (currentCost + 1e-9 < bestCost) {
                bestCost = currentCost;
                best = deepCopy.apply(current);
                noImprove = 0;
            } else {
                noImprove++;
            }
        }
        return new Result<>(best, bestCost);
    }

    /** Resultado simple: mejor solución hallada y su costo. */
    public static class Result<S> {
        public final S bestSolution;
        public final double bestCost;
        public Result(S s, double c) { this.bestSolution = s; this.bestCost = c; }
    }
}
